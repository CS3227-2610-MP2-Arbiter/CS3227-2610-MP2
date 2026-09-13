# Architecture

Arbiter is an offline Java desktop app for teams that label data. Two people use it: **annotators**,
who label items one at a time from a blind queue, and **adjudicators**, who import a corpus, split it,
assign annotators, resolve disagreements and export the finished dataset.

This document is the design the two feature tracks code against. It is deliberately prescriptive about
boundaries: the whole point is that zheng-jj and Whimsyturtle can build their halves in parallel
without stepping on each other.

## Why it is shaped this way

Two constraints drive almost every decision below.

**It runs locally.** There is no server, no network and no accounts department. One SQLite file lives
in a workspace folder, reached over a shared drive, and JavaFX talks to it directly. Anything that
assumes a backend - background jobs, webhooks, a message queue - is off the table. When the app is
not running, nothing is happening, which is a genuinely simplifying fact and worth exploiting: there
are no eventual-consistency problems, no retries and no distributed state.

**It is a school project.** The two roles exist to give the team a clean split of work, not because
There is no plugin system and no
dependency-injection framework, because a container would add indirection to an app with a dozen
screens and one database file. We do use an ORM: hand-mapping eleven tables is exactly the kind of
boilerplate it removes. Where a decision below looks like it is choosing the simpler option, that is
usually why.

The separation that *is* worth the effort is the one that lets two people work at once, so that is
where the structure goes: a shared core both tracks depend on, and two independent UI packages that
never import each other.

## Layers

```
        +---------------------------+   +----------------------------+
        |   arbiter.ui.annotator    |   |  arbiter.ui.adjudicator    |
        |   (zheng-jj)              |   |  (Whimsyturtle)            |
        +------------+--------------+   +-------------+--------------+
                     |                                |
                     v                                v
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
        |    (implementations)      |   |                            |
        +---------------------------+   +----------------------------+
                                    |
                                    v
                          +---------------------+
                          |  arbiter.workspace  |
                          |  paths, lock,       |
                          |  migrations         |
                          +---------------------+
```

Dependencies point downward only. `ui.annotator` never imports `ui.adjudicator` or the reverse; the
only thing they share is `service`, `model` and a small `ui.shared` kit. That single rule is what
makes parallel work possible, and it is the rule most worth enforcing in review.

| Package | Owns | Owner |
| --- | --- | --- |
| `arbiter.model` | Value objects and enums. No behaviour beyond validation. | Shared (`A0`) |
| `arbiter.data` | Repository interfaces. Signatures only, no SQL. | Shared (`A0`) |
| `arbiter.data.sqlite` | The ORM mappings, schema and migrations. | Whimsyturtle (`A2`) |
| `arbiter.workspace` | Workspace paths, the single-writer lock, asset resolution. | Shared (`A5`, `A6`) |
| `arbiter.service` | All business rules: assignment, resolution, export, earnings. | Shared (`A0`) |
| `arbiter.ui.shared` | Shell, navigation, routing, the UI kit, error handling. | zheng-jj (`A1`, `A4`) |
| `arbiter.ui.annotator` | The annotator screens. | zheng-jj (`B*`) |
| `arbiter.ui.adjudicator` | The adjudicator screens. | Whimsyturtle (`C*`) |

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
that knows about persistence. That boundary, not the choice of mapper, is what keeps the two tracks
from colliding.

**Single writer.** The database sits on a shared drive (rule 11). SQLite is not safe against
concurrent writers over a network share, so `arbiter.workspace` takes an advisory lock on the
workspace and Arbiter refuses to open a second writer. This is crude and it is deliberate: the
alternative is a package-exchange protocol with a merge step, which is far more machinery than a
two-person project needs. The lock makes the failure explicit instead of silent corruption.

## Services

Business rules live in `arbiter.service`, not in controllers and not in repositories. Both UIs call
the same services, which is what keeps a rule from being implemented twice - the specific risk the
flows call out for the exporter.

- `AuthService` - login, session, password hashing (PBKDF2 or bcrypt with a per-user salt).
- `WorkspaceService` - first-run setup, the lock, paths.
- `ProjectService`, `CorpusService` - import, splits, taxonomy.
- `AssignmentService` - assignment, *k*, load, completion and the reward lock.
- `AnnotationService` - autosave, submit, flags.
- `ResolutionService` - majority, ties and disputes; idempotent.
- `EarningsService` - derived earnings, released versus pending.
- `ExportService` - the only place that knows about COCO, YOLO, Pascal VOC, CSV and JSON.

`ExportService` deserves a note: annotators persist canonical annotations and never choose a format,
so export formatting exists in exactly one place. Building it twice is the most expensive mistake
available in this codebase.

## UI

JavaFX, one `Stage` with a swappable content area, routed by role after login (`A1`). Each role has
its own package and its own screens; `arbiter.ui.shared` holds the shell, navigation, and one small UI
kit plus one error-handling convention (`A4`), agreed before either track builds its screens so the
app does not end up with two ideas of what an error looks like.

Controllers call services and bind results to the view. They hold no business rules and no SQL - if a
controller is computing an agreement rate, it is in the wrong layer.

## Testing and CI

JUnit 5 under `src/test/java`, Checkstyle at 120 characters via `config/checkstyle`.

- **Unit tests** for services and model logic, with no database.
- **Repository tests** against a temp SQLite file per test, seeded by fixture builders from `A7`.
- **A blindness test.** A test that fails if any annotator-facing code path can reach another
  annotator's annotation. This is the core invariant (rule 1) and it is worth an explicit test rather
  than trusting review.

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
| Two UI packages, shared services | One merged UI tree | Risk of duplication in shared kit |
| Single-writer lock | Optimistic concurrency | Cannot have two annotators open at once |

## Cross-references

- `docs/UserFlows.md` - the behavioural spec this design implements.
- `context/swe.md` - the process: skills, workflow, and who approves what.
- Issue `A0` - the model and repository interfaces, the contract both tracks code against.
- Issue `A2` - the schema and repository implementations.