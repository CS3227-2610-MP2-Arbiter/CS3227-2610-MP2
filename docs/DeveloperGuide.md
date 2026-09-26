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

Arbiter runs locally. There is no server, no network service and no accounts department: both roles use the shared JSON workspace in [rule 11](UserFlows.md#3-rules-both-tracks-share), and JavaFX calls services backed by its data store. Anything that assumes a backend - background jobs, webhooks, a message queue - is off the table. When the app is not running, nothing is happening, so there are no background retries or eventual-consistency problems.

Beyond that, the choices are about keeping the code honest. There is no plugin system and no dependency-injection framework, because a container would add indirection to a small app with one data store. Jackson serializes the JSON snapshot behind the repository interfaces; no ORM or SQL layer is needed.

The code-level rules that follow from this design, which the agent works from, are in [architecture context](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/blob/main/context/architecture.md).

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
          arbiter.data.json                          <-- JSON snapshot and repository implementations
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

- **At the query boundary.** Annotator screens go through `AnnotationService`, which returns only the session user's own work, never another annotator's or a resolved result.
- **By a test.** [#11] includes a test that fails if any annotator-facing code path can reach another annotator's annotation.

This is stronger than package separation: it holds for code written later, by anyone, in any package.

### Data and persistence

- **Atomic submission.** Each completed logical action commits immediately. An unsubmitted choice is transient UI state, while **Submit & next** persists the answer and queue advance in one transaction (rule 18 in [User Flows](UserFlows.md#3-rules-both-tracks-share)).
- **One Jackson snapshot.** [#6] stores the workspace's records and ID state in one versioned JSON snapshot behind the existing repository interfaces. Replacing one file can commit an action across repositories without a SQL transaction, at the cost of rewriting the snapshot for each commit. The enforcement points are in [the architecture context](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/blob/main/context/architecture.md#persistence).
- **A single writer.** Atomic replacement alone does not coordinate separate app instances. The JSON store serializes actions within one process; [#61] supplies the workspace lock across instances. That limits the shared workspace to one writer at a time.
- **One exporter.** Annotators persist canonical annotations and never choose a file format, so formatting is written once, in `ExportService`.

### Errors and logging

Both roles report problems through one UI kit in `arbiter.ui.shared` ([#8]), so a failure looks the same on every screen and nothing fails silently. Whether a user reads a failure's own message is decided in one place, `ErrorMessages`, by where the exception is declared rather than by a list of types, so a new service's exceptions need no registration. Diagnostics go to the workspace's own `logs/` folder, beside the data they concern.

What each class guarantees is in its Javadoc. The rules agents follow, and the test that enforces them, are in [the architecture context](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/blob/main/context/architecture.md#ui).

### Design decisions and their costs

| Decision | Alternative rejected | Cost accepted |
| --- | --- | --- |
| One shared JSON workspace with a writer lock ([#61]) | Package exchange with merge | Only one person can write at a time |
| Jackson snapshot behind repository interfaces | SQL database with an ORM | Rewrites the snapshot for each commit |
| Services in one shared package | Per-role service layers | Both tracks edit the same package |
| Classification components shared by both roles | One editor per role | `ui.shared` is a shared dependency |
| Blindness enforced in the service and a test | Enforced by package separation | Relies on discipline in the read path |
| JDK logging to the workspace's `logs/` folder | A logging framework, or a per-user log | One log per workspace, shared by both roles |
| ASCII-only names and passwords | Unicode in all text | Names cannot use accents or non-Latin scripts |

The shared-components and blindness rows are the load-bearing ones. Sharing the annotation components means the roles cannot be built as independent silos, so blindness cannot come from keeping packages apart. Pushing it down to the query boundary and a test is what makes the sharing safe.

Names and passwords are ASCII so that a length limit counts what the user sees and two names that look alike are equal. Unicode would bring characters that count twice, invisible zero-width characters and look-alike letters, each needing its own handling.

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

Each skill declares its input, steps and completion criteria, and states what it must not do: `create-pull-request` follows the explicit delivery approval rule in [`context/swe.md`](../context/swe.md), and `review` never weakens a test to make it pass.

**Branching.** `write-plan` reuses or creates a descriptively named branch per task. Both of us work on `main` otherwise and keep the shared packages (`model`, `data`, `service`) agreed in [#4] before feature code starts, since that is where conflicts would come from.

**Markdown.** Never hard-wrap `.md` files: a paragraph, bullet or table row is one line, however many sentences it holds, and the viewer wraps it. The Java line limit does not apply.

**CI.** GitHub Actions runs `./gradlew check shadowJar` on Linux, macOS and Windows for every push and pull request, since the deliverable is a desktop jar that must launch on all three. A second job catches the release jar failing to start on Apple Silicon. A separate workflow publishes the `docs/` folder to GitHub Pages.

**Definition of done.** Behaviour implemented and reachable from the UI, `./gradlew check` passing (JUnit and Checkstyle), new logic unit-tested, data-store code tested against temporary JSON workspaces, user-visible wording matching the shared rules in `docs/UserFlows.md`, and a PR reviewed by the other person.

## Testing

| Level | Where | What |
| --- | --- | --- |
| Unit | `src/test/java` | Services and model logic, no persistent store |
| Repository | `src/test/java` | Temporary JSON workspace per test; [#11] adds shared fixtures |
| Blindness | `src/test/java` | Fails if annotator code can reach another annotator's work |
| Shared component | `src/test/java` | Classification editor modes, tested once where the component lives |
| Acceptance | Manual | A human walks the agreed scenarios |

## Acknowledgements

- The inherited Checkstyle configuration follows the [SE-Education Java coding standard](https://se-education.org/guides/conventions/java/intermediate.html).
- Pages automation follows the official [GitHub Pages workflow documentation](https://docs.github.com/en/pages/getting-started-with-github-pages/using-custom-workflows-with-github-pages).
- JavaFX is used under the [GPL v2 with the Classpath Exception](https://openjfx.io/).
- JUnit 5 is used under the [Eclipse Public License 2.0](https://junit.org/junit5/).
- Gradle and the Shadow plugin produce the release jar.
- Jackson provides JSON serialization for the workspace snapshot.
- The skill-and-workflow structure was developed by this team for this project; the per-task skills in `.codex/skills/` are our own.

[Back to home](index.md)

[#4]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/4
[#8]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/8
[#11]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/11
[#34]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/34
