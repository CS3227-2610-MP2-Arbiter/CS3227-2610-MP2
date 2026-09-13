---
name: implement-feature
description: Implement an agreed Arbiter feature or bug fix, following its approved plan when one is needed.
---

# implement-feature

**Input:** The agreed change, acceptance criteria and approved plan when applicable.

1. Read the plan and relevant code. Confirm the task branch and preserve unrelated work. Apply the small-task exceptions in `context/swe.md` when appropriate.
2. Implement the agreed behavior. Create or reuse components as appropriate; keep their responsibilities focused and avoid duplicating logic.
3. Use `write-test` for behavior that needs automated coverage. For bug fixes, reproduce the failure and add a regression test where practical.
4. Use `review` to verify the change, fix failures within scope and rerun affected checks. Record progress and material deviations in the plan; refer unresolved design or scope decisions to the human.

**Done when:** The agreed behavior is implemented and relevant checks pass. Report changes, actual verification results and scenarios still needing human acceptance.
