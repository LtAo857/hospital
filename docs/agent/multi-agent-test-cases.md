# 多 Agent 挂号系统测试案例与问题总结

## 测试环境

- 后端：`patient-wx-api-mysql`（Spring Boot 2.7 + MyBatis）
- NLU 服务：`nlu-service`（Python，FastAPI/stdlib HTTP）
- 前端：`patient-wx`（UniApp 微信小程序）
- 会话记忆：`MultiAgentMemoryService`（基于 sessionId 的 Map 存储）

## 已修复问题清单

### 1. RAG 脏缓存跨会话泄漏

**发现**：用户问"头疼"，RAG 回复中包含了上一轮"胸痛"会话的知识片段和推理过程。

**根因**：
- `MultiAgentRagService.buildPrompt()` 直接把 `memory` 完整 JSON dump 进 prompt
- LLM 系统提示词未禁止输出内部推理
- `sanitizeMemory()` 缺少 `symptoms`（复数）字段，导致 LLM 看到旧症状数据

**修复**：
- `buildPrompt()` 改为结构化上下文（`症状=X 科室=Y ...`），不再 dump JSON
- 系统提示词增加：`禁止输出你的内部推理过程、过滤逻辑，不要引用"当前 memory"等内部变量`
- `cleanResponse()` 正则兜底 strip 泄漏文本
- `sanitizeMemory()` 增加 `symptoms` 字段

**测试**：`MultiAgentRagServiceTest` — 2 个测试通过
```
输入：头疼 → RAG prompt 不包含上一轮"胸痛"的 symptomDeptGraph
输入：为什么推荐这个 → cleanResponse 不包含"与X无关，故不采纳"
```

---

### 2. 症状→科室映射错误

**发现**：用户说"头疼"，系统推荐"口腔科"而非"神经内科"。

**根因**：
- `putTextIfAbsent` 语义：不会覆盖已有值
- 上一轮会话的 `deptName="口腔科"` 残留在 memory 中
- NLU 解析"头疼"→科室"神经内科"后，`putTextIfAbsent` 发现 memory 已有 `deptName="口腔科"`，不覆盖

**修复**：
- `parseNlu` 签名增加 `memory` 参数
- 新增症状切换检测：NLU 提取到新症状 → 检查旧 `deptName` 是否在新症状的预期科室列表中 → 不在则清除 `deptName/deptId/deptSubId/deptSubName/recommendedDeptName`
- `parseNlu` 总是设置 `symptomDeptGraph`（有新症状时设为新图谱，无新症状时设为 null 清除旧图谱）
- `nluSymptomFresh` 标记确保只在有新症状时触发清除

**测试**：`MultiAgentCoordinatorServiceTest` — 7 个测试通过（回归验证）
```
输入1: 牙疼挂口腔科 → 口腔科 + 牙疼图谱
输入2: 头疼重新挂号 → 口腔科被清除，重新匹配神经内科
```

---

### 3. LLM 内部推理文本泄漏给用户

**发现**：用户看到的回复包含：
- `当前 memory 中 pendingOrder.doctorId 为 null，传入无效 JSON`
- `与胸痛无关，故不采纳该推荐`
- `所有分诊建议均严格依据...`（内部过滤逻辑）
- `不采纳用户自述症状"胸痛"...`
- `未提供有效科室名`

**根因**：
- RAG `buildPrompt()` 传入完整 memory JSON（含 `symptomDeptGraph`、`pendingOrder` 等内部字段）
- LLM 系统提示词未明确禁止输出过滤逻辑
- 无后处理清洗

**修复**：
- `buildPrompt()` 改为仅传结构化字段：`症状=X 科室=Y 患者性别=Z`
- 系统提示词严格限制：`不要输出你的内部推理过程、过滤逻辑`
- `cleanResponse()` 正则清洗 5 种泄漏模式：
  - `当前\s*memory\s*中...`
  - `与\S+无关，故不采纳该推荐`
  - `所有分诊建议均严格依据...`
  - `不采纳...`
  - `未提供...`

**测试**：`MultiAgentRagServiceTest.shouldCleanLeakedInternalReasoning` — 通过
```
LLM输出: "当前 memory 中 pendingOrder 为空，与胸痛无关，故不采纳该推荐。建议挂神经内科。"
清洗后: "建议挂神经内科。"
```

---

### 4. 查询号源被误判为挂号并跳转确认

**发现**：用户问"骨科明天哪些医生有号源"，系统回复"挂号条件校验通过，请确认是否提交挂号"而非展示医生列表。

