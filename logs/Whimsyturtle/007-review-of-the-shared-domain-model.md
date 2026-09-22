# Review of the shared domain model

Status: Human verified.

## Original request

- The user was reviewing PR #58, which adds `arbiter.model` and `arbiter.data` for #4, and asked what `TaxonomyKind` and `ResolutionMethod` mean.

## Follow-ups, corrections, and reflection

- The user then asked about several other parts of the model: where `ResolutionMethod.GOLD` came from, whether `FlagReason.DUPLICATE`, `TaxonomyKind.MULTI` and `Flag.excluded` were needed, whether ORMLite stops `labelId` and `scaleValue` being kept exclusive, and what the `countByRole` comment guards. The agent agreed `DUPLICATE` should go, but because hashing already blocks exact duplicates, not because annotators see one item at a time.
- The user posted their own review on #58 and asked the agent to check it. Three requests conflicted with agreed behaviour: dropping `RETURNED`, removing `AccountStatus` and splitting the packages. The user narrowed or deleted those comments, added two of their own and left the PR's four open questions for later.
- After `zheng-jj` replied with fixes, the user asked what had been claimed but not applied, and what both of them had missed. The user questioned two findings; the agent kept both but argued each from the case that fails. Of the scale tolerance and end labels, the agent said only the end labels could go. The user then posted a second round of seven comments.
- On 21 September, after more claimed fixes, the user asked for a fresh audit and an inventory before fixing anything, and said they preferred a smaller, more reliable v1. They then made a series of scope decisions:
  - scales kept and resolved by averaging, with tolerance deferred, instead of deferring scales as the agent proposed;
  - detection resolved by selecting one complete submitted box set;
  - setup frozen at the first assignment and completed projects sealed;
  - provenance limited to the latest submissions and the current decision;
  - batches by count only, with training/validation/test partitioning deferred;
  - forward-only submission with no return, rework, repair or review pass. The agent conceded it had understated the cost of returns and withdrew its claim that pre-submission review was essential;
  - no reassignment at all;
  - money tracking deferred;
  - statistics kept, after the user pushed back on cutting them, with agreement for single-select labels only;
  - media registered in place under `<workspace>/media/`. The user pointed out that both roles share one disk, so the agent withdrew its copy-on-import proposal;
  - one fixed adjudicator per workspace, who creates annotator accounts and resets passwords directly, with email deferred.
- The user then asked for a second inventory, and for its problems to be fixed a few at a time, finishing on 22 September.
- The user asked for the getter fix's temporary JUnit tests to be deleted and re-added later. They chose to commit the fixes themselves and authorized issue and PR description edits.

## Agent responses and outcomes

- First review: `GOLD`, the rest of `ResolutionMethod` and all of `FlagReason` came from the old `docs/UserFlows.md`, deleted in `e030a82`, so #4's "agreed constants" no longer existed. Other findings: `MULTI` could not be stored, an integer `Resolution.scaleValue` could not hold an average, `DUPLICATE` gave no way to act on it, `Flag.excluded` sat on the wrong class, `countByRole` still counted deactivated adjudicators, and answer exclusivity belongs in the service layer. The user posted the review with 25 inline comments.
- Checking `zheng-jj`'s fixes: three replies misdescribed the branch, removing `Flag.excluded` and `RETURNED` left #35, #12 and #17 unmet, and nothing recorded which items a split held. `af804a4`, `f8d56b5` and `7f443e6` fixed most of what both rounds raised. The flag disposition, the `AUTO_SCALE` rule, project deletion, the skip reason, the PR body and several issue bodies were still open.
- The 21 September audit found that the new getters hid an invalid row by returning null from both, and that the requirements conflicted over earnings, provenance and mutable splits. It recommended settling scope before adding more model machinery, and corrected the agent's earlier claim that no issue asks for project deletion.
- Each scope decision was published as issue edits and a documentation commit on `feat/shared-domain-model`, from `1157c87` for scales to `ef96bdd` for accounts. No Java changed.
- The second inventory, at `ef96bdd`, found ten problems, a set of design questions and a fix order.
- Conflicting answer fields: both scalar getters now throw `IllegalStateException` when label and scale fields are both set, without clearing either.
- Accepted removals, counts and transactions: removed return/rework state, the repaired disposition, allocation strategies and per-item rewards. Renamed the counter to `countValidSubmittedByAnnotator`, assigned the transaction boundary to #6, and published #6's revised body.
- Deletes: removed the answer, assignment and single-membership deletes, and limited the rest to setup or incomplete projects.
- Stale claims and #12: corrected stale comments and docs, rewrote the PR #58 body, and revised #12 so an unfinished assignment opens for editing only before its project is complete.
- Submission outcome: replaced `Annotation.submitted` with `submittedAt` and an explicit `reportOnly` marker, with `isSubmitted()` and `isValidAnswer()` as the only predicates.
- Detection selection: added `Resolution.selectedAnnotationId`. Contributors are derived, not stored. Updated #34, #36 and the PR body.
- Batch metadata: added `Split.requestedBatchSize`, made the seed the one actually used and removed claims that items move between splits.
- Still open: owner password recovery; when automatic resolution runs; dashboard definitions, input validation, export provenance scope and shuffle order; regression tests for the getter fix; and enforcement in #6 and the services.

## Verification

- The early rounds relied on read-only `gh` reads of the PR, the branch at each fix commit, all review comments and the relevant issues. Gradle was not run. The ORMLite claims came from general knowledge; the audit later checked them against the official manual, which narrowed the identity claim (corrected with the stale claims).
- For the audit and inventory, `check shadowJar --no-daemon` passed on JDK 25.0.4.1 with zero Checkstyle errors; tests were `NO-SOURCE`. A reflection probe outside the repository reproduced the getter defect.
- For each scope decision, the issues were re-read before editing and read back afterwards with metadata preserved, links and `git diff --check` passed, and CI ran on the pushed commit. Java checks were not rerun, because only documentation changed.
- Six temporary tests covered the getter fix. As expected, the two corrupt-row tests failed on the original getters, and all six passed after the fix. They have since been deleted, so nothing currently guards this defect.
- After each inventory fix, `check shadowJar --no-daemon` succeeded with zero Checkstyle errors and `git diff --check` passed. Throwaway probes checked the submission predicates and the resolution result shapes. The #6, #12, #34 and #36 bodies and the PR body matched their drafts on read-back.
- The user approved the concise rewrite of this log.
