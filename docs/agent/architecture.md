# Agent 架构说明（一期）

## 设计目标
把现有患者端挂号系统改造成“可扩展的 Agent 项目”，但第一期不引入真实模型，不升级框架，不重写既有业务逻辑。

## 一期落地方式
- 后端：在 `patient-wx-api-mysql` 新增 `agent` 模块
- 前端：在 `patient-wx` 新增独立 `AI挂号助手` 页面
- 编排方式：规则引擎 + Tool 封装 + Redis 会话记忆 + 风险确认

## 后端结构
- `agent/controller/AgentChatController.java`：统一入口 `/agent/chat`
- `agent/service/AgentOrchestratorService.java`：主编排器
- `agent/service/NoModelAgentEngine.java`：无模型规则引擎
- `agent/tool/*`：复用既有 service 的工具封装
- `agent/memory/*`：Redis 短期记忆
- `agent/dto/*`：请求、响应、卡片、工具日志、确认结构

## 前端结构
- `patient-wx/agent/chat/chat.vue`：独立助手页
- `patient-wx/pages/index/index.vue`：首页入口
- `patient-wx/pages/mine/mine.vue`：个人中心入口
- `patient-wx/main.js`：新增 `agentChat` API
- `patient-wx/pages.json`：注册 `agent` 分包

## 运行流程
1. 用户进入 AI 助手页
2. 前端调用 `/agent/chat`
3. 后端读取会话记忆并确定当前阶段
4. 按阶段调用只读工具或准备确认动作
5. 用户确认后，调用挂号工具执行
6. 返回结构化结果、步骤、卡片和日志

## 为什么先不接模型
- 当前仓库还没有稳定的 AI SDK 接入
- 一期目标是先把 Agent Runtime、工具层、确认机制和前端交互跑通
- 后续可在 `NoModelAgentEngine` 前面替换为真实模型决策层
