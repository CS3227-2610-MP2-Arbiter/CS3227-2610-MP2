---
title: Developer Guide
---

# Developer Guide

Arbiter is an offline Java desktop app for teams that label data. This guide describes how the system is designed, how the team works on it, and how to set up and verify a change.

## Setting up

- **JDK 25.** The Gradle wrapper downloads everything else, including JavaFX.
- **Run:** `./gradlew run`
- **Test and check style:** `./gradlew check`
- **Build the release jar:** `./gradlew shadowJar` produces `build/libs/arbiter.jar`

On Windows use `.\gradlew.bat` instead of `./gradlew`.

The `review` skill runs `./gradlew check shadowJar` together, which is what CI does.

## Design

Arbiter runs locally. There is no server, no network and no accounts department: one SQLite file lives in a workspace folder, reached over a shared drive, and JavaFX talks to it directly. Anything that assumes a backend - background jobs, webhooks, a message queue - is off the table. When the app is not running, nothing is happening, so there are no retries, no eventual-consistency problems and no distributed state.

Beyond that, the choices are about keeping the code honest. There is no plugin system and no dependency-injection framework, because a container would add indirection to an app with a dozen screens and one database file. There is an ORM, because hand-mapping eleven tables is exactly the kind of boilerplate it removes.

The code-level rules that follow from this design, which the agent works from, are in [`context/architecture.md`](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/blob/main/context/architecture.md).

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

