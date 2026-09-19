# Architecture

Written for agents: rules as tables and bullets, with no diagrams or rationale. The design, its reasoning and the test levels are in the "Design" and "Testing" sections of [docs/DeveloperGuide.md](../docs/DeveloperGuide.md), written for human readers. "Rule N" means rule N in section 3 of [docs/UserFlows.md](../docs/UserFlows.md).

## Packages

Layers run from 1 (top) to 5 (bottom). Dependencies point downward only, and neither role package imports the other; they meet in `arbiter.ui.shared` and `arbiter.service`.

| Layer | Package | Owns | Owner |
| --- | --- | --- | --- |
| 1 | `arbiter.ui.annotator` | The annotator screens, using the shared components. | zheng-jj (`B*`) |
| 1 | `arbiter.ui.adjudicator` | The adjudicator screens, using the shared components. | Whimsyturtle (`C*`) |
| 2 | `arbiter.ui.shared` | Shell, navigation, routing, UI kit, error handling, and the annotation components. | zheng-jj ([#5], [#8]) |
| 3 | `arbiter.service` | All business rules: assignment, resolution, export, earnings, access scoping. | Shared ([#4]) |
| 4 | `arbiter.data` | Repository interfaces. Signatures only, no SQL. | Shared ([#4]) |
| 4 | `arbiter.data.sqlite` | The ORM mappings, schema and migrations. | Whimsyturtle ([#6]) |
| 4 | `arbiter.model` | Value objects and enums. No queries, no UI logic. | Shared ([#4]) |
| 5 | `arbiter.workspace` | Workspace paths, the single-writer lock, asset resolution. | Shared ([#9], [#10]) |

## Rules

### Shared code

- `AnnotationEditor`, `BoxCanvas` and `ItemView` exist only in `arbiter.ui.shared`. A role difference is a mode flag on the shared component, never a second implementation.
- `AnnotationEditor` is editable for annotators and for adjudicators supplying their own label ([#34]) or repairing flagged items ([#35]), and read-only in the resolution screen ([#34]).
- Every rule about the data lives in `arbiter.service`, never in a controller or a repository.

### Blindness (rule 1)

- Annotator-facing code loads annotations only through `AnnotationService.forCurrentUser(...)`, which scopes every read to the session user. That path never loads resolved labels or per-item agreement stats.
- Do not rely on package separation for blindness.
- The blindness test ([#11]) must cover every annotator-facing code path, including new ones.

### Data

- Never store money: no `balance` column and no ledger table. Earnings are derived from annotations and per-item rewards ([#20]).
- Resolution never deletes the losing annotations; [#36] and [#37] need them.
- Retire users and items instead of deleting them (rule 5). Deleting a label is the exception: it removes the annotations that used it and returns those items to unannotated ([#26], rule 3).

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

Classes in `arbiter.model` carry ORM mapping annotations but no queries or UI logic, and tests must be able to construct them without a database.

- Classes: `User`, `Project`, `Label`, `Item`, `Split`, `Assignment`, `Annotation`, `BoundingBox`, `Resolution`, `Flag`.
- Enums: `Role`, `TaskType`, `SourceType`, `TaxonomyKind`, `OutputFormat`, `AssignmentStatus`, `ResolutionMethod`, `FlagReason`.

## Services

- `AuthService`: login, session, password hashing (PBKDF2 or bcrypt with a per-user salt).
- `WorkspaceService`: first-run setup, the lock, paths.
- `ProjectService`, `CorpusService`: import, splits, taxonomy.
- `AssignmentService`: assignment, *k*, load, completion and the reward lock.
- `AnnotationService`: autosave, submit, flags, and the annotator-facing read path (rule 1).
- `ResolutionService`: majority, ties and disputes; idempotent.
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
[#34]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/34
[#35]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/35
[#36]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/36
[#37]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/37
