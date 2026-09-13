---
name: write-plan
description: Reuse or create a Git branch for a task and draft or update an Arbiter implementation plan for human review.
---

# write-plan

**Input:** Agreed requirements or a GitHub issue with acceptance criteria.

1. Inspect the current Git branch and working tree. Reuse a branch for the same task, or create a descriptively named branch from the agreed base. Preserve unrelated work; use an isolated worktree if switching would mix tasks or disturb local changes.
2. Read the relevant parts of the current code and architecture, including `context/architecture.md`. Identify affected components, role boundaries and shared contracts. Compare design alternatives only where the choice matters.
3. Write or update `plans/<task>.md` with the issue, branch, goal, non-goals, proposed changes and how each acceptance criterion will be verified, such as unit tests, integration tests or human acceptance checks. Identify the coverage here; `write-test` writes automated tests during implementation. Note significant risks and open decisions.
4. Present the plan for human review before implementation. Reuse approval already given for the same scope; seek a decision when a material change falls outside it.

**Done when:** The task branch and plan path are reported, and the plan's approval status and unresolved decisions are clear. Keep the plan current as work proceeds.
