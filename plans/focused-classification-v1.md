# Focus V1 on text classification

**Issue:** [#62] - Reduce v1 to a focused text-classification workflow
**Branch:** `scope/focused-classification-v1`
**Status:** Implemented and verified; awaiting human acceptance.

## Goal

Remove the accepted v1 feature families before the SQLite schema and role UIs are implemented, leaving a coherent two-role text-classification workflow with durable submissions, automatic and manual resolution, basic operational progress, CSV/JSON export and the required delivery artifacts.

## Accepted scope

- Projects classify plain-text files using either a single-select taxonomy or an integer scale. Detection, images, bounding boxes and COCO are deferred.
- Flags, report-only outcomes, flag review and flag-driven exclusion are deferred. Every submitted annotation is a valid classification answer.
- Annotators and adjudicators see the basic counts and statuses needed to perform work. Session timing, lifetime/activity charts and label-agreement analytics are deferred.
- Import synchronously registers selected `.txt` files already under workspace `media/`. Recursive folder import, dry-run statistics and background progress are deferred; path containment, readability, content hashing, changed-source detection and per-project duplicate rejection remain.
- `Submit & next` validates and atomically persists one immutable answer and advances the queue. No annotation record exists before submission; draft persistence/restoration, rationale, session timing and keyboard shortcuts are deferred. Restart derives the next position from submitted answers and the saved split order.
- The COMPLETE state and permanent project seal are deferred. Corpus/taxonomy and split setup still freeze at the first assignment, submissions remain immutable, and export stays available. A project may be deleted only before its first assignment.
- The standalone provenance browser is deferred. Manual resolution still compares the submitted evidence needed to decide an item, and export includes compact provenance for the current decision.

## Non-goals

- Do not remove scale classification, count-based batch generation, configurable annotations per item, CSV or JSON export, annotator password replacement/deactivation, or pre-assignment project deletion.
- Do not weaken authentication, authorization, annotator blindness, submission atomicity, source integrity, resolution persistence or automated verification.
- Do not implement the remaining role workflow in this task; this task re-baselines its contracts before feature implementation begins.

## Proposed changes

### Requirements and issue backlog

- Make #62 the task-level record for the accepted scope reduction while retaining feature behavior in each feature's issue.
- Close #15, #16, #19, #29, #35, #36 and #46 as not planned for v1.
- Rewrite the affected retained issue bodies and dependency clauses, particularly #6, #8, #10-#14, #17-#18, #24-#28, #30-#34, #37 and #41, so no active v1 issue depends on removed behavior.
- Mark the stale issue comments that contradict immutable submissions or frozen setup as superseded by the current issue bodies and shared rules.
- Adjust labels, titles and estimates only where the reduced issue is materially different; preserve attribution and discussion history.

### Shared model and repository contracts

- Delete `TaskType`, `SourceType`, `BoundingBox`, `Flag`, `FlagReason`, `FlagDisposition`, `BoundingBoxRepository` and `FlagRepository`.
- Remove `COCO` from `OutputFormat`.
- Remove task type, source type and completion state from `Project`; keep name, description, CSV/JSON output format and creation time.
- Retain `Item.contentHash` and source-integrity semantics. Remove `Item.retired`; before the first assignment the adjudicator may unregister an item, removing its database record and any never-assigned membership without touching its source file.
- Remove rationale, draft timestamps and report-only state from `Annotation`; retain item, assignment, annotator, single/scale answer fields and submission time. Every stored annotation is a submitted answer.
- Remove `Resolution.selectedAnnotationId` and detection-specific result logic while retaining label and averaged-scale results.
- Remove the lifetime annotation counter from `AnnotationRepository` and revise repository Javadoc that refers to drafts, reports, flags, boxes, completion or post-assignment retirement.
- Revise `ProjectRepository.deleteById` to describe the first-assignment deletion guard rather than completion.

### Product and architecture documentation

- Update the role summaries in `docs/index.md`, domain terms and fixed values in `docs/Glossary.md`, shared workflow and rules in `docs/UserFlows.md`, and the quick reference in `docs/UserGuide.md`.
- Update `docs/DeveloperGuide.md` and `context/architecture.md` to remove detection components, flag services, draft write-through claims, COMPLETE guards, advanced analytics and the provenance screen while retaining the shared service and blindness boundaries.
- Link derived pages to the authoritative feature issues or shared rules instead of copying detailed behavior.
- Leave setup/build commands, skill-process descriptions and unrelated delivery documentation unchanged.

### Automated coverage

- Add focused model tests for the retained mutually exclusive single-label/scale answer and resolution shapes after the removed fields are deleted.
- Rely on compilation and Checkstyle to catch stale imports or references to deleted contracts.
- Reserve repository/service tests for their owning implementation issues, but ensure their planned fixtures no longer contain removed variants.

## Verification

| Acceptance criterion | Verification |
| --- | --- |
| Removed feature families no longer appear in active v1 requirements | Re-fetch affected issue bodies and dependency clauses; search current docs and source for detection, image, COCO, flag, report-only, draft autosave, rationale, lifetime/activity/agreement analytics, COMPLETE and provenance-screen references; review any remaining historical/log references separately |
| Retained feature contracts remain coherent | Review #62 plus the retained feature issues for SINGLE/SCALE classification, count-based splitting, configurable *k*, CSV/JSON export, account administration, source integrity, automatic resolution and manual classification resolution |
| Shared model has no removed state | Compile main and test sources; inspect the model/repository diff and run focused model tests |
| Documentation has one home per fact and no broken local links | Scan Markdown references and anchors, inspect the rendered structure where headings/tables change, and run `git diff --check` |
| The reduced foundation remains buildable | Run `./gradlew check shadowJar` on JDK 25 and distinguish executed tests from skipped or cached tasks |
| GitHub backlog matches the branch | Re-read #62 and every edited/closed issue after publication; verify the v1 milestone no longer treats closed cuts as active dependencies |

## Risks and decisions

- Existing verified decisions kept several features now being cut. #62 and this plan record that the deadline-driven decision supersedes them for v1; historical logs remain unchanged.
- Removing the sole enum value rather than retaining one-value `TaskType` and `SourceType` makes classification/TXT structural product constraints. Reintroducing other task or source types later will require an explicit schema evolution.
- Without draft records, an app close loses only the current unsubmitted selection. A successful submission remains atomic and durable, and restart resumes at the first item without a submitted answer.
- Content hashes and changed-source detection remain because they are cheap integrity controls and already fit `Item`; only the rich import UI is cut.
- Setup-time item removal becomes a real unregister operation because no submitted work can exist before the first assignment; flag-driven retirement and retained excluded evidence are gone.
- Without COMPLETE, the first assignment is the permanent boundary for configuration and project deletion. This avoids both a new lifecycle state and destructive deletion of attributed work.
- The human approved this plan before the Java contract cleanup began.

## Implementation outcome

- Created #62, closed #15, #16, #19, #29, #35, #36 and #46 as not planned, rewrote 26 retained issue bodies, and added superseding comments where historical comments contradicted the current requirements. The issue audit found no retained open-V1 dependency on a closed cut.
- Removed the planned model and repository contracts for detection, flags, source/task variants, drafts, rationales, lifetime analytics and completion sealing. Retained SCALE, count-based splitting, configurable *k*, CSV/JSON, account administration and source hashes.
- Updated the product, user, developer and architecture documentation. Independent review found and corrected two contract-description issues: `docs/UserFlows.md` now locks taxonomy kind and output format at creation while labels or scale range freeze at first assignment, and `SplitRepository` now states that a split remains removable until that split's first assignment. `Resolution` Javadoc was also tightened so disputed SINGLE resolutions do not imply that adjudicators select a submitted answer record.
- Added eight focused tests across `AnnotationTest` and `ResolutionTest`, covering both setter transitions and corrupt ORM-loaded rows with both scalar fields populated.
- `./gradlew.bat check shadowJar --rerun-tasks --no-daemon` passed on JDK 25 with 32 tests, zero failures, Checkstyle for main and tests, and six executed Gradle tasks. Resource tasks were `NO-SOURCE`; `build/libs/arbiter.jar` was produced. A sandboxed run first failed because the sandbox denied access to a cached JavaFX JAR, then the identical elevated command passed.
- Source and active-document stale-term scans, removed-issue-reference scans, local Markdown link checks and `git diff --check` passed.

[#62]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/62
