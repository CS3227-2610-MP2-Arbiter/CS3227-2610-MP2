---
name: write-test
description: Add or update automated tests for Arbiter requirements and regressions, independently or during implementation.
---

# write-test

**Input:** Acceptance criteria or a reproduced bug, plus the relevant code and existing tests.

1. Derive expected outcomes from requirements. Choose representative valid and invalid inputs, boundary values and error cases for important logic.
2. Use the project's JUnit setup under `src/test/java`. Prefer unit tests for isolated logic and integration tests for role isolation, shared services and persistence where relevant.
3. Use synthetic fixtures and temporary storage. Assert observable behavior; avoid duplicating implementation details or adding tests for trivial documentation edits.
4. Run the focused tests. For a regression, demonstrate that the test catches the original bug when practical. Report the command, results and coverage gaps; use `review` for full change verification.

**Done when:** Tests cover the agreed cases and their actual results are reported, with remaining failures or gaps clearly identified.
