# Architecture

Arbiter is an offline Java desktop app for teams that label data. Two people use it: **annotators**,
who label items one at a time from a blind queue, and **adjudicators**, who import a corpus, split it,
assign annotators, resolve disagreements and export the finished dataset.

This document is the design the code follows. It is prescriptive about where things live, because the
two roles operate on the same data from different directions and the boundaries between them are where
mistakes would otherwise be expensive.

## The shape of the problem

One constraint drives almost every decision below.

**It runs locally.** There is no server, no network and no accounts department. One SQLite file lives
in a workspace folder, reached over a shared drive, and JavaFX talks to it directly. Anything that
assumes a backend - background jobs, webhooks, a message queue - is off the table. When the app is not
running, nothing is happening, which is a genuinely simplifying fact and worth exploiting: there are no
eventual-consistency problems, no retries and no distributed state.

Beyond that, the choices are about keeping the code honest. There is no plugin system and no
dependency-injection framework, because a container would add indirection to an app with a dozen
screens and one database file. We do use an ORM: hand-mapping eleven tables is exactly the kind of
boilerplate it removes.

## The two roles are not independent

The most important thing to understand about this design is that **the roles are two views on one
workflow, not two applications**. They are producer and consumer on the same data:

- The annotator produces an `Annotation`: a label, a rationale, optional boxes, a flag.
- The adjudicator consumes those annotations: compares them, resolves disagreements, and exports them.

Several places make the coupling unavoidable:

- **Manual resolution (`C12`)** shows competing annotations side by side, with the item, rationale text
  and boxes. The adjudicator screen has to render an annotation exactly as the annotator made it.
- **Box geometry (`B4`)** is non-trivial: drag to draw, snap, clamp to bounds, handle editing, and
  coordinates that stay correct at any zoom level. Two implementations of that would drift.
- **Adjudicators also annotate.** `C12` lets them "supply their own label", and `C13` lets them repair
  flagged items, so they need the same editing widgets the annotator uses.

Designing these as two separate silos would mean duplicating the hardest UI code in the app and then
keeping the copies in agreement. That is the opposite of DRY, and it is where a subtle disagreement
between two coordinate transforms would hide.

So the sharing is deliberate, and it is concentrated in two places:

1. **`arbiter.ui.shared` owns the annotation vocabulary.** `AnnotationEditor`, `BoxCanvas` and
   `ItemView` live here, used by both roles. The annotator uses `AnnotationEditor` in edit mode; the
   adjudicator uses the same component read-only in the resolution screen, and in edit mode when
   supplying their own label. `BoxCanvas` is written once, so zoom and clamping behave identically
   wherever a box appears. Role differences are **configuration, not duplication**: one component, a
   mode flag.
2. **`arbiter.service` owns every rule about the data.** Both roles call the same services, so a rule
   cannot be implemented twice with two different answers.

### Where blindness is actually enforced

Rule 1 says an annotator must never see another annotator's annotation, a resolved label, or a
per-item agreement stat. It is tempting to enforce that by keeping the packages apart. **That does not
work here**, precisely because we are now sharing components: a shared `AnnotationEditor` can be
handed any annotation, so package isolation would give false confidence while providing no guarantee.

Blindness is enforced where it can be seen and tested:

- **At the query boundary.** The annotator-facing path loads annotations through
  `AnnotationService.forCurrentUser(...)`, which scopes every read to the session's user. A resolved
  label is not a kind of annotation the annotator path ever loads, so it cannot leak by accident.
- **By an explicit test.** `A7` includes a blindness test that fails if any annotator-facing code path
  can reach another annotator's annotation. This is the core invariant of the product and it is worth
  a test rather than trusting review.

This is strictly stronger than separation: it holds for code written later, by anyone, in any package.

## Layers

