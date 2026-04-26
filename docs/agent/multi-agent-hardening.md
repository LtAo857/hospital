# Multi-Agent 挂号链路加固说明

## 范围
- 本轮只加固患者侧多 Agent 挂号链路，不把它扩展成通用 Agent 平台。
- 入口仍是 `POST /agent/multi/chat`，前端页面仍是 `patient-wx/agent/chat/chat.vue`。
- 本轮没有引入 MCP、在线学习或通用工具打分排序。

## 本轮补齐的 5 个点

### 1. 写操作确认改成服务端闭环
- `MultiAgentCoordinatorService` 会先规范化聊天 `payload`，并在确认动作进入 Worker 前做一次服务端确认校验。
- 只有同时满足下面条件，才允许确认请求继续进入写路径：
  - `memory.awaitingConfirmation == true`
  - `memory.pendingOrder` 非空
  - 本次确认 `payload` 的关键字段与 `pendingOrder` 一致
- 若前端伪造 `confirmed=true` 或 `CREATE_REGISTRATION`，但当前不处于待确认态，或关键字段已变化：
  - 返回结构化错误
  - 清理 `awaitingConfirmation` / `pendingOrder`
  - 记录 `badCaseType=confirmation_mismatch`
- `PolicyAgentWorker` 与 `ExecutionAgentWorker` 都会再次做防御性校验，不再信任前端单独传来的确认标记。

### 2. 给弱类型 payload 补共享规范化与校验
- 新增 `MultiAgentRegistrationPayloadValidator`，只服务当前挂号多 Agent 路径。
- 覆盖三类输入：
  - 聊天态 `payload`
  - 确认态 `payload`
  - 执行态挂号单
- 统一处理：
  - 字符串 trim
  - 正整数安全解析
  - `yyyy-MM-dd` 日期规范化
  - `slot` 范围校验
  - `amount` 金额格式校验
  - `confirmed` 布尔归一化
- 非法数字、非法日期、非法金额不再把异常抛到 Worker/Service 外，而是返回结构化错误，并补 `badFields` / `badCaseType`。

### 3. 把 `requestId` 变成真正的回放键，并补坏例留痕
- `RegistrationServiceImpl.registerMedicalAppointment()` 在真正进入写路径前，会先按 `requestId` 查 audit。
- 当前回放决策：
  - `SUCCESS`：直接复用上一次成功结果，不重复写库
  - `RESERVED`：返回“处理中，请勿重复提交”
  - `FAIL` / `COMPENSATED`：仅当本次关键参数与原请求一致时才允许继续重试
  - 参数不一致：拒绝回放
- audit `trace_json` 从简单快照扩成嵌套结构，至少包含：
  - `request`
  - `confirmation`
  - `failure`
  - `replay`
- telemetry 会额外统计：
  - `errorCode`
  - `badCaseType`
  - `retryable`
  - `replayDecision`

### 4. 只读工具最多重试一次，再做确定性降级
- `ScheduleAgentWorker` 和 `PolicyAgentWorker` 的只读工具调用现在都会：
  - 首次失败立即重试一次
  - 第二次仍失败时进入确定性降级
- `ScheduleAgentWorker` 的降级策略：
  - 保留已知上下文
  - 返回可重试错误
  - 允许用户改走普通挂号
- `PolicyAgentWorker` 的降级策略：
  - fail-closed
  - 不允许继续进入 `EXECUTE_APPOINTMENT`
- `createRegistrationOrder` 这种写工具不自动重试，避免真实二次下单。

### 5. 增加对抗、回放与鲁棒性测试
- 新增/补齐的重点测试覆盖：
  - 未处于待确认态时伪造确认
  - 待确认后篡改 `date/slot/scheduleId/amount` 再确认
  - 非法数字串、非法日期、非法金额
  - 只读工具抛异常后的“重试一次 + 降级”
  - 同一 `requestId` 的成功复用、处理中拦截、参数不一致拒绝回放
- 当前重点测试文件包括：
  - `MultiAgentCoordinatorServiceTest`
  - `ScheduleAgentWorkerTest`
  - `PolicyAgentWorkerTest`
  - `ExecutionAgentWorkerTest`
  - `MultiAgentRegistrationAuditServiceTest`
  - `RegistrationServiceImplTest`

## 主要涉及文件
- 编排与确认守卫：
  - `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/service/MultiAgentCoordinatorService.java`
- 共享校验器：
  - `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/support/MultiAgentRegistrationPayloadValidator.java`
- 条件校验阶段：
  - `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/worker/PolicyAgentWorker.java`
- 执行阶段：
  - `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/worker/ExecutionAgentWorker.java`
- 真实写路径与回放：
  - `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/service/impl/RegistrationServiceImpl.java`
- 审计与 trace：
  - `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/service/MultiAgentRegistrationAuditService.java`
- telemetry：
  - `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/service/MultiAgentTelemetryService.java`

## 前端可见变化
- 确认信息失效或被篡改时，不会继续写库，而是要求用户重新选号。
- 参数格式错误时，会返回结构化错误，而不是直接抛异常。
- explain / 挂号结果之外，`response.memory` 现在还能暴露更适合联调的字段，例如：
  - `badCaseType`
  - `badCaseStage`
  - `badFields`
  - `replayDecision`

## 当前边界
- 当前多 Agent 仍只支持挂号相关流程，不是通用闲聊助手。
- RAG 仍只负责规则说明和推荐解释，不参与写操作决策。
- 当前已更适合试点/上线，但还不是全院正式生产级：支付、取消/退款、统一认证、HIS/EMR 深度对接、压测和高可用仍需后续补齐。

## 回归测试命令
```bash
mvn -f "D:/Code/code/Java/hospital/hospital/patient-wx-api-mysql/pom.xml" -Dtest=MultiAgentCoordinatorServiceTest,ScheduleAgentWorkerTest,PolicyAgentWorkerTest,ExecutionAgentWorkerTest,MultiAgentRegistrationAuditServiceTest,RegistrationServiceImplTest test
```
