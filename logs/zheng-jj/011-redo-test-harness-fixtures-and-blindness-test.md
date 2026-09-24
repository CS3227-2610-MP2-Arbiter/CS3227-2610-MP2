# Redo test harness, classification fixtures and blindness test

Status: Awaiting human verification.

## Original request

- Redo [#11]. PR #69 already existed but was poorly done: undo the whole `feat/test-harness` branch and start again from what is now on `main`.

## Follow-ups, corrections, and reflection

- PR #69 had no review comments, so the agent had to decide what "poorly done" meant by reading it. It found four problems. It hashed source files itself instead of using [#10]'s resolver, which has since merged. Its blindness rule worked on whole classes, which breaks once a shared service holds both a scoped read and a read of every answer. Its plan described APIs it never built. Its log said "Human verified" although the owner had not verified it.
- The main design lesson: blindness is about what flows back to the annotator, not about what service code touches internally. Submitting the *k*th answer must read every answer to resolve the item ([#27]). So a rule that forbids anything reachable from annotator code needs a named trust boundary. It walks method by method and stops at listed scoped entry points, and those entry points' signatures are still checked.
- A rule that passes on a code base with no annotator screens proves nothing, so it was run against fixtures that each leak one way, and against a temporary shipped class that had to fail.
- The fixture tests found a real defect in the first draft (see Verification). Writing the partition list before the tests also exposed two failures that would have left the workspace half-written.

## Agent responses and outcomes

- Used `write-plan`, `implement-feature`, `write-test`, `review` and `log`; `clarify-requirements` was skipped because [#11]'s scope and criteria were already agreed. The plan is `plans/test-harness.md`, and neither the plan nor the acceptance has been approved yet.
- Reset local `feat/test-harness` to `origin/main` (13b6c35). The two old commits stay reachable in PR #69's history.
- Added under `src/test/java`:
  - `arbiter.testing.TestWorkspace`: one fresh workspace per test folder. `writeSource` registers files through `SourceResolver.resolveForImport`, and `signIn` returns a signed-in `AuthService`.
  - `arbiter.testing.Records`: `JsonStoreFixtures` moved and made public, plus scale builders and an item from a registered source. The store tests change only their imports.
  - `arbiter.testing.ClassificationWorkflow`: seeds SINGLE or SCALE projects. It supports configurable *k* (defaulting as the model does), answers for the first items in split order, derived assignment status, unassigned and disabled annotators, and resolutions the test names. It refuses unreachable states before writing anything.
  - `arbiter.service.TestAccounts`: real `PasswordHasher` credentials, until [#31] provides account creation.
  - `arbiter.blindness.BlindnessRule` and `AnnotatorBlindnessTest`, plus fixtures under `arbiter.blindness.fixture`. ArchUnit 1.4.1 was added as a test-only dependency.
- Added one bullet to `context/architecture.md` saying what the blindness test treats as annotator-facing and where trusted reads are listed.

## Verification

- Before relying on ArchUnit 1.4.1, a probe confirmed that it imports the Java 25 classes, attributes lambda bodies to their enclosing method, reports method references and lists interface implementations. One PBKDF2 hash took about 65 ms, so each fixture account gets its own real credentials.
- A temporary `arbiter.ui.annotator.TempLeakScreen`, reading a resolution through a temporary `arbiter.service.TempResults`, made `annotatorBlindness_shippedCode_noViolations` fail with the full call path. Both classes were then deleted.
- The first fixture run had 8 failures. They came from a fixture defect, not a test defect: `Integer k = ... ? annotationsPerItem : assignees.size()` unboxed a null. It was fixed by an explicit branch, and the tests were left unchanged.
- JDK 25 `./gradlew cleanTest check shadowJar --no-daemon` passed: 178 JUnit tests, 44 of them new (11 blindness, 5 workspace, 28 workflow), none skipped, both Checkstyle tasks, and the release jar.
- Pending: human approval of the plan, especially the open decision on shared components and resolutions; human acceptance of the fixture API; teammate review; CI on the pushed revision.

[#10]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/10
[#11]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/11
[#27]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/27
[#31]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/31
