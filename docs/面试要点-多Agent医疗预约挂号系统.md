# 多Agent智能医疗预约挂号系统 — 面试要点

> 项目周期：2025.12 ～ 2026.06 | 角色：项目负责人
> 开源地址：https://gitee.com/LtAo857/hospital

---

## 一、整体架构速览

```
┌─ patient-wx (UniApp 小程序) ─────────────────────────────┐
│  AI 对话挂号 │ 普通挂号 │ 视频问诊 │ 消息中心 │ 就诊卡  │
└────────────────────────┬─────────────────────────────────┘
                         │ HTTP
┌─ patient-wx-api-mysql (Spring Boot :8095) ───────────────┐
│  Multi-Agent 编排  │  NLU 解析  │  RAG 解释  │  挂号业务  │
│  Redis 会话记忆    │  审计日志  │  Quartz 补偿           │
└────────────────────────┬─────────────────────────────────┘
                         │
┌─ Python NLU (:8001) ───┴─ MySQL ─── Redis ───────────────┐
│  DashScope qwen-plus                                       │
│  /infer 意图识别 + 槽位提取                                │
└───────────────────────────────────────────────────────────┘
```

**技术栈：** Spring Boot 2.7 + MyBatis + Sa-Token + Redis + Quartz + WebSocket + Vue3 + UniApp + Python + DashScope LLM + Multi-Agent + RAG + 工具调用 + 会话记忆

---

## 二、工作要点 1：服务端受控多智能体编排架构

### 2.1 架构设计理念

**核心问题：** LLM 直接调用写操作存在幻觉风险（编造号源、重复挂号、绕过校验），需要将"AI 理解"与"业务执行"彻底分离。

**解决方案：** 设计 **"NLU + Worker 流水线"** 架构 — LLM 只做意图识别（NLU），业务决策和执行全部由确定性的 Worker 代码控制。

```
用户消息
  → TriageAgentWorker    意图识别（LLM只在这里参与，失败不影响主链路）
  → ScheduleAgentWorker  号源查询（内部轻量 ReAct 循环，最多4步）
  → PolicyAgentWorker    规则校验（登录/就诊卡/当日上限/重复挂号）
  → ExecutionAgentWorker 写操作执行（二次确认 + 幂等 + 审计）
```

### 2.2 四个 Worker 的分工（面试核心）

| Worker | 职责 | 关键设计 |
|--------|------|---------|
| **TriageAgentWorker** | 入口分流，识别用户意图 | LLM NLU + 关键词双重识别，高危意图直接拦截 |
| **ScheduleAgentWorker** | 补全科室/医生/日期，查询号源 | 内部轻量 ReAct 循环（rule-based，不依赖 LLM），读工具最多重试 1 次 |
| **PolicyAgentWorker** | 校验登录态、就诊卡、当日上限、重复挂号 | fail-closed 设计，校验失败阻止进入写路径 |
| **ExecutionAgentWorker** | 真正提交挂号 | 二次校验 pendingOrder 一致性，幂等键防重放，不自动重试写操作 |

### 2.3 防幻觉 & 防重复提交机制（85% 改善率的数据支撑）

**三层安全防护：**

1. **确认机制（pendingOrder 服务端快照）**
   - ScheduleAgentWorker 查到号源后，将 `pendingOrder`（科室、医生、日期、时段、金额）写入 Redis 会话
   - 前端只能展示确认卡片，不能修改任何字段
   - 用户确认后，PolicyAgentWorker 和 ExecutionAgentWorker 各自独立校验传入参数与 `pendingOrder` 逐字段一致（含 BigDecimal 金额比对）
   - 伪造确认或篡改关键字段 → 清理待确认态，返回错误

2. **幂等键（requestId）**
   - 使用确定性 UUID v5：`UUID.nameUUIDFromBytes(sessionId + userId + scheduleId + date + slot)`
   - 同一个挂号意图永远生成同一个 requestId
   - 审计表按 requestId 去重：成功直接返回缓存结果，处理中拦截重复提交，参数不一致重放被拒绝

3. **失败不回滚 pendingOrder 的分类处理**
   - 网络超时 / 系统异常：保留 pendingOrder，允许用户重试
   - 业务拒绝（号源已满、当日上限）：清理 pendingOrder，引导重新选择

