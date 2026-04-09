# Agent 架构说明

## 目标
在不重写现有挂号业务的前提下，为患者侧增加一个可扩展的挂号助手编排层。

## 核心结构
- 后端模块：`patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent`
- 前端入口：`patient-wx/agent/chat/chat.vue`
- 接口入口：`POST /agent/chat`

## 主要组件
- `AgentChatController`
  - 对外提供 `/agent/chat`
- `AgentOrchestratorService`
  - 主编排器
  - 负责意图分发、记忆读写、自动挂号、强确认挂号
  - 负责对模型结果做二次校正
- `NoModelAgentEngine`
  - 无模型规则识别
  - 负责基础意图识别与流程推进
- `DashScopeAgentService`
  - 可选模型决策层
  - 仅负责生成结构化意图，不直接写业务
- `AgentConversationMemoryService`
  - Redis 会话记忆
- `agent/tool/*`
  - 复用现有 service 的工具封装

## 当前决策链路
1. 前端发送用户消息到 `/agent/chat`
2. 后端加载会话记忆
3. 规则引擎先给出基础动作
4. 若开启 LLM，则调用 `DashScopeAgentService` 返回结构化 JSON
5. `AgentOrchestratorService` 合并模型结果与本地规则
6. 编排层强制修正关键字段：
   - 科室症状词映射
   - 用户原话日期优先
   - 流程阶段优先
7. 根据动作调用只读工具或生成确认挂号动作
8. 返回回复、步骤、卡片、工具日志

## 这轮补充的稳定性规则
- 症状词可自动补全科室，例如“口腔难受”映射为 `口腔科`
- 用户原话中的 `今天/明天/后天` 会覆盖模型误判日期
- 医生无价格记录时，查询医生列表不再被过滤
- 当天过期时段不会被自动挂号选中

## 风险边界
- 自动挂号只做“查找最早可挂号源 + 生成确认动作”
- 真正提交挂号必须经过 `checkRegistrationCondition` 和用户确认
- 不允许模型直接决定写操作结果
