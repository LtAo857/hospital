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
- `patient-wx` 当前默认对接 `http://127.0.0.1:8095/patient-wx-api`
- `hospital-api-mysql` 默认端口 `8094`，context-path 为 `/hospital-api`
- `patient-wx-api-mysql` 当前默认端口 `8095`，context-path 为 `/patient-wx-api`
- 视频问诊能力需要前后端共同配置 TRTC 参数
- 管理端消息推送依赖 WebSocket；患者侧就诊提醒依赖 Quartz 定时任务
- 患者侧多 Agent 页面仍为 `patient-wx/agent/chat/chat.vue`，后端接口为 `patient-wx-api-mysql` 的 `POST /agent/multi/chat`
- 当前多 Agent 仅支持挂号相关操作，不作为通用闲聊助手使用

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

### 当前多 Agent 说明
- 前端页面仍使用：`patient-wx/agent/chat/chat.vue`
- 后端接口：`POST /agent/multi/chat`
- 当前能力范围：仅支持挂号相关操作（查科室、查医生、查号源、条件校验、确认挂号、失败兜底）
- 当前已补充的轻闭环入口：查看我的挂号、查看消息、查看就诊卡
- 当前已补充的解释与兜底能力：推荐理由说明卡、普通挂号兜底入口
- 当前确认写操作已收紧为服务端闭环：只有命中 `awaitingConfirmation + pendingOrder` 且确认参数与待确认快照一致时，才允许继续进入执行阶段；伪造确认或篡改关键字段会被拦下并清理待确认态
- 当前多 Agent 聊天入口仍是弱类型 `payload`，但已补共享规范化与结构化校验：非法整数、非法日期、非法金额不会直接抛异常，而会返回结构化错误并补 `badCaseType/badFields`
- 当前解释型 RAG 仅用于规则说明与推荐理由解释，知识源来自 `docs/agent/*.md`；实时号源、医生排班、就诊卡状态与挂号结果仍以真实工具和 MySQL 事实为准
- 当前挂号写路径已把 `requestId` 作为真正的回放/幂等键使用；成功可复用结果，处理中会拦截重复提交，参数不一致的重放会被拒绝
- 当前 `ScheduleAgentWorker` / `PolicyAgentWorker` 的只读工具调用已补“最多重试一次 + 确定性降级”；写工具 `createRegistrationOrder` 不自动重试，避免真实二次下单
- 前端可见区块：当前步骤、Agent 执行流程、可执行卡片、错误态提示
- 关键编排器：`patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/service/MultiAgentCoordinatorService.java`
- 关键 Worker 目录：`patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/worker/`
- 审计与巡检：`patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/service/MultiAgentRegistrationAuditService.java`、`patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/job/MultiAgentRegistrationRepairJob.java`
- SQL 升级脚本：`sql/patient_wx_multi_agent_registration_upgrade.sql`

#### 当前多 Agent 分工
- `MultiAgentCoordinatorService`：负责统一编排、维护 session memory、决定下一跳 stage、组装前端步骤/流程/卡片/错误态，并在 Worker 执行前做 payload 规范化与确认态预校验。
- `TriageAgentWorker`：负责识别用户是否进入挂号流程，提取日期等基础意图信息；非挂号请求会被拦回挂号入口。
- `ScheduleAgentWorker`：负责补全科室/诊室/日期，查询医生与时段号源，并挑选候选挂号单写入 `pendingOrder`。当前该 Worker 内部已试点轻量 ReAct 式决策：按“决定下一步查询 → 守卫跳步 → 执行工具 → 根据观察继续”的小循环推进，但对外仍保持 `missing_slots_input / no_slot_available / slot_selected` 和原有 handoff 契约不变；其只读工具调用默认最多重试一次，随后降级为可重试错误或普通挂号兜底。
- `PolicyAgentWorker`：负责校验登录态、就诊卡、当日上限、重复挂号等规则；通过后进入确认或执行，失败则返回结构化错误码。当前不再信任前端单独传入的确认标记，并且 `checkRegistrationCondition` 失败时会 fail-closed，阻止继续进入写路径。
- `ExecutionAgentWorker`：负责真正提交挂号，传递 `requestId/sessionId`，并把业务异常转换成结构化失败结果；执行前会再次校验确认态、执行参数和 `pendingOrder` 一致性。