```
        +---------------------------+   +----------------------------+
        |   arbiter.ui.annotator    |   |  arbiter.ui.adjudicator    |
        |   (zheng-jj)              |   |  (Whimsyturtle)            |
        +------------+--------------+   +-------------+--------------+
                     |         \            /         |
                     |          v          v          |
                     |      +----------------------+  |
                     |      |  arbiter.ui.shared   |  |
                     |      |  shell, routing,     |  |
                     |      |  AnnotationEditor,   |  |
                     |      |  BoxCanvas, ItemView |  |
                     |      +----------------------+  |
                     |                 |              |
                     v                 v              v
        +------------------------------------------------------------+
        |                    arbiter.service                        |
        |  AssignmentService  ResolutionService  ExportService  ...  |
        +------------------------------------------------------------+
                     |                                |
                     v                                v
        +---------------------------+   +----------------------------+
        |      arbiter.data         |   |       arbiter.model        |
        |  repository interfaces    |   |   value objects, enums     |
        |  + arbiter.data.sqlite    |   |                            |
        |    (ORM mappings)         |   |                            |
        +---------------------------+   +----------------------------+
                                    |
                                    v
                          +---------------------+
                          |  arbiter.workspace  |
                          |  paths, lock,       |
                          |  migrations         |
                          +---------------------+
```

Dependencies point downward only, and **neither role package imports the other** - they meet in
`arbiter.ui.shared` and `arbiter.service`. That rule is not there to keep the roles independent; they
are not. It is there so that the shared code has exactly one home and neither track can quietly grow a
private copy of it.

| Package | Owns | Owner |
| --- | --- | --- |
| `arbiter.model` | Value objects and enums. No queries, no UI logic. | Shared (`A0`) |
| `arbiter.data` | Repository interfaces. Signatures only, no SQL. | Shared (`A0`) |
| `arbiter.data.sqlite` | The ORM mappings, schema and migrations. | Whimsyturtle (`A2`) |
| `arbiter.workspace` | Workspace paths, the single-writer lock, asset resolution. | Shared (`A5`, `A6`) |
| `arbiter.service` | All business rules: assignment, resolution, export, earnings, access scoping. | Shared (`A0`) |
| `arbiter.ui.shared` | Shell, navigation, routing, UI kit, error handling, and the annotation components. | zheng-jj (`A1`, `A4`) |
| `arbiter.ui.annotator` | The annotator screens, using the shared components. | zheng-jj (`B*`) |
| `arbiter.ui.adjudicator` | The adjudicator screens, using the shared components. | Whimsyturtle (`C*`) |

Because `ui.shared` is on both critical paths, it is the one package that must be settled first.
`A0` (model and repository interfaces) and `A4` (UI kit and error handling) come before feature code
for exactly this reason.

## Model

Value objects in `arbiter.model`, agreed in `A0` before either track starts. They carry mapping
annotations for the ORM but no queries and no UI logic, so both tracks can construct them in tests
without a database.

`User`, `Project`, `Label`, `Item`, `Split`, `Assignment`, `Annotation`, `BoundingBox`, `Resolution`,
`Flag`.

Enums: `Role`, `TaskType`, `SourceType`, `TaxonomyKind`, `OutputFormat`, `AssignmentStatus`,
`ResolutionMethod`, `FlagReason`.

Three things are worth settling explicitly because they are easy to get wrong and expensive to change:

- **Money is never stored.** `B9` derives earnings from annotations and per-item rewards. There is no
  `balance` column and no ledger table, so earnings cannot drift out of agreement with the data they
  are computed from.
- **Annotations are never silently discarded.** When resolution picks a winner, the losing annotations
  stay, because `C14` and `C15` need them for provenance.
- **Deletion is usually soft.** Users, labels and items are retired rather than purged, so historical
  annotations stay interpretable. The exception is a label deletion, which genuinely removes the
  annotations that used it and returns those items to unannotated (`C4`).

## Persistence

One SQLite file at `<workspace>/arbiter.db`, with migrations applied on open and foreign keys enabled.
Write-through: every annotator action commits immediately, because a power cut must not lose work
(rule 2). Repositories are interfaces in `arbiter.data` with implementations in `arbiter.data.sqlite`,
so the two tracks can code against signatures before the SQL exists - that is the whole reason `A0`
comes before `A2`.

**A lightweight ORM, not Hibernate.** `arbiter.data.sqlite` maps rows onto the model objects with
ORMLite rather than by hand-written `ResultSet` unpacking, so the tables do not each need their own
boilerplate. Hibernate was rejected: it wants a session lifecycle and manages lazy associations that
make little sense in a desktop app holding one connection and passing detached objects to the UI.
ORMLite gives the mapping without the lifecycle. The cost is that complex queries still drop to raw
SQL - which stays inside the repository implementations, so repositories remain the only component
that knows about persistence.

