# Architecture

Written for agents: rules as tables and bullets, with no diagrams or rationale. The design, its reasoning and the test levels are in the "Design" and "Testing" sections of [docs/DeveloperGuide.md](../docs/DeveloperGuide.md), written for human readers. "Rule N" means rule N in section 3 of [docs/UserFlows.md](../docs/UserFlows.md).

## Packages

Layers run from 1 (top) to 5 (bottom). Dependencies point downward only, and neither role package imports the other; they meet in `arbiter.ui.shared` and `arbiter.service`.

| Layer | Package | Owns | Owner |
| --- | --- | --- | --- |
| 1 | `arbiter.ui.annotator` | The annotator screens, using the shared components. | zheng-jj (annotator track) |
| 1 | `arbiter.ui.adjudicator` | The adjudicator screens, using the shared components. | Whimsyturtle (adjudicator track) |
| 2 | `arbiter.ui.shared` | Shell, navigation, routing, UI kit, error handling, and the annotation components. | zheng-jj ([#5], [#8]) |
| 3 | `arbiter.service` | All business rules: assignment, resolution, export, access scoping. | Shared ([#4]) |
| 4 | `arbiter.data` | Repository interfaces. Signatures only, no SQL. | Shared ([#4]) |
| 4 | `arbiter.data.sqlite` | The ORM mappings, schema and migrations. | Whimsyturtle ([#6]) |
| 4 | `arbiter.model` | Value objects and enums. No queries, no UI logic. | Shared ([#4]) |
| 5 | `arbiter.workspace` | Workspace paths, the single-writer lock, asset resolution. | Shared ([#9], [#10]) |

## Rules

### Shared code

- `AnnotationEditor`, `BoxCanvas` and `ItemView` exist only in `arbiter.ui.shared`. A role difference is a mode flag on the shared component, never a second implementation.
- `AnnotationEditor` edits only the current annotator draft. Adjudicator classification resolution uses a shared label picker to record a separate final decision ([#34]); it never edits the original answer. Detection comparison uses read-only `BoxCanvas`; flag review has keep/exclude only ([#35], rules 13/16).
- Every rule about the data lives in `arbiter.service`, never in a controller or a repository.

### Blindness (rule 1)

- Annotator-facing code loads annotations only through `AnnotationService.forCurrentUser(...)`, which scopes every read to the session user. That path never loads resolved labels or agreement statistics, including aggregate agreement.
- Do not rely on package separation for blindness.
- The blindness test ([#11]) must cover every annotator-facing code path, including new ones.

### Data

- Retain every immutable submitted answer/report-only outcome, including unselected evidence, with attribution and submission time. Persist the current decision's exact valid answer contributors/selected set and metadata. Drafts and report-only outcomes are not resolution inputs ([#36], [#37]).
- Preserve annotations/attribution when deactivating users or retiring items; item retirement is limited to setup before assignment and the pre-completion flagged exclusion exception. Label deletion is only for unused labels before first assignment; never cascade it into annotations ([#26], rules 3/5). Completed projects cannot be deleted.
- Money tracking is outside v1. The current source still has the obsolete `Split.itemReward` field and earnings-specific repository/model comments, including the earnings paragraph on `AnnotationRepository.countSubmittedByAnnotator`; remove those during model/repository cleanup while retaining a valid-annotation count for [#19]. No reward, earnings or income-statement service contract belongs in the v1 design ([#20], [#21]).

### Accounts (rule 12)

- Bootstrap exactly one ACTIVE adjudicator per workspace in a transaction; close bootstrap after successful initialization. Reject duplicate/repeated bootstrap, invalid initialized owner state and attempts to add, replace, disable, delete or demote the owner ([#7], [#6]).
- All later account creation is authenticated adjudicator-only and creates ANNOTATOR accounts; persisted roles cannot change. Retain `Role`, `AccountStatus`, user IDs and decision/review attribution. Only annotators may be disabled ([#31]).
- Reuse one username/password validation and salted-hashing boundary for bootstrap, annotator creation and direct replacement. A [#23] reset updates credentials atomically without changing identity, status, assignments or project records; never store/log plaintext or retrieve credentials for display.
- No self-signup, reset-code/token/expiry model, email-verification state or additional-owner/transfer contract belongs in v1. Owner password recovery remains unresolved, with no rebootstrap bypass. Narrow the obsolete last-adjudicator rationale on `UserRepository.countActiveByRole` during model/repository cleanup; actual bootstrap/account guards remain service/persistence work.

### Source media (rule 21)

- Register existing supported images/TXT only beneath `<workspace>/media/`, including nested folders. Import never copies, moves, renames or modifies source files ([#25]).
- `Item.path` is normalized workspace-relative text such as `media/corpus/cat.jpg`; resolve it against the current workspace root from [#9]. Validate resolved containment at registration and source access; absolute/external paths, traversal and symlink/junction escapes must not bypass the media boundary ([#10]).
- Retain the hash of the registered bytes. Preview/confirmation revalidate candidate paths/content; display, valid answer submission and included-source export use the common integrity boundary. Missing/corrupt/changed/out-of-root files must not silently update paths/hashes or replace source content. Caches cannot bypass validation ([#10], [#37]).
- A source error alone never retires an item, submits work or advances progress. The explicit report-only route remains before COMPLETE. Exact-byte restoration at the recorded location permits later reads without database writes or reopening submitted work; no per-file relocation/relink or source-replacement contract is required.
- Treat source media as user-provided read-only inputs. Incomplete-project deletion removes owned database records, never media files or the workspace directory. Export writes normal outputs without overwriting registered source media. The seal covers app mutations; it cannot prevent file-system edits. Path/hash comments and validation/service contracts remain implementation work.

### Statistics (rule 20)

- Keep progress/session statistics ([#18]) and valid-annotation totals/activity charts ([#19]). Persist the terminal submission time required by [#17]; activity reads must not substitute mutable draft timestamps. Session timing does not require a persistent edit/event log.
- For [#33], compute SINGLE agreement from distinct original valid submitted labels with the same per-item eligibility as the shared rule. Aggregate the sum of unrounded item scores and eligible-item count so each item has equal weight across splits with different sizes or *k*. Do not derive agreement from resolutions or majority size.
- Return unavailable agreement for SCALE/DETECTION or no eligible items (*k* below 2 supplies no pairs); the UI renders N/A with its reason. Keep these reads adjudicator-only and do not filter out valid evidence because its author was deactivated. Reads after COMPLETE never write new resolutions or records.
- The current model/repository interfaces still need the submission-time and aggregate query contracts; this documentation does not implement those features or add a stored agreement counter.

### Lifecycle (rules 3, 5, 6, 9, 13, 14, 17, 18, 19)

- First assignment freezes corpus and taxonomy project-wide; each split's first assignment locks its definition, including membership/order and `annotationsPerItem`. Persist the first-assignment evidence/locks atomically with assignment creation. Assignments cannot be removed/reassigned; account deactivation must not unfreeze setup.
- `SplitItem` records stay unchanged after assignment. Pre-completion flagged exclusion changes item retirement, retaining memberships and current answers/provenance; it does not move or delete memberships.
- `Project.complete` seals all project-owned records. Every mutating service must check the seal in its write transaction, including autosave, per-item submission, assignment, flag batches, resolution and deletion. Completion commits atomically; export/viewing may read but never mutate sealed data.
- Submit & next validates, fixes submitted content, records submission time, advances the queue and updates assignment completion in one transaction. Retry must be idempotent; failure leaves the current draft editable. Submitted content cannot change through either role, including stale editors (rules 13/18).
- A current draft becomes a permanent submitted answer or explicit report-only outcome; no old snapshot plus revised draft, withdrawal, return or resubmission representation is required. Report-only outcomes handle queue work but never become valid labels/ratings/box sets. Resolution readiness is per item, independent of other unfinished items in the assignment.
- Keep immutable reporter content and current adjudicator dispositions/reviewer/time; no historical event collections or return metadata are required. Exclusion retains evidence and memberships but removes items from active queues/results/export; recompute affected assignment completion. Account replacement is deferred; existing assignments, including those of disabled accounts, retain their original ownership and place within the split's fixed *k*. Enforce source integrity under rule 21; freezing database rows alone cannot prevent file-system edits.

### Persistence

- One SQLite file at `<workspace>/arbiter.db`, with migrations applied on open and foreign keys enabled.
- Commit every annotator action immediately (rule 2).
- Code against the repository interfaces in `arbiter.data`. Their implementations in `arbiter.data.sqlite` map rows with ORMLite, not Hibernate, and are the only code with SQL.
- Take the workspace lock before writing, and refuse to open a second writer (rule 11).

### UI

- JavaFX, with one `Stage` whose content area is swapped, routed by role after login ([#5]).
- Report errors through the one convention in `arbiter.ui.shared` ([#8]).
- Controllers call services and bind results to the view. They hold no business rules and no SQL.

### General

- Nothing that assumes a backend: no background jobs, webhooks or message queues (rule 8).
- No plugin system and no dependency-injection framework.
- Java lines are at most 120 characters (Checkstyle in `config/checkstyle`). Markdown is exempt:
  never hard-wrap prose, tables or bullets in `.md` files.

## Model

Model classes are plain value objects: no queries, no UI logic and no annotations. They are deliberately **not** annotated for the ORM, so the mapping is designed once in `arbiter.data.sqlite` alongside the schema rather than guessed at here. Tests must be able to construct them without a database.

They are grouped into subpackages by area, mirroring the repository interfaces in `arbiter.data`:

| Subpackage | Classes | Enums |
| --- | --- | --- |
| `arbiter.model.user` | `User` | `Role`, `AccountStatus` |
| `arbiter.model.project` | `Project`, `TaxonomySettings`, `Label`, `Item`, `Split`, `SplitItem`, `Assignment` | `TaskType`, `SourceType`, `TaxonomyKind`, `OutputFormat`, `AssignmentStatus`, `SplitStrategy` |
| `arbiter.model.annotation` | `Annotation`, `BoundingBox`, `Flag` | `FlagReason`, `FlagDisposition` |
| `arbiter.model.resolution` | `Resolution` | `ResolutionMethod` |

A few rules keep the model honest:

- **One answer shape at a time.** `Annotation` and `Resolution` each carry a label *or* a scale value. Their setters clear the other, **and so do their getters**: ORMLite sets fields reflectively, so a row read back from the database never passes through the setters.
- **`Project` holds only its own settings.** What a taxonomy needs - the scale range - lives in `TaxonomySettings`, so a project is not a bag of optional numbers that matter for one taxonomy kind only. `TaxonomySettings` is a separate entity with its own `id` and `projectId`, because ORMLite has no equivalent of JPA's `@Embedded` and can only persist a type that has its own identity. **`Project` holds no reference back**: `projectId` is the only link, in the same direction as `Label`, `Item` and `Split`, so the relationship cannot be recorded twice and disagree.
- **An annotator's scale answer is an `Integer`.** `Resolution.scaleValue` is a `Double` so the arithmetic mean required by rule 10 retains fractional results.
- **Detection resolution needs a selected-annotation relationship.** Rule 16 requires `Resolution` to identify one submitted `Annotation` for the same item; that annotation's `BoundingBox` records provide the complete final set. The current scalar-only model does not yet represent this relationship. It must be added for [#34] and [#37], while preserving unselected submissions.
- **A split names its items through `SplitItem`.** Membership is a record of its own, not a copy of the items. Generate it through count-based batching before that split's first assignment; there is no manual membership editor. Retain it unchanged afterwards, including when an item is excluded (rules 6/14). Retirement after generation does not alter membership; exclude retired members from work and reject new assignments with no remaining non-retired members.
- ***k* and allocation metadata describe the split, not an assignment.** Every annotator on a split sees the same items. Count-only creation uses one specified seeded shuffle over a stable input order. Persist the actual seed, requested batch size and generated `SplitItem` membership/order; reopening uses saved membership, not a new allocation. Seed alone does not reconstruct past inputs (rule 7, [#28]). The current model still needs obsolete strategy values/contracts removed and requested batch size represented.
- **`Label` has no soft-delete flag.** An unused label can be deleted during setup only. Once the project has been assigned, taxonomy writes and deletion are forbidden; there is no annotation-deletion cascade (rules 3/5).
- **Assignment completion is automatic and one-way.** When every non-retired required item is terminally handled, the assignment becomes SUBMITTED; exclusion may remove its final outstanding requirement. There is no final batch-submit, rework or backward-navigation state. Remove obsolete `RETURNED` and return metadata during model cleanup ([#12], [#17]).
- **Flags support keep/exclude review.** The accepted values are PENDING, EXCLUDED and KEPT; REPAIRED is obsolete and awaits model cleanup. Reporter content becomes immutable with the terminal item submission. Adjudicator disposition/reviewer/time may change only before COMPLETE; KEEP cannot create an answer or unretire the item ([#35], [#36]).
- **Settings that lock at creation are not `final`.** ORMLite builds rows through a no-arg constructor and then sets fields reflectively, so `final` would mean hand-written mappers. Rule 4 is enforced in `arbiter.service` instead, like every other rule about the data.
- **Defaults live in the service, not the model.** A model class stores what was chosen; it never decides. *k* defaults to 2 in `AssignmentService`, so `Split.annotationsPerItem` stays null until a split is cut and assigned.

## Services

- `AuthService`: sole-owner bootstrap, login/session and shared credential validation/hashing (PBKDF2 or bcrypt with a per-user salt). Account operations enforce rule 12: owner-created annotators, annotator deactivation and direct annotator password replacement; no role change or reset-code flow.
- `WorkspaceService`: first-run setup, the lock, paths.
- `ProjectService`, `CorpusService`: project completion/deletion guards, import, splits and taxonomy, respecting project freeze and completion.
- `AssignmentService`: create assignments to distinct active annotators up to the fixed *k*, counting all existing assignments including disabled owners. Reject ownership/split changes, individual deletion and duplicate split/annotator assignments even before work starts. Persist capacity/uniqueness checks and first-assignment locks atomically; COMPLETE forbids new assignments. Normal queue/status updates remain allowed before completion. Whole incomplete-project deletion belongs only to the separate confirmed project operation (rule 19).
- `AnnotationService`: current-draft autosave, atomic one-way answer/report submission and queue advancement, immutable-content guards, flags, and the annotator-scoped read path (rule 1).
- `ResolutionService`: branch on task type first. Classification uses strict majority and disputes for `SINGLE`, arithmetic mean for `SCALE` (rule 10, [#27]); detection requires manual selection of one complete submitted box set (rule 16, [#34]). No automatic detection matching. Re-running resolution with unchanged inputs is idempotent.
- `ExportService`: the only code that knows about CSV, JSON and COCO. Annotators persist canonical annotations and never choose a format. Write one dataset with current provenance; no train/validation/test partitioning in v1 (rule 15).

[#4]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/4
[#5]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/5
[#6]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/6
[#7]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/7
[#8]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/8
[#9]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/9
[#10]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/10
[#11]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/11
[#12]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/12
[#17]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/17
[#18]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/18
[#19]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/19
[#20]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/20
[#21]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/21
[#23]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/23
[#25]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/25
[#26]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/26
[#27]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/27
[#28]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/28
[#31]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/31
[#33]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/33
[#34]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/34
[#35]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/35
[#36]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/36
[#37]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/37