**根因**：
- `REGISTRATION_KEYWORDS` 包含"号"和"号源"，"骨科明天哪些医生有号源"被分类为 `registration`
- `rules.py` 中 `registration` 检查（第58行）排在 `query_doctor`（第60行）前面
- `ScheduleAgentWorker.buildSuccessResult()` 永远返回 `HANDOFF → POLICY_CHECK`，触发确认流程
- LLM 系统提示词写 `registration: 挂号、预约、看诊、查号源`，把"查号源"归入 registration

**修复**（三层）：
1. **Python `rules.py`**：`query_doctor` 检查提前到 `registration` 之前（当有"医生/大夫/主任"且无"挂/预约"时）
2. **Python `parser.py`**：LLM prompt 修正：`registration: 挂号、预约（用户明确要执行挂号操作）`；`query_doctor: 查医生信息、查号源、医生排班、哪个医生有号（仅查询，不挂号）`
3. **Java `MultiAgentCoordinatorService.java`**：流水线循环中拦截 `query_doctor` 意图，ScheduleAgentWorker 返回医生列表后直接 FINISH，清除 `awaitingConfirmation/pendingOrder` 阻止确认卡片

**测试**：`MultiAgentCoordinatorServiceTest` — 8 个测试通过
```
输入: 骨科明天哪些医生有号源 → intent=query_doctor → 展示医生列表 → stop（不跳转确认）
输入: 挂骨科医生的号 → intent=registration → 正常挂号流程（跳转确认）
```

---

### 5. 连续查询日期残留

**发现**：
- 第1次："骨科明天哪些医生有号源" → `2026-06-20 骨科暂无可挂号医生`
- 第2次："骨科今天哪些医生有号源" → `2026-06-20 骨科暂无可挂号医生`（应为 2026-06-19）

**根因**：
- `parseNlu` 使用 `putTextIfAbsent(nluPatch, "date", ...)` — 本次 nluPatch 是新的，一定能写入
- `applyMemoryPatch` 用 `memory.put()` 覆盖，理论上能更新
- 但旧有的 stale clearing 仅在 `pendingOrder instanceof Map` 时触发
- 第1次查询无号源 → `pendingOrder=null` → 第2次查询日期变化不被检测
- memory 中 `stage` 保留为 `SLOT_QUERY`，第2次仍然走 ScheduleAgentWorker
- 但某些边缘情况下 `putTextIfAbsent` 结合 memory 中已解析的 `date="2026-06-20"` 阻止了 `"今天"` 写入

**修复**：
- 扩宽 stale clearing 逻辑，增加第2个检测条件：
  - 即使 `pendingOrder` 为 null，也对比 `nluPatch` 中的 newDate/newDept 与 memory 中已有的 memDate/memDept
  - 若不同，强制覆盖变更的字段（仅覆盖真正变化的 key，避免误清 deptName）
  - 重置 `stage=INTENT_PARSE`，清除所有挂号中间态

**测试**：`shouldResolveFreshDateWhenPreviousQueryHasDifferentDate` — 通过
```
第1次: 明天 → memory 存 date="2026-06-20"
第2次: 今天 → stale clearing 检测 date 不同 → 强制覆盖 → 解析为 2026-06-19
```

---

### 6. 短跟进语"今天呢"被拒为 NLU 不可用

**发现**：
- 用户先问"骨科明天谁有号源" → 正常返回（无号源）
- 追问"今天呢" → `NLU 服务暂不可用，请改走普通挂号流程`
- 但用户期望系统理解"今天"是在改日期，沿用前面的"骨科"

**根因**：
- Python `parser.py` 中规则引擎回退路径硬编码 `confidence = 0.0`（第87行）
- 规则引擎正确提取了 `date="今天"`，但 intent=`unknown`，confidence=0.0
- Java `HttpModelIntentParser.parse()` 检查 confidence < minThreshold → 返回 `Optional.empty()`
- Java `handleNluDirectIntent()` 收到空 NLU → 返回"NLU 服务暂不可用"

**修复**：
- Python `parser.py`：规则引擎在提取到有效 slot（date/department/doctorName/symptoms）时，confidence 设为 0.9 而非 0.0
- Java stale clearing：修复 force-overwrite 只覆盖变化字段（deptStale/dateStale 独立判断）

**测试**：Python 单元测试
```
今天呢 → intent=unknown, confidence=0.9, date=今天  ← 新行为
你好   → intent=unknown, confidence=0.0            ← 无slot，保持0.0
```
Java 端：confidence=0.9 >= threshold → intent=unknown + memory.deptName="骨科" → 进入流水线 → ScheduleAgentWorker 查 2026-06-19 骨科号源

---

### 7. 聊天输入框位置

**发现**：聊天框（composer）不在页面底部，消息列表有固定高度 `760rpx`。