### Agent 架构对比

| 维度 | 一期 | 二期（单 Agent ReAct） | 多 Agent（生产主力） |
| --- | --- | --- | --- |
| 核心定位 | 项目一期、概念验证快速落地 | 单 Agent 成熟过渡架构 | 线上生产核心链路 |
| 核心思路 | 规则编排为主，LLM 仅辅助识别用户意图 | LLM 自主完成工具选择，基于 ReAct 循环运行 | 拆分多专业 Worker，协调器统一路由流转 |
| 核心决策逻辑 | LLM 只能选择固定动作，后端二次校正 | LLM 自主决定调用工具 / 结束流程 | 协调器管控整体流程，Worker 流水线推进，入口 LLM 识别意图 |
| LLM 定位 | 意图识别辅助角色 | 系统工具调度核心 | NLU 入口 + RAG 解释，失败不影响主链路 |
| 故障降级能力 | 降级本地规则，系统能力大幅下降 | 完整降级链路，依托记忆恢复流程 | LLM 超时/低置信度自动回退规则引擎，主业务不受影响 |
| 安全体系 | 提示词约束 + 后端校验 + 操作确认 | 提示词管控 + 工具资源隔离 + 故障兜底 | 分权管控、操作确认、幂等设计、日志审计、多层降级 |
| 数据写操作管控 | 模型仅提交操作意向，最终执行由后端校验 | 提示词强制二次确认，工具层完成写入 | 统一交由专属执行 Worker 执行，服务端强制审核 |
| 新增功能成本 | 修改动作枚举、调整编排代码 | 注册新工具、补充兜底规则 | 新增 / 调整 Worker 或配套工具即可 |
| 代码开发复杂度 | 低，逻辑简单 | 中等，ReAct 逻辑有一定开发量 | 架构复杂，但工程体系完整规范 |
| 面试讲解亮点 | 讲解提示词工程约束模型输出的实践 | 拆解 ReAct 架构，区分决策逻辑与工具执行逻辑 | LLM 做 NLU + Worker 流水线做执行，演示企业级 AI 安全边界 |

> 三期 CC Agent 本质是二期的 prompt 风格变体，代码结构相同，不单独列为独立阶段。

### NLU 接入方案

多 Agent 已接入真实 LLM NLU 管道，架构如下：

```
用户说"明天牙疼挂骨科"
  → Java TriageAgentWorker
    → HttpModelIntentParser HTTP 调用 Python /infer (127.0.0.1:8001)
      → parser._llm_parse() 调 DashScope qwen-plus
        → 返回 {intent:"registration", slots:{department:"骨科", date:"明天"}, confidence:0.95}
    → 命中 isRegistrationIntent → 进入 Schedule 查询号源
```

关键文件：

- Python NLU 服务：`model-inference-demo/inference_demo/parser.py`
- Python 启动：`model-inference-demo/server_stdlib.py`
- Java NLU 接口：`patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/nlu/ModelIntentParser.java`
- Java HTTP 实现：`patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/nlu/HttpModelIntentParser.java`
- Java 调用入口：`patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/worker/TriageAgentWorker.java`
- 配置：`patient-wx-api-mysql/src/main/resources/application.yml` 的 `agent.multi.model-parser-*`
- 对外暴露：`MultiAgentCoordinatorService.exposeMemory` 白名单含 `nluIntent/nluSource/nluModel/nluConfidence/nluLatencyMs`

设计要点：

- Java 管业务，Python 管理解，HTTP + JSON 解耦
- LLM 超时或低置信度自动回退关键词匹配，业务不受影响
- Python 侧支持规则引擎 / 真 LLM 双模式，`parser.py` 中改 `self.llm_enabled` 即可切换
- Java 侧通过 `@Autowired(required = false)` 可选注入，Python 没起也不报错

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

