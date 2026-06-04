# Gitee CI/CD 说明

## 目标

当前仓库使用 Gitee 托管时，可以用 Gitee Go 做轻量 CI。第一阶段只做持续集成，不做自动发布：

- 自动编译 `patient-wx-api-mysql`
- 自动运行 Agent 核心测试
- 发现多 Agent、RAG、NLU、ReAct 相关改动导致的回归问题

流水线文件：

```text
.workflow/master-pipeline.yml
.workflow/branch-pipeline.yml
.workflow/pr-pipeline.yml
```

## 当前流水线

当前 CI 触发条件分三类：

- `master-pipeline.yml`：push 到 `master` 时触发
- `branch-pipeline.yml`：push 到非 `master` 分支时触发
- `pr-pipeline.yml`：向 `master` 发起 PR 时触发

若仓库默认分支是 `main` 或其他分支，需要把这些文件里的 `master` 改成实际默认分支名。

当前执行命令：

```powershell
mvn -f patient-wx-api-mysql/pom.xml -B "-Dtest=DashScopeAgentServiceTest,AgentOrchestratorServiceTelemetryTest,MultiAgentKnowledgeBaseTest,MultiAgentRagServiceTest,MultiAgentCoordinatorServiceTest,MultiAgentRequestGuardServiceTest,TriageAgentWorkerTest,ScheduleAgentWorkerTest,PolicyAgentWorkerTest,ExecutionAgentWorkerTest" test
```

选择这批测试的原因：

- 覆盖一期 DashScope 意图识别和遥测逻辑
- 覆盖多 Agent 的 Coordinator、Triage、Schedule、Policy、Execution
- 覆盖 RAG 检索、解释和请求治理
- 避免 CI 直接依赖 MySQL、Redis、微信、腾讯云等外部环境

说明：`MultiAgentRagEvaluationTest` 属于离线评估测试，当前不纳入默认 CI。后续如果评估样本和检索阈值稳定，可以再把它加入流水线，作为坏例回归测试的一部分。

## 启用方式

1. 在 Gitee 仓库中开通 Gitee Go。
2. 确认 `.workflow/master-pipeline.yml`、`.workflow/branch-pipeline.yml`、`.workflow/pr-pipeline.yml` 已提交到仓库默认分支。
3. 根据仓库默认分支调整 `triggers` 中的 `master`。
4. 如果 Gitee Go 页面仍显示“暂无流水线”，在 Gitee Go 页面点击“新建流水线”。
5. 页面如果没有“选择已有 YAML”入口，就选择 Java/Maven 模板或空白流水线。
6. 在 Maven 构建步骤里，把构建命令改成本文档中的 Maven 测试命令。
7. 保存流水线后，Gitee Go 通常会在 `.workflow/` 下生成或更新流水线 YAML；如果生成了新文件，需要检查触发分支和 Maven 命令是否与当前配置一致。
8. push 代码后，在 Gitee Go 页面查看流水线执行日志。

## 后续扩展

如果继续生产化，可以按这个顺序增强：

1. 增加 JaCoCo 覆盖率统计，重点观察 Agent 模块覆盖率。
2. 增加 Docker Compose，一键启动 MySQL、Redis、Java 后端和 Python NLU 服务。
3. 增加 Docker 镜像构建和推送步骤。
4. 增加 K8s 部署清单，通过 ConfigMap、Secret、Deployment、Service、Ingress 管理生产发布。

## 面试说法

项目托管在 Gitee，所以工程化上可以接入 Gitee Go。当前先做轻量 CI：每次提交后自动执行 Maven 编译和 Agent 核心测试，覆盖意图识别、号源查询、规则校验、执行提交、RAG 和请求治理。这样可以保证多 Agent 链路改动后不会把历史问题重新引入。后续再扩展 Docker Compose、镜像构建和 K8s 发布，把项目从功能闭环进一步推进到可测试、可部署、可回滚的生产化体系。
