# 第二套传统 Agent 架构

## 目标

在不修改第一套现有 Agent 代码的前提下，新增一套独立的传统 Agent 实现，用于和当前项目架构做对照。

## 入口

- 接口：`POST /agent/react/chat`
- 控制器：`agent/react/controller/TraditionalAgentChatController.java`

## 架构特点

- 使用传统 ReAct 风格的“思考 -> 选工具 -> 观察 -> 再决策”循环。
- LLM 每轮只选择一个工具，不直接操作底层业务服务。
- 工具层复用现有业务封装，不改动第一套 Agent 的工具与编排代码。
- 写操作保留确认守卫，`create_registration` 在 `confirmed=true` 前不会真正执行。
- 会话记忆独立存储在 Redis，键前缀为 `react_agent_session:`，避免与第一套冲突。

## 当前提供的工具

- `get_user_card_status`
- `list_departments`
- `list_sub_departments`
- `list_register_dates`
- `list_doctors_in_day`
- `list_schedule_slots`
- `list_messages`
- `create_registration`

## 隔离方式

- 新增独立子包：`agent/react/*`
- 新增独立配置前缀：`agent.react.*`
- 新增独立接口路径：`/agent/react/chat`
- 不修改第一套的 controller、service、memory、prompt、DTO
