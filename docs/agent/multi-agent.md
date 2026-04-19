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
