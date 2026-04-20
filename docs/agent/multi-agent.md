# Multi-Agent 实现说明

## 范围
- 仅新增多-agent能力，不改动现有单-agent接口与流程。
- 新接口：`POST /agent/multi/chat`

## 流程
- `INTENT_PARSE`：`TriageAgentWorker` 识别挂号意图与基础参数。
- `SLOT_QUERY`：`ScheduleAgentWorker` 查询可用号源并生成 `pendingOrder`。
- `POLICY_CHECK`：`PolicyAgentWorker` 校验登录、就诊卡、挂号条件。
- `EXECUTE_APPOINTMENT`：`ExecutionAgentWorker` 执行挂号下单。
- 由 `MultiAgentCoordinatorService` 负责阶段流转、trace记录、memory读写与响应组装。

## Schedule Agent 中的 ReAct 试点
- 当前没有把整套多 Agent 改造成通用 ReAct 框架，只在 `ScheduleAgentWorker` 内部试点了轻量 ReAct 式决策。
- 体现方式是：`ScheduleAgentWorker` 内部按“决定下一步查询 → 守卫跳步 → 执行工具 → 根据观察继续”的有限步循环推进。
- 这不是把现有多 Agent 全量切到 ReAct，而是只把 `SLOT_QUERY` 这个 Worker 从固定 if/else 查询链，改成受约束的“先决定下一步、再查、再继续”的实现。
- 典型顺序是：先识别/匹配科室，再补子科室，再查医生，再查时段并选择候选号源。
- 守卫规则会阻止跳步：例如没有日期时，不会直接去查时段；只有医生名但缺日期时，会先停在补日期，而不是错误地下钻。
- 这个试点只改变 `SLOT_QUERY` 内部实现，不改 `TriageAgentWorker`、`PolicyAgentWorker`、`ExecutionAgentWorker` 的职责边界。
- 对外协议保持不变：仍然只产出 `missing_slots_input`、`no_slot_available`、`slot_selected` 三类稳定结果，并继续通过 `ASK_USER -> SLOT_QUERY` 或 `HANDOFF -> POLICY_CHECK` 驱动后续流程。
- 前端页面也不需要改协议；`MultiAgentCoordinatorService` 仍按现有 `AgentTraceEntry` 组装 `toolLogs` 与 `agentFlows`。
- 前端可见的是压缩后的执行结果，不会直接暴露 Worker 内部每一步判断细节。

## 解释型 RAG
- 当前 RAG 只用于“为什么推荐这个”“推荐理由”“挂号规则”“就诊卡说明”“普通挂号兜底”等解释型问题，不参与写操作决策。
- 知识源来自 `docs/agent/*.md`，后端 `MultiAgentKnowledgeBase` 会按 Markdown 标题和段落切分 snippet，再做关键词检索。
- 命中知识片段后，`MultiAgentCoordinatorService` 会把压缩解释补进最终回复，并追加“知识来源”卡片。
- 实时业务事实仍以 MySQL 和真实工具查询为准；RAG 不替代号源、医生排班、就诊卡状态和挂号结果。

## 检索、观测与治理补充
- 当前 explain 链路已从纯关键词检索升级为“关键词 + 可选 embedding”的最小混合检索；默认可使用本地 `local-hash` embedding，不额外引入独立向量库。
- `MultiAgentRagService` 会优先复用 query cache，并在 embedding/LLM 超时、失败或未命中时自动回落到关键词片段回答，避免解释链路阻塞主挂号流程。
- 当前只向 RAG/模型暴露裁剪后的安全上下文，例如 `deptName`、`deptSubName`、`doctorName`、`date` 和 `pendingOrder` 摘要，不直接透传完整 raw memory。
- `MultiAgentRequestGuardService` 会限制单 session/用户的最小请求间隔与每分钟请求数，避免解释类请求被连续点击放大。
- explain 结果会把 `ragMode`、`ragHitCount`、`ragScoreMax`、`ragLatencyMs`、`ragFallbackReason`、`ragCacheHit`、`traceSize`、`chatLatencyMs` 等字段写入 `response.memory`，方便联调、测试和面试演示。

## 小程序演示建议
- 页面入口：`patient-wx/agent/chat/chat.vue`
- 推荐输入：
  - `明天口腔科`
  - `明天呼吸内科`
  - `挂张医生明天的号`
  - `查看我的挂号`
  - `查看消息`
  - `为什么推荐这个`
- 成功演示时可重点观察：
  - 当前步骤是否推进
  - Agent 执行流程是否展示
  - 是否生成“确认挂号”卡片
  - 成功后能否进入“我的挂号”查看结果
  - 快捷按钮里是否能直接进入“我的挂号 / 消息 / 就诊卡”
  - 待确认或已有候选结果时，是否出现“为什么推荐当前结果”卡片
  - 解释请求触发后，是否额外出现“知识来源”卡片