> **85% 降低率的来源：** 对比一期（直接信任前端确认参数 + 无幂等机制）与当前方案，幻觉导致的错误挂号从约 20 次/百次测试降至约 3 次；重复提交从偶发的 5%+ 降至接近 0。

### 2.4 面试可能的追问

**Q: 为什么不用 LangChain/LangGraph？**
A: 挂号业务 rule 确定性极高（科室→诊室→医生→号源），不需要 LLM 做复杂推理。用 Java Enum 状态机 + Worker 流水线更可控、性能更好、出问题时也更容易排查。

**Q: ScheduleAgentWorker 里的 ReAct 是什么意思？**
A: 一个轻量级的 Thought→Action→Observation 循环，但"思考"由规则决定而非 LLM。4 种决策（MATCH_DEPARTMENT → SEARCH_SUB_DEPARTMENTS → SEARCH_DOCTORS → SEARCH_SLOTS）按状态机推进，有守卫条件防跳步，到达最大步数自动降级。

**Q: 如果 AI 编造了一个不存在的科室怎么办？**
A: ScheduleAgentWorker 查询的是真实 MySQL 科室表，模糊匹配不到会直接返回"未找到该科室，请重新输入"，不会走后续流程。

---

## 三、工作要点 2：会话记忆 + RAG + NLU 解耦 + 三级降级

### 3.1 多轮会话记忆（Redis）

```
sessionId → Redis Hash {
  stage: "POLICY_CHECK",
  pendingOrder: {...},
  awaitingConfirmation: true,
  deptName: "口腔科",
  date: "2026-06-16",
  trace: [...]
}
TTL: 30 分钟自动清理
```

- 前端无状态，每次请求带 `sessionId`
- 服务端从 Redis 恢复完整对话上下文
- 只向前端暴露白名单字段（nluIntent / nluConfidence / 当前步骤 / 卡片数据），不泄露内部状态

### 3.2 解释型 RAG

**边界明确：** RAG 只用于"解释推荐理由"，不参与号源查询或挂号决策。

```
用户问"为什么推荐这个医生"
  → TriageAgentWorker 识别为 explain_recommendation
  → buildResponse 阶段调用 MultiAgentRagService
    → 检索 docs/agent/*.md（混合检索：关键词 + 本地 64 维向量）
    → LLM 生成解释（System Prompt 约束："只能依据知识片段，禁止编造"）
    → 缓存到 Redis（15 分钟 TTL）
```

**三层 RAG 降级：**
1. LLM 生成（DashScope）→
2. 知识片段拼接（LLM 不可用时）→
3. 硬编码模板兜底（无匹配知识片段时）

### 3.3 Python NLU 推理服务独立拆分

**解耦方式：**

```
Java (TriageAgentWorker)
  → HttpModelIntentParser (HTTP POST)
    → Python http.server :8001/infer
      → parser.parse()
        → DashScope qwen-plus（temperature=0.1, response_format=json_object）
        → 返回 {intent, slots, confidence}
```

- Python 启动与否不影响 Java 主流程
- Java 通过 `@Autowired(required = false)` 可选注入
- NLU 结果存入 session memory，通过白名单暴露前端

### 3.4 三级降级模式（失败率 0.8% 以内的关键）

```
Level 1: Python NLU (DashScope LLM, 5s 超时)
   ↓ 超时/异常/置信度 < 0.75
Level 2: Java 关键词匹配（TriageAgentWorker 内置中文关键词库）
   ↓ 无匹配
Level 3: 默认兜底 "当前仅支持挂号相关操作，请告诉我科室和日期"
```

**为什么失败率能控制在 0.8% 以内：**
- Level 1 LLM 调用有 5s 超时，不阻塞用户
- Java 侧额外做置信度门槛（0.75），低质量结果直接丢弃
- Level 2 关键词覆盖 6 类意图（挂号/查医生/查消息/就诊卡/我的挂号/解释推荐）的 50+ 个中文关键词
- 读工具调用内部还有"最多重试一次"的保护
- **注意：** 0.8% 是 LLM 调用失败后进入降级的比例，不是整体系统错误率。整体业务可用性得益于"LLM 只做 NLU、业务逻辑不依赖 LLM"的架构选择。

### 3.5 面试可能的追问

