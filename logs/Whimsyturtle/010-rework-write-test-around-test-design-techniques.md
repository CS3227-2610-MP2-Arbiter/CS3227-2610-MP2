# Rework write-test around test-design techniques

Status: Human verified.

## Original request

- The user did not like how the tests on `feat/workspace-setup` were written, and blamed the `write-test` skill. They pasted a prompt to adapt, not copy: derive tests from observable behaviour and comments before the implementation, then apply equivalence partitioning, boundary value analysis, positive and negative tests, and pairwise combinations. The test files were to stay unchanged for now.

## Follow-ups, corrections, and reflection

- The user pointed out that the skill named both `src/test` and `src/test/java`, and asked to drop the rule about `build.gradle` and dependencies.
- The user asked whether the skill still fitted the workflow in `context/swe.md`. The agent found no conflicts and raised five gaps. The user chose to fix the first three: the missing plan in the input, `write-plan` also choosing the test types, and the dropped report of the command.
- The user asked for test methods to be named `featureUnderTest_testScenario_expectedBehavior`, with the last part or last two omitted when a test covers more.

## Agent responses and outcomes

- Rewrote `write-test` into five steps: derive expected outcomes from the specification, design the cases with the four techniques, check the implementation for missed paths, write the tests, and run them.
- Kept the project-specific guidance from the old skill. Added that the skill leaves production code unchanged, and that it keeps a test that exposes a suspected defect and reports the defect.
- The input now includes the approved plan, and the skill follows the plan's test types when there is one.
- Replaced "descriptive names" in step 4 with the naming convention and its examples.

## Verification

- Checked the skill against `context/swe.md` and the other skills. No tests were written or run, and the skill has not been tried on a task.
