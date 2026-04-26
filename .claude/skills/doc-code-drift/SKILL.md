---
name: doc-code-drift
description: Check whether project docs describing AI registration match the current implementation. Use to compare README, docs/agent/*.md, and current multi-agent/rag code for drift.
---

# Doc-Code Drift

Use this skill when you need to verify whether the current documentation still matches the implementation in this repository.

## What this skill is for

Answer questions like:
- Does README overstate the current AI capability?
- Do docs say the system is fully ReAct when the code is not?
- Do RAG descriptions still match the implementation?
- Which statements are now stale or too broad?

## Scope

Focus on AI registration documentation drift only.

## Required reading order

1. `README.md`
2. `docs/agent/multi-agent.md`
3. `docs/agent/traditional-agent.md`
4. `docs/agent/cc-agent.md`
5. Current multi-agent worker/coordinator/rag implementation files

## Output rules

- Compare documentation claims against current code.
- Separate findings into:
  - accurate
  - partially accurate
  - stale / overstated
- Suggest the minimal wording correction.
- Use `file_path:line_number` anchors when citing code.

## Important project-specific conclusions to verify, not assume

- Multi-agent documentation should distinguish staged orchestration from local ReAct usage.
- RAG documentation should distinguish explanation from real-time business facts.
- Public-facing docs may simplify; call out where simplification becomes inaccurate.

## Good response shape

1. One-sentence overall verdict
2. Accurate statements
3. Drifted statements
4. Recommended wording fixes
5. Key code anchors