**Q: Python 服务挂了会影响挂号吗？**
A: 不会。Java 侧 catch 所有 HTTP 异常返回 Optional.empty()，TriageAgentWorker 自动切到关键词匹配模式。用户完全无感知。

**Q: RAG 的向量检索怎么做的？**
A: 没有引入 Milvus/Pinecone 等外部向量库。使用的是自研的 64 维本地哈希向量（字符 + n-gram → hash → 向量 + 归一化），余弦相似度排序，轻量无依赖。

**Q: 会话记忆为什么要设 30 分钟 TTL？**
A: 挂号场景的对话周期通常在 2-5 分钟内完成。30 分钟足够覆盖，超时自动清理避免 Redis 内存堆积。

---

## 四、工作要点 3：挂号核心业务 + 高并发 + 可靠性

### 4.1 Redis 乐观锁防号源超卖（核心问点）

**场景：** 某医生的某个时段只剩 1 个号，10 个用户同时提交。

**方案：Redis WATCH/MULTI/EXEC 乐观锁**

```java
// 数据结构：doctor_schedule_{scheduleId} → {maximum: 30, num: 28}
private boolean reserveScheduleSlot(String key) {
    return redisTemplate.execute(new SessionCallback() {
        public Object execute(RedisOperations ops) {
            ops.watch(key);                          // 1. 监视
            Map entry = ops.opsForHash().entries(key);
            if (num >= maximum) { ops.unwatch(); return null; }
            ops.multi();                             // 2. 开启事务
            ops.opsForHash().increment(key, "num", 1);
            return ops.exec();                       // 3. 提交（key 被改过则返回空）
        }
    });
}
```

**原理：** WATCH 监控 key，如果另一个请求在 MULTI 和 EXEC 之间修改了 key，EXEC 返回 null，上层感知并发冲突。

**配套机制：**
- SETNX 提交锁：防止同一用户 15 秒内重复提交同一号源
- DB 快照二次校验：Redis 操作前重新读 DB 确认号源状态未变
- 释放时校验 owner 身份：防止误删其他线程的锁

### 4.2 JMeter 压测（性能亮点）

| 指标 | 50 并发 | 100 并发 |
|------|---------|----------|
| 吞吐量 | 2,385 req/s | 17,754 req/s |
| 中位数延迟 | 2ms | 2ms |
| P95 | 120ms | 44ms |
| 错误率 | 0.01% | 0.0005% |

**压测方案设计：**
- 4 个线程组：只读基线 / 鉴权读 / 挂号写链路 / 阶梯加压
- setUp 线程组预取 token，后续线程组复用（避免登录成为瓶颈）
- 阶梯加压：10→50→100→200→400 并发，每阶段 2-3 分钟
- 全部 HTTP 请求配置 `keepalive=true` + `HttpClient4`，防止 Windows 临时端口耗尽

### 4.3 审计日志 + Quartz 定时补偿

**审计状态机：**
```
INIT → RESERVED → SUCCESS       (正常路径)
              ↘ FAIL            (不可恢复)
              ↘ COMPENSATED     (补偿恢复)
```

**关键设计：**
- 所有审计写操作使用 `REQUIRES_NEW` 事务传播，确保即使主业务事务回滚，审计记录也持久化
- 按 `requestId` 去重（幂等），同一请求多次到达只记录一行

**Quartz 补偿 Job（每 2 分钟）：**
```
扫描 RESERVED 状态且超过 2 分钟的记录
  → 已有挂号记录？ → 标记 SUCCESS（写入成功，状态未更新）
  → 无挂号记录？   → 回滚号源计数器 + Redis num -1 → 标记 COMPENSATED
  → 回滚失败？     → 标记 FAIL，人工介入
```

### 4.4 面试可能的追问

**Q: 为什么用乐观锁而不是悲观锁（SETNX）锁整个号源？**
A: Redis WATCH/MULTI/EXEC 只在提交时检测冲突，读取不阻塞。悲观锁会串行化所有对同一号源的请求，高并发下性能差一个数量级。挂号场景冲突率不高（多数时段号源充足），乐观锁是更优选择。

**Q: 如果 Redis 挂了怎么办？**
A: 挂号写链路在 Redis 操作前会重新读 DB 确认号源状态。理论上可以降级为纯 DB 乐观锁（version 字段）。当前方案中 Redis 与 MySQL 在同一内网，可用性有保障。

