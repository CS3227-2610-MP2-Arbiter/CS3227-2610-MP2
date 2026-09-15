---
title: Developer Guide
---

# Developer Guide

Arbiter is an offline Java desktop app for teams that label data. This guide describes how the system
is designed, how the team works on it, and how to set up and verify a change.

## Setting up

- **JDK 25.** The Gradle wrapper downloads everything else, including JavaFX.
- **Run:** `./gradlew run`
- **Test and check style:** `./gradlew check`
- **Build the release jar:** `./gradlew shadowJar` produces `build/libs/arbiter.jar`

On Windows use `.\gradlew.bat` instead of `./gradlew`.

The `review` skill runs `./gradlew check shadowJar` together, which is what CI does.

## Design

Arbiter runs locally. There is no server, no network and no accounts department: one SQLite file
lives in a workspace folder, reached over a shared drive, and JavaFX talks to it directly. Anything
that assumes a backend - background jobs, webhooks, a message queue - is off the table. When the app
is not running, nothing is happening, so there are no retries, no eventual-consistency problems and no
distributed state.

Beyond that, the choices are about keeping the code honest. There is no plugin system and no
dependency-injection framework, because a container would add indirection to an app with a dozen
screens and one database file. There is an ORM, because hand-mapping eleven tables is exactly the
kind of boilerplate it removes.

The code-level rules that follow from this design, which the agent works from, are in
[`context/architecture.md`](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/blob/main/context/architecture.md).

### Architecture

```
arbiter.ui.annotator      arbiter.ui.adjudicator      <-- role screens
        |          \            /           |
        |           v          v            |
        |        arbiter.ui.shared          |   <-- shell, AnnotationEditor, BoxCanvas, ItemView
        |               |                   |
        +---------------+-------------------+
                        v
                arbiter.service                      <-- all business rules
                        v
          arbiter.data   +   arbiter.model           <-- repository interfaces + value objects
          arbiter.data.sqlite                        <-- ORM mappings, schema, migrations; the only SQL
                        v
                arbiter.workspace                    <-- paths, single-writer lock, asset resolution
```

Dependencies point downward only, and neither role package imports the other - they meet in
`arbiter.ui.shared` and `arbiter.service`. That rule does not exist to keep the roles independent;
they are not. It exists so the shared code has one home and neither track can grow a private copy.
Because `arbiter.ui.shared` is on both critical paths, `A0` (model and repository interfaces) and
`A4` (UI kit and error handling) come before feature code.

### The two roles share one workflow

The roles are two views on one workflow, not two applications. The annotator produces an
annotation - a label, a rationale, optional boxes and a flag - and the adjudicator consumes it:
compares it with others, resolves disagreements and exports the result. Three places make the
coupling unavoidable:

- **Manual resolution** (`C12`) shows competing annotations side by side with the item, rationale
  and boxes, so the adjudicator screen must render an annotation exactly as the annotator made it.
- **Box geometry** (`B4`) is hard: drawing, snapping, clamping, and coordinates that stay correct at
  any zoom level. Two implementations would drift.
- **Adjudicators also annotate.** They supply their own label in `C12` and repair flagged items in
  `C13`, so they need the annotator's editing widgets.

Building the roles as separate silos would duplicate the hardest UI code in the app, and a subtle
disagreement between two coordinate transforms would hide there. So `AnnotationEditor`, `BoxCanvas`
and `ItemView` live in `arbiter.ui.shared` and are used by both roles, with a role difference as a
mode flag rather than a second implementation. Every rule about the data lives in
`arbiter.service`, which both roles call, so no rule is implemented twice with two different answers.

### How blindness is enforced

Annotators must never see another annotator's annotation, a resolved label or a per-item agreement
stat. Keeping the role packages apart cannot guarantee that once they share components, because a
shared `AnnotationEditor` can be handed any annotation. Blindness is enforced where it can be seen
and tested instead:

- **At the query boundary.** Annotator reads go through `AnnotationService`, which scopes every read
  to the session user. Resolved labels are never loaded on that path.
- **By a test.** `A7` includes a test that fails if any annotator-facing code path can reach another
  annotator's annotation.

This is stronger than package separation: it holds for code written later, by anyone, in any
package.

### Data and persistence

- **Money is derived, never stored.** There is no balance column and no ledger. Earnings are
  recomputed from annotations and per-item rewards, so they cannot drift from the data.
- **Losing annotations are kept.** When resolution picks a winner, the other annotations stay,
  because the per-item breakdown and the export's provenance need them.
