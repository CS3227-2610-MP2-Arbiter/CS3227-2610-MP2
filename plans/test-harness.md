# Test harness, classification fixtures and blindness test

**Issue:** [#11] - Test harness, classification fixtures and blindness test
**Branch:** `feat/test-harness`, restarted from `main` after [#10] merged; the first attempt (PR #69) was discarded.
**Status:** Implemented and locally verified at the owner's request to redo [#11]. Plan approval and human acceptance are pending.

What the feature does is in [#11], and what each class means is in its Javadoc. This plan records what was decided and what is still open.

## Goal and scope

Give later tests one way to build a fresh, seeded workspace, one fixture for the retained two-role classification workflow, and one automated check that annotator-facing code cannot reach another annotator's answers, a resolved result or cross-annotator progress (rule 1).

Non-goals, from [#11]: feature behaviour owned by later issues, and fixtures for anything [#62] deferred. No production code changes.

## Why the first attempt was discarded

- It hashed source files itself because [#10] had not merged; `SourceResolver.resolveForImport` is now on `main` and is the one registration boundary.
- Its blindness rule followed whole classes. A shared service such as `AnnotationService` will hold both the annotator's scoped read and code that reads every answer (the *k*th submission triggers resolution, [#27]), so a class-level rule either flags legitimate code or has to exempt whole classes.
- Its plan described APIs that were never built, and its log claimed human verification the owner had not given.

## Proposed changes

### Harness and fixtures (`src/test/java/arbiter/testing`)

- **`TestWorkspace`** creates a workspace with `WorkspaceService.create` and `JsonStore.initializeNew` in a folder the test owns (normally under `@TempDir`), so no two tests share stored state. It exposes the paths and store, `writeSource(path, text)`, which writes a file under `media/` and registers it through `SourceResolver.resolveForImport`, and `signIn(username)`, which returns an `AuthService` signed in with the fixture password.
- **`Records`** is `JsonStoreFixtures` moved out of `arbiter.data.json` and made public, so the store tests and the workflow build records one way. Existing builders keep their behaviour; scale builders are added. The store tests change only their imports.
- **`ClassificationWorkflow`** seeds one project, its taxonomy, a registered corpus, one split, accounts, assignments, submissions and resolutions in one committed action:
  - `single(labels...)` or `scale(min, max)`; `items(n)`; `annotationsPerItem(k)`.
  - `assign(annotator, answers...)` assigns the annotator and submits the given labels or ratings for the first items in split order, which is the only shape forward-only submission can produce (rule 18). Assignment status follows from the answers (`NOT_STARTED`, `IN_PROGRESS`, `SUBMITTED`).
  - `annotator(name)` creates an unassigned account, and `disabled(name)` disables an account while keeping its assignment and answers (rule 19).
  - `majority`, `adjudicated` and `mean` record a resolution the test chooses. The fixture never computes one, because that is [#27]'s logic.
  - If *k* is not set, it is the number of assignees for an assigned split and null for an unassigned one, matching the model (`Split.annotationsPerItem` is null until assignment).
  - It refuses states the app cannot reach before writing anything: an unknown label, a rating outside the range, an answer of the wrong kind, more answers than items, a duplicate assignee, more assignees than *k*, and a resolution before *k* answers.
  - It creates the owner through `AuthService.bootstrapOwner` if the workspace has none, and reuses an existing account with the same username.
- **`arbiter.service.TestAccounts`** (test source set) builds annotator accounts with real `PasswordHasher` credentials, so a fixture annotator can sign in. It sits in `arbiter.service` because `PasswordHasher` is package-private; once [#31] adds annotator creation to the service, the fixture should call that instead.

### Blindness test (`src/test/java/arbiter/blindness`)

- **Annotator-facing code** is every class under `arbiter.ui` except `arbiter.ui.adjudicator`, including shared components and any new UI package, so coverage does not depend on a class's name or package (rule "Do not rely on package separation").
- **`BlindnessRule`** imports the compiled classes with ArchUnit and walks method by method from every annotator-facing method, through method and constructor calls, method references and lambda bodies, into service and UI code. A call through an interface or overridable method also follows its implementations. It stops at repository interfaces, the model and the workspace layer, and reports the path to each violation:
  - **Another annotator's answers:** any `AnnotationRepository` read. [context/architecture.md](../context/architecture.md#blindness-rule-1) already requires annotator reads to go through `AnnotationService.forCurrentUser(...)`.
  - **Resolved results:** any `ResolutionRepository` method, or a resolution type in a reached method's signature.
  - **Cross-annotator progress:** `AssignmentRepository.listBySplit` and `listByStatus`.
  - **The adjudicator's screens:** any call into `arbiter.ui.adjudicator`.
- **Trusted scoped entry points** are named in one list in the test, currently `AnnotationService.forCurrentUser`. The walk does not enter them, because their scoping is behaviour that [#13] and [#17] test with the fixtures; it still checks their signatures. Adding an entry point, such as [#17]'s submission, is a one-line change to that list, which the reviewer sees in the diff.
- **The rule is proven against fixtures.** Test-only classes under `arbiter.blindness.fixture` each leak one way: a direct read in a new UI package, a resolution read inside a lambda, a progress read two calls deep through a service, a method reference, an interface implementation, a call into an adjudicator screen, a trusted entry point whose signature returns a resolution, and a shared view model that takes one. Each must be reported with its own violation. A blind screen that uses a trusted read (which itself reads every answer) and calls a service method whose sibling methods read every answer must not be reported, and neither may adjudicator fixture code. The same rule then runs over the shipped classes and must report nothing.

## Design alternatives considered

| Choice | Alternative rejected | Why |
| --- | --- | --- |
| Method-level call walk | Class-level dependency rule (the first attempt, or ArchUnit's `transitivelyDependOnClassesThat`) | Shared services mix scoped and unscoped reads; class granularity gives false positives or needs whole-class exemptions |
| ArchUnit 1.4.1 as a test-only dependency | The JDK `java.lang.classfile` API | Mapping `invokedynamic` back to lambda bodies and method references is solved in ArchUnit; hand-writing it breaks the "do not reinvent the wheel" rule |
| Static rule plus behavioural scoping tests in the feature issues | A behavioural test that calls every annotator service | No annotator service exists yet, and a list of services to call would not cover new ones |
| Register fixture sources through `SourceResolver.resolveForImport` | Hash in the fixture | One registration boundary; fixture items are exactly what [#25] would accept |
| Move `JsonStoreFixtures` into `arbiter.testing` | A second record builder | One home for fixture records |

## Risks and open decisions

- **All of `arbiter.ui` except the adjudicator package counts as annotator-facing, including shared components.** So a shared component may not load or accept a resolution even in adjudicator mode; the adjudicator's screen passes it plain values such as a `Label`. This matches the architecture rule that `AnnotationEditor` holds only the annotator's unsubmitted choice, but the adjudicator track ([#34]) should confirm it.
- **Per-annotator assignment reads (`findById`, `listByAnnotator`) are allowed**, because the annotator's home and queue ([#12], [#13]) need their own assignments. The static rule cannot see whose identifier is passed, so scoping those reads to the session user is tested behaviourally in those issues.
- **Implementations inside `arbiter.ui.adjudicator` are not followed** when shared code calls an interface: they run only after routing to the adjudicator. A value built in `arbiter.Arbiter` wiring and handed to an annotator screen is also outside the walk. Both are limits of a static check; routing is covered by [#5].
- **Reflection and reading `arbiter.json` directly** are not modelled. The architecture already keeps storage behind `arbiter.data.json`.
- **One new test dependency** (ArchUnit 1.4.1, Apache 2.0), pinned like JUnit. Verified to import Java 25 classes, attribute lambda bodies to their enclosing method and report method references.

## Verification

| Acceptance criterion | How it is verified |
| --- | --- |
| Tests can use a freshly seeded temporary database without sharing state | `TestWorkspaceTest`: a new workspace is pristine with an initialized snapshot, and a write to one workspace leaves another untouched. `ClassificationWorkflowTest`: two seeded workflows in separate workspaces are independent |
| Fixtures cover the retained two-role classification workflow and configurable annotations per item | `ClassificationWorkflowTest`: SINGLE and SCALE, owner and annotators signing in, explicit and default *k* including unfilled places, not-started, in-progress and submitted assignments, disabled accounts, the three resolution methods, registered sources that resolve, and each refused state with the workspace left unchanged |
| The blindness test fails if annotator-facing code can reach another annotator's answers, resolved results or cross-annotator progress | `AnnotatorBlindnessTest`: each leaking fixture is reported with its own violation, compliant and adjudicator fixtures are not, and the shipped classes report nothing |
| No fixture depends on detection, images, flags, drafts, completion sealing or advanced statistics | Review: the fixtures use only the model, which has no such types after [#62] |
| `./gradlew check` passes | JDK 25 `./gradlew check shadowJar` |

## Implementation and verification

- Added `TestWorkspace`, `Records` (moved from `JsonStoreFixtures`), `ClassificationWorkflow` and `arbiter.service.TestAccounts` under `src/test/java`, plus `BlindnessRule`, `AnnotatorBlindnessTest` and its fixtures in `arbiter.blindness`. No production code changed; `build.gradle` gains the ArchUnit test dependency, and `context/architecture.md` gains one bullet saying what the blindness test treats as annotator-facing and where trusted reads are listed.
- The blindness check was shown to fail on shipped code: a temporary `arbiter.ui.annotator.TempLeakScreen` calling a temporary `arbiter.service.TempResults.decided`, which read `ResolutionRepository.findByItem`, failed `annotatorBlindness_shippedCode_noViolations` with the full path `TempLeakScreen.show -> TempResults.decided -> ResolutionRepository.findByItem`. Both classes were then deleted.
- The fixture tests found a defect in the first draft: a conditional expression mixing `Integer` and `int` unboxed a null *k* for every unassigned split. It is now an explicit branch. Two more gaps were closed before the tests ran: an annotator named `owner`, and two names differing only in case, would each have failed after files were written rather than before.
- JDK 25 `./gradlew cleanTest check shadowJar --no-daemon` passed: 178 JUnit tests (44 new), none skipped, both Checkstyle tasks, and the release jar.

## Open questions

- Should shared UI components be allowed to accept a resolution in adjudicator mode? (See risks; the answer would change the annotator-facing definition, not the walk.)
- When [#31] lands, `TestAccounts` should give way to the service's annotator creation.

[#5]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/5
[#10]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/10
[#11]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/11
[#12]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/12
[#13]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/13
[#17]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/17
[#25]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/25
[#27]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/27
[#31]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/31
[#34]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/34
[#62]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/62
