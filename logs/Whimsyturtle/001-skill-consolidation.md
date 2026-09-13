# Skill consolidation

Status: Human verified.

## Original request

- The user asked whether `context/swe.md` had unnecessary skill boundaries, especially a separate `create-branch` skill, and inconsistent notation such as mixing arrows and prose.

## Follow-ups, corrections, and reflection

- The agent initially suggested putting branch setup in `implement-feature`. The user chose `write-plan`, merged `test` into `review`, and kept `write-test` independent. They also requested consistent naming and simple initial skill instructions to refine through use.
- The user questioned vague issue and branch terminology, inconsistent ordering of existing versus new resources, and whether planning examines current code. The agent made GitHub issues explicit, explained that Git branches can exist locally, and changed planning to reuse or create a branch and inspect the current code and architecture.
- The user questioned whether tests belonged in planning. The agent clarified that planning identifies how requirements will be verified, potentially including unit tests; automated tests are written during implementation. This clarified the boundary between `write-plan` and `write-test`.
- The user asked for consistent human responsibilities and challenged the unspecified reporter of verification results. They also requested documentation wording focused on new or updated behavior and clearer handling of failures. The revisions named the agent as reporter and fixer, with humans repeating affected acceptance tests.
- While clarifying acceptance records, the agent introduced a requirement for the tester to record their name. The user rejected that unnecessary bookkeeping. Acceptance was simplified to testing the agreed scenarios and confirming whether the acceptance criteria are met.
- The user challenged mandatory component reuse, mixed abbreviation styles and detailed test-case guidance in the overview. The agent simplified Implement first, which made it uneven with the other stages; further feedback led to shortening the remaining stages. The lesson was to keep the overview at a consistent level of detail and leave specialized instructions in the skills.
- The user requested plain language for skill-trigger checks and removal of the formal skill-comparison bullet, improvement/reflection bullet and entire Boundaries and release section. They later challenged the blind-annotation reminder in `implement-feature`; the agent acknowledged it as product behavior already described in the README and removed the product-specific sentence from the general skill.

## Agent responses and outcomes

- Inspection found ten stub skills. Using `skill-creator`, the agent filled the eight retained skills in `.codex/skills/` with inputs, short steps and completion criteria. The agent also renamed `dissecting-feature-requirement` to `clarify-requirements` and `create-merge-request` to `create-pull-request`.
- Retired skill files and empty folders were removed. The sandbox initially blocked folder removal; an approved escalation removed only the four named folders after confirming they were empty.
- The resulting `context/swe.md` contains a concise six-stage workflow, guidance for fixes and small tasks, and a Skills and logging section. The corresponding skills contain the detailed procedures. No application code changed.

## Verification

- At consolidation, direct structural checks passed for all eight skills: simple frontmatter structure, matching folder names and headings, no placeholders or trailing whitespace, complete workflow references and no references to retired skills. `git diff --check` also passed during the edits.
- No behavioral evaluation of the skills was performed. Gradle checks were not run because no application code changed.
- The user edited the summary and approved it.