## 测试与压测

### 微信登录 dev 旁路
- 患者端 `loginOrRegister` 在 `getOpenId()` 中已补 dev 旁路：code 以 `test_` 开头时，直接将 code 值作为 openId 使用，不走微信 API。
- 改动位置：`patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/service/impl/UserServiceImpl.java:73`
- 首次调用自动注册新用户，重复调用同一 code 返回同一用户。压测时可用 `test_pressure`、`test_001` 等。

### JMeter 压测
- 压测方案与基线数据：`docs/pressure-testing.md`
- JMeter 测试计划：`docs/jmeter/hospital-pressure-test.jmx`（GUI 打开直接可用）
- CLI 执行脚本：`docs/jmeter/run-pressure-test.ps1`
- 详细说明：`docs/jmeter/README.md`
- 测试计划包含 4 个线程组：只读基线、鉴权读、挂号写链路、阶梯加压
- setUp 线程组负责获取患者/MIS token 并写入全局属性，后续线程组复用
- 所有 HTTP Request 默认已配置 `use_keepalive=true` + `implementation=HttpClient4`

## 协作约束
- 先读现有 README 和关键入口文件，再改代码。
- 除非用户特别要求，否则优先修改 MySQL 版本相关目录，不默认改 HBase 版本。
- 不要把 `patient-wx` 与 `hospital-vue` 混为一个前端项目：前者是患者小程序，后者是 MIS 管理端。
- 不在文档、注释、提交信息中扩散真实敏感配置。
- 如果文档与代码不一致，以当前代码为准，并同步修正文档。

## 敏感配置提醒
两个后端的 `application.yml` 都包含数据库、Redis、微信、腾讯云/TRTC 等配置项。阅读时可用来定位配置位置，但不能直接把其中的值当作可复用凭证。若后续需要补文档，只描述“改哪里”，不要抄写真实密钥。

## Karpathy 协作补充
- 先想再写：如果需求存在歧义、信息缺失或有多种解释，先明确假设或先问清，不要静默选择一种理解直接实现。
- 简单优先：默认采用能解决当前问题的最小改动，不为单次场景额外设计抽象、扩展点或配置化。
- 手术式修改：只改与当前需求直接相关的代码，不顺手重构无关逻辑、注释或格式；只清理因本次改动产生的无用代码。
- 目标驱动：非琐碎任务优先给出简短步骤与验证方式，完成后以可验证结果判断是否达成目标。

## Claude Code 本地 Skills/命令
- 本仓库已接入本地 Karpathy 风格协作能力。
- Skills 文件：`.claude/skills/karpathy-guidelines/SKILL.md`
- 命令文件：`.claude/commands/karpathy.md`
- 在 Claude Code 中可直接使用 `/karpathy`。
- 也可带参数调用，例如：`/karpathy patient-wx 挂号流程`、`/karpathy hospital-vue 页面重构`
- 该命令用于在当前任务中强化“先想再写、简单优先、手术式修改、目标驱动”的执行方式。
- 当前仓库还补充了 5 个仅面向项目实现细节的本地 Skills：
  - `/multi-agent-map`：梳理当前多 Agent 挂号架构、Worker 分工、阶段流转与局部 ReAct 使用位置
  - `/rag-boundary-check`：核对当前 RAG 的知识源、混合检索方式、解释型边界与非职责范围
  - `/registration-trace`：追踪真实挂号链路，从确认到校验、提交、幂等锁、审计和补偿的完整路径
  - `/agent-guardrail-check`：检查 AI 挂号流程中的阶段守卫、写操作确认、请求治理、会话记忆与兜底机制
  - `/doc-code-drift`：检查 README、`docs/agent/*.md` 与当前多 Agent / RAG 实现之间是否存在文档漂移
- 这 5 个 Skills 只服务于当前医院项目实现细节，不用于简历、面经或通用表达类任务。
