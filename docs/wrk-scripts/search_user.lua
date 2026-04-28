-- 查用户信息（需 token，通过 -H 传入）
-- 用法: wrk -t4 -c100 -d60s -H "token: <your_token>" -s search_user.lua http://localhost:8095/patient-wx-api/user/searchUserInfo

wrk.method = "GET"
wrk.headers["Content-Type"] = "application/json"
