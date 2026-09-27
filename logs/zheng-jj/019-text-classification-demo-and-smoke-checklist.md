# Text-classification demo and smoke checklist

Status: Awaiting human verification.

## Original request

- After asking what was left once #37 merged, the owner said "do 41 and 43". This log covers [#41]; [#43] has its own.

## Follow-ups, corrections, and reflection

- Partway through, the owner approved every plan in this batch ("i approve of all plans, so keep the logs approved") and asked for Whimsyturtle to be requested as reviewer once all three tasks were done, which covers delivery.
- The test fixture `ClassificationWorkflow` writes records directly, which would have been the quickest way to seed. #41 forbids a role bypass, so the seeder goes through the public services, signed in as each account. That also means automatic resolution runs exactly as it does in the app.
- Split order is random (rule 7), so answers keyed by queue position would have given a clearly negative review a "positive" majority. Keying each planned answer by file fixes every outcome regardless of order, and the file names stay neutral so they never hint at a label.
- `UiConventionTest` scans every non-test `arbiter` class, so the demo's classes are covered too. The seeder therefore prints nothing, and the Gradle task reports the result.

## Agent responses and outcomes

- Used `write-plan`, `implement-feature`, `write-test`, `review`, `maintain-docs` and `log`. The plan is `plans/demo-and-smoke-checklist.md`, on branch `feat/demo-seed`.
- Added a `demo` Gradle source set holding `arbiter.demo.DemoWorkspace` and a twelve-file corpus, the `seedDemo` task, `docs/SmokeChecklist.md` (Part A by hand from a fresh workspace, Part B on the seeded one, and a results table), and the command and checklist link in the Developer Guide.
- The seeded workspace has `SINGLE` Product reviews (*k* = 3, four majorities, two disputes, CSV) and `SCALE` Answer helpfulness (*k* = 2, four means, two files left for bob, JSON).

## Verification

- `DemoWorkspaceTest` adds 5 tests. A mutation run that changed one planned label and one planned rating failed the two outcome tests.
- JDK 25 `./gradlew cleanTest check shadowJar --no-daemon` passed: 471 JUnit tests, with 3 existing case-sensitivity tests skipped on macOS. The first run failed Checkstyle on one 121-character test line, which was rewrapped.
- The release jar contains no `arbiter/demo` entry, and `Launcher` and `Arbiter` are unchanged.
- Not verified: the checklist has not been run in the app. The owner runs it and records the result in its table.
- The owner approved the plan and delivery on 28 September 2026.

[#41]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/41
[#43]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/43