- **Deletion is soft, except for labels.** Accounts and items are retired rather than purged, so
  historical annotations stay interpretable. Deleting a label removes the annotations that used it.
- **Write-through.** Every annotator action commits immediately, because a power cut must not lose
  work.
- **A lightweight ORM.** `arbiter.data.sqlite` maps rows with ORMLite over JDBC rather than by hand.
  Hibernate was rejected as too heavy: it wants a session lifecycle and lazy associations that do not
  fit a desktop app with one connection. Complex queries still drop to raw SQL, inside the repository.
- **A single writer.** The database is shared over a shared drive, and SQLite is not safe against
  concurrent writers there, so `arbiter.workspace` takes an advisory lock. The failure is explicit
  rather than silent corruption.
- **One exporter.** Annotators persist canonical annotations and never choose a file format. Only
  `ExportService` knows about COCO, YOLO, Pascal VOC, CSV and JSON.

### Design decisions and their costs

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
cannot be built as independent silos, so blindness cannot come from keeping packages apart. Pushing
it down to the query boundary and a test is what makes the sharing safe.

The behaviour this implements is specified step by step in [User Flows](UserFlows.md), including the
cross-cutting rules in section 3.

## Software engineering process

We run one AI agent with task-specific skills, and humans own requirements, design decisions,
acceptance and merging. The process is defined in
[`context/swe.md`](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/blob/main/context/swe.md);
the skills live in `.codex/skills/`.

| Stage | Skill | Human gate |
| --- | --- | --- |
| Clarify | `clarify-requirements` | Confirms scope and acceptance criteria |
| Plan | `write-plan` | Approves the plan before implementation |
| Implement | `implement-feature` | - |
| Test | `write-test` | - |
| Verify | `review` | Reviews findings and check results |
| Accept | `maintain-docs` | Tests the agreed scenarios |
| Deliver | `create-pull-request` | Merges after CI and review |

Two skills do not fit the linear flow. `log` records each task into `logs/<user>/<NNN>-<name>.md` for
human review, and `review` also handles standalone test-running requests.

Each skill declares its input, steps and completion criteria, and states what it must not do:
`create-pull-request` commits, pushes and publishes only with authorization, and `review` never
weakens a test to make it pass.

**Branching.** `write-plan` reuses or creates a descriptively named branch per task. Both of us work
on `main` otherwise and keep the shared packages (`model`, `data`, `service`) agreed in `A0` before
feature code starts, since that is where conflicts would come from.

**CI.** GitHub Actions runs `./gradlew check shadowJar` on Linux, macOS and Windows for every push and
pull request, since the deliverable is a desktop jar that must launch on all three. A second workflow
publishes the `docs/` folder to GitHub Pages.

**Definition of done.** Behaviour implemented and reachable from the UI, `./gradlew check` passing
(JUnit and Checkstyle), new logic unit-tested, database-touching code tested against the temp-DB
harness, user-visible wording matching `docs/UserFlows.md`, and a PR reviewed by the other person.

## Testing

| Level | Where | What |
| --- | --- | --- |
| Unit | `src/test/java` | Services and model logic, no database |
| Repository | `src/test/java` | Temp SQLite file per test, seeded by `A7` fixtures |
| Blindness | `src/test/java` | Fails if annotator code can reach another annotator's work |
| Shared component | `src/test/java` | Box geometry and editor modes, tested once where the component lives |
| Acceptance | Manual | A human walks the agreed scenarios |

The blindness test exists because rule 1 (annotators never see each other's annotations) is the core
invariant of the product. It is too important to leave to review alone.

## Acknowledgements

- The inherited Checkstyle configuration follows the
  [SE-Education Java coding standard](https://se-education.org/guides/conventions/java/intermediate.html).
- Pages automation follows the official
  [GitHub Pages workflow documentation](https://docs.github.com/en/pages/getting-started-with-github-pages/using-custom-workflows-with-github-pages).
- JavaFX is used under the [GPL v2 with the Classpath Exception](https://openjfx.io/).
- JUnit 5 is used under the [Eclipse Public License 2.0](https://junit.org/junit5/).
- Gradle and the Shadow plugin produce the release jar.
- [ORMLite](https://ormlite.com/) provides the object-relational mapping over SQLite.
- The skill-and-workflow structure was developed by this team for this project; the per-task skills in
  `.codex/skills/` are our own.

[Back to home](index.md)
