-- MIS 查全部科室（需 token，通过 -H 传入）
-- 用法: wrk -t4 -c100 -d60s -H "token: <your_token>" -s search_dept_all.lua http://localhost:8094/hospital-api/medical/dept/searchAll

wrk.method = "GET"
wrk.headers["Content-Type"] = "application/json"
