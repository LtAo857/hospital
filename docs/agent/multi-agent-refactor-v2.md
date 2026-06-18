# 多 Agent 挂号架构重构 (v2)

2026-06-17

## 重构目标

将"LLM + 规则引擎混合理解"改造为"LLM 唯一入口、Java 纯执行"的 NLU-first 流水线架构，消除 Java 侧的文本理解和关键词匹配代码。

## 架构对比

### 重构前

```
用户输入
  → TriageAgentWorker (LLM + 关键词混合判断意图)
  → ScheduleAgentWorker (伪 ReAct 循环 + 自己从原文提取科室/日期)
  → PolicyAgentWorker
  → ExecutionAgentWorker
```

- Java 在 Triage 和 Schedule 中都有文本理解代码
- Schedule 的"轻量 ReAct"实际是 if-else 循环，无 LLM 参与决策
- NLU 结果和关键词匹配并行，主次不分

### 重构后

```
用户输入
  → Coordinator 直接调 NLU (LLM 一次推理)
  → 路由：非挂号意图直接返回 / 挂号意图进入流水线
  → ScheduleAgentWorker (顺序查库)
  → PolicyAgentWorker (条件校验)
  → ExecutionAgentWorker (写挂号)
```

- Java 侧 0 行文本理解代码
- LLM 是唯一的 NLU 入口
- NLU 不可用时返回"NLU 服务暂不可用，请改走普通挂号流程"

## 改动文件

### 删除

| 文件 | 原因 |
|------|------|
| `TriageAgentWorker.java` | NLU + 意图路由合并到 Coordinator |
| `TriageAgentWorkerTest.java` | 对应测试 |

### 修改

#### 1. `nlu-service/hospital_nlu/parser.py`

| 改动 | 说明 |
|------|------|
| System prompt 动态化 | `NLU_SYSTEM_PROMPT` 静态常量 → `_build_system_prompt()` 方法，运行时注入科室列表 |
| LLM 升为主路径 | LLM 成功+高置信度时直接返回，规则引擎仅为兜底 |
| 规则引擎 confidence 固定 0.0 | 删除 `_confidence()` 方法，兜底路径不再伪造置信度 |
| prompt 约束科室 | 新增规则：`department必须是可用科室列表中的科室，不要编造不存在的科室` |

当前 system prompt 结构：
```
你是医院挂号NLU引擎...
当前可用科室：口腔科、呼吸内科、消化内科、...
意图类型：registration / query_doctor / ... / dangerous
槽位字段：symptom / department / doctorName / date / timePreference
```

#### 2. `ScheduleAgentWorker.java`

| 改动 | 说明 |
|------|------|
| 去掉伪 ReAct 循环 | `execute()` 从 for 循环改为顺序流水线 5 步 |
| 删除 ReAct 调度方法 | `decideNextTool()`, `guardDecision()`, `runToolStep()` |
| 删除文本提取方法 | `extractDeptName()`, `extractDoctorName()`, `parseDateHint()` |
| 删除 `ToolDecision` 枚举 | 不再需要决策枚举 |
| 删除未使用常量 | `MAX_REACT_STEPS`, `DATE_HINT_PATTERN`, `DOCTOR_HINT_PATTERN`, `DATE_FORMATTER` |
| 删除未使用导入 | `LocalDate`, `DateTimeFormatter`, `Pattern`, `Matcher` |
| 上下文相关错误提示 | 每种失败场景返回针对性消息（见下方） |

错误提示对照：
| 场景 | 回复 |
|------|------|
| 没给科室名 | "请告诉我想挂哪个科室，例如：明天口腔科。" |
| 科室名 DB 匹配不上 | "没有找到{科室名}这个科室，当前可选：口腔科、呼吸内科、..." |
| 科室下无诊室 | "{科室名}下暂无可选诊室，请换一个科室试试。" |
| 没给日期 | "请告诉我你想挂哪天的号，例如：明天、下周一、2026-06-20。" |
| 当天无医生排班 | "未查询到该诊室当天的医生排班，请换个日期试试。" |
| 号源全满 | "暂时没有找到可用号源，请换个日期或诊室再试。" |
| DB 查询异常 | "查询号源时出现波动，请稍后重试或改走普通挂号。" |

#### 3. `MultiAgentCoordinatorService.java`

| 改动 | 说明 |
|------|------|
| 注入 `ModelIntentParser` | 可选注入，NLU 不可用时返回兜底提示 |
| 新增 `parseNlu()` | 调用 Python NLU，将 intent + slots 写入 memory |
| 新增 `handleNluDirectIntent()` | 按意图路由：非挂号直接返回，挂号进入流水线 |
| 新增 `isDirectCreate()` | 判断是否已有完整挂号参数+确认 |
| `resolveStage` 默认值改为 `SLOT_QUERY` | 原为 `INTENT_PARSE` |
| NLU 不可用处理 | `nluUnavailable=true` → 提示走手动挂号 + 跳转卡片 |
| 删除 `containsAny()` | Java 侧不再做文本关键词匹配 |
| 删除 `containsDeptKeyword()` | 同上 |
| `buildSteps` triage 步骤固定为 "completed" | 不再区分 in_progress 状态 |

#### 4. `MultiAgentStage.java`

保留 `INTENT_PARSE` 枚举值以兼容已有 Redis session，`resolveStage` 自动将其映射到 `SLOT_QUERY`。

## 完整链路

```
POST /agent/multi/chat
  │
  ▼
MultiAgentCoordinatorService.chat()
  │
  ├─ composePayload()        ← 规范化 payload + 防伪造确认
  ├─ parseNlu()              ← LLM 一次推理 → intent + slots → 写入 memory
  ├─ handleNluDirectIntent() ← 路由
  │   ├─ registration    → 继续流水线
  │   ├─ dangerous       → 直接阻断
  │   ├─ query_*         → 直接返回对应卡片
  │   └─ NLU 不可用       → "NLU 服务暂不可用，请改走普通挂号流程"
  │
  ├─ ScheduleAgentWorker    ← 顺序查库 5 步
  │   1. 科室名模糊匹配 → deptId
  │   2. 查子科室 → deptSubId
  │   3. 确认日期存在
  │   4. 查当天医生排班 → doctors[]
  │   5. 查号源时段 → Candidate → pendingOrder + awaitingConfirmation
  │
  ├─ PolicyAgentWorker      ← 条件校验
  │   - 登录态 / 就诊卡 / 当日上限 / 重复挂号
  │   - 未确认 → 等待用户点击确认卡片
  │
  └─ ExecutionAgentWorker   ← 写挂号
      - pendingOrder 一致性二次校验
      - requestId 幂等写入 (UUID v5)
      - 写失败不重试，进入补偿修复
```

## AI 的使用范围

| 位置 | AI 做了什么 | 调用次数 |
|------|-----------|---------|
| NLU (Python /infer) | 意图识别 + 槽位提取 | 每次请求 1 次 |
| RAG 解释 | 关键词检索 + 可选 LLM 总结 | 仅用户问"为什么"时 |

其余所有步骤（查库、校验、写操作）均为确定性 Java 代码，LLM 不参与决策。

## 兜底链路

```
LLM 超时/低置信度
  → Python 规则引擎 (关键词+正则)
    → 返回 confidence=0.0
      → Java 端拦截 (< minConfidence)
        → 返回 "NLU 服务暂不可用，请改走普通挂号流程。"
          → 附带手动挂号跳转卡片
```

Java 侧不再有任何关键词兜底代码。Python 的规则引擎是最后的 fallback，再往下就是明确的不可用提示。