**Q: P95 延迟从 50 并发的 120ms 降到 100 并发的 44ms，为什么？**
A: 这跟 JIT 预热有关。50 并发测试先跑，JVM 还在解释执行阶段。100 并发时 JIT 已充分编译热点代码（C2 编译），方法调用从解释执行切换为本地代码，延迟反而更低。

**Q: 定时补偿的 2 分钟间隔是怎么定的？**
A: 挂号不是秒杀场景，用户对"2 分钟内恢复"的容忍度较高。间隔太短会增加 DB 压力，太长则用户等待焦虑。2 分钟是业务容忍度和系统开销的平衡点。

---

## 五、面试常见追问汇总

### 5.1 "为什么选 Multi-Agent 而不是单 Agent ReAct？"

| 维度 | 单 Agent ReAct | Multi-Agent（当前方案） |
|------|---------------|----------------------|
| 路由控制 | LLM 自主决定下一步 | 协调器硬编码 Enum 状态机 |
| 可调试性 | LLM 决策不透明 | 每步 trace 可回溯 |
| 安全边界 | 提示词约束，可能被越狱 | Worker 代码强制校验 |
| 故障隔离 | 模型挂了全链路不可用 | 单个 Worker 失败不影响其他 |
| 扩展成本 | 新增工具 + 补提示词 | 新增 Worker 或工具 |

核心结论：挂号是确定性业务，不需要 LLM 做开放式推理，Multi-Agent 的工程可控性远优于单 Agent。

### 5.2 "NLU 为什么不用 Java 直接调 DashScope SDK？"

- **语言优势分离：** Python 生态在 NLP/分词（jieba）、模型调用方面比 Java 更成熟
- **独立扩缩容：** Python 服务可以独立部署/重启/升级模型，不影响 Java 主业务
- **故障隔离：** HTTP 超时或 Python 崩溃不会拖垮 Java 进程
- **面试加分：** 展示了"用合适的语言做合适的事"的工程判断

### 5.3 "微调/量化/部署的完整链路在哪里？"

- 微调（LoRA/QLoRA + LLaMA-Factory）和加速（vLLM INT4）属于面试扩展方向
- 当前线上使用的是提示词工程 + DashScope qwen-plus，已满足挂号意图识别的准确率需求
- 如需展示完整 ML 链路：微调→vLLM 部署→Docker→/infer 服务的四步架构已设计，可随时替换 DashScope

### 5.4 "你的方案跟业界（如阿里云、腾讯）医疗 AI 方案的区别？"

- 大厂方案通常是"通用对话 + 意图识别 + 人工兜底"，AI 只做信息咨询
- 本方案的核心差异：**AI 直接参与写操作（挂号提交），但通过 Worker 流水线确保安全边界**
- 这比纯对话机器人更难做，也是面试中最容易出彩的点

---

## 六、安全防护体系（容易被忽略但面试加分）

### 6.1 高危意图识别与拦截

Python NLU 侧设有机密操作黑名单，LLM prompt 中也要求模型标记危险意图：

```
DANGEROUS_KEYWORDS = [
    "删库", "删表", "drop table", "delete from", "truncate",
    "批量删除", "批量修改", "rm -rf", "shutdown", "sudo",
    "format", "提权", "注入", "exploit", "xss"
]
```

- **双重保障：** Python 关键词匹配（硬规则，无法绕过）+ LLM system prompt 语义识别（"禁止执行任何删除、修改、提权操作"）
- **Java 侧兜底：** TriageAgentWorker 收到 `intent=dangerous` 直接返回阻断，不进入任何后续 Worker
- **面试话术：** "用户输入被两层独立机制审查，即使 LLM 被 prompt injection 绕过，硬编码黑名单仍会拦截"

### 6.2 请求治理（Guard）

在 MultiAgentCoordinatorService 中内置两层限流：

| 机制 | 配置 | 作用 |
|------|------|------|
| 每分钟请求上限 | `guard-request-limit-per-minute=12` | 防止单用户高频刷接口 |
| 最小请求间隔 | `guard-min-interval-millis=1000` | 防止脚本自动化攻击 |

### 6.3 写操作多层防护全景

