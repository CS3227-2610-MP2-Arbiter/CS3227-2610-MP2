# Packaging: shadowJar and run scripts

Status: Awaiting human verification.

## Original request

- The owner asked what was unblocked, heard that [#42] could start ahead of [#41] merging by stacking on its branch, and said "sure start 42".

## Follow-ups, corrections, and reflection

- The existing CI job ran the release jar only on Apple Silicon, so the Windows and Linux JavaFX natives in the jar had never been exercised. [#42]'s "runs on all three" called for extending that job to all three platforms rather than adding a new one.
- Shadow's generated start scripts were the obvious off-the-shelf choice. They were rejected because they fail on an old Java with `UnsupportedClassVersionError`, the least helpful message for the most likely problem.
- `shellcheck` flagged `CDPATH= cd` (SC1007), which was changed to `CDPATH=''`.
- The Windows script was first written with parenthesized `if` blocks. It was changed to labels, because a Java path such as `C:\Program Files (x86)\...` expanded inside a block would end it early.
- Whimsyturtle's review of PR #92 requested changes: "The jar is enough. I'd drop the scripts, there's a lot of edge cases currently unhandled". The owner asked the agent to resolve it and approved delivery, so the scripts and the zip were dropped and [#42]'s title, scope and acceptance criteria were updated to match. The friendlier old-Java message is the cost accepted.
- Before that, PR #92 conflicted with `main` in "Setting up" in `docs/DeveloperGuide.md`: the branch added the `releaseZip` line while #90 reworded the `seedDemo` line. Both were kept, and the merge was committed and pushed with the owner's approval.

## Agent responses and outcomes

- Used `write-plan`, `implement-feature`, `review`, `maintain-docs` and `log`. The plan is `plans/packaging.md`, on branch `feat/packaging`, stacked on `feat/demo-seed`.
- Added `scripts/arbiter.sh`, `scripts/arbiter.bat`, `.gitattributes`, the `releaseZip` task, a three-platform `release` CI job, the README's "Running a release" section, and links from the Developer and User Guides.
- After review: removed `scripts/`, `.gitattributes` and the `releaseZip` task; the CI `release` job now builds the jar and starts it with `java -jar` on all three platforms, keeping the macOS arm64 check; the README, Developer Guide and plan describe the jar alone. Asked Whimsyturtle to re-review.

## Verification

- Local macOS runs of `arbiter.sh` from the unpacked zip covered a launch, an old Java, no Java and no jar.
- `shellcheck` and `actionlint` pass.
- JDK 25 `./gradlew check shadowJar releaseZip --no-daemon` passed: 471 JUnit tests, with 3 skipped on macOS.
- After dropping the scripts: `./gradlew check shadowJar` passed, and `java -jar build/libs/arbiter.jar` stayed open for 20 seconds on macOS arm64 with no startup errors.
- Pending: the CI run on all three platforms, the human checks in the plan, and the re-review.
- The owner approved the plan and delivery on 28 September 2026.

[#41]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/41
[#42]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/42
