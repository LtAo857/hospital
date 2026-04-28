-- 查科室列表（无需鉴权）
-- 用法: wrk -t4 -c100 -d60s -s search_dept.lua http://localhost:8095/patient-wx-api/medical/dept/searchMedicalDeptList

wrk.method = "POST"
wrk.headers["Content-Type"] = "application/json"
wrk.body = '{"page":1,"length":10}'
