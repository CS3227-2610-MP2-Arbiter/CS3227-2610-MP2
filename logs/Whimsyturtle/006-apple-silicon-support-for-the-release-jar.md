# Apple Silicon support for the release jar

Status: Human verified.

## Original request

- The user asked for Apple Silicon support and a CI check for it, after an MP1 peer review reported that the GUI did not open on an M3 Mac.

## Follow-ups, corrections, and reflection

- The agent proposed a universal jar carrying every architecture. The user chose the one-line classifier swap and a macOS-arm64-only CI job.
- The user dropped the "Supported platforms" section the agent had added to the Developer Guide, and asked for shorter issue, pull request and log text. The lesson is to keep supporting prose short and high level by default.
- A teammate merged PR #53 before the agent had finished, so the remaining edits went into PR #54.

## Agent responses and outcomes

- `build.gradle` used the `mac` JavaFX classifier, which is x86_64 only, so the jar could not load its native libraries on Apple Silicon. Changed it to `mac-aarch64`.
- Only one macOS architecture can be shipped, because both store their natives under the same paths in the jar.
- CI used a JDK with JavaFX bundled in, which hid the problem. Added a job that runs the jar on Apple Silicon with a plain JDK.
- Issue [#52](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/52), pull requests [#53](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/pull/53) and [#54](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/pull/54).

## Verification

- `./gradlew check shadowJar` passed on Windows. The repository has no tests yet, so `test` reported `NO-SOURCE`.
- The bundled macOS natives are now arm64, and the Windows and Linux natives are unchanged.
- CI passed, including the new job, which ran the jar on an Apple Silicon runner with no native library errors.
- Restoring the old classifier on a throwaway branch made the new job fail, confirming that it catches the problem.
