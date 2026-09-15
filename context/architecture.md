# Architecture

Written for agents: rules as tables and bullets, with no diagrams or rationale. The design, its
reasoning and the test levels are in the "Design" and "Testing" sections of
[docs/DeveloperGuide.md](../docs/DeveloperGuide.md), written for human readers. "Rule N" means rule N
in section 3 of [docs/UserFlows.md](../docs/UserFlows.md), and section 5 there maps issue codes such
as `A0` to GitHub issues.

## Packages

Layers run from 1 (top) to 5 (bottom). Dependencies point downward only, and neither role package
imports the other; they meet in `arbiter.ui.shared` and `arbiter.service`.

| Layer | Package | Owns | Owner |
| --- | --- | --- | --- |
| 1 | `arbiter.ui.annotator` | The annotator screens, using the shared components. | zheng-jj (`B*`) |
| 1 | `arbiter.ui.adjudicator` | The adjudicator screens, using the shared components. | Whimsyturtle (`C*`) |
| 2 | `arbiter.ui.shared` | Shell, navigation, routing, UI kit, error handling, and the annotation components. | zheng-jj (`A1`, `A4`) |
| 3 | `arbiter.service` | All business rules: assignment, resolution, export, earnings, access scoping. | Shared (`A0`) |
| 4 | `arbiter.data` | Repository interfaces. Signatures only, no SQL. | Shared (`A0`) |
| 4 | `arbiter.data.sqlite` | The ORM mappings, schema and migrations. | Whimsyturtle (`A2`) |
| 4 | `arbiter.model` | Value objects and enums. No queries, no UI logic. | Shared (`A0`) |
| 5 | `arbiter.workspace` | Workspace paths, the single-writer lock, asset resolution. | Shared (`A5`, `A6`) |

## Rules

### Shared code

- `AnnotationEditor`, `BoxCanvas` and `ItemView` exist only in `arbiter.ui.shared`. A role difference
  is a mode flag on the shared component, never a second implementation.
- `AnnotationEditor` is editable for annotators and for adjudicators supplying their own label (`C12`)
  or repairing flagged items (`C13`), and read-only in the resolution screen (`C12`).
- Every rule about the data lives in `arbiter.service`, never in a controller or a repository.

### Blindness (rule 1)

- Annotator-facing code loads annotations only through `AnnotationService.forCurrentUser(...)`, which
  scopes every read to the session user. That path never loads resolved labels or per-item agreement
  stats.
- Do not rely on package separation for blindness.
- The blindness test (`A7`) must cover every annotator-facing code path, including new ones.

### Data

- Never store money: no `balance` column and no ledger table. Earnings are derived from annotations
  and per-item rewards (`B9`).
- Resolution never deletes the losing annotations; `C14` and `C15` need them.
- Retire users and items instead of deleting them (rule 5). Deleting a label is the exception: it
  removes the annotations that used it and returns those items to unannotated (`C4`, rule 3).

### Persistence

- One SQLite file at `<workspace>/arbiter.db`, with migrations applied on open and foreign keys
  enabled.
- Commit every annotator action immediately (rule 2).
- Code against the repository interfaces in `arbiter.data`. Their implementations in
  `arbiter.data.sqlite` map rows with ORMLite, not Hibernate, and are the only code with SQL.
- Take the workspace lock before writing, and refuse to open a second writer (rule 11).

### UI

- JavaFX, with one `Stage` whose content area is swapped, routed by role after login (`A1`).
- Report errors through the one convention in `arbiter.ui.shared` (`A4`).
- Controllers call services and bind results to the view. They hold no business rules and no SQL.

### General

- Nothing that assumes a backend: no background jobs, webhooks or message queues (rule 8).
- No plugin system and no dependency-injection framework.
- Lines are at most 120 characters (Checkstyle in `config/checkstyle`).

## Model

Classes in `arbiter.model` carry ORM mapping annotations but no queries or UI logic, and tests must be
able to construct them without a database.

- Classes: `User`, `Project`, `Label`, `Item`, `Split`, `Assignment`, `Annotation`, `BoundingBox`,
  `Resolution`, `Flag`.
- Enums: `Role`, `TaskType`, `SourceType`, `TaxonomyKind`, `OutputFormat`, `AssignmentStatus`,
  `ResolutionMethod`, `FlagReason`.

## Services

- `AuthService`: login, session, password hashing (PBKDF2 or bcrypt with a per-user salt).
- `WorkspaceService`: first-run setup, the lock, paths.
- `ProjectService`, `CorpusService`: import, splits, taxonomy.
- `AssignmentService`: assignment, *k*, load, completion and the reward lock.
- `AnnotationService`: autosave, submit, flags, and the annotator-facing read path (rule 1).
- `ResolutionService`: majority, ties and disputes; idempotent.
- `EarningsService`: derived earnings, released versus pending.
- `ExportService`: the only code that knows about CSV, JSON and COCO. Annotators persist canonical
  annotations and never choose a format.
