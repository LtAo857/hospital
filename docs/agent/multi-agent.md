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
- 典型顺序是：先识别/匹配科室，再补子科室，再查医生，再查时段并选择候选号源。
- 守卫规则会阻止跳步：例如没有日期时，不会直接去查时段；只有医生名但缺日期时，会先停在补日期，而不是错误地下钻。
- 这个试点只改变 `SLOT_QUERY` 内部实现，不改 `TriageAgentWorker`、`PolicyAgentWorker`、`ExecutionAgentWorker` 的职责边界。
- 对外协议保持不变：仍然只产出 `missing_slots_input`、`no_slot_available`、`slot_selected` 三类稳定结果，并继续通过 `ASK_USER -> SLOT_QUERY` 或 `HANDOFF -> POLICY_CHECK` 驱动后续流程。
- 前端页面也不需要改协议；`MultiAgentCoordinatorService` 仍按现有 `AgentTraceEntry` 组装 `toolLogs` 与 `agentFlows`。

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