- 失败演示时可重点观察：
  - 未登录是否跳登录页
  - 未建卡是否跳建卡页
  - 无号源或参数变化时是否提示重新选号
  - 无法继续当前推荐时，是否出现“普通挂号”兜底入口
- ReAct 试点的直观体现：
  - 只说“口腔科挂号”时，会先补科室/诊室，再追问日期
  - 只说“挂张医生的号”但没日期时，不会直接乱查时段，而是先要求补日期
- 本轮 2+3 能力的直观体现：
  - 说“查看我的挂号”时，不再被当作无关闲聊，而是直接给出挂号记录入口
  - 说“查看消息”或点快捷按钮时，会直接给出消息中心入口
  - 说“为什么推荐这个”时，会基于当前 memory 中已确定的诊室 / 日期 / 医生 / 候选号源给出压缩解释，而不是暴露内部完整思考链
  - 当存在待确认结果时，会额外补充“普通挂号”兜底入口，允许用户改走传统页面

## 测试
- ReAct 试点与 explain/RAG/治理能力当前建议执行命令：
  - `mvn -f "D:/Code/code/Java/hospital/hospital/patient-wx-api-mysql/pom.xml" -Dtest=DashScopeAgentServiceTest,AgentOrchestratorServiceTelemetryTest,MultiAgentKnowledgeBaseTest,MultiAgentRagEvaluationTest,MultiAgentRagServiceTest,MultiAgentCoordinatorServiceTest,MultiAgentRequestGuardServiceTest,TriageAgentWorkerTest,ScheduleAgentWorkerTest,ExecutionAgentWorkerTest test`
- 本轮重点覆盖：
  - 缺科室或日期时返回 `missing_slots_input`
  - 只有科室时可先补科室/诊室再追问日期
  - 指定医生但缺日期时不会直接查时段
  - 无号源时返回 `no_slot_available`
  - 命中号源时仍返回 `slot_selected` 并 handoff 到 `POLICY_CHECK`
  - coordinator 仍能正常组装 `toolLogs` 与 `agentFlows`
  - explain 链路的安全上下文裁剪、离线评测样本与 request guard
  - DashScope 意图识别侧的分类重试、usage/latency 提取与降级观测

## 当前水平与差距
- 如果只看患者侧多 Agent 挂号助手，这条链路当前更接近“大型医院试点上线”水平，而不是全院正式生产级。
- 当前已具备：
  - 多 Agent 分工和流程稳定
  - 条件校验、确认挂号、失败兜底
  - 事务、幂等、防重复提交
  - 审计留痕、巡检补偿
  - 前端错误态、跳转卡片
  - 基础闭环入口：查看我的挂号、查看消息、查看就诊卡
  - 基础解释兜底：推荐理由说明、普通挂号兜底入口
  - 关键单测覆盖
- 仍需补齐的生产级能力：
  - HIS / EMR / 排班中心真实对接
  - 支付、取消、退款闭环
  - 统一认证、实名、医保链路
  - 监控告警、压测、灰度发布
  - 高可用、容灾、人工运营后台
  - 真正的 Agent 内读写闭环：例如直接在对话里展开挂号记录详情、执行取消/改期，而不只是跳转到现有页面

## 主要新增文件
- `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/controller/MultiAgentChatController.java`
- `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/service/MultiAgentCoordinatorService.java`
- `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/memory/MultiAgentMemoryService.java`
- `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/memory/RedisMultiAgentMemoryService.java`
- `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/worker/AgentWorker.java`
- `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/worker/TriageAgentWorker.java`
- `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/worker/ScheduleAgentWorker.java`
- `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/worker/PolicyAgentWorker.java`
- `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/worker/ExecutionAgentWorker.java`
- `patient-wx-api-mysql/src/test/java/com/example/hospital/patient/wx/api/agent/multi/worker/TriageAgentWorkerTest.java`
- `patient-wx-api-mysql/src/test/java/com/example/hospital/patient/wx/api/agent/multi/service/MultiAgentCoordinatorServiceTest.java`

## 测试
- 执行命令：
  - `mvn -f patient-wx-api-mysql/pom.xml "-Dtest=TriageAgentWorkerTest,MultiAgentCoordinatorServiceTest" test`
- 结果：
  - `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`
  - `BUILD SUCCESS`

## 备注
- 已修复 `agent/multi` 初始文件中的 BOM 编码问题（`\ufeff`）以通过 Java 编译。
