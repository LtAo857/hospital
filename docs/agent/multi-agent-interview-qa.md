# 多 Agent / RAG 面试问答

## 适用范围
- 项目：`patient-wx-api-mysql` + `patient-wx`
- 场景：患者侧挂号多 Agent
- 目标：回答 embedding、RAG、Function Calling、对话记忆、协议、流式响应、前后端分工这类高频问题

---

## 1. Embedding 向量检索的原理是什么？如何保证检索准确性？

### 简答版
向量检索的核心是把查询和文本都映射成高维向量，再通过相似度计算找到最接近的片段。当前项目不是纯向量库方案，而是“关键词 + embedding”的轻量混合检索，而且只用于解释型 RAG，不参与实时挂号写决策。准确性主要靠受控知识源、合理切片、混合召回、相似度阈值，以及把 RAG 和真实业务事实隔离开。

### 展开版
我们当前的知识源不是全网数据，也不是医院所有业务表，而是 `docs/agent/*.md` 这批受控文档。系统会先把 markdown 按标题和段落切成 snippet，再先做关键词召回，随后结合 embedding 相似度做重排。默认 embedding 甚至不一定走外部模型，而是本地 `local-hash` 方案：把字符和 n-gram 做哈希映射，再归一化成固定维度向量；如果开配置，也支持走远程 embedding HTTP 接口。最终用余弦相似度筛选命中片段，低于阈值的直接过滤。更关键的是，RAG 只负责“为什么推荐这个”“挂号规则解释”这类说明问题，不负责判断实时号源、就诊卡状态和最终挂号结果，所以不会把解释链路直接变成写决策链路。

### 代码落点
- `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/rag/MultiAgentKnowledgeBase.java`
- `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/rag/EmbeddingVectorIndex.java`
- `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/rag/EmbeddingClient.java`

---

## 2. Function Calling 如何解析用户意图？你们项目里有用到 Function Calling 吗？

### 简答版
原生 Function Calling 的思路是：模型先识别用户意图，再从预定义函数里选一个，并按 schema 输出结构化参数。当前项目没有直接用 OpenAI/Claude 这种原生 Function Calling，而是用后端的 coordinator + worker 分阶段解析意图，相当于把 function router 放在服务端。

### 展开版
当前多 Agent 不是让模型直接返回 `tool_calls`，而是由 `TriageAgentWorker` 先识别用户是要挂号、查看消息、查看就诊卡，还是请求解释；然后再把请求 handoff 给 `Schedule / Policy / Execution` 这些阶段化 worker。真实工具调用发生在 Java tool / service 层，比如查医生、查号源、查挂号条件、提交挂号，都是后端明确控制的，不让模型直接决定写操作。旧的一期 `DashScopeAgentService` 虽然会让模型返回 JSON，但那也是普通 chat completion + `response_format=json_object`，不是原生 Function Calling 协议。

### 代码落点
- `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/worker/TriageAgentWorker.java`
- `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/tool/RegistrationAgentTools.java`
- `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/service/DashScopeAgentService.java`

---

## 3. 对话记忆功能是怎么实现的？

### 简答版
我们用的是 Redis 会话记忆。它不是保存所有聊天全文，而是按 `sessionId` 保存结构化上下文，比如当前阶段、已选科室、日期、候选挂号单、确认状态和错误状态。

### 展开版
每次请求进来，coordinator 会先根据 `sessionId` 从 Redis 读 memory，然后把 worker 返回的 `memoryPatch` 合进去，最后再写回 Redis。TTL 默认是 30 分钟，所以它更像一个“会话级状态机存储”，不是永久知识库。对于挂号这种强流程场景，结构化状态比保存全文对话更稳定，也更容易做确认闭环和幂等校验。

### 代码落点
- `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/memory/RedisMultiAgentMemoryService.java`
- `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/service/MultiAgentCoordinatorService.java`
- `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/config/MultiAgentProperties.java`

---

## 4. 如何将文本导入向量数据库？切割的依据是什么？PDF 文件怎么处理的？和普通文本有区别吗？

### 简答版
当前项目里其实没有独立向量数据库。现在的做法是直接读取 `docs/agent/*.md`，按 markdown 标题和段落切块，构建进程内知识库和向量缓存。PDF 入库链路当前没有做。

### 展开版
现在的“导入”不是传统 ETL 入库到 Milvus、PGVector 或 ES Vector，而是启动后扫描 `docs/agent` 目录下的 markdown 文件。切片规则主要是：
1. 按 markdown 标题识别语义层级；
2. 遇到空行或新标题时 flush 一个 block；
3. 把列表、表格做归一化后合并成 snippet。