```
用户发起确认挂号
  → composePayload: 校验 awaitingConfirmation=true + payload 与 pendingOrder 一致
    → PolicyAgentWorker: 再次校验登录态、就诊卡、当日上限、重复挂号（fail-closed）
      → ExecutionAgentWorker: 第三次校验 pendingOrder 一致性 + 构建幂等键
        → RegistrationService: Redis SETNX 提交锁 + DB 快照二次校验 + WATCH/MULTI/EXEC 乐观锁
          → 审计表: REQUIRES_NEW 事务独立持久化
```

**每一层失败都有明确的错误码和处理策略**（保留/清理 pendingOrder、是否可重试、是否进入 MANUAL_FALLBACK）。

---

## 七、架构演进路径（展示技术决策能力）

### 7.1 三期演进对比

| 维度 | 一期（规则编排） | 二期（单 Agent ReAct） | 三期（多 Agent，生产主力） |
|------|-----------------|----------------------|--------------------------|
| LLM 角色 | 仅辅助意图识别 | 自主决定工具调用 | NLU 入口 + RAG 解释 |
| 路由决策 | 后端硬编码 | LLM 自主选择下一步 | 协调器 Enum 状态机 |
| 扩展方式 | 修改动作枚举 | 注册新工具 | 新增/调整 Worker |
| 故障隔离 | 降级本地规则 | 记忆恢复 + 兜底 | LLM 失败不影响主链路 |
| 安全边界 | 提示词 + 后端校验 | 提示词 + 工具隔离 + 兜底 | 分权 Worker + 多层校验 + 幂等 |
| 适用场景 | 快速验证 | 通用灵活 | 确定性业务（挂号） |

### 7.2 为什么经历三期而不是直接做多 Agent？

**面试回答框架：**

1. **一期（2 周）：** 快速 MVP，验证"AI + 挂号"是否可行。规则编排足够简单，先跑通闭环。
2. **二期（3 周）：** 发现规则编排扩展困难，每次新增功能要改枚举和编排代码。引入 ReAct 让 LLM 做工具选择，灵活但不可控。
3. **三期（4 周，当前）：** ReAct 在生产中出现幻觉（编造号源、跳过确认），认识到"挂号不需要 LLM 做开放式推理"。重新设计为 Worker 流水线，LLM 退回到纯 NLU 角色。

> **关键洞察：** "不是越智能越好，是越可控越好。我们经历了从'让 AI 做决策'到'让 AI 做输入、代码做决策'的认知转变。"

---

## 八、工程化与可观测性

### 8.1 Trace 追踪体系

每次请求的所有 Worker 执行步骤都记录到 `AgentTraceEntry` 列表：

```
trace: [
  {stage: "INTENT_PARSE", worker: "TriageAgentWorker", summary: "registration_intent", latencyMs: 45},
  {stage: "SLOT_QUERY", worker: "ScheduleAgentWorker", step: "MATCH_DEPARTMENT", toolCall: "searchDepartments", latencyMs: 23},
  {stage: "SLOT_QUERY", worker: "ScheduleAgentWorker", step: "SEARCH_DOCTORS", toolCall: "searchDoctorPlansInDay", latencyMs: 67},
  {stage: "SLOT_QUERY", worker: "ScheduleAgentWorker", step: "SEARCH_SLOTS", toolCall: "searchScheduleSlots", outcome: "SUCCESS", latencyMs: 34},
  {stage: "POLICY_CHECK", worker: "PolicyAgentWorker", checks: ["login", "userCard", "dailyLimit", "duplicate"], outcome: "PASSED"},
  {stage: "EXECUTE_APPOINTMENT", worker: "ExecutionAgentWorker", requestId: "A1B2C3...", outcome: "SUCCESS", latencyMs: 156}
]
```

- 前端按 trace 渲染 Agent 执行流程可视化（当前步骤高亮、已完成步骤打勾）
- 出现问题时按 `sessionId` 回放完整调用链
- 审计表的 `traceJson` 字段保存快照，补偿 Job 可以据此判断当前处于哪个阶段

### 8.2 NLU 双模式切换

Python 侧 `parser.py` 的 `self.llm_enabled` 布尔开关：

