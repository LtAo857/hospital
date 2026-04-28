# JMeter 压测方案

## 文件说明

| 文件 | 用途 |
|------|------|
| `hospital-pressure-test.jmx` | JMeter 测试计划（GUI 打开直接可用） |
| `run-pressure-test.ps1` | PowerShell CLI 一键执行脚本 |

## 测试计划结构

```
智慧医疗压测计划
├── setUp-登录准备 (1线程)
│   ├── 患者登录 loginOrRegister → 提取 PATIENT_TOKEN → 写入全局属性
│   └── MIS 登录 → 提取 MIS_TOKEN → 写入全局属性
├── 1-只读基线(无需鉴权)
│   ├── MIS 登录 POST /hospital-api/mis_user/login
│   └── 查科室列表 POST /patient-wx-api/medical/dept/searchMedicalDeptList
├── 2-鉴权读(带token)
│   ├── 查可挂号日期 POST /patient-wx-api/registration/searchCanRegisterInDateRange
│   ├── 查医生排班 POST /patient-wx-api/registration/searchDeptSubDoctorPlanInDay
│   └── 查用户信息 GET /patient-wx-api/user/searchUserInfo
├── 3-挂号写链路(低并发)
│   └── 挂号条件校验 POST /patient-wx-api/registration/checkRegisterCondition
│       └── 断言：响应码 200
├── 4-阶梯加压(MIS登录) — 单接口阶梯压测模板
└── 监听器
    ├── 汇总报告
    ├── 聚合报告
    └── 察看结果树(仅错误)
```

## 快速开始

### GUI 模式

1. JMeter → File → Open → 选择 `hospital-pressure-test.jmx`
2. 确认 User Defined Variables 中的参数（`TARGET_HOST`、`TEST_DATE`、`DEPT_SUB_ID`）
3. 右键 `setUp-登录准备` → Start（验证 token 获取正常）
4. 确认 setUp 无报错后，点击绿色播放按钮跑全部

### CLI 模式

```powershell
cd docs/jmeter

# 默认：localhost, 100并发, 120s
.\run-pressure-test.ps1

# 自定义
.\run-pressure-test.ps1 -Concurrency 200 -Duration 180 -Target 192.168.1.100
```

## 可配置变量

在测试计划的 User Defined Variables 中修改：

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `TARGET_HOST` | localhost | 目标主机 |
| `MIS_PORT` | 8094 | MIS 后端端口 |
| `PATIENT_PORT` | 8095 | 患者端后端端口 |
| `MIS_BASE` | /hospital-api | MIS context-path |
| `PATIENT_BASE` | /patient-wx-api | 患者端 context-path |
| `READ_CONCURRENCY` | 100 | 读接口并发数 |
| `WRITE_CONCURRENCY` | 10 | 写接口并发数 |
| `DURATION` | 120 | 持续时间(秒) |
| `DEPT_SUB_ID` | 1 | 测试用诊室ID（需数据库中真实存在） |
| `TEST_DATE` | 2026-05-10 | 测试用日期（需数据库中有排班） |

## 阶梯加压建议

修改 `READ_CONCURRENCY` 后重新运行：

| 轮次 | 并发 | 持续时间 | 目标 |
|------|------|---------|------|
| 预热 | 10 | 120s | JIT 预热 |
| 基线 | 50 | 120s | 取正常 QPS 基线 |
| 中等 | 100 | 180s | 观察响应拐点 |
| 高压 | 200 | 180s | 打到 Jetty max 线程 |
| 过载 | 400 | 120s | 验证降级行为 |

## 测试账号

### MIS 登录
- username: `admin` / password: `admin123`（需数据库 `mis_user` 表中存在）

### 患者登录（微信小程序）

生产环境通过微信 `jscode2session` 换 openId，压测使用 dev 旁路：

- code 以 `test_` 开头时，直接使用 code 值作为 openId，不走微信 API
- 首次调用自动注册新用户
- 反复调用同一 test code 返回同一用户

测试 code 示例：`test_pressure`、`test_001`、`test_002`...

旁路代码位置：`patient-wx-api-mysql/.../service/impl/UserServiceImpl.java:getOpenId()`

## 压测前准备

1. 关掉 MyBatis SQL 日志：`log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl`
2. 提高日志级别：`logging.level.root: warn`
3. 提前用低并发预热 2 分钟
4. 确认 Windows 端口配置（如果并发 ≥ 200）：

```powershell
netsh int ipv4 set dynamicport tcp start=1025 num=64511
reg add "HKLM\SYSTEM\CurrentControlSet\Services\Tcpip\Parameters" /v TcpTimedWaitDelay /t REG_DWORD /d 30 /f
```

5. 确认 `TEST_DATE` 和 `DEPT_SUB_ID` 与数据库中的数据匹配

## 压测基线结果（2026-04-28）

| 并发 | 吞吐量 | 中位数 | P90 | P95 | P99 | 错误率 | 样本数 |
|------|--------|--------|-----|-----|-----|--------|--------|
| 50 | 2,385/s | 2ms | 19ms | 120ms | 227ms | 0.01% | 143,132 |
| 100 | 17,754/s | 2ms | 21ms | 44ms | — | 0.0005% | 1,065,322 |

## 常见问题

### BindException: Address already in use: connect

根因：JMeter 没有复用连接，每次请求新建 TCP 连接导致 Windows 客户端端口耗尽。

修复：
- 所有 HTTP Request 必须设置 `use_keepalive=true` + `implementation=HttpClient4`（当前 JMX 已默认配置）
- JMeter `user.properties` 中加 `httpclient4.time_to_live=60000`

### 患者登录返回"临时登陆凭证错误"

检查 `application.yml` 中 `wechat.app-id` 是否配置了真实值。压测时确保使用 `test_` 前缀的 code（如 `test_pressure`），旁路只对 `test_` 前缀生效。

### setUp 拿到 token 但后续线程组鉴权失败

确认 run 顺序：先单独跑 setUp，等绿色通过后再跑全部。setUp 失败时 `PATIENT_TOKEN` 属性为空，鉴权读线程组会全部 401。
