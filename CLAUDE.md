# CLAUDE.md

## 仓库范围
本仓库同时保留了 HBase 版与 MySQL 版实现。日常开发、排错和功能说明优先关注 MySQL 链路：
- `hospital-api-mysql`
- `patient-wx-api-mysql`
- `hospital-vue`
- `patient-wx`

如果用户没有特别说明，一般默认围绕这 4 个目录展开分析或修改。

## 子项目速览
### 1. hospital-api-mysql
- 角色：MIS 管理端/医生/管理员后端。
- 技术栈：Spring Boot 2.7、MyBatis、Sa-Token、Redis、Quartz、WebSocket、Jetty。
- 启动入口：`hospital-api-mysql/src/main/java/com/example/hospital/api/HospitalApiApplication.java`
- 关键配置：`hospital-api-mysql/src/main/resources/application.yml`
- 常见阅读路径：`controller/` → `service/` → `db/` → `config/`
- 额外关注：WebSocket 位于 `hospital-api-mysql/src/main/java/com/example/hospital/api/socket/WebSocketService.java`

### 2. patient-wx-api-mysql
- 角色：患者微信小程序后端。
- 技术栈：Spring Boot 2.7、MyBatis、Sa-Token、Redis、Quartz、Jetty。
- 启动入口：`patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/PatientWxApiApplication.java`
- 关键配置：`patient-wx-api-mysql/src/main/resources/application.yml`
- 常见阅读路径：`controller/` → `service/` → `db/` → `config/`
- 额外关注：每日就诊提醒配置位于 `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/config/QuartzConfig.java`

### 3. hospital-vue
- 角色：MIS 管理端前端，面向管理员与医生。
- 技术栈：Vue3、Vite、Element Plus、vue-router、jQuery Ajax、WebSocket、TRTC。
- 入口文件：`hospital-vue/src/main.js`
- 路由文件：`hospital-vue/src/router/index.js`
- 页面目录：`hospital-vue/src/views/`
- 当前约定：全局 `$http` 封装在 `src/main.js`，页面通常直接发接口请求。

### 4. patient-wx
- 角色：患者端 UniApp/微信小程序。
- 技术栈：uni-app、Vue2、uView、微信 OCR 插件、TRTC。
- 入口文件：`patient-wx/main.js`
- 页面配置：`patient-wx/pages.json`
- 小程序配置：`patient-wx/manifest.json`
- 当前约定：接口地址、文件地址、token 与 `ajax()` 请求封装都在 `main.js`。

## 联调关系
- `hospital-vue` 默认对接 `http://localhost:8094/hospital-api`
- `patient-wx` 默认对接 `http://127.0.0.1:8092/patient-wx-api`
- `hospital-api-mysql` 默认端口 `8094`，context-path 为 `/hospital-api`
- `patient-wx-api-mysql` 默认端口 `8092`，context-path 为 `/patient-wx-api`
- 视频问诊能力需要前后端共同配置 TRTC 参数
- 管理端消息推送依赖 WebSocket；患者侧就诊提醒依赖 Quartz 定时任务
- 患者侧 Agent 一期入口为 `patient-wx/agent/chat/chat.vue`，后端接口为 `patient-wx-api-mysql` 的 `/agent/chat`

## Agent 模块导航
### patient-wx-api-mysql
- Agent 入口：`patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/controller/AgentChatController.java`
- 主编排器：`patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/service/AgentOrchestratorService.java`
- 无模型引擎：`patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/service/NoModelAgentEngine.java`
- Tool 封装：`patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/tool/`
- 会话记忆：`patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/memory/`
- 配置项：`patient-wx-api-mysql/src/main/resources/application.yml` 下的 `agent.*`

### patient-wx
- Agent 页面：`patient-wx/agent/chat/chat.vue`
- 前端 API：`patient-wx/main.js` 中的 `api.agentChat`
- 页面注册：`patient-wx/pages.json`
- 首页入口：`patient-wx/pages/index/index.vue`
- 个人中心入口：`patient-wx/pages/mine/mine.vue`

## 推荐阅读顺序
### 后端问题
1. 先看启动入口类是否扫描到对应包。
2. 再看 `application.yml` 中端口、数据库、Redis、存储配置。
3. 然后沿着 `controller` → `service` → `mapper/xml` 阅读。
4. 涉及实时通知时补看 `WebSocketService`；涉及就诊提醒时补看 `QuartzConfig` 和相关 job。

### hospital-vue 问题
1. 先看 `src/main.js` 里的 `baseUrl`、`$http`、WebSocket 配置。
2. 再看 `src/router/index.js` 确认页面入口与权限拦截。
3. 最后看 `src/views/` 下目标页面。

### patient-wx 问题
1. 先看 `main.js` 中的 `baseUrl`、`api`、`ajax()`、token 存储。
2. 再看 `pages.json` 找页面所属主包/分包。
3. 最后看对应页面目录，例如 `registration/`、`video_diagnose/`、`display/`、`user/`。

## 协作约束
- 先读现有 README 和关键入口文件，再改代码。
- 除非用户特别要求，否则优先修改 MySQL 版本相关目录，不默认改 HBase 版本。
- 不要把 `patient-wx` 与 `hospital-vue` 混为一个前端项目：前者是患者小程序，后者是 MIS 管理端。
- 不在文档、注释、提交信息中扩散真实敏感配置。
- 如果文档与代码不一致，以当前代码为准，并同步修正文档。

## 敏感配置提醒
两个后端的 `application.yml` 都包含数据库、Redis、微信、腾讯云/TRTC 等配置项。阅读时可用来定位配置位置，但不能直接把其中的值当作可复用凭证。若后续需要补文档，只描述“改哪里”，不要抄写真实密钥。