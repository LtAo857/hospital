# 压测方案

## 环境概览

| 服务 | 端口 | context-path | 容器线程 |
|------|------|-------------|---------|
| hospital-api-mysql | 8094 | `/hospital-api` | Jetty acceptors=4, selectors=8, min=8, max=200 |
| patient-wx-api-mysql | 8095 | `/patient-wx-api` | 同上 |
| Redis | 6379 | - | jedis pool max-active=1000, max-idle=16 |
| MySQL | 3306 | - | Druid 连接池（默认配置） |

---

## 压测前准备

### 1. 关掉 MyBatis SQL 日志

两个后端 `application.yml` 中临时改为：

```yaml
mybatis:
  configuration:
    log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl
```

### 2. 提高日志级别

```yaml
logging:
  level:
    root: warn
```

### 3. 预热 JVM

正式压测前先用低并发跑 2 分钟让 JIT 预热。

### 4. 确认目标机器 ulimit

```bash
ulimit -n   # 至少 65535
```

---

## 接口清单（按压测难度分层）

### 无需鉴权（直接打）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/hospital-api/mis_user/login` | POST | MIS 登录 |
| `/patient-wx-api/medical/dept/searchMedicalDeptList` | POST | 查科室列表 |

### 需 Sa-Token 鉴权（先登录取 token）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/hospital-api/medical/dept/searchAll` | GET | 查全部科室 |
| `/patient-wx-api/registration/searchCanRegisterInDateRange` | POST | 查可挂号日期 |
| `/patient-wx-api/registration/searchDeptSubDoctorPlanInDay` | POST | 查某天医生排班 |
| `/patient-wx-api/user/searchUserInfo` | GET | 查用户信息 |

### 写 + 事务（最重，谨慎加压）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/patient-wx-api/registration/checkRegisterCondition` | POST | 挂号条件校验 |
| `/patient-wx-api/registration/registerMedicalAppointment` | POST | 提交挂号 |
| `/patient-wx-api/agent/multi/chat` | POST | 多 Agent 挂号（含 Redis 锁 + 事务 + 审计） |

---

## 方案一：wrk 快速压测

### 安装

```bash
# Ubuntu/Debian
sudo apt install wrk

# macOS
brew install wrk

# Windows 用 WSL 或直接装 JMeter
```

### 1. 先打 MIS 登录（获取基线）

```bash
# 单接口只读基线——先打 MIS 登录
wrk -t4 -c100 -d60s \
  -s wrk-scripts/mis_login.lua \
  http://localhost:8094/hospital-api/mis_user/login
```

`wrk-scripts/mis_login.lua`：

```lua
wrk.method = "POST"
wrk.headers["Content-Type"] = "application/json"
wrk.body = '{"username":"admin","password":"admin123"}'
```

### 2. 打患者端查科室（无需登录）

```bash
wrk -t4 -c100 -d60s \
  -s wrk-scripts/search_dept.lua \
  http://localhost:8095/patient-wx-api/medical/dept/searchMedicalDeptList
```

`wrk-scripts/search_dept.lua`：

```lua
wrk.method = "POST"
wrk.headers["Content-Type"] = "application/json"
wrk.body = '{"page":1,"length":10}'
```

### 3. 带 token 的鉴权接口

```bash
# 先取一次 token
TOKEN=$(curl -s -X POST http://localhost:8095/patient-wx-api/user/loginOrRegister \
  -H "Content-Type: application/json" \
  -d '{"code":"test","nickname":"test","photo":"","sex":"男"}' \
  | jq -r '.token')

# 用 token 压测
wrk -t4 -c100 -d60s \
  -H "token: $TOKEN" \
  -s wrk-scripts/search_user.lua \
  http://localhost:8095/patient-wx-api/user/searchUserInfo
```

`wrk-scripts/search_user.lua`：

```lua
wrk.method = "GET"
-- token 由命令行 -H 传入，这里只设 Content-Type
wrk.headers["Content-Type"] = "application/json"
```

### 4. 阶梯加压脚本

```bash
#!/bin/bash
# stair_step_test.sh —— 逐步加压，观察拐点

ENDPOINT="http://localhost:8094/hospital-api/mis_user/login"
DURATION=60
THREADS=4

for c in 10 50 100 200 400; do
  echo "=== 并发: $c ==="
  wrk -t$THREADS -c$c -d${DURATION}s \
    -s wrk-scripts/mis_login.lua \
    $ENDPOINT 2>&1 | grep -E "Requests/sec|Latency|errors"
  echo ""
  sleep 10
done
```

---

## 方案二：JMeter 完整流程压测

### 测试计划结构