**Single writer.** The database sits on a shared drive (rule 11). SQLite is not safe against concurrent
writers over a network share, so `arbiter.workspace` takes an advisory lock on the workspace and
Arbiter refuses to open a second writer. The lock makes the failure explicit instead of silent
corruption.

## Services

Business rules live in `arbiter.service`, not in controllers and not in repositories. Both UIs call
the same services, which is what keeps a rule from being implemented twice - the specific risk the
flows call out for the exporter.

- `AuthService` - login, session, password hashing (PBKDF2 or bcrypt with a per-user salt).
- `WorkspaceService` - first-run setup, the lock, paths.
- `ProjectService`, `CorpusService` - import, splits, taxonomy.
- `AssignmentService` - assignment, *k*, load, completion and the reward lock.
- `AnnotationService` - autosave, submit, flags, and **the annotator-facing read path**, which scopes
  every load to the session user. This is where rule 1 is enforced.
- `ResolutionService` - majority, ties and disputes; idempotent.
- `EarningsService` - derived earnings, released versus pending.
- `ExportService` - the only place that knows about COCO, YOLO, Pascal VOC, CSV and JSON.

`ExportService` deserves a note: annotators persist canonical annotations and never choose a format,
so export formatting exists in exactly one place. Building it twice is the most expensive mistake
available in this codebase.

## UI

JavaFX, one `Stage` with a swappable content area, routed by role after login (`A1`). Each role has its
own package and its own screens; `arbiter.ui.shared` holds the shell, navigation, the UI kit and one
error-handling convention (`A4`), so the app does not end up with two ideas of what an error looks like.

It also holds the components both roles need, which is the point made above: `AnnotationEditor` (edit
or read-only), `BoxCanvas`, `ItemView`. A role difference is a mode passed to a shared component, never
a second implementation.

Controllers call services and bind results to the view. They hold no business rules and no SQL - if a
controller is computing an agreement rate, it is in the wrong layer.

## Testing and CI

JUnit 5 under `src/test/java`, Checkstyle at 120 characters via `config/checkstyle`.

- **Unit tests** for services and model logic, with no database.
- **Repository tests** against a temp SQLite file per test, seeded by fixture builders from `A7`.
- **A blindness test.** Fails if any annotator-facing code path can reach another annotator's
  annotation. This is the core invariant (rule 1).
- **Shared-component tests.** `BoxCanvas` geometry and `AnnotationEditor` mode behaviour are tested
  once, where the component lives, since both roles depend on them.

`./gradlew check shadowJar` runs both. GitHub Actions runs it on Linux, macOS and Windows on every
push, because the release is a desktop jar that has to launch on all three.

## Release

`./gradlew shadowJar` produces `build/libs/arbiter.jar` with JavaFX bundled, published as a formal
GitHub release. Cross-platform JavaFX artifacts are declared per-OS in `build.gradle`, so the jar is
built once and runs everywhere.

## Design decisions and their costs

Recorded so the trade-offs are visible rather than implicit.

| Decision | Alternative rejected | Cost accepted |
| --- | --- | --- |
| One shared SQLite file with a workspace lock | Package exchange with merge | Only one person can write at a time |
| Lightweight ORM (ORMLite) over JDBC | Hibernate | Complex queries still need raw SQL |
| Services in one shared package | Per-role service layers | Both tracks edit the same package |
| Money derived, never stored | Balance or ledger column | Recomputed on every read |
| Annotation components shared by both roles | One editor per role | `ui.shared` is a shared dependency |
| Blindness enforced in the service and a test | Enforced by package separation | Relies on discipline in the read path |
| Single-writer lock | Optimistic concurrency | Cannot have two annotators open at once |

The fifth and sixth rows are the load-bearing ones. Sharing the annotation components means the roles
cannot be built as independent silos, which in turn means blindness cannot be enforced by keeping
packages apart. Pushing that invariant down into the query boundary and a test is what makes the
sharing safe.

## Cross-references

- `docs/UserFlows.md` - the behavioural spec this design implements.
- `context/swe.md` - the process: skills, workflow, and who approves what.
- Issue `A0` - the model and repository interfaces, the contract both tracks code against.
- Issue `A2` - the schema and repository implementations.
- Issue `A4` - the shared UI kit and error-handling convention.
- Issue `A7` - the test harness, including the blindness test.