# Packaging: shadowJar and run scripts

**Issue:** [#42] - Packaging: shadowJar and run scripts
**Branch:** `feat/packaging`, stacked on `feat/demo-seed` ([#41], PR #90)
**Status:** Plan approved by the owner on 28 September 2026, after it was implemented at their request ("sure start 42"). Delivery approved; awaiting the three-platform CI run, the human checks and teammate review.

The scope and acceptance criteria live in [#42]. This plan records what was built and what is still open.

## Goal and scope

Ship `arbiter.jar` with run scripts that start it on Windows, macOS and Linux, document running it in the README, and prove in CI that the release starts on all three platforms with nothing but a plain JDK.

Non-goals, from [#42]: installers and code signing.

## Proposed changes

- **Run scripts** in `scripts/`: `arbiter.sh` (macOS and Linux) and `arbiter.bat` (Windows). Each finds Java through `JAVA_HOME` or the `PATH`, refuses one older than 25 with a message naming its version (instead of Java's `UnsupportedClassVersionError`), refuses if `arbiter.jar` is not beside it, and otherwise runs `java -jar arbiter.jar`. The jar's manifest already enables native access, so the scripts pass no JVM flags. The Windows script pauses after an error so a double-clicked window stays open, and uses labels rather than parenthesized blocks so a Java path with parentheses cannot break it.
- **`./gradlew releaseZip`** builds `build/distributions/arbiter.zip`: an `arbiter/` folder with the jar and both scripts, the shell script marked executable.
- **`.gitattributes`** keeps `scripts/*.bat` in CRLF and `scripts/*.sh` in LF, whatever the checkout platform.
- **CI:** the "Release jar on Apple Silicon" job becomes a `release` job on all three platforms. It uses a plain Temurin JDK, whose JavaFX comes only from the jar. It builds the zip, unpacks it into a path with a space, starts it through its run script (Linux under `xvfb-run`), and fails if the app exits within 25 seconds or logs a native-library or uncaught exception. macOS keeps the arm64 natives check. macOS and Linux also check that the script refuses a fake Java 21. No branch rule requires the old job's name.
- **Docs:** a "Running a release" section in the README (requirements, starting on each platform, supported architectures). "Setting up" in the Developer Guide gains the `releaseZip` command, and its CI paragraph describes the new job. The User Guide's first step links the README.

## Design alternatives considered

| Choice | Alternative rejected | Why |
| --- | --- | --- |
| Short hand-written scripts | Shadow's generated `shadowDistZip` start scripts | The generated scripts give no clear message for the most likely failure, an old Java, and put the jar under `lib/` instead of beside the scripts |
| Smoke-launch through the scripts on all three platforms | Only on Apple Silicon | [#42] requires the jar to run on all three; Windows and Linux natives were never exercised by CI |
| Supported: Windows x64, Linux x64, macOS arm64 | Shading more architectures | The jar can hold only one macOS architecture, because both store natives at the same paths (see `build.gradle`) |

## Open decisions

None.

## Verification

| Acceptance criterion | How it is verified |
| --- | --- |
| `arbiter.jar` runs on Windows, macOS and Linux | CI `release` job on all three platforms; human check on at least one real machine |
| Run scripts added and documented | Review of `scripts/` and the README; `shellcheck` and local runs of `arbiter.sh` |
| First run from a clean machine reaches the workspace wizard | CI starts the unpacked release with only a plain JDK and checks it stays open; the wizard is the first thing it shows, which a human confirms |
| CI green across the full matrix | The pull request's checks |

## Implementation and verification

- Locally on macOS arm64, from the unpacked zip in a folder with a space in its name: `arbiter.sh` started Arbiter, which stayed open; with a fake Java 21 it refused and named the version; with no Java and with no jar it refused with its message. Each refusal exited with status 1.
- `shellcheck scripts/arbiter.sh` and `actionlint .github/workflows/gradle.yml` report nothing. `arbiter.bat` cannot run here; the Windows CI job is its first run.
- JDK 25 `./gradlew check shadowJar releaseZip --no-daemon` passed: 471 JUnit tests, with 3 case-sensitivity tests skipped on macOS.

### Human acceptance checks

1. On Windows, unzip `arbiter.zip` and double-click `arbiter.bat`: the workspace wizard opens.
2. On macOS or Linux, run `./arbiter.sh` from the unzipped folder: the workspace wizard opens.
3. With `JAVA_HOME` set to a Java older than 25, each script refuses with a message naming that version.

[#41]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/41
[#42]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/42