Dependencies point downward only, and neither role package imports the other - they meet in `arbiter.ui.shared` and `arbiter.service`. That rule does not exist to keep the roles independent; they are not. It exists so the shared code has one home and neither track can grow a private copy. Because `arbiter.ui.shared` is on both critical paths, [#4] (model and repository interfaces) and [#8] (UI kit and error handling) come before feature code.

### The two roles share one workflow

The roles are two views on one workflow, not two applications. The annotator produces an annotation - a label, a rationale, optional boxes and a flag - and the adjudicator consumes it: compares it with others, resolves disagreements and exports the result. Three places make the coupling unavoidable:

- **Manual resolution** ([#34]) shows anonymous submitted annotations side by side with the item and rationale. For classification, the adjudicator chooses or supplies a label. For detection, they select one complete submitted box set, so the read-only view must render every box and label exactly as the annotator made it.
- **Box geometry** ([#15]) is hard: drawing, snapping, clamping, and coordinates that stay correct at any zoom level. Two implementations would drift.
- **Adjudicators record separate final decisions.** They may supply a classification label in [#34], using the shared picker without changing the annotator's answer. Detection comparison is read-only and selects a submitted set. Flag review in [#35] only keeps or excludes; direct annotation repair is deferred.

Building the roles as separate silos would duplicate the hardest UI code in the app, and a subtle disagreement between two coordinate transforms would hide there. So `AnnotationEditor`, `BoxCanvas` and `ItemView` live in `arbiter.ui.shared` and are used by both roles, with a role difference as a mode flag rather than a second implementation. Every rule about the data lives in `arbiter.service`, which both roles call, so no rule is implemented twice with two different answers.

### How blindness is enforced

Annotators must never see another annotator's annotation, a resolved label or a per-item agreement stat. Keeping the role packages apart cannot guarantee that once they share components, because a shared `AnnotationEditor` can be handed any annotation. Blindness is enforced where it can be seen and tested instead:

- **At the query boundary.** Annotator reads go through `AnnotationService`, which scopes every read to the session user. Resolved labels are never loaded on that path.
- **By a test.** [#11] includes a test that fails if any annotator-facing code path can reach another annotator's annotation.

This is stronger than package separation: it holds for code written later, by anyone, in any package.

### Data and persistence

- **Money is derived, never stored.** There is no balance column and no ledger. Earnings are recomputed from eligible submitted work and per-item rewards; completing the project seals all inputs, so later edits or account deactivation cannot reduce released earnings.
- **Keep immutable evidence and the current decision.** Retain all submitted answers/report-only outcomes with attribution, the current decision and contributors, and current flag dispositions/review metadata. Earlier versions, return metadata and edit timelines are deferred. A draft becomes an immutable submission; it never needs to coexist with a revised draft of the same submitted answer.
- **One-way submission.** Submit & next atomically locks an answer/report and advances the queue; a failed save leaves the current draft editable and a repeated request cannot submit twice. No Back, review pass, return or repair flow is required. [User Flows rules 13/17/18](UserFlows.md#3-rules-both-tracks-share) define this accepted contract; persistence/service enforcement remains pending.
- **Retain the evidence.** Deactivated accounts and retired items keep their annotations and attribution. Unused labels may be deleted only before the first assignment, without deleting answers; whole-project deletion remains available only for incomplete projects with a loss confirmation.
- **Freeze setup, then seal completed work.** First assignment freezes the project's corpus/taxonomy and each assigned split's definition. Before completion, annotators submit items once and adjudicators resolve answers and review flags; exclusion preserves evidence while removing the item from active work/results. COMPLETE seals all project data, including unfinished drafts, while viewing/export remain available. Services must enforce the [shared lifecycle rules](UserFlows.md#3-rules-both-tracks-share); the current model alone does not provide these guarantees.
- **One batch-creation mode.** Automatic count-based batches avoid separate percentage, manual and stratified allocation flows. Persist the generated membership/order so reopening work never reshuffles it; [User Flows rule 7](UserFlows.md#3-rules-both-tracks-share) bounds the reproducibility promise. This accepted scope still needs model/service implementation.
- **Write-through.** Every annotator action commits immediately, because a power cut must not lose work.
- **A lightweight ORM.** `arbiter.data.sqlite` maps rows with ORMLite over JDBC rather than by hand. Hibernate was rejected as too heavy: it wants a session lifecycle and lazy associations that do not fit a desktop app with one connection. Complex queries still drop to raw SQL, inside the repository.
- **A single writer.** The database is shared over a shared drive, and SQLite is not safe against concurrent writers there, so `arbiter.workspace` takes an advisory lock. The failure is explicit rather than silent corruption.
- **One exporter.** Annotators persist canonical annotations and never choose a file format. Only `ExportService` knows about CSV, JSON and COCO. It exports one dataset with current provenance; training/validation/test partitioning is deferred.

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

The fifth and sixth rows are the load-bearing ones. Sharing the annotation components means the roles cannot be built as independent silos, so blindness cannot come from keeping packages apart. Pushing it down to the query boundary and a test is what makes the sharing safe.

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

**Markdown.** Never hard-wrap prose in `.md` files: write one sentence or bullet per line and let the viewer wrap it. The 120-character Checkstyle limit applies to Java only.

**CI.** GitHub Actions runs `./gradlew check shadowJar` on Linux, macOS and Windows for every push and pull request, since the deliverable is a desktop jar that must launch on all three. A second job catches the release jar failing to start on Apple Silicon. A separate workflow publishes the `docs/` folder to GitHub Pages.

**Definition of done.** Behaviour implemented and reachable from the UI, `./gradlew check` passing (JUnit and Checkstyle), new logic unit-tested, database-touching code tested against the temp-DB harness, user-visible wording matching the shared rules in `docs/UserFlows.md`, and a PR reviewed by the other person.

## Testing

| Level | Where | What |
| --- | --- | --- |
| Unit | `src/test/java` | Services and model logic, no database |
| Repository | `src/test/java` | Temp SQLite file per test, seeded by [#11] fixtures |
| Blindness | `src/test/java` | Fails if annotator code can reach another annotator's work |
| Shared component | `src/test/java` | Box geometry and editor modes, tested once where the component lives |
| Acceptance | Manual | A human walks the agreed scenarios |

The blindness test exists because rule 1 (annotators never see each other's annotations) is the core invariant of the product. It is too important to leave to review alone.

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
[#15]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/15
[#34]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/34
[#35]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/35
