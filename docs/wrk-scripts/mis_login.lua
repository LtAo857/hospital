-- MIS 登录压测脚本
-- 用法: wrk -t4 -c100 -d60s -s mis_login.lua http://localhost:8094/hospital-api/mis_user/login

wrk.method = "POST"
wrk.headers["Content-Type"] = "application/json"
wrk.body = '{"username":"admin","password":"admin123"}'