**修复**：`patient-wx/agent/chat/chat.vue`
- `.page`: `display: flex; flex-direction: column; height: 100vh`
- `.message-list`: `flex: 1; overflow-y: auto`（替代固定 760rpx）
- `.composer`: `flex-shrink: 0; padding-bottom: calc(20rpx + env(safe-area-inset-bottom))`

**测试**：手动验证，输入框固定在底部，消息区自动填充剩余空间

---

## 测试用例列表

### Java 测试（patient-wx-api-mysql）

| # | 测试方法 | 覆盖问题 |
|---|---------|---------|
| 1 | `shouldCompleteFlowAndPersistDoneState` | 完整挂号流水线（SLOT→POLICY→EXECUTE→DONE） |
| 2 | `shouldExposeFallbackCardForLoginRequiredFailure` | 登录失败返回 Fallback 卡片 |
| 3 | `shouldBuildToolLogsAndFlowsForScheduleWorkerContract` | 工具日志和 Agent 流程展示 |
| 4 | `shouldAppendRequestedViewCardsAndExplainCard` | 查看挂号/解释推荐卡片附加 |
| 5 | `shouldUseRagAnswerForExplanationRequest` | RAG 解释请求的干净回复 + 知识来源卡片 |
| 6 | `shouldRejectForgedConfirmationBeforeCallingWorker` | 伪造确认被拒绝（防篡改） |
| 7 | `shouldRejectInvalidChatPayloadBeforeCallingWorker` | 非法 payload 被拒绝并返回 badCaseType/badFields |
| 8 | `shouldResolveFreshDateWhenPreviousQueryHasDifferentDate` | 连续查询日期不残留 |
| - | `MultiAgentRagServiceTest.shouldCleanLeakedInternalReasoning` | RAG 泄漏文本清洗 |
| - | `MultiAgentRagServiceTest.shouldSanitizeMemoryForCacheKey` | RAG 缓存键脱敏 |
| - | `MultiAgentRagEvaluationTest.shouldHitExpectedTitlesOnOfflineCases` | RAG 离线知识命中（已知失败） |
| - | `ScheduleAgentWorkerTest` ×7 | 号源查询 5 步流水线各分支 |
| - | `PolicyAgentWorkerTest` ×6 | 挂号条件校验各规则 |
| - | `ExecutionAgentWorkerTest` ×8 | 挂号执行各路径 |
| - | `MultiAgentRegistrationFlowIntegrationTest` ×11 | 挂号全链路集成测试 |
| - | `MultiAgentRegistrationAuditServiceTest` ×2 | 审计服务 |
| - | `MultiAgentRequestGuardServiceTest` ×2 | 请求治理 |

### Python 测试（nlu-service）

| # | 测试 | 覆盖问题 |
|---|------|---------|
| 1 | `server_stdlib.py --self-test` | API 自检 |
| 2 | `scripts/eval.py` | 意图准确率（10 样本，当前 0.9） |
| 3 | `骨科明天哪些医生有号源` → `query_doctor` | 号源查询分类 |
| 4 | `挂骨科医生的号` → `registration` | 挂号操作分类不变 |
| 5 | `今天呢` → confidence=0.9, date=今天 | 短跟进语置信度 |
| 6 | `你好` → confidence=0.0 | 无意义输入保持 0.0 |

---

## 已知遗留问题

1. **RAG 离线评估**：`MultiAgentRagEvaluationTest.shouldHitExpectedTitlesOnOfflineCases` 预期命中特定知识标题，当前命中率不达标（与本次修复无关，属 RAG 检索质量优化方向）
2. **"我要取消昨天的挂号"** → 被规则引擎分类为 `registration` 而非 `unsupported`（"取消" + "挂号" 同时存在时的优先级需调整）
3. **测试数据库无号源**：骨科 2026-06-19/20 返回"暂无可挂号医生"可能是测试数据缺失，非代码 bug

---

## 关键设计原则（从本次修复中提炼）

1. **NLU 置信度不应二值化**：规则引擎提取到有效 slot 时应给合理置信度，而非硬编码 0.0
2. **putTextIfAbsent 是一把双刃剑**：保护下游决策的同时会阻止合法的上下文更新，需要配合显式的 stale clearing
3. **Stale clearing 要覆盖所有残留路径**：不仅检查 `pendingOrder`，还要对比 memory 本身的值
4. **Force-overwrite 要精确**：只覆盖真正变化的字段，避免连锁清除无关上下文
5. **LLM 输出需要多层防护**：系统提示词约束 + 结构化输入 + 正则后处理，缺一不可
6. **意图分类要区分查询与操作**：含"号源""医生"但无"挂""预约" → `query_doctor`，含"挂""预约" → `registration`
