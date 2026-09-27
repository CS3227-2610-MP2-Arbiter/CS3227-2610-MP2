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

`Arbiter` wires everything by hand when a workspace opens: one `JsonStore`, one `AuthService` holding the session, and one instance of each service sharing them. Each screen is handed only the services it calls:

```
Screen (role)                       Services it calls              What those services reach
----------------------------------  -----------------------------  ------------------------------------------
AuthScreen (both)                   AuthService                    JsonStore
MySplitsScreen, QueueScreen         AnnotationService              JsonStore, SourceResolver (reads media/)
  (annotator)
AccountsScreen (adjudicator)        AuthService                    JsonStore
ProjectsScreen, ProjectPage and     ProjectService, CorpusService, JsonStore, SourceResolver (reads media/),
  its views (adjudicator)           AssignmentService,             ExportService also writes exports/
                                    ResolutionService, ExportService
```

Every service checks the session itself, so being handed a service grants nothing (see [Authorization and blindness](#authorization-and-blindness)). Services never call the UI, and only `SourceResolver` reads source files.

### Domain model

The model classes in `arbiter.model` are plain value objects that refer to one another by identifier, as rows in the snapshot do. What each field means is in its Javadoc, and the terms are defined in the [Glossary](Glossary.md).

```
Project 1 ---- 1    TaxonomySettings   kind SINGLE or SCALE, and a SCALE project's range
Project 1 ---- *    Label              a SINGLE project's answers, in order
Project 1 ---- *    Item               a text file in media/: its path and content hash
Project 1 ---- *    Split  1 ---- *  SplitItem  * ---- 1  Item     an item's place in one split
Split   1 ---- *    Assignment  * ---- 1  User (annotator)         k places per split
Assignment 1 - *    Annotation  * ---- 1  Item                     one per annotator and item
Item    1 ---- 0..1 Resolution  * -- 0..1 User (adjudicator)       set for manual decisions only

Annotation holds a Label (SINGLE) or an integer (SCALE); Resolution holds a Label or a mean.
```

- **Identifiers** come from one counter in the snapshot (`nextId`), so an identifier is unique across every record type and is never reused.
- **Two answer shapes share one record.** An `Annotation` holds a label for a `SINGLE` project or an integer for a `SCALE` project, and a `Resolution` holds a label or a mean; the services decide which is set. In the service layer, the sealed `Answer` (`LabelChoice` or `Rating`) is what a screen submits, and `TaxonomySummary.accepts` checks it against the project.
- **Settings fixed at creation** (the project's kind and format, rule 4) and the freezes at first assignment (rules 3 and 14) are enforced in services, not in the model, whose fields are not `final`.
- **Nothing is deleted once work depends on it.** Projects, items and splits can be deleted only before their first assignment (rules 5 and 14), and there is no method that changes or deletes an `Annotation` (rule 13).

### The two roles share one workflow

The roles are two views on one workflow, not two applications. The annotator submits a label or integer scale rating, and the adjudicator consumes those immutable answers to monitor work, settle label disputes and export the result. Two places make the coupling unavoidable:

- **Manual resolution** ([#34]) shows an item's submitted labels together, so the adjudicator's read-only view must render the same taxonomy choices the annotators used.
- **Adjudicators pick labels too.** Supplying a classification label in [#34] uses the annotator's label picker, recorded as a separate decision rather than an edit.

Building the roles as separate silos would duplicate the classification controls and risk showing or storing the same taxonomy differently. So `AnnotationEditor` and `ItemView` live in `arbiter.ui.shared` and are used by both roles, with a role difference as a mode flag rather than a second implementation. Every rule about the data lives in `arbiter.service`, which both roles call, so no rule is implemented twice with two different answers.

### Authorization and blindness

**Authorization is checked in every service call, not in the UI.** `AuthService` holds the signed-in account's identifier. Every public method of the other services that reads or changes records starts with `requireAdjudicator()` or `requireAnnotator()`, which re-reads the account from the snapshot, so a deactivated account loses access on its next call rather than at its next login. The shell's `ScreenRegistry` routes each role to its own screens and refuses another role's route, but that is navigation: if a screen were reachable by mistake, the service calls behind it would still refuse.

| Caller | Services it may use |
| --- | --- |
| Anyone | `AuthService` owner setup (once, while the workspace has no owner) and login |
| Adjudicator | Account management in `AuthService`, and `ProjectService`, `CorpusService`, `AssignmentService`, `ResolutionService` and `ExportService` |
| Annotator | `AnnotationService` only |

Passwords are stored as PBKDF2-HMAC-SHA256 hashes with a random salt per account, by `PasswordHasher`, which owner setup, annotator creation and password replacement share.

**Blindness** ([rule 1](UserFlows.md#3-rules-both-tracks-share)) cannot come from keeping the role packages apart once they share components, because a shared `AnnotationEditor` can be handed any annotation. It is enforced where it can be seen and tested instead:

- **At the query boundary.** Annotator screens go through `AnnotationService`, whose methods read only the signed-in annotator's own assignments and answers and never return another annotator's work, a resolution or cross-annotator progress. Its `submit` runs automatic resolution inside its action but returns only the annotator's own queue.
- **By a test.** `AnnotatorBlindnessTest` ([#11]) walks every call from annotator-facing code, which is every class under `arbiter.ui` except `arbiter.ui.adjudicator`, and fails if one can reach another annotator's answers, a resolved result, cross-annotator progress or an adjudicator screen. It trusts only `AnnotationService.forCurrentUser` and `AnnotationService.submit`; trusting another method is a reviewed change to the test.
- **By what is not shown.** Annotators never see file names or folders, because a name can hint at a label, and a refused file is reported without its path.

This is stronger than package separation: it holds for code written later, by anyone, in any package.

### Persistence

**One snapshot.** [#6] stores every record and the identifier counter in one versioned JSON file, `arbiter.json`, behind the repository interfaces in `arbiter.data`. Replacing one file commits an action across repositories without a SQL transaction, at the cost of rewriting the whole snapshot for each commit, which is small for a corpus of text files.

**The write action is the transaction.** A service passes `JsonStore.write` one function. The store reads the current snapshot into a private `RepositorySession`, runs the function, validates the result and publishes it; if the function throws, nothing is published and the file is unchanged. So every check a service makes inside its action, such as the setup freezes in rules 3, 5 and 14, sees exactly the state it commits. `JsonStore.read` gives the same private view without publishing. A source check that guards the commit, such as the hash check at registration or submission, runs inside the action; reading a file's text to show it, and writing an export, happen after it.

**Integrity.** `JsonIntegrity` checks the snapshot every time it is read and before every commit: unique identifiers below `nextId`, references that point at existing records, and required fields such as submission and decision times. A snapshot that fails is refused with a `JsonStoreException` rather than repaired.

**Atomic publication.** `JsonStore.publishAtomically` writes the new bytes to a temporary file beside the target, forces them to disk and moves the file over the target in one atomic move, so a reader or a crash sees the old snapshot or the new one, never part of one. If publishing fails, the store refuses further writes until the workspace is reopened, since it can no longer be sure what is on disk. Exports are written the same way.

**One writer.** Within the app, the store serializes actions on each data file. Across processes, `WorkspaceLock` holds an operating-system lock on `workspace.json` from opening the workspace until it closes ([#61]), so a second Arbiter instance cannot open it. That limits the shared workspace to one writer at a time.

**Versions and migrations.** Two numbers guard the data: `workspaceVersion` in `workspace.json` describes the folder layout, and `schemaVersion` in `arbiter.json` describes the snapshot. Both are 1, and v1 has no migrations: a workspace or snapshot from a newer build is refused with a message to update Arbiter, and any other version is refused as unsupported, in both cases without changing a file. Changing the snapshot's shape therefore means raising `JsonSnapshot.CURRENT_VERSION` and adding a migration that reads the old shape, upgrades it and validates the result before the first write. Unknown fields are ignored on reading so that a newer file reaches the version check instead of failing earlier.

**Source hashing.** Registration ([#25]) records each file's path relative to the workspace and the SHA-256 of its bytes, without copying it ([rule 21](UserFlows.md#3-rules-both-tracks-share)). Every later read goes through `SourceResolver`, which checks that the path stays inside `media/`, so a link or junction cannot escape it, that the file exists, is at most 10 MB and is UTF-8 text, and that its hash still matches. A missing or changed file becomes a `SourceException` with a reason: the queue shows the reason instead of the text, and manual resolution and export refuse until the file is restored. Nothing is cached, so restoring the original bytes restores access.

**Submission atomicity.** An unsubmitted choice is only screen state. **Submit & next** ([#17], rule 18) is one write action:

```
QueueScreen                  AnnotationService.submit              JsonStore.write (one action)
    |  submit(assignment,        |                                      |
    |    item on screen, answer) |  requireAnnotator()                  |
    |--------------------------->|------------------------------------->|  read snapshot into a session
    |                            |  assignment is theirs and unfinished |
    |                            |  item is their next file, unanswered |
    |                            |  answer fits the taxonomy            |
    |                            |  source still matches its hash       |
    |                            |  insert Annotation                   |
    |                            |  resolve the item if this is its kth |
    |                            |  answer (ResolutionService)          |
    |                            |  set assignment IN_PROGRESS or       |
    |                            |  SUBMITTED; work out the next file   |
    |                            |<-------------------------------------|  validate, publish atomically
    |                            |  read the next file's text           |
    |<---------------------------|  (outside the action)                |
    |  show the returned queue   |                                      |
```

Any refusal throws before publishing, so a double-click, a retry or a stale screen cannot add a second answer or move the queue twice. Restarting resumes at the first file without an answer, which is worked out from stored answers each time rather than stored as a position.

### Resolution and export

**Resolution** ([rule 10](UserFlows.md#3-rules-both-tracks-share)) is decided in one place, `ResolutionService`. Automatic resolution ([#27]) runs inside the submission's own write action when an item receives its *k*th answer: a strict majority settles a `SINGLE` item, the mean settles a `SCALE` item, and a `SINGLE` item without a majority is left as a dispute. Manual resolution ([#34]) stores the adjudicator's decision as the disputed item's `ADJUDICATED` resolution, which they can replace later; submitted answers and automatic resolutions never change.

**Export** ([#37]) is the only code that writes a dataset, `ExportService`. Annotators store canonical answers and never choose a format. An export reads one snapshot, checks every item's source against its hash outside the store's action, builds one in-memory dataset of every item with its current decision and provenance ([rules 15 and 17](UserFlows.md#3-rules-both-tracks-share)), and only then formats it: JSON nests each item's submissions, and CSV flattens them into numbered column groups. The file is published atomically to `exports/project-<id>.<format>`, replacing the previous export, and nothing in the store changes.

**Adding an output format.** v1 supports CSV and JSON only; a new format is future work, not a hidden option. To add one:

1. Add the value to `OutputFormat` and its meaning to the [Glossary](Glossary.md#fixed-value-sets). Because a project's format is fixed at creation (rule 4) and stored in the snapshot, an older build cannot read a project that uses it, so raise the snapshot version as described under [Persistence](#persistence).
2. Add its case to the `switch` in `ExportService.export`. The switch has no default branch, so the compiler reports every place a new value is not handled.
3. Build it from the same dataset the other formats use, so its provenance matches theirs, and choose its file extension there.
4. Add tests beside `ExportServiceTest` for a resolved, an unresolved and a manually resolved item, and document it in the User Guide once it is released.

### Scope reduction and extension

[#62] reduced v1 to plain-text classification so the two-role workflow could be finished and tested properly. Detection with bounding boxes and COCO output, image input, flags, persisted drafts, session timing and analytics, a separate completion state with a project seal, and a standalone provenance browser were deferred, and their issues were closed as not planned for v1. The model and repository interfaces hold no state for any of them, and nothing in this guide describes them as implemented.

Adding another source or task type later is a schema change, not a plug-in:

- **Another source type, such as images.** `Item` assumes a text file identified by a path and a content hash, and `SourceResolver` accepts only `.txt` files and decodes them as text. Images would need the resolver to accept and return binary media, `ItemView` to display it, and possibly new `Item` fields such as a media type. The hash-and-path registration would carry over unchanged.
- **Another task type, such as detection.** `TaxonomyKind`, `Annotation` and `Resolution` model one label or one number per item. Regions would need a new kind, answer records that hold several shapes per item, a new `Answer` variant for submission, a new resolution rule in place of majority and mean, and output formats such as COCO.
- **Either way,** existing workspaces would need the snapshot version raised and a migration, the blindness test would cover the new annotator screens automatically, and each new answer shape would need its own integrity checks.

### Errors and logging

Both roles report problems through one UI kit in `arbiter.ui.shared` ([#8]), so a failure looks the same on every screen and nothing fails silently. Whether a user reads a failure's own message is decided in one place, `ErrorMessages`, by where the exception is declared rather than by a list of types, so a new service's exceptions need no registration. Diagnostics go to the workspace's own `logs/` folder, beside the data they concern.

What each class guarantees is in its Javadoc. The rules agents follow, and the test that enforces them, are in [the architecture context](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/blob/main/context/architecture.md#ui).

### Design decisions and their costs

| Decision | Alternative rejected | Cost accepted |
| --- | --- | --- |
| Focused text classification for v1 ([#62]) | Detection, images, flags, drafts and analytics in v1 | Those features wait for a later version and a schema change |
| One shared JSON workspace with a writer lock ([#61]) | Package exchange with merge | Only one person can write at a time |
| Jackson snapshot behind repository interfaces | SQL database with an ORM | Rewrites the snapshot for each commit |
| Refuse any other data version | Best-effort reading of old or new files | Every future shape change needs a migration |
| Persist only on **Submit & next** | Autosaved drafts | A choice not yet submitted is lost if Arbiter closes |
| Services in one shared package | Per-role service layers | Both tracks edit the same package |
| Classification components shared by both roles | One editor per role | `ui.shared` is a shared dependency |
| Blindness enforced in the service and a test | Enforced by package separation | Relies on discipline in the read path |
| Register sources in place, checked by hash | Copy sources into the workspace | A moved or edited file blocks its item until restored |
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

Each skill declares its input, steps and completion criteria, and states what it must not do: `create-pull-request` follows the explicit delivery approval rule in [`context/swe.md`](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/blob/main/context/swe.md), and `review` never weakens a test to make it pass.

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

- **Real storage, temporary folders.** Service tests run against a real `JsonStore` in a JUnit temporary folder, created by `TestWorkspace`, so every test exercises the same transaction, integrity and publication code as the app. `ClassificationWorkflow` seeds a whole project in one call, and refuses a state the app could not reach, such as a resolution that does not follow from the answers.
- **Refusals leave the data unchanged.** A test of a refused action compares `arbiter.json` byte for byte before and after, which proves the refusal happened inside the write action.
- **Architecture as tests.** `AnnotatorBlindnessTest` checks the blindness boundary described above, including against fixture classes that deliberately leak, so the test itself is shown to catch violations. `UiConventionTest` checks that only `Dialogs` builds dialogs, only `DiagnosticLog` logs, nothing writes to the standard streams and nothing sets inline styles. Both use ArchUnit and scan the shipped classes, so they cover code written later.
- **Screens.** Service logic is tested without JavaFX. Screen behaviour that matters, such as text never being cut off, is checked by the human acceptance steps each plan lists.

## Acknowledgements

- The inherited Checkstyle configuration follows the [SE-Education Java coding standard](https://se-education.org/guides/conventions/java/intermediate.html).
- Pages automation follows the official [GitHub Pages workflow documentation](https://docs.github.com/en/pages/getting-started-with-github-pages/using-custom-workflows-with-github-pages).
- JavaFX is used under the [GPL v2 with the Classpath Exception](https://openjfx.io/).
- JUnit 5 is used under the [Eclipse Public License 2.0](https://junit.org/junit5/).
- Gradle and the Shadow plugin produce the release jar.
- Jackson provides JSON serialization for the workspace snapshot, and writes the JSON and CSV exports.
- ArchUnit checks the architecture rules in the blindness and UI convention tests, under the [Apache License 2.0](https://www.archunit.org/).
- The skill-and-workflow structure was developed by this team for this project; the per-task skills in `.codex/skills/` are our own.

[Back to home](index.md)

[#4]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/4
[#6]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/6
[#8]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/8
[#11]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/11
[#17]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/17
[#25]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/25
[#27]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/27
[#34]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/34
[#37]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/37
[#61]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/61
[#62]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/62
