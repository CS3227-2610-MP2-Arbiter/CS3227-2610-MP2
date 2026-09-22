# Deduplicate documentation and code comments

Status: Human verified.

## Original request

- After reading the PR #58 review inventory, the user asked for duplicated information across the documentation and code to be removed so each fact has one home, to avoid drift. Some loss of precision was acceptable, and contradictions were to be raised rather than resolved.

## Follow-ups, corrections, and reflection

- The agent deleted copies that agreed with their home and left both sides of each contradiction untouched. It fixed directly only stale statements that could be checked against the code.
- The agent found that the first pass had lost two open model questions and introduced wrong wording, such as rule 5 calling whole-project deletion the only other deletion. These were fixed before handover.
- The user then asked for this log, kept concise.
- The user asked for future agents to follow the single-home rule. This settled the question of merging the two lists of homes. The development process's home is still undecided.
- After the agent explained each contradiction, the user decided them: one split per item; no label guideline; an optional rationale; "dispute" for a single-select conflict and "unresolved" for any item without a final answer; taxonomy kind fixed at creation; annotators see nothing about other annotators; best judgement, with flags only for source problems; several sentences per Markdown line; and Whimsyturtle builds the lock.
- The user asked the agent to make the fixes and review them. The agent took "optional" as always optional and "fixed once created" as the taxonomy kind only, with labels editable until the first assignment, and told the user.
- The user then asked for the per-annotator answer lookup to return one optional answer and for the UserGuide to cover text items as well as images.
- Asked for a simpler answer to the dispute question, the user chose to drop the dispute flag, so a resolution exists only once an item is settled. They also chose an optional rationale for detection, to match classification.

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
- Model: removed `Label.guideline` and `Project.rationaleRequired`, removed `Resolution.unresolved` and its queries, and made `SplitItemRepository.findByItem` and `AnnotationRepository.findByItemAndAnnotator` return one optional record. The plan records each.
- Edited #6, #9, #13, #14, #15, #17, #24, #26, #27, #28, #29, #33 and #35, and created #61 for the lock, assigned to Whimsyturtle. #27 now resolves automatically as the *k*th answer is recorded, inside #17's submission step.
- Still open: #61's stale-lock and read-only questions, and who owns services.

## Verification

- `check shadowJar --no-daemon` succeeded with zero Checkstyle errors; tests are `NO-SOURCE`. `git diff --check` passed, and all links, anchors and reference definitions resolve.
- For the `AGENTS.md` follow-up, `git diff --check` passed and the linked Developer Guide sections exist. Gradle was not rerun because only Markdown changed.
- The user approved the summary after each follow-up, editing it along the way.
- For the contradiction fixes, `check shadowJar --no-daemon` succeeded with zero Checkstyle errors, `git diff --check` passed and every issue reference in the edited docs has a definition. Each issue was checked against its live body before writing and read back. The same checks passed after the dispute and detection-rationale changes.
