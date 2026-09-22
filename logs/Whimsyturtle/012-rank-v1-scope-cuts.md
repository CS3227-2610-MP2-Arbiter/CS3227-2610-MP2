# Rank V1 scope cuts

Status: Awaiting human verification.

## Original request

- With limited time and two team members, the user asked for a ranked list of Arbiter features to cut based on low importance and unnecessary complexity. They wanted a simpler product while retaining two meaningful roles and balancing the reduction across the annotator and adjudicator tracks.

## Follow-ups, corrections, and reflection

- A reliable end-to-end vertical slice is safer than retaining more feature variants than the team can implement and verify well.
- Equal issue counts would not create an equal workload: the open adjudicator feature backlog is substantially larger, and several apparently small adjudicator features add rules to most write paths. Balance should be judged by estimated effort retained or removed.
- A human-verified earlier task kept detection, scales, statistics, completion sealing and several other broad variants. The renewed scope concern reopens those decisions, but no scope change is accepted until the team confirms it and updates the feature issues and their shared rules.
- The IDE named this log, but it did not yet exist on disk. This task created it as the next Whimsyturtle log.
- The user accepted cuts 1, 3, 5, 7, 8, 9 and 10 from the recommendation and authorized a branch and GitHub issue changes. Therefore SCALE classification, count-based splitting, configurable *k*, CSV and JSON, account administration and pre-assignment project deletion remain; they were not silently removed with the rejected cuts.
- The accepted detection/import cuts were interpreted as TXT-only v1 after the agent stated that assumption and the user instructed it to continue. Simplified import retains path containment, readability, content hashing, change detection and duplicate rejection. With no COMPLETE state, project deletion is limited to before the first assignment.
- The user reviewed `plans/focused-classification-v1.md` and explicitly approved implementation. The model, documentation and retained-issue rewrites then proceeded in parallel under that plan.

## Agent responses and outcomes

- Inspected the product and architecture documentation, the current source tree, recent history, live v1 issue metadata and the acceptance criteria of the main feature issues. `main` contains workspace setup and the shared model/repository interfaces, but no SQLite repository implementation, authentication, role shell, service layer or end-to-end role workflow. No pull request was open during the audit.
- Recommended a minimum V1 in which the adjudicator creates active annotator accounts, creates a plain-text single-select project, enters labels, imports text items and assigns one project-wide batch to exactly two annotators. Annotators see only their own batch and submit one immutable label per item. Matching labels resolve automatically, differing labels go to a manual adjudicator decision, basic progress is visible and one JSON export contains the result and modest provenance.
- Ranked the main cut bundles as: detection/images/bounding boxes/COCO; numeric scales; flags/report-only outcomes/exclusion; general batch generation and configurable `k`; advanced annotator and adjudicator analytics; multiple export formats; rich import behavior; persistent drafts and queue conveniences; project completion/sealing; secondary account administration; and the dedicated provenance browser. The first five are paired or cross-role cuts; later cuts deliberately remove more adjudicator scope because that track is larger.
- Recommended preserving two authenticated and separately routed roles, blindness, persistent atomic submissions, basic progress, classification resolution, parseable export, automated tests and CI, the release, accurate guides and site, logs and reflections.
- Found a stale comment on #17 saying adjudicators may edit or delete submitted annotations, while the current issue body and `docs/UserFlows.md` rules 13 and 18 make submissions immutable. Older comments on #25 and #26 also describe superseded mutation rules. These contradictions should be marked superseded when the scope is re-baselined rather than used as implementation requirements.
- Created `scope/focused-classification-v1` from the up-to-date `main`, preserving unrelated working-tree content.
- Created tracking issue #62 with the accepted changes, retained scope and observable acceptance criteria. Closed #15, #16, #19, #29, #35, #36 and #46 as not planned, each linking to #62.
- Drafted and received human approval for `plans/focused-classification-v1.md`, which maps the issue, documentation and shared-model cleanup to verification.
- Rewrote 26 retained GitHub issues, preserving their labels, assignees and V1 milestone, and added superseding comments to historical threads that contradicted the current requirements. A final audit found no retained open-V1 issue body that referenced or depended on a closed cut.
- Removed detection, flag, source/task-variant, draft, rationale, lifetime-statistics and completion-only model/repository contracts. Kept SCALE, count-based splits, configurable *k*, CSV/JSON, account administration and source hashes. Added focused `AnnotationTest` and `ResolutionTest` coverage for setter switching and corrupt ORM-loaded scalar state.
- Updated the product, glossary, flow, user, developer and architecture documentation. An independent review found that the first revision put taxonomy-kind locking at first assignment; this contradicted #24/#26, so the final flow fixes taxonomy kind and output format at project creation while labels or the scale range freeze at first assignment.
- A final code-contract review found that `SplitRepository` described its deletion guard at project scope instead of split scope; its Javadoc now correctly permits deleting a split only before that split's first assignment. `Resolution` Javadoc was also clarified so a disputed SINGLE decision is not described as selecting an annotation record.

## Verification

- Read `docs/index.md`, `docs/Glossary.md`, `docs/UserFlows.md`, `docs/UserGuide.md`, `docs/DeveloperGuide.md`, `context/architecture.md`, the existing implementation plans, relevant prior logs and both reflection drafts.
- Queried the live GitHub milestone, open pull requests, and detailed issue bodies for the core workflow and proposed cut candidates. No GitHub data was changed.
- Inspected `main` at `6c5438d`; the pre-existing working tree was preserved before this log was added. No build was run because this was a read-only requirements audit apart from its required summary log.
- Re-read #62, every edited issue and the seven closed cuts. The retained issues preserve metadata and contain no active dependency on a closed cut; #62 alone mentions them as scope history.
- Focused model tests and Checkstyle passed with all eight new cases. After the final review corrections, the integrated `./gradlew.bat check shadowJar --rerun-tasks --no-daemon` run passed on JDK 25: 32 tests, zero failures, six executed tasks, main/test Checkstyle and `build/libs/arbiter.jar`; resource tasks were `NO-SOURCE`.
- The first final run inside the sandbox failed because access to a cached JavaFX JAR was denied. The identical approved command passed outside the sandbox, confirming this was an environment permission failure rather than a code failure.
- Active source/document scans found none of the removed symbols, closed issue references or cut-feature terminology. Retained-scope scans found SCALE, configurable *k*, CSV/JSON and account administration. Local Markdown link/reference checks and `git diff --check` passed. A rendered Pages preview was not run because the repository has no local Jekyll/Gemfile setup and the changes are prose/table edits.
