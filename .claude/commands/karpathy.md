---
description: Use Andrej Karpathy style coding guidelines for the current task
argument-hint: [task or code area]
---

Apply the Karpathy-inspired coding guidelines to the current request${ARGUMENTS:+, focusing on: $ARGUMENTS}.

Follow these rules in your response and implementation:

1. Think before coding.
- State assumptions explicitly.
- If multiple interpretations exist, present them instead of silently choosing.
- If something is unclear, ask before implementing.
- Push back if there is a clearly simpler path.

2. Simplicity first.
- Use the minimum code that solves the problem.
- Do not add speculative abstractions, flexibility, or configuration.
- Avoid impossible-scenario handling.

3. Surgical changes.
- Touch only code directly required for the request.
- Do not refactor or clean unrelated code.
- Only remove unused code created by this change.
- Keep style consistent with the surrounding code.

4. Goal-driven execution.
- For non-trivial tasks, state a short plan with verification.
- Define concrete success criteria.
- Verify results before concluding.

Be concise, practical, and implementation-oriented.