- `True`：走 DashScope LLM 远程推理（生产模式）
- `False`：纯本地 jieba + 关键词规则引擎（离线/降级模式）
- 切换不需要重启 Java，改 Python 变量重启 Python 服务即可
- 本地模式也能跑通 90% 以上的挂号场景（关键词覆盖面广）

### 8.3 RAG 知识库热更新

- 知识源在 `docs/agent/*.md`，修改即生效
- `MultiAgentKnowledgeBase` 每次请求检测文件修改时间，增量重载变更文件
- 不需要重启服务即可更新挂号规则说明、推荐理由模板

---

## 九、场景演练（面试时的白板题目）

### 场景 1："用户说'明天牙疼挂骨科'"

```
TriageAgentWorker:
  1. Python NLU: {intent:"registration", slots:{symptom:"牙疼", department:"骨科", date:"明天"}, confidence:0.92}
  2. 关键词二次确认: "挂号"命中 → isRegistrationIntent=true
  3. 日期提取: "明天" → 2026-06-16
  → HANDOFF to SLOT_QUERY

ScheduleAgentWorker (ReAct循环):
  4. 无 deptId → decideNextTool: MATCH_DEPARTMENT
  5. 调用 searchDepartments() → 返回 11 个科室列表
  6. 模糊匹配"骨科" → deptId=8, deptName="骨科"
  7. 无 deptSubId → decideNextTool: SEARCH_SUB_DEPARTMENTS
  8. 调用 searchSubDepartments(8) → 脊柱外科/关节外科/创伤骨科
  9. 无医生列表 → decideNextTool: SEARCH_DOCTORS
  10. 调用 searchDoctorPlansInDay(deptSubId, "2026-06-16") → 3 位医生
  11. 有医生 → decideNextTool: SEARCH_SLOTS
  12. 遍历医生查号源 → 张医生时段 09:00-09:30 余 2 号
  → 生成 pendingOrder, HANDOFF to POLICY_CHECK

PolicyAgentWorker:
  13. 登录检查: 已登录 ✓
  14. 就诊卡: 存在 ✓
  15. checkRegistrationCondition: 今日已挂 1 次, 上限 3 次, 未重复 ✓
  → ASK_USER (awaitingConfirmation=true)
  → 前端展示确认卡片: "骨科-创伤骨科-张医生-6月16日 09:00-09:30-¥20"

用户点击"确认挂号":
  16. composePayload 校验: awaitingConfirmation=true, payload 与 pendingOrder 一致 ✓
  → HANDOFF to EXECUTE_APPOINTMENT

ExecutionAgentWorker:
  17. 再次校验 pendingOrder 一致性 ✓
  18. buildRequestId → A1B2C3D4...
  19. RegistrationService 提交:
    - SETNX 锁获取 ✓
    - DB 号源快照二次校验 ✓
    - Redis WATCH/MULTI/EXEC 预约 ✓
    - DB INSERT + 计数器更新 ✓
    - 审计 markSuccess ✓
  → FINISH, 返回 "挂号成功！张医生 6月16日 09:00-09:30"
```

### 场景 2："LLM 返回了错误的科室名'心脏科'（标准名是'心内科'）"

```
Python NLU: {intent:"registration", slots:{department:"心脏科"}, confidence:0.88}

ScheduleAgentWorker:
  searchDepartments() → [口腔科, 呼吸内科, 心内科, 皮肤科, ...]
  模糊匹配"心脏科" → 无精确匹配
  → Python LLM 结果中的 department 在 parser._normalize() 阶段
    被 symptom_department_map 修正（心脏 → 心内科）
  → 如果仍未匹配: Java 侧返回 ASK_USER "未找到心脏科，请确认科室名称"
```

> **设计意图：** Python 侧的 LLM 结果不是最终结果，Java 侧始终以 MySQL 真实科室表为准做"过筛"。

### 场景 3："网络闪断导致挂号提交超时，用户再次点击"

```
第一次提交:
  ExecutionAgentWorker 调用 createRegistrationOrder → HTTP 超时（但 DB 写入已成功）
  → 审计状态: RESERVED（markReserved 在 REQUIRES_NEW 事务中已持久化）

用户再次点击（相同参数）:
  composePayload: pendingOrder 未被清理（超时保留策略）→ 校验通过
  RegistrationService.decideReplay(requestId):
    → 审计查询: requestId=A1B2C3..., 状态=RESERVED
    → "请勿重复提交，系统正在处理中"
  → 1 分钟后 Quartz RepairJob:
    → 扫描到 RESERVED 记录, 查 DB 已有挂号记录
    → markSuccess → 前端下次刷新可见挂号成功
```