```
智慧医疗压测计划
├── 1. 线程组-只读基线（无需鉴权）
│   ├── HTTP请求默认值（localhost:8094）
│   ├── POST /hospital-api/mis_user/login
│   └── POST /patient-wx-api/medical/dept/searchMedicalDeptList
│
├── 2. 线程组-鉴权读（带 token）
│   ├── setUp Thread Group（先登录取 token）
│   │   └── POST /hospital-api/mis_user/login → 提取 token 到变量
│   ├── HTTP请求默认值 + HTTP Header Manager（token: ${token}）
│   ├── GET /hospital-api/medical/dept/searchAll
│   ├── POST /patient-wx-api/registration/searchCanRegisterInDateRange
│   └── POST /patient-wx-api/registration/searchDeptSubDoctorPlanInDay
│
├── 3. 线程组-挂号写链路（低并发）
│   ├── setUp Thread Group（登录取 token）
│   ├── POST checkRegisterCondition
│   ├── POST registerMedicalAppointment
│   └── 断言：返回码 200 + result=true
│
└── 4. 监听器
    ├── 聚合报告
    ├── 响应时间图
    ├── 活动线程数随时间变化
    └── 事务吞吐量
```

### JMeter 关键配置

| 参数 | 建议值 | 说明 |
|------|--------|------|
| ThreadGroup.ramp_up | 并发数/10 秒 | 避免瞬间冲垮 |
| 循环次数 | 勾选"永远" + 设置 Duration | 控制总时长 |
| HTTP Request.Connect Timeout | 3000ms | 连接超时 |
| HTTP Request.Response Timeout | 10000ms | 响应超时 |
| CSV Data Set Config | 准备 1000 行测试数据 | 参数化 |

### 参数化数据准备

挂号压测需要准备真实数据，避免全部命中缓存或全部失败：

```csv
# test_data.csv —— 挂号条件校验参数
deptSubId,doctorId,date
1,1,2026-05-01
1,2,2026-05-01
2,3,2026-05-02
2,4,2026-05-02
...
```

### 运行 JMeter（命令行模式，适合服务器）

```bash
# 先 GUI 编辑保存 test-plan.jmx，再命令行跑
jmeter -n -t test-plan.jmx -l result.jtl -e -o report/

# -n  非 GUI 模式
# -t  测试计划文件
# -l  原始结果文件
# -e  生成报告
# -o  报告输出目录
```

---

## 监控命令

### JVM

```bash
# 找到 Java 进程 PID
jps -l | grep -E "HospitalApi|PatientWxApi"

# GC 监控（每秒一次）
jstat -gc <pid> 1000

# 线程状态
jstack <pid> | grep -E "BLOCKED|WAITING" | wc -l
```

### Redis

```bash
# 连接数
redis-cli -h 62.234.37.187 -p 6379 -a abc123456 INFO clients

# 内存与命中率
redis-cli -h 62.234.37.187 -p 6379 -a abc123456 INFO stats | grep -E "keyspace_hits|keyspace_misses|evicted_keys"
```

### MySQL

```sql
-- 当前连接数
SHOW STATUS LIKE 'Threads_connected';

-- 慢查询（压测期间）
SHOW VARIABLES LIKE 'slow_query_log';
SHOW VARIABLES LIKE 'long_query_time';

-- 锁等待
SELECT * FROM information_schema.INNODB_LOCK_WAITS;
```

### 系统级

```bash
# CPU + 内存
top -p <pid>

# 网络连接状态
ss -s
netstat -an | grep -E "8094|8095" | wc -l
```

---

## 压测阶梯与目标

| 阶段 | 并发 | 持续时间 | 目标 | 通过标准 |
|------|------|---------|------|---------|
| 预热 | 10 | 120s | JIT 预热 | 无报错 |
| 基线 | 50 | 120s | 拿到正常 QPS 基线 | 错误率 < 0.1% |
| 中等 | 100 | 180s | 观察响应时间拐点 | P99 < 1s |
| 高压 | 200 | 180s | 打到 Jetty max 线程 | 错误率 < 1%，无雪崩 |
| 过载 | 400 | 120s | 验证降级行为 | 有失败但不崩溃，恢复后正常 |

### 关注指标

| 指标 | 含义 | 红线 |
|------|------|------|
| QPS | 每秒请求数 | 取基线的 80% 作为安全水位 |
| P50/P99 Latency | 响应时间分位值 | P99 < 2000ms |
| Error Rate | 错误率 | < 1% |
| Jetty 线程使用率 | active/max | < 80% |
| Redis 连接数 | - | < max-active 的 80% |
| GC 暂停时间 | - | 单次 < 200ms |

---

## 常见瓶颈与应对

| 瓶颈 | 表现 | 调整方向 |
|------|------|---------|
| Jetty 线程打满 | 请求排队，P99 飙升 | 调大 `server.jetty.threads.max` |
| Redis 连接耗尽 | `JedisConnectionException` | 调大 `spring.redis.jedis.pool.max-active` |
| MySQL 连接池满 | `CannotGetJdbcConnectionException` | 调大 Druid `maxActive` |
| MyBatis 日志 IO | CPU 不高但 RT 高 | 确认已切到 `NoLoggingImpl` |
| 慢 SQL | 特定接口 RT 尖刺 | 加索引，确认 SQL 走索引 |
| GC 频繁 | CPU 间歇性飙升 | 调大堆内存，检查是否有内存泄漏 |
