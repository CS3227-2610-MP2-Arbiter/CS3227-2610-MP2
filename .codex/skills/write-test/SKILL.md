---
name: write-test
description: Add or update automated tests for Arbiter requirements and regressions, independently or during implementation.
---

# write-test

**Input:** The feature or classes to test, their acceptance criteria and the approved plan when applicable, or a reproduced bug, plus the relevant code and existing tests.

1. Test only the named feature or classes. Derive expected outcomes from the requirements, observable behavior and Javadoc before reading the implementation.
2. Design the cases:
   - **Equivalence partitioning:** Cover each meaningful valid and invalid partition of every input and relevant object state. Add another case from a covered partition only if it could find a different defect.
   - **Boundary value analysis:** For each meaningful boundary, test the values just below, at and just above it.
   - **Positive and negative tests:** Exercise each valid partition in a positive test. Test each invalid input on its own with the other inputs valid before combining invalid inputs.
   - **Combinations:** Use at-least-once or pairwise coverage for inputs that affect one another. Test every combination only when each behaves differently.
3. Check the implementation for branches, conditions, exception paths and state- or time-dependent behavior the cases miss. Merge or remove tests that add no distinct coverage.
4. Write JUnit tests under `src/test/java` with descriptive names and focused assertions on the specific expected result or exception. Use the test types in the plan; without one, prefer unit tests for isolated logic and integration tests for role isolation, shared services and persistence. Use synthetic fixtures and temporary storage. Leave production code unchanged.
5. Run the focused tests. The suite must compile. If a test fails because of a suspected production defect, keep it as written and report the defect; never delete, disable or weaken it to pass. For a regression, show that the test catches the original bug when practical. Use `review` for full change verification.

**Done when:** The designed cases are covered, the tests compile and the command and actual results are reported, with suspected production defects and coverage gaps clearly identified.