这样切的好处是片段边界更贴近业务语义，而不是简单按固定 token 长度硬切。至于 PDF，原理上一般要先抽文本，如果是扫描版还要先 OCR，再做标题识别、去页眉页脚和分块切片；但当前仓库没有这条 PDF pipeline，所以这里要诚实回答“暂未实现”。

### 代码落点
- `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/rag/MultiAgentKnowledgeBase.java`

---

## 5. 对话记忆是所有数据都保存吗？超出限度怎么办？

### 简答版
不是所有数据都保存。当前主要保存流程需要的结构化状态，超出会话范围靠 Redis TTL 回收；送进 RAG 前还会做 memory 白名单裁剪和长度截断。

### 展开版
我们不会把所有历史对话原文无限堆进 memory。当前重点保存的是 `stage`、`deptId/deptSubId`、`doctorId`、`date`、`pendingOrder`、`awaitingConfirmation`、`errorCode` 这些流程字段。即便 memory 里还有更多内容，进入 RAG 时也会先走 `sanitizeMemory()`，只保留 `deptName`、`doctorName`、`date`、`pendingOrder` 摘要这类安全字段；随后再用 `ragMaxContextChars` 做长度截断。也就是说，这套设计不是“无限上下文记忆”，而是“结构化会话状态 + 安全裁剪”。

### 代码落点
- `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/rag/MultiAgentRagService.java`
- `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/service/MultiAgentCoordinatorService.java`

---

## 6. 非阻塞式响应是怎么实现的？需要引入什么依赖？

### 简答版
当前项目其实没有做真正的后端非阻塞流式响应。现在是同步 HTTP 返回完整 JSON，前端再用打字机效果模拟“流式输出”。

### 展开版
后端 `/agent/multi/chat` 是普通 REST POST，请求到了以后同步执行 coordinator，然后一次性返回结果。前端看到像“逐字出现”的效果，不是 SSE，也不是 WebSocket token 流，而是页面里用 `setInterval` 每 25ms 往气泡里追加一个字符。所以现在这版不需要额外引入 SSE 或 WebFlux 依赖。如果后面真要做后端流式：Spring MVC 可以用 `SseEmitter` / `ResponseBodyEmitter`；如果走响应式链路，再引 `spring-boot-starter-webflux`。

### 代码落点
- `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/controller/MultiAgentChatController.java`
- `patient-wx/agent/chat/chat.vue`

---

## 7. 整个项目是基于什么协议的？为什么不用 SSE？你负责什么部分？前端内容是你自己实现的吗？

### 简答版
当前多 Agent 主链路是 HTTP/JSON 的 REST 协议，不是 SSE。不用 SSE 的原因是：当前是阶段式短响应场景，重点在写安全和流程闭环，不在 token 级流式输出；前端的实时感先用伪流式解决，复杂度更低。

### 展开版
小程序端通过 `uni.request` 发 HTTP 请求，后端 Spring Boot Controller 返回 JSON，这就是当前最主的协议形态。之所以没上 SSE，主要是因为：
1. 当前返回内容不长，更多是阶段结果，不是长篇 token streaming；
2. 小程序端先用同步 JSON + 卡片交互更稳；
3. 现阶段优先级更高的是确认闭环、参数校验、幂等、审计补偿这些业务安全能力。

关于“你负责什么部分”，建议回答：我主要负责患者侧多 Agent 的后端编排和加固，包括意图分流、Schedule / Policy / Execution 编排、Redis 会话记忆、服务端确认闭环、payload 校验、`requestId` 幂等回放、审计 trace、坏例留痕、只读工具重试降级和测试补齐。

关于“前端是不是你自己实现的”，建议按实际情况二选一：
- 如果这页前端是你自己做的：可以直接说“这页多 Agent 页面是我自己落的，包括消息区、步骤区、Agent flow、操作卡片、确认弹窗和打字机式展示。”
- 如果你主要负责后端：可以说“前端页面不是我一个人从 0 独立包完，但这页联调和交互落地我有参与，尤其是确认卡片、错误态和 agent 结果展示这部分。”

### 代码落点
- `patient-wx/main.js`
- `patient-wx/agent/chat/chat.vue`
- `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/`

---

## 一句话收口

这套系统我最强调的不是“用了多少 AI 名词”，而是把 AI 放在合适边界里：解释能力可以走 RAG，但真实挂号事实还是以 MySQL 和业务服务为准；意图识别可以带一点模型能力，但真正的写操作必须经过后端确认闭环、参数校验、幂等、审计和补偿。
