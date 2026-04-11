# 第三套 CC Agent 架构

## 目标

在不改动第一套与第二套代码的前提下，新增一套独立的 Claude Code 风格 Agent，对比计划式执行架构在挂号场景下的可维护性与交互表现。

## 入口

- 接口：`POST /agent/cc/chat`
- 控制器：`agent/cc/controller/ClaudeCodeAgentChatController.java`
- 小程序页面：`patient-wx/user/cc_chat/cc_chat.vue`

## 当前实现特点

- 独立包：`patient-wx-api-mysql/.../agent/cc/*`
- 独立配置前缀：`agent.cc.*`
- 独立 Redis 会话前缀：`cc_agent_session:`
- 独立前端入口，不影响第一套和第二套页面

## 当前执行模式

- 复用现有工具层查询真实科室、诊室、日期、医生、时段
- 通过独立执行器维护会话状态与确认流程
- 保留写操作确认守卫，挂号提交仍需用户确认
- 前端以第三套独立页面承接测试与对比

## 当前阶段说明

- 第三套已完成独立后端接口与独立小程序入口
- 当前版本优先保证可运行、可对比、可继续演进
- 后续可继续把执行器从第二套风格进一步收缩到更纯的 CC 计划模式
