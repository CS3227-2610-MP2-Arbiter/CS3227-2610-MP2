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
| 4 | `arbiter.data` | Repository interfaces. Signatures only, no storage logic. | Shared ([#4]) |
| 4 | `arbiter.data.json` | JSON snapshot access and repository implementations. | Whimsyturtle ([#6]) |
| 4 | `arbiter.model` | Value objects and enums. No queries, no UI logic. | Shared ([#4]) |
| 5 | `arbiter.workspace` | Workspace paths, the single-writer lock, asset resolution. | Whimsyturtle (paths [#9], lock [#61]); zheng-jj (asset resolution [#10]) |

## Rules

### Shared code

- `AnnotationEditor` and `ItemView` exist only in `arbiter.ui.shared`. A role difference is a mode flag on the shared component, never a second implementation.
- `AnnotationEditor` holds only the annotator's current unsubmitted choice. Adjudicator screens reuse the label picker for resolution and never edit a submission (rule 13).
- Every rule about the data lives in `arbiter.service`, never in a controller or a repository. Services check setup freezes and the pre-assignment deletion guard (rules 3, 5, 14) inside the write transaction; a disabled control is not enforcement. For a whole project (rules 3 and 5), they use `FirstAssignment` rather than their own check.

### Blindness (rule 1)

- Annotator-facing code loads annotations only through `AnnotationService.forCurrentUser(...)`, which scopes every read to the session user. That path never loads resolved labels or another annotator's work.
- Do not rely on package separation for blindness.
- The blindness test ([#11]) must cover every annotator-facing code path, including new ones.
- `AnnotatorBlindnessTest` treats every class under `arbiter.ui` except `arbiter.ui.adjudicator` as annotator-facing, shared components included, and fails if their calls reach another annotator's answers, a resolved result, cross-annotator progress or an adjudicator screen. A shared component therefore never loads or accepts a `Resolution`, even in adjudicator mode; the adjudicator's screen passes it plain values such as a `Label`. Service reads it trusts are listed in that test; adding one is a reviewed change, and its scoping must be tested by its feature.

### Source media (rule 21)

- Every source read goes through the one resolver in `arbiter.workspace` ([#10]), which checks containment and content. Caches never bypass it.

### Persistence

- `WorkspacePaths.DATA_FILE` names the shared snapshot in rule 11; `JsonStore` opens and validates its version and integrity.
- Commit each completed logical action immediately (rule 2). Repository calls within one `JsonStore.write` action change a private snapshot; `JsonStore` publishes it with one atomic replacement.
- Code against the repository interfaces in `arbiter.data`. Their implementations and storage-level validation live in `arbiter.data.json`; business rules remain in services.
- `WorkspaceSetupDialog` validates layout, acquires `WorkspaceLock`, and uses lock-aware `JsonStore` entry points; `Arbiter` closes the handle when the app closes (rule 11, [#61]).

### UI

- JavaFX, with one `Stage` whose content area is swapped, routed by role after login ([#5]).
- Report errors through the one convention in `arbiter.ui.shared` ([#8]): a screen catches only the exceptions of the action it runs and shows them with `Dialogs.showError`, or inline with `ErrorMessages.of` when the user can correct the input on the same form. Anything else reaches the uncaught-exception handler `Arbiter` installs. What the user reads is decided by `ErrorMessages`, whose Javadoc says which exceptions' messages must be safe to show.
- Only `DiagnosticLog` logs, only `Dialogs` builds dialogs, and colours, fonts and spacing live in `arbiter.css` behind `Styles`. No code sets inline styles, prints to `System.out` or `System.err`, or calls `printStackTrace`; `UiConventionTest` enforces all of this.
- Controllers call services and bind results to the view. They hold no business rules and do not access repositories directly.

### General

- Nothing that assumes a backend: no background jobs, webhooks or message queues (rule 8).
- No plugin system and no dependency-injection framework.
- Accept only ASCII in user-entered text that the code counts, compares or matches, such as usernames, passwords and project names. Allow Unicode only where a feature needs it or the text is only stored and shown, such as source text and descriptions.
- Do not reinvent the wheel: use the Java standard library where it suffices, and otherwise a reputable, maintained third-party library. Never hand-write a solved problem (e.g., JSON parsing).
- Use a record for any class that only carries immutable data, instead of hand-writing its constructor, accessors, `equals` and `hashCode`. Model classes are the exception, because their fields are not `final` (see [Model](#model)).
- Java lines are at most 120 characters (Checkstyle in `config/checkstyle`). Markdown is exempt; see "Markdown" in [docs/DeveloperGuide.md](../docs/DeveloperGuide.md#software-engineering-process).

## Model

Model classes are plain value objects in `arbiter.model`, grouped into subpackages by area (`user`, `project`, `annotation`, `resolution`) that mirror `arbiter.data`. What each class and field means is in its Javadoc.

- No queries, no UI logic and no persistence annotations. JSON mapping belongs in `arbiter.data.json`.
- Tests must be able to construct every model class without a data store.
- Fields are not `final`. Settings fixed at creation (rule 4) are enforced in services.
- A model class stores what was chosen and never decides. Defaults are applied by services: *k*'s by `AssignmentService`, so `Split.annotationsPerItem` is null until then.

## Services

- `AuthService`: sole-owner bootstrap, login/session, annotator accounts and password replacement (rule 12). Bootstrap, annotator creation and replacement share one username/password validation and salted-hashing boundary (PBKDF2 or bcrypt with a per-user salt).
- `WorkspaceService`: first-run setup and paths; `WorkspaceLock` owns the file lock ([#61]).
- `ProjectService`: creating, listing and pre-assignment deletion of projects (rule 5). It has no way to change a project's kind or format (rule 4).
- `CorpusService`: import, splits and taxonomy.
- `AssignmentService`: assignments and the first-assignment freezes (rules 3, 14, 19).
- `AnnotationService`: atomic submission and queue advancement (rule 18), plus the annotator-scoped read path (rule 1).
- `ResolutionService`: automatic and manual classification resolution (rule 10).
- `ExportService`: the only code that writes a dataset in an output format. Annotators persist canonical annotations and never choose a format.

[#4]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/4
[#5]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/5
[#6]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/6
[#8]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/8
[#9]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/9
[#10]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/10
[#11]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/11
[#61]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/61
