# Software engineering process

TODO: Define development conventions, planning, testing, review, documentation, and logging practices.

## Intended feature workflow (to be refined)

Local:

1. User requests a feature -> `dissecting-feature-requirement` (check GitHub for an existing issue, create one if needed, ask clarifying questions)
2. `create-branch`
3. `write-plan` -> human checks the plan (user asks to execute, or to update it)
4. `implement-feature` (uses `write-test`) -> `test`
5. Human manual test -> user accepts the implementation
6. `maintain-docs` -> `create-merge-request`

GitHub:

7. GitHub Actions -> `review` -> human review -> merge
8. CI/CD: GitHub Pages, build, Checkstyle, tests

`log` summarises agent interactions into `logs/` throughout.
