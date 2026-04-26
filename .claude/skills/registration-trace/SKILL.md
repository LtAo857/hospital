---
name: registration-trace
description: Trace the real registration execution path in this hospital project. Use to follow the flow from intent/confirmation through validation, submit lock, DB write, audit, and repair-related code.
---

# Registration Trace

Use this skill when you need to trace the **actual registration path** in the current repository.

## What this skill is for

Answer questions like:
- What happens after the user confirms registration?
- Where is registration validation done?
- Where is the actual write executed?
- Where are idempotency, audit, and repair handled?
- Which parts are AI orchestration versus core business service logic?

## Scope

Focus on the MySQL patient registration flow:
- `patient-wx-api-mysql`
- relevant SQL upgrade scripts
- related multi-agent execution path

## Required reading order

1. `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/controller/RegistrationController.java`
2. `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/service/impl/RegistrationServiceImpl.java`
3. `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/tool/RegistrationAgentTools.java`
4. `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/worker/PolicyAgentWorker.java`
5. `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/worker/ExecutionAgentWorker.java`
6. `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/service/MultiAgentRegistrationAuditService.java`
7. `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/job/MultiAgentRegistrationRepairJob.java`
8. `sql/patient_wx_multi_agent_registration_upgrade.sql`

## Output rules

- Separate read/query steps from write/submit steps.
- Clearly mark where business validation happens.
- Clearly mark where idempotency/locking happens.
- Clearly mark where audit and repair/compensation happen.
- Use `file_path:line_number` anchors.

## Important project-specific conclusions to verify, not assume

- Real registration truth is still in the service layer and database flow, not in the LLM.
- Multi-agent execution delegates to the same core registration business capability.
- Submit locking is distinct from request throttling.
- Audit/repair are extra hardening layers around the multi-agent registration path.

## Good response shape

1. One-sentence summary of the path
2. Ordered flow from user action to DB write
3. Validation / lock / audit / repair sections
4. Key code anchors
