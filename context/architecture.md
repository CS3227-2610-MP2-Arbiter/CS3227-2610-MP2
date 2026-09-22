# Architecture

Written for agents: rules as tables and bullets, with no diagrams or rationale. The design, its reasoning and the test levels are in the "Design" and "Testing" sections of [docs/DeveloperGuide.md](../docs/DeveloperGuide.md), written for human readers. "Rule N" means rule N in section 3 of [docs/UserFlows.md](../docs/UserFlows.md); product rules live there and in the GitHub issues, and this file only says where the code enforces them.

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
| 5 | `arbiter.workspace` | Workspace paths, the single-writer lock, asset resolution. | Whimsyturtle (paths [#9], lock [#61]); zheng-jj (asset resolution [#10]) |

## Rules

### Shared code

- `AnnotationEditor`, `BoxCanvas` and `ItemView` exist only in `arbiter.ui.shared`. A role difference is a mode flag on the shared component, never a second implementation.
- `AnnotationEditor` edits only the current annotator draft. Adjudicator screens use the shared label picker and a read-only `BoxCanvas`, and never edit a submission (rule 13).
- Every rule about the data lives in `arbiter.service`, never in a controller or a repository. Services check the project seal (rule 9) and setup freezes (rules 3, 14) inside the write transaction; a disabled control is not enforcement.

### Blindness (rule 1)

- Annotator-facing code loads annotations only through `AnnotationService.forCurrentUser(...)`, which scopes every read to the session user. That path never loads resolved labels or agreement statistics.
- Do not rely on package separation for blindness.
- The blindness test ([#11]) must cover every annotator-facing code path, including new ones.

### Source media (rule 21)

- Every source read goes through the one resolver in `arbiter.workspace` ([#10]), which checks containment and content. Caches never bypass it.

### Persistence

- One SQLite file at `<workspace>/arbiter.db`, with migrations applied on open and foreign keys enabled.
- Commit each completed logical action immediately (rule 2). Repository calls within one action share [#6]'s transaction and never commit on their own.
- Code against the repository interfaces in `arbiter.data`. Their implementations in `arbiter.data.sqlite` map rows with ORMLite, not Hibernate, and are the only code with SQL.
- Take the workspace lock before writing, and refuse to open a second writer (rule 11).

### UI

- JavaFX, with one `Stage` whose content area is swapped, routed by role after login ([#5]).
- Report errors through the one convention in `arbiter.ui.shared` ([#8]).
- Controllers call services and bind results to the view. They hold no business rules and no SQL.

### General

- Nothing that assumes a backend: no background jobs, webhooks or message queues (rule 8).
- No plugin system and no dependency-injection framework.
- Java lines are at most 120 characters (Checkstyle in `config/checkstyle`). Markdown is exempt; see "Markdown" in [docs/DeveloperGuide.md](../docs/DeveloperGuide.md#software-engineering-process).

## Model

Model classes are plain value objects in `arbiter.model`, grouped into subpackages by area (`user`, `project`, `annotation`, `resolution`) that mirror `arbiter.data`. What each class and field means is in its Javadoc.

- No queries, no UI logic and no annotations. The ORM mapping is designed once in `arbiter.data.sqlite`, alongside the schema.
- Tests must be able to construct every model class without a database.
- Fields are not `final`. Settings fixed at creation (rule 4) are enforced in services.
- A model class stores what was chosen and never decides. Defaults are applied by services: *k*'s by `AssignmentService`, so `Split.annotationsPerItem` is null until then.

## Services

- `AuthService`: sole-owner bootstrap, login/session, annotator accounts and password replacement (rule 12). Bootstrap, annotator creation and replacement share one username/password validation and salted-hashing boundary (PBKDF2 or bcrypt with a per-user salt).
- `WorkspaceService`: first-run setup, the lock, paths.
- `ProjectService`, `CorpusService`: project completion and deletion, import, splits and taxonomy.
- `AssignmentService`: assignments and the first-assignment freezes (rules 3, 14, 19).
- `AnnotationService`: draft autosave, submission and queue advancement (rule 18), flags, and the annotator-scoped read path (rule 1).
- `ResolutionService`: automatic and manual resolution, branching on task type first (rules 10, 16).
- `ExportService`: the only code that knows about output formats. Annotators persist canonical annotations and never choose a format.

[#4]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/4
[#5]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/5
[#6]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/6
[#8]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/8
[#9]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/9
[#10]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/10
[#11]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/11
[#61]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/61
