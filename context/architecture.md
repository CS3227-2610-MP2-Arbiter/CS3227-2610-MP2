# Architecture

Written for agents: rules as tables and bullets, with no diagrams or rationale. The design, its reasoning and the test levels are in the "Design" and "Testing" sections of [docs/DeveloperGuide.md](../docs/DeveloperGuide.md), written for human readers. "Rule N" means rule N in section 3 of [docs/UserFlows.md](../docs/UserFlows.md).

## Packages

Layers run from 1 (top) to 5 (bottom). Dependencies point downward only, and neither role package imports the other; they meet in `arbiter.ui.shared` and `arbiter.service`.

| Layer | Package | Owns | Owner |
| --- | --- | --- | --- |
| 1 | `arbiter.ui.annotator` | The annotator screens, using the shared components. | zheng-jj (annotator track) |
| 1 | `arbiter.ui.adjudicator` | The adjudicator screens, using the shared components. | Whimsyturtle (adjudicator track) |
| 2 | `arbiter.ui.shared` | Shell, navigation, routing, UI kit, error handling, and the annotation components. | zheng-jj ([#5], [#8]) |
| 3 | `arbiter.service` | All business rules: assignment, resolution, export, earnings, access scoping. | Shared ([#4]) |
| 4 | `arbiter.data` | Repository interfaces. Signatures only, no SQL. | Shared ([#4]) |
| 4 | `arbiter.data.sqlite` | The ORM mappings, schema and migrations. | Whimsyturtle ([#6]) |
| 4 | `arbiter.model` | Value objects and enums. No queries, no UI logic. | Shared ([#4]) |
| 5 | `arbiter.workspace` | Workspace paths, the single-writer lock, asset resolution. | Shared ([#9], [#10]) |

## Rules

### Shared code

- `AnnotationEditor`, `BoxCanvas` and `ItemView` exist only in `arbiter.ui.shared`. A role difference is a mode flag on the shared component, never a second implementation.
- `AnnotationEditor` is editable for annotators, for adjudicators supplying their own classification label ([#34]), and in the separate flagged-item repair flow ([#35]). Submitted answers are read-only during comparison; detection resolution uses a read-only `BoxCanvas` and selects one complete submitted box set ([#34], rule 16).
- Every rule about the data lives in `arbiter.service`, never in a controller or a repository.

### Blindness (rule 1)

- Annotator-facing code loads annotations only through `AnnotationService.forCurrentUser(...)`, which scopes every read to the session user. That path never loads resolved labels or per-item agreement stats.
- Do not rely on package separation for blindness.
- The blindness test ([#11]) must cover every annotator-facing code path, including new ones.

### Data

- Never store money: no `balance` column and no ledger table. Earnings are derived from eligible submitted work and per-item rewards ([#20]).
- Resolution never deletes the losing annotations; [#36] and [#37] need them.
- Preserve annotations/attribution when deactivating users or retiring items; item retirement is limited to setup before assignment and the pre-completion flagged exclusion exception. Label deletion is only for unused labels before first assignment; never cascade it into annotations ([#26], rules 3/5). Completed projects cannot be deleted.

### Lifecycle (rules 3, 5, 6, 9, 13, 14)

- First assignment freezes corpus and taxonomy project-wide; each split's first assignment locks its definition, including membership/order, `annotationsPerItem` and `itemReward`. Persist the first-assignment evidence/locks atomically with assignment creation. Removing the last current assignment must not unfreeze setup.
- `SplitItem` records stay unchanged after assignment. Pre-completion flagged exclusion changes item retirement, retaining memberships and all answers/provenance; it does not move or delete memberships.
- `Project.complete` seals all project-owned records. Every mutating service must check the seal in the transaction that writes, including autosave, assignment/return, flag batches, resolution and deletion. UI controls alone are insufficient. Completion commits atomically; export/viewing may read but never mutate sealed data.
- Earnings read sealed inputs after completion and must not filter away contributions merely because a user is disabled. No stored balance/ledger is introduced.
- Return/repair resolution invalidation and account replacement remain explicit follow-ups before implementing those flows. By-reference source integrity also needs validation: freezing database records alone cannot prevent external file changes.

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
- **A split names its items through `SplitItem`.** Membership is a record of its own, not a copy of the items. Configure it before that split's first assignment; retain it unchanged afterwards, including when an item is excluded (rules 6/14).
- ***k* and the seed live on `Split`, not `Assignment`.** Both describe the work rather than one annotator's link to it: every annotator on the split sees the same items, and rule 7 needs the seed kept so a split can be reproduced.
- **`Label` has no soft-delete flag.** An unused label can be deleted during setup only. Once the project has been assigned, taxonomy writes and deletion are forbidden; there is no annotation-deletion cascade (rules 3/5).
- **A submitted assignment can be returned, and the return is recorded.** `AssignmentStatus.RETURNED` exists because an adjudicator may send an assignment back for rework before project completion ([#12], [#17]), and `Assignment` carries `returnedAt`, `returnedByUserId` and `returnReason` so rule 13's "recorded" is true and the annotator learns what to fix.
- **Every flag has a disposition.** `FlagDisposition` is `PENDING`, `EXCLUDED`, `REPAIRED` or `KEPT`, so the review queue empties once each flag is dealt with ([#35]) and the disposition can be shown per item ([#36]). Excluding before completion retires the item while preserving its records, so `Item.retired` stays the one place that decides whether an item is in the dataset; COMPLETE blocks all disposition changes.
- **Settings that lock at creation are not `final`.** ORMLite builds rows through a no-arg constructor and then sets fields reflectively, so `final` would mean hand-written mappers. Rule 4 is enforced in `arbiter.service` instead, like every other rule about the data.
- **Defaults live in the service, not the model.** A model class stores what was chosen; it never decides. *k* defaults to 2 in `AssignmentService`, so `Split.annotationsPerItem` stays null until a split is cut and assigned.

## Services

- `AuthService`: login, session, password hashing (PBKDF2 or bcrypt with a per-user salt).
- `WorkspaceService`: first-run setup, the lock, paths.
- `ProjectService`, `CorpusService`: project completion/deletion guards, import, splits and taxonomy, respecting project freeze and completion.
- `AssignmentService`: assignment, *k*, load, first-assignment freeze/locks and the completion guard.
- `AnnotationService`: autosave, submit, flags, and the annotator-facing read path (rule 1).
- `ResolutionService`: branch on task type first. Classification uses strict majority and disputes for `SINGLE`, arithmetic mean for `SCALE` (rule 10, [#27]); detection requires manual selection of one complete submitted box set (rule 16, [#34]). No automatic detection matching. Re-running resolution with unchanged inputs is idempotent.
- `EarningsService`: derived earnings, released versus pending.
- `ExportService`: the only code that knows about CSV, JSON and COCO. Annotators persist canonical annotations and never choose a format.

[#4]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/4
[#5]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/5
[#6]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/6
[#8]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/8
[#9]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/9
[#10]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/10
[#11]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/11
[#20]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/20
[#26]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/26
[#27]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/27
[#34]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/34
[#35]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/35
[#36]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/36
[#37]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/37
