---
title: Developer Guide
---

# Developer Guide

Arbiter is an offline Java desktop app for teams that label data. This guide describes how the system
is designed, how the team works on it, and how to set up and verify a change.

## Setting up

- **JDK 25.** The Gradle wrapper downloads everything else, including JavaFX.
- **Run:** `./gradlew run` (Windows: `.\gradlew.bat run`)
- **Test and check style:** `./gradlew check`
- **Build the release jar:** `./gradlew shadowJar` produces `build/libs/arbiter.jar`

The `review` skill runs `./gradlew check shadowJar` together, which is what CI does.

## Design

The full design lives in
[`context/architecture.md`](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/blob/main/context/architecture.md).
The short version:

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
          arbiter.data.sqlite                        <-- ORM mappings; the only SQL
                        v
                arbiter.workspace                    <-- paths, single-writer lock, migrations
```

Dependencies point downward only, and neither role package imports the other - they meet in
`arbiter.ui.shared` and `arbiter.service`. That rule does not exist to keep the roles independent;
they are not. It exists so the shared code has one home and neither track can grow a private copy.

**The two roles are not independent.** They are producer and consumer on the same data. Manual
resolution shows competing annotations side by side with their boxes, adjudicators supply their own
labels, and box geometry is genuinely hard - so `AnnotationEditor`, `BoxCanvas` and `ItemView` live
in `arbiter.ui.shared` and are used by both. A role difference is a mode flag, never a second
implementation.

Because blindness cannot then be enforced by keeping packages apart, it is enforced at the query
boundary instead: annotator reads go through `AnnotationService`, which scopes them to the session
user, and `A7` carries an explicit test that fails if any annotator path can reach another
annotator's annotation.

Five decisions are load-bearing:

- **A lightweight ORM.** `arbiter.data.sqlite` maps rows with ORMLite over JDBC rather than by hand.
  Hibernate was rejected as too heavy: it wants a session lifecycle and lazy associations that do not
  fit a desktop app with one connection. Complex queries still drop to raw SQL, inside the repository.
- **Single SQLite file with a workspace lock.** The database is shared over a shared drive, and SQLite
  is not safe against concurrent writers there, so `arbiter.workspace` takes an advisory lock. The
  failure is explicit rather than silent corruption.
- **Money is derived, never stored.** No `balance` column, no ledger. Earnings are recomputed from
  annotations and per-item rewards, so they cannot drift.
- **One exporter.** Annotators persist canonical annotations and never choose a file format. Only
  `ExportService` knows about CSV, JSON and COCO.
- **Shared annotation components.** `AnnotationEditor` and `BoxCanvas` are written once and used by
  both roles, with the difference as a mode flag. Two implementations would drift, most dangerously
  in box coordinates.

The behaviour this implements is specified step by step in
[`docs/UserFlows.md`](UserFlows.md), including the fifteen cross-cutting rules in section 3.

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
