---
name: multi-agent-map
description: Map the current multi-agent registration architecture in this hospital project. Use to explain stage flow, worker responsibilities, coordinator behavior, and where lightweight ReAct is actually used.
---

# Multi-Agent Map

Use this skill when you need the **current, code-accurate architecture** of the patient-side multi-agent registration flow in this repository.

## What this skill is for

Answer questions like:
- How does the current multi-agent flow work?
- Is the current multi-agent implementation fully ReAct?
- Which worker handles which responsibility?
- Where is the coordinator doing stage transitions, memory, trace, and response assembly?

## Scope

Focus on the MySQL chain only:
- `patient-wx-api-mysql`
- `patient-wx`
- `docs/agent/multi-agent.md`

Do not drift into resume/interview phrasing unless explicitly asked.

## Required reading order

1. `docs/agent/multi-agent.md`
2. `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/service/MultiAgentCoordinatorService.java`
3. `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/worker/TriageAgentWorker.java`
4. `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/worker/ScheduleAgentWorker.java`
5. `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/worker/PolicyAgentWorker.java`
6. `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/worker/ExecutionAgentWorker.java`

## Output rules

- Report the actual stage flow.
- Separate **overall architecture** from **local implementation details**.
- Be explicit when something is only used in one worker rather than the whole system.
- Call out file anchors in `file_path:line_number` format.

## Important project-specific conclusions to verify, not assume

- The overall system is a staged multi-agent coordinator, not a fully free-form ReAct agent.
- Lightweight ReAct is only piloted inside `ScheduleAgentWorker`.
- `Policy` and `Execution` are guardrail-heavy deterministic stages, not open-ended reasoning stages.
- Frontend-visible flow is compressed output, not the full internal reasoning chain.

## Good response shape

1. One-sentence conclusion
2. Stage-by-stage breakdown
3. Where ReAct is and is not used
4. Key code anchors
