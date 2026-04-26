---
name: rag-boundary-check
description: Verify the real RAG boundaries in this hospital project. Use to confirm knowledge sources, retrieval mode, whether it is explanation-only, and what is intentionally not handled by RAG.
---

# RAG Boundary Check

Use this skill when you need a **code-accurate answer** about what the current RAG implementation does and does not do in this repository.

## What this skill is for

Answer questions like:
- Is the current RAG a vector database solution?
- Does RAG handle real-time registration facts?
- Is RAG only used for explanations?
- Is retrieval keyword-only, vector-only, or hybrid?
- What documents are used as the knowledge source?

## Scope

Focus on:
- `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/rag/`
- `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/service/MultiAgentCoordinatorService.java`
- `docs/agent/multi-agent.md`

## Required reading order

1. `docs/agent/multi-agent.md`
2. `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/service/MultiAgentCoordinatorService.java`
3. `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/rag/MultiAgentRagService.java`
4. `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/rag/MultiAgentKnowledgeBase.java`
5. `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/rag/EmbeddingClient.java`
6. `patient-wx-api-mysql/src/main/java/com/example/hospital/patient/wx/api/agent/multi/rag/EmbeddingVectorIndex.java`

## Output rules

- Distinguish clearly between **static explanation knowledge** and **real-time business facts**.
- Explicitly state whether the implementation uses:
  - markdown slicing
  - keyword retrieval
  - embedding retrieval
  - hybrid retrieval
  - independent vector database or not
- State whether RAG participates in write decisions.
- Use `file_path:line_number` anchors.

## Important project-specific conclusions to verify, not assume

- Current RAG is explanation-oriented, not the source of live schedule or registration truth.
- Knowledge comes from `docs/agent/*.md`, not a hospital-wide FAQ platform.
- Retrieval is lightweight hybrid retrieval when embedding is enabled.
- The current implementation uses in-process indexing/vector cache rather than a standalone vector database.
- Safe memory is deliberately trimmed before being sent into the RAG generation path.

## Good response shape

1. One-sentence conclusion
2. What RAG is used for
3. What RAG is not used for
4. Retrieval/data source details
5. Key code anchors
