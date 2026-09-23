---
title: Developer Guide
---

# Developer Guide

This guide describes how Arbiter is designed, how the team works on it, and how to set up and verify a change.

## Setting up

- **JDK 25.** The Gradle wrapper downloads everything else, including JavaFX.
- **Run:** `./gradlew run`
- **Test and check style:** `./gradlew check`
- **Build the release jar:** `./gradlew shadowJar` produces `build/libs/arbiter.jar`

On Windows use `.\gradlew.bat` instead of `./gradlew`.

## Design

Arbiter runs locally. There is no server, no network and no accounts department: one SQLite file lives in the workspace folder both roles share, and JavaFX talks to it directly. Anything that assumes a backend - background jobs, webhooks, a message queue - is off the table. When the app is not running, nothing is happening, so there are no retries, no eventual-consistency problems and no distributed state.

Beyond that, the choices are about keeping the code honest. There is no plugin system and no dependency-injection framework, because a container would add indirection to a small app with one database file. There is an ORM, because hand-mapping every table is exactly the kind of boilerplate it removes.

The code-level rules that follow from this design, which the agent works from, are in [`context/architecture.md`](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/blob/main/context/architecture.md).

### Architecture

```
arbiter.ui.annotator      arbiter.ui.adjudicator      <-- role screens
        |          \            /           |
        |           v          v            |
        |        arbiter.ui.shared          |   <-- shell, AnnotationEditor, ItemView
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

Dependencies point downward only, and neither role package imports the other - they meet in `arbiter.ui.shared` and `arbiter.service`. That rule does not exist to keep the roles independent; they are not. It exists so the shared code has one home and neither track can grow a private copy. Because `arbiter.ui.shared` is on both critical paths, [#4] (model and repository interfaces) and [#8] (UI kit and error handling) come before feature code.

### The two roles share one workflow

The roles are two views on one workflow, not two applications. The annotator submits a label or integer scale rating, and the adjudicator consumes those immutable answers to monitor work, settle label disputes and export the result. Two places make the coupling unavoidable:

- **Manual resolution** ([#34]) shows submitted labels side by side, so the adjudicator's read-only view must render the same taxonomy choices the annotators used.
- **Adjudicators pick labels too.** Supplying a classification label in [#34] uses the annotator's label picker, recorded as a separate decision rather than an edit.

Building the roles as separate silos would duplicate the classification controls and risk showing or storing the same taxonomy differently. So `AnnotationEditor` and `ItemView` live in `arbiter.ui.shared` and are used by both roles, with a role difference as a mode flag rather than a second implementation. Every rule about the data lives in `arbiter.service`, which both roles call, so no rule is implemented twice with two different answers.

### How blindness is enforced

Keeping the role packages apart cannot guarantee [blindness](UserFlows.md#3-rules-both-tracks-share) once they share components, because a shared `AnnotationEditor` can be handed any annotation. Blindness is enforced where it can be seen and tested instead:

- **At the query boundary.** Annotator reads go through `AnnotationService`, which scopes every read to the session user. Resolved labels are never loaded on that path.
- **By a test.** [#11] includes a test that fails if any annotator-facing code path can reach another annotator's annotation.

This is stronger than package separation: it holds for code written later, by anyone, in any package.

### Data and persistence

- **Atomic submission.** Each completed logical action commits immediately. An unsubmitted choice is transient UI state, while **Submit & next** persists the answer and queue advance in one transaction (rule 18 in [User Flows](UserFlows.md#3-rules-both-tracks-share)).
- **A lightweight ORM.** `arbiter.data.sqlite` maps rows with ORMLite over JDBC rather than by hand. Hibernate was rejected as too heavy: it wants a session lifecycle and lazy associations that do not fit a desktop app with one connection. Complex queries still drop to raw SQL, inside the repository. ORMLite builds rows through a no-arg constructor and sets fields reflectively, so model fields cannot be `final`; settings fixed at creation are enforced in services instead.
- **A single writer.** The database is shared over a shared drive, and SQLite is not safe against concurrent writers there, so `arbiter.workspace` takes an advisory lock. The failure is explicit rather than silent corruption.
- **One exporter.** Annotators persist canonical annotations and never choose a file format, so formatting is written once, in `ExportService`.

### Design decisions and their costs

| Decision | Alternative rejected | Cost accepted |
| --- | --- | --- |
| One shared SQLite file with a workspace lock | Package exchange with merge | Only one person can write at a time |
| Lightweight ORM (ORMLite) over JDBC | Hibernate | Complex queries still need raw SQL |
| Services in one shared package | Per-role service layers | Both tracks edit the same package |
| Classification components shared by both roles | One editor per role | `ui.shared` is a shared dependency |
| Blindness enforced in the service and a test | Enforced by package separation | Relies on discipline in the read path |
| Single-writer lock | Optimistic concurrency | Cannot have two annotators open at once |

The shared-components and blindness rows are the load-bearing ones. Sharing the annotation components means the roles cannot be built as independent silos, so blindness cannot come from keeping packages apart. Pushing it down to the query boundary and a test is what makes the sharing safe.

The product context, the shape of both flows and the cross-cutting rules in section 3 are in [User Flows](UserFlows.md); the step-by-step behaviour is in the GitHub issues each step links to.

## Software engineering process

We run one AI agent with task-specific skills, and humans own requirements, design decisions, acceptance and merging. The process is defined in [`context/swe.md`](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/blob/main/context/swe.md); the skills live in `.codex/skills/`.

| Stage | Skill | Human gate |
| --- | --- | --- |
| Clarify | `clarify-requirements` | Confirms scope and acceptance criteria |
| Plan | `write-plan` | Approves the plan before implementation |
| Implement | `implement-feature` | - |
| Test | `write-test` | - |
| Verify | `review` | Reviews findings and check results |
| Accept | `maintain-docs` | Tests the agreed scenarios |
| Deliver | `create-pull-request` | Merges after CI and review |

Two skills do not fit the linear flow. `log` records each task into `logs/<user>/<NNN>-<name>.md` for human review, and `review` also handles standalone test-running requests.

Each skill declares its input, steps and completion criteria, and states what it must not do: `create-pull-request` commits, pushes and publishes only with authorization, and `review` never weakens a test to make it pass.

**Branching.** `write-plan` reuses or creates a descriptively named branch per task. Both of us work on `main` otherwise and keep the shared packages (`model`, `data`, `service`) agreed in [#4] before feature code starts, since that is where conflicts would come from.

**Markdown.** Never hard-wrap `.md` files: a paragraph, bullet or table row is one line, however many sentences it holds, and the viewer wraps it. The Java line limit does not apply.

**CI.** GitHub Actions runs `./gradlew check shadowJar` on Linux, macOS and Windows for every push and pull request, since the deliverable is a desktop jar that must launch on all three. A second job catches the release jar failing to start on Apple Silicon. A separate workflow publishes the `docs/` folder to GitHub Pages.

**Definition of done.** Behaviour implemented and reachable from the UI, `./gradlew check` passing (JUnit and Checkstyle), new logic unit-tested, database-touching code tested against the temp-DB harness, user-visible wording matching the shared rules in `docs/UserFlows.md`, and a PR reviewed by the other person.

## Testing

| Level | Where | What |
| --- | --- | --- |
| Unit | `src/test/java` | Services and model logic, no database |
| Repository | `src/test/java` | Temp SQLite file per test, seeded by [#11] fixtures |
| Blindness | `src/test/java` | Fails if annotator code can reach another annotator's work |
| Shared component | `src/test/java` | Classification editor modes, tested once where the component lives |
| Acceptance | Manual | A human walks the agreed scenarios |

## Acknowledgements

- The inherited Checkstyle configuration follows the [SE-Education Java coding standard](https://se-education.org/guides/conventions/java/intermediate.html).
- Pages automation follows the official [GitHub Pages workflow documentation](https://docs.github.com/en/pages/getting-started-with-github-pages/using-custom-workflows-with-github-pages).
- JavaFX is used under the [GPL v2 with the Classpath Exception](https://openjfx.io/).
- JUnit 5 is used under the [Eclipse Public License 2.0](https://junit.org/junit5/).
- Gradle and the Shadow plugin produce the release jar.
- [ORMLite](https://ormlite.com/) provides the object-relational mapping over SQLite.
- The skill-and-workflow structure was developed by this team for this project; the per-task skills in `.codex/skills/` are our own.

[Back to home](index.md)

[#4]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/4
[#8]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/8
[#11]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/11
[#34]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/34