---

## 十、数据指标补充

### 10.1 各层耗时分布（单次挂号请求）

| 阶段 | 典型耗时 | 说明 |
|------|---------|------|
| NLU 意图识别 | 45-80ms | Python HTTP + DashScope LLM |
| 科室匹配 | 12-25ms | MySQL 查询 + 内存模糊匹配 |
| 医生号源查询 | 50-120ms | 2-3 次 DB 查询（科室→医生→号源） |
| 规则校验 | 15-30ms | DB 查就诊卡 + 挂号记录 |
| 写操作提交 | 100-200ms | Redis 锁 + DB 事务 + 消息推送 |
| RAG 解释生成 | 200-500ms | 检索 + LLM 生成（仅当用户请求解释时） |

### 10.2 降级触发频率（估算）

| 降级路径 | 预估占比 | 触发条件 |
|---------|---------|---------|
| Python LLM → Java 关键词 | ~2-5% | LLM 超时/异常/低置信度 |
| RAG LLM → 片段拼接 | ~3-8% | LLM 超时/异常 |
| 读工具重试 | ~0.5-1% | 首次 DB 查询异常 |
| Worker 失败 → MANUAL_FALLBACK | ~1-2% | 规则校验失败/号源变更 |

---

## 十一、面试时的"亮点展示"建议

### 优先级排序（按面试官兴奋度）

1. **写操作安全性**（最炸） — "我们的 AI 能做挂号写操作，但有 6 层防护，不是那种只敢做问答的聊天机器人"
2. **Python/Java 解耦 + 三级降级** — 展示"用合适的语言做合适的事"的工程判断
3. **自研 ReAct + 为什么不用 LangChain** — 展示对 AI 框架的深度理解
4. **Redis 乐观锁防超卖** — 经典的"高并发写"问题，标准答案
5. **Quartz 定时补偿** — 展示分布式系统最终一致性的理解
6. **三期架构演进** — 展示技术决策能力，不是"一步到位"而是"踩坑后优化"

### 面试官可能的追问预判

**Q: 你那个 85% 是怎么算出来的？**
A: 对比一期（无确认机制 + 无幂等）和当前的测试结果。一期 100 次用户测试中，约 20 次出现参数篡改导致的错误挂号或 LLM 幻觉编造号源；当前方案同样的测试集下降到约 3 次。主要改善来自 pendingOrder 服务端快照确认（杜绝前端篡改）和请求级幂等去重（杜绝重复提交）。

**Q: 为什么不用 Spring AI / LangChain4j？**
A: 评估过。Spring AI 当时对国内模型支持不完善，LangChain4j 对中文医疗场景的定制成本高于自研。我们的 Worker 流水线本质就是一个简化版的状态机编排，代码量不到 2000 行，引入框架反而增加复杂度。

**Q: 如果 LLM 完全不工作，整个系统还能跑吗？**
A: 能。LLM 在系统中只做两件事：NLU 意图识别（可降级为关键词）和 RAG 解释生成（可降级为模板）。核心的科室查询、医生排班、号源判断、规则校验、写操作执行全部是确定性代码，不经过 LLM。

**Q: 这个系统最大的技术挑战是什么？**
A: 不是 AI 本身，而是在"让 AI 参与写操作"的前提下保证不出一笔错账。医院号源是稀缺资源，挂错号不是"用户再试一次"的问题，而是"号被占用了别人挂不了"。所以我把最多的时间花在 pendingOrder 确认机制、幂等键设计和补偿 Job 上，而不是模型调优上。

---

## 十二、一句话总结（电梯演讲用）

> 我主导设计了一个四阶段 Multi-Agent 挂号架构，LLM 只做意图理解、业务逻辑全部由 Java Worker 代码控制，通过服务端快照确认、幂等键去重、审计追踪和定时补偿，在保证 AI 交互体验的同时，将幻觉和重复提交风险降低了 85%，并压测验证了万级 QPS 的高并发稳定性。
