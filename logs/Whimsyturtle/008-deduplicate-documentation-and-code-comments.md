# Deduplicate documentation and code comments

Status: Human verified.

## Original request

- After reading the PR #58 review inventory, the user asked for duplicated information across the documentation and code to be removed so each fact has one home, to avoid drift. Some loss of precision was acceptable, and contradictions were to be raised rather than resolved.

## Follow-ups, corrections, and reflection

- The agent deleted copies that agreed with their home and left both sides of each contradiction untouched. It fixed directly only stale statements that could be checked against the code.
- The agent found that the first pass had lost two open model questions and introduced wrong wording, such as rule 5 calling whole-project deletion the only other deletion. These were fixed before handover.
- The user then asked for this log, kept concise.
- The user asked for future agents to follow the single-home rule. This settled the question of merging the two lists of homes. The development process's home is still undecided.

## Agent responses and outcomes

- Homes: the index for roles, the Glossary for terms and enum values, UserFlows section 3 for shared rules, issues for feature detail, architecture for enforcement only, the Developer Guide for rationale, Javadoc for per-class notes and the plan for #4's decision history.
- Edited 24 files, cutting about 370 lines to 190 with rule numbers unchanged. No issue, PR body or commit was changed.
- Fixed stale statements: "eleven tables", "fifth and sixth rows", the Glossary's "Batch allocation" row, two completed architecture to-dos and Resolution's "after adjudication".
- Raised for the user:
  - Contradictions:
    - whether an item can be in several splits, which the derived contributors depend on;
    - label description vs guideline;
    - the optional rationale;
    - what "unresolved" means;
    - when taxonomy kind is set;
    - annotator aggregate progress;
    - "flag anything unclear";
    - the Markdown line-break rule;
    - who owns services and the workspace lock.
  - Decisions: deduplicating issues, the redundant model fields, which file is home for the process, and whether to keep the plan.
- `AGENTS.md` now has the one table of homes, with rules to link rather than restate, to raise contradictions and to leave counts out of documentation. `maintain-docs` step 3 now points to that table instead of keeping its own incomplete list.

## Verification

- `check shadowJar --no-daemon` succeeded with zero Checkstyle errors; tests are `NO-SOURCE`. `git diff --check` passed, and all links, anchors and reference definitions resolve.
- For the `AGENTS.md` follow-up, `git diff --check` passed and the linked Developer Guide sections exist. Gradle was not rerun because only Markdown changed.
- The user edited and approved the summary.
