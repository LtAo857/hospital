#!/bin/bash
# 阶梯加压脚本 —— 逐步提升并发，观察 QPS/Latency 拐点
#
# 用法: bash stair_step_test.sh
# 可修改下面的 ENDPOINT 和 SCRIPT 指向你要压测的接口

ENDPOINT="${1:-http://localhost:8094/hospital-api/mis_user/login}"
SCRIPT="${2:-mis_login.lua}"
DURATION=60
THREADS=4
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "目标: $ENDPOINT"
echo "脚本: $SCRIPT"
echo "=============================="

for c in 10 50 100 200 400; do
  echo ""
  echo "=== 并发=$c 线程=$THREADS 持续=${DURATION}s ==="
  wrk -t$THREADS -c$c -d${DURATION}s \
    -s "$SCRIPT_DIR/$SCRIPT" \
    "$ENDPOINT" 2>&1 | grep -E "Requests/sec|Latency|Socket errors|Non-2xx"
  echo ""
  sleep 10
done

echo "=============================="
echo "阶梯加压完成"
