# Text-classification demo and smoke checklist

**Issue:** [#41] - Text-classification demo and smoke checklist
**Branch:** `feat/demo-seed`, from `main` at `6db6fc6`
**Status:** Plan approved by the owner on 28 September 2026, while it was being implemented at their request ("do 41 and 43"). Verified locally; the checklist's first run and teammate review are pending.

The scope and acceptance criteria live in [#41]. This plan records how the demo is built and what is still open.

## Goal and scope

Ship a small plain-text demo corpus, one command that seeds a demo workspace through the app's own services, and a manual smoke checklist that walks the retained workflow from owner setup to export.

Non-goals, from [#41]: production data, any change to how Arbiter normally starts, and anything [#62] deferred (detection or images, flags, advanced statistics, persisted drafts, completion seals, a provenance browser).

## Proposed changes

- **A `demo` Gradle source set** (`src/demo/java`, `src/demo/resources`) holds the seed action and the corpus. It depends on the main classes, but nothing in `main` depends on it, so `arbiter.jar`, `Launcher` and `Arbiter` are unchanged and normal startup never sees demo code. Checkstyle covers it through the `checkstyleDemo` task that `check` runs.
- **`./gradlew seedDemo`** runs `arbiter.demo.DemoWorkspace`, which creates a new workspace (default `build/demo-workspace`, or `-PdemoWorkspace=<folder>`). It refuses a folder that already holds a workspace, so it never changes existing data; running it again into a new folder is how it is repeated.
- **No role bypass.** The seed action uses only the public services a user's clicks reach, each signed in as the right account: owner bootstrap, `AuthService.createAnnotator`, `ProjectService.create`, `CorpusService.register`, the taxonomy and split methods, `AssignmentService.assign`, and `AnnotationService.submit` as each annotator. It never writes the store directly, so automatic resolution ([#27]) happens exactly as it does in the app.
- **The corpus.** Two sets of six neutral-named `.txt` files, copied into the new workspace's `media/`: `reviews/` (product reviews) and `answers/` (answers to support questions). File names never hint at a label.
- **The seeded state:**

  | Project | Kind and format | Split | *k* | Annotators | Answers seeded |
  | --- | --- | --- | --- | --- | --- |
  | Product reviews | `SINGLE` (positive, negative, mixed), CSV | one split of 6 | 3 | alice, bob, carol | All: four files reach a strict majority, two are 1-1-1 disputes |
  | Answer helpfulness | `SCALE` 1 to 5, JSON | one split of 6 | 2 | alice, bob | alice rates all six; bob rates the first four in his queue, leaving two to answer by hand |

  Each planned answer is keyed by file, so the outcome of every file is fixed even though split order is shuffled (rule 7). The two disputes stay open for manual resolution ([#34]), and the two unrated files leave annotation to try in the demo.
- **Accounts:** the owner `owner` and annotators `alice`, `bob` and `carol`, all with the documented demo password. They sign in through the normal login screen ([#7], [#31]).
- **The checklist** is a new page, `docs/SmokeChecklist.md`, linked from the Developer Guide's testing section. Part A starts from a fresh workspace and uses the corpus by hand: owner setup, accounts, a `SINGLE` project, import, taxonomy, splits, assignment with *k* = 2, annotation as two annotators, one agreement and one dispute, manual resolution, CSV export, and a content-hash failure (editing a registered file, seeing the queue and export refuse it, then restoring it). Part B opens the seeded workspace to check *k* = 3 majority and disputes, scale averaging, the two files left to annotate, and JSON export. The page ends with a results table that records the first run.
- **Developer Guide:** "Setting up" gains the `seedDemo` command, its home.

## Design alternatives considered

| Choice | Alternative rejected | Why |
| --- | --- | --- |
| A separate `demo` source set run by Gradle | Demo code in `main`, run from the jar | Keeps the shipped jar and startup free of demo code, as [#41] requires |
| Seeding through the services | Writing records like the test fixture `ClassificationWorkflow` | A direct write could reach states the app cannot, and would skip the checks and automatic resolution the demo should show |
| Answers keyed by file | Answers keyed by queue position | Split order is random, so position-keyed answers would label a clearly negative review "positive" |
| A committed, pre-built demo workspace | Seeding on demand | A committed snapshot goes stale with every schema change; the seeder always matches the code |

## Open decisions

- **Running the checklist.** The first run is a human action in the running app; the agent cannot drive the GUI. The owner runs it, and the results table records who ran it, when, on which commit, and any failure. Until then, [#41]'s "executed once" criterion stays open.

## Verification

| Acceptance criterion | How it is verified |
| --- | --- |
| Demo corpus and repeatable seed action without changing normal startup | `DemoWorkspaceTest` seeds two fresh folders and gets the same outcomes; `Launcher`, `Arbiter` and the shadow jar's contents are unchanged (review), and the jar contains no `arbiter/demo` class |
| Accounts follow [#7]/[#31], no role bypass | `DemoWorkspaceTest` signs in as each account through `AuthService.login` and checks its role; review confirms the seeder calls only public services |
| SINGLE majority, a dispute and manual resolution, SCALE averaging, configurable *k*, CSV and JSON export | `DemoWorkspaceTest` checks the four majorities, the two disputes, the scale means, *k* of 3 and 2 and both export previews; Part A and B of the checklist exercise them in the app, including manual resolution |
| Checklist covers owner setup through export, with a content-hash failure | Review of `docs/SmokeChecklist.md` |
| No deferred scenario | Review of the checklist and seeder |
| Checklist executed once, result recorded | The owner's run, recorded in the checklist's results table |
| `./gradlew check` passes | JDK 25 `./gradlew check shadowJar` |

## Implementation and verification

- Added the `demo` source set, `./gradlew seedDemo`, `arbiter.demo.DemoWorkspace`, the twelve corpus files, `docs/SmokeChecklist.md`, and two lines in the Developer Guide: the command under "Setting up" and the checklist in the testing table.
- The seeder prints nothing; the Gradle task reports where the workspace is. `UiConventionTest` scans every `arbiter` class outside the test output, including the demo's, and forbids the standard streams.
- `DemoWorkspaceTest` adds 5 tests: the accounts' roles through `AuthService.login`, the `SINGLE` project's *k*, disputes and CSV outcomes, the `SCALE` project's *k* and JSON means, two seeds giving the same outcomes, and an existing workspace refused with its data unchanged. A mutation run that changed one planned label and one planned rating failed the two outcome tests.
- `./gradlew seedDemo` produced 4 majorities, 4 scale means and 28 answers, as planned.
- JDK 25 `./gradlew cleanTest check shadowJar --no-daemon` passed: 471 JUnit tests, with 3 existing case-sensitivity tests skipped on macOS, and Checkstyle clean for main, test and demo. `unzip -l build/libs/arbiter.jar` lists no `arbiter/demo` entry.

[#7]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/7
[#27]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/27
[#31]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/31
[#34]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/34
[#41]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/41
[#62]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/62
