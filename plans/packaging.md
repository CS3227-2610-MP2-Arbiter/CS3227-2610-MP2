# Packaging: shadowJar

**Issue:** [#42] - Packaging: shadowJar
**Branch:** `feat/packaging`, stacked on `feat/demo-seed` ([#41], PR #90)
**Status:** Plan approved by the owner on 28 September 2026, after it was implemented at their request ("sure start 42"). On 28 September 2026 the teammate review of PR #92 asked to drop the run scripts ("The jar is enough ... there's a lot of edge cases currently unhandled"), and the owner approved doing so. Delivery approved; awaiting the three-platform CI run, the human checks and the teammate's re-review.

The scope and acceptance criteria live in [#42]. This plan records what was built and what is still open.

## Goal and scope

Ship `arbiter.jar`, document running it in the README, and prove in CI that the jar starts on all three platforms with nothing but a plain JDK.

Non-goals: installers, code signing and, after review, run scripts and a release zip.

## Proposed changes

- **CI:** the "Release jar on Apple Silicon" job becomes a `release` job on all three platforms. It uses a plain Temurin JDK, whose JavaFX comes only from the jar. It builds the jar, starts it with `java -jar` (Linux under `xvfb-run`), and fails if the app exits within 25 seconds, logs a native-library or uncaught exception, or records a failure through `DiagnosticLog`. The launch is one PowerShell step on every platform, so the check cannot drift between shells. macOS keeps the arm64 natives check. No branch rule requires the old job's name.
- **Docs:** a "Running a release" section in the README (requirements, starting the jar, supported architectures). "Setting up" in the Developer Guide links the README from the `shadowJar` command, and its CI paragraph describes the new job. The User Guide's first step links the README.

## Design alternatives considered

| Choice | Alternative rejected | Why |
| --- | --- | --- |
| The jar alone, started with `java -jar` | Hand-written run scripts in a release zip | Review of PR #92: the scripts had edge cases left unhandled, and the jar already runs on its own. The cost is that a Java older than 25 fails with Java's `UnsupportedClassVersionError` rather than a friendlier message |
| Smoke-launch the jar on all three platforms | Only on Apple Silicon | [#42] requires the jar to run on all three; Windows and Linux natives were never exercised by CI |
| Supported: Windows x64, Linux x64, macOS arm64 | Shading more architectures | The jar can hold only one macOS architecture, because both store natives at the same paths (see `build.gradle`) |

## Open decisions

None. [#42]'s title, scope and acceptance criteria were updated to match the reviewed scope.

## Verification

| Acceptance criterion | How it is verified |
| --- | --- |
| `arbiter.jar` runs on Windows, macOS and Linux | CI `release` job on all three platforms; human check on at least one real machine |
| Running the jar is documented | Review of the README |
| First run from a clean machine reaches the workspace wizard | CI starts the jar with only a plain JDK and checks it stays open; the wizard is the first thing it shows, which a human confirms |
| CI green across the full matrix | The pull request's checks |

## Implementation and verification

- Before review, the jar started through the run scripts locally on macOS arm64, and JDK 25 `./gradlew check shadowJar --no-daemon` passed.
- After dropping the scripts, `./gradlew check shadowJar` passes and `java -jar build/libs/arbiter.jar` starts Arbiter locally on macOS arm64.

### Human acceptance checks

1. On Windows, run `java -jar arbiter.jar`: the workspace wizard opens.
2. On macOS or Linux, run `java -jar arbiter.jar`: the workspace wizard opens.

[#41]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/41
[#42]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/42
