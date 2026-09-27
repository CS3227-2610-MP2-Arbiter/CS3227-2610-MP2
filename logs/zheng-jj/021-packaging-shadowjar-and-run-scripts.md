# Packaging: shadowJar and run scripts

Status: Awaiting human verification.

## Original request

- The owner asked what was unblocked, heard that [#42] could start ahead of [#41] merging by stacking on its branch, and said "sure start 42".

## Follow-ups, corrections, and reflection

- The existing CI job ran the release jar only on Apple Silicon, so the Windows and Linux JavaFX natives in the jar had never been exercised. [#42]'s "runs on all three" called for extending that job to all three platforms rather than adding a new one.
- Shadow's generated start scripts were the obvious off-the-shelf choice. They were rejected because they fail on an old Java with `UnsupportedClassVersionError`, the least helpful message for the most likely problem.
- `shellcheck` flagged `CDPATH= cd` (SC1007), which was changed to `CDPATH=''`.
- The Windows script was first written with parenthesized `if` blocks. It was changed to labels, because a Java path such as `C:\Program Files (x86)\...` expanded inside a block would end it early.

## Agent responses and outcomes

- Used `write-plan`, `implement-feature`, `review`, `maintain-docs` and `log`. The plan is `plans/packaging.md`, on branch `feat/packaging`, stacked on `feat/demo-seed`.
- Added `scripts/arbiter.sh`, `scripts/arbiter.bat`, `.gitattributes`, the `releaseZip` task, a three-platform `release` CI job, the README's "Running a release" section, and links from the Developer and User Guides.

## Verification

- Local macOS runs of `arbiter.sh` from the unpacked zip covered a launch, an old Java, no Java and no jar.
- `shellcheck` and `actionlint` pass.
- JDK 25 `./gradlew check shadowJar releaseZip --no-daemon` passed: 471 JUnit tests, with 3 skipped on macOS.
- Pending: the CI run on all three platforms (the first run of `arbiter.bat`), and the human checks in the plan.
- The owner approved the plan and delivery on 28 September 2026.

[#41]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/41
[#42]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/42
