# Software engineering process

Use one agent with task-specific skills in `.codex/skills/`. Humans own requirements, design decisions, acceptance and merging.

## Feature workflow

1. **Clarify:** Use `clarify-requirements` to find an existing GitHub issue or prepare a new one and define the requirements. A human confirms the scope and acceptance criteria.
2. **Plan:** Use `write-plan` to reuse or create a Git branch and plan the changes and verification based on the current code and architecture. A human approves the plan before implementation.
3. **Implement:** Use `implement-feature` to implement the approved plan within the agreed scope. Use `write-test` to write automated tests for the agreed behavior.
4. **Verify:** Use `review` to run the required checks and assess the changes against the agreed requirements. The agent reports its findings and the results of the checks.
5. **Accept:** A human tests the agreed scenarios and confirms whether the changes meet the acceptance criteria. Use `maintain-docs` to document the new or updated behavior in the guides and product site.
6. **Deliver:** Use `create-pull-request` to prepare a GitHub pull request with verification evidence. A human merges after CI passes and a teammate reviews the final revision.

Before committing, pushing, or creating or updating a pull request, prepare the exact changes and request explicit human approval for those delivery actions. One approval may cover them together. Approval of the scope, plan, implementation, or acceptance tests does not grant delivery approval. Leave the changes uncommitted and provide a ready-to-use PR draft when approval has not been given.

If verification or acceptance testing finds problems, the agent fixes them and reruns the affected checks. A human repeats any affected acceptance tests.

For bugs, reproduce, fix and add regression tests. Small edits and read-only tasks use only applicable steps.

## Skills and logging

- Define each skill's inputs, steps, outputs and completion criteria. Check that the agent uses it for the intended tasks.
- Use `log` to record prompts, skill versions, failures, corrections and results in `logs/`. A human verifies each summary. Keep plans current for resumption.
