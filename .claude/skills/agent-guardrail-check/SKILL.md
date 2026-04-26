---
name: agent-guardrail-check
description: Inspect project-specific guardrails in the AI registration flow. Use to verify stage guards, confirmation requirements, request throttling, memory boundaries, idempotency, and fallback behavior.
---

# Agent Guardrail Check

Use this skill when you need to inspect the **safety and control mechanisms** of the current AI registration flow.

## What this skill is for

Answer questions like:
- How does the system prevent step skipping?
- How does it prevent accidental writes?
- How does it handle repeated clicks or frequent requests?
- What memory is persisted, and how is it bounded?
- What fallback or repair logic exists when execution fails?

## Scope

Focus on current multi-agent registration guardrails only.

## Required reading order

1. `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/worker/ScheduleAgentWorker.java`
2. `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/worker/PolicyAgentWorker.java`
3. `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/worker/ExecutionAgentWorker.java`
4. `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/service/MultiAgentRequestGuardService.java`
5. `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/memory/RedisMultiAgentMemoryService.java`
6. `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/service/impl/RegistrationServiceImpl.java`
7. `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/service/MultiAgentRegistrationAuditService.java`
8. `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/job/MultiAgentRegistrationRepairJob.java`

## Output rules

- Categorize guardrails by type:
  - stage/order guardrails
  - confirmation/write guardrails
  - request throttling
  - memory/session guardrails
  - idempotency/submission guardrails
  - audit/repair guardrails
- State what each one protects against.
- Use `file_path:line_number` anchors.

## Important project-specific conclusions to verify, not assume

- Request guard and submit lock are different layers with different purposes.
- Schedule guardrails are mainly about slot-completion order and step legality.
- Policy/Execution guardrails are mainly about write safety.
- Memory is persisted with TTL rather than permanently.
- Repair logic exists because execution correctness matters more than conversational smoothness.

## Good response shape

1. One-sentence conclusion
2. Guardrail categories
3. Concrete protections per category
4. Key code anchors
