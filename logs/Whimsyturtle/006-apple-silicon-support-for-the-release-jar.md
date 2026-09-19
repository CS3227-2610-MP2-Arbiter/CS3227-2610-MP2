# Apple Silicon support for the release jar

Status: Awaiting human verification.

## Original request

- The user asked for Apple Silicon support even though MP2 does not prescribe it, and for a CI check if possible. The motivation was an MP1 peer review reporting that the GUI did not open on an M3 Mac, and a grader noting that the release file and `build.gradle` had to be modified for grading.

## Follow-ups, corrections, and reflection

- The agent proposed a universal jar that carries every architecture under `native/<os>-<arch>/` and points `java.library.path` at the matching set. The user chose the one-line classifier swap, macOS-arm64-only CI, and a new issue with a branch and PR.
- The agent noted before implementing that the swap drops Intel Mac support on a plain JDK. The user had already accepted this in choosing the approach.
- A green CI job proves nothing on its own, so the agent pushed a throwaway branch that reintroduced the `mac` classifier to confirm the check fails. The branch was deleted afterwards.
- The user dropped the "Supported platforms" section the agent had added to the Developer Guide as unnecessary, and asked for shorter issue and pull request descriptions. Both were rewritten. The lesson is to keep supporting prose short by default rather than explaining the full reasoning in every artefact.

## Agent responses and outcomes

- Root cause: `build.gradle` used the `mac` classifier, which is x86_64 only. Disassembling `NativeLibLoader` confirmed JavaFX resolves natives from the JDK's lib directory, then the classpath resource, then `java.library.path`, then `System.loadLibrary`. On a plain ARM JDK the classpath resource is the x86_64 dylib and fails.
- `javafx-graphics-*-mac.jar` and `*-mac-aarch64.jar` store natives under identical paths, so a shaded jar can carry only one macOS architecture. Adding `mac-aarch64` alongside `mac` would leave one silently overwriting the other.
- The existing CI matrix sets `java-package: jdk+fx`, so the JDK's own JavaFX modules win over the classpath and the jar's natives are never exercised. The matrix was structurally unable to detect the defect.
- Changed the classifier to `mac-aarch64` and added an `apple-silicon-jar` CI job on a plain Temurin JDK 25. The Developer Guide notes the new job in its CI paragraph.
- Issue [#52](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/52), branch `build/apple-silicon-native-support`, plan `plans/apple-silicon-native-support.md` (first file under `plans/`), PR [#53](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/pull/53).

## Verification

- `./gradlew check shadowJar` on Windows with Temurin 25.0.4.1: BUILD SUCCESSFUL. `test` and `checkstyleTest` reported `NO-SOURCE`; the repository has no tests yet, which is pre-existing.
- Mach-O `cputype` of the bundled `libglass.dylib` moved from `0x01000007` (x86_64) to `0x0100000C` (arm64). All eight bundled `.dylib` files are arm64; the 54 Windows `.dll` and 10 Linux `.so` natives are unchanged.
- CI run 35448155925 passed on all four jobs. The new job ran on `macos-26-arm64` with a plain arm64 Temurin JDK, `lipo -archs` reported `arm64` for every dylib, and the jar stayed open for 25 seconds with no native library errors.
- Regression check: run 35448277756 with the `mac` classifier restored failed with `libdecora_sse.dylib is x86_64, not arm64`, while all three `jdk+fx` matrix jobs passed. This confirms both that the new job catches the defect and that the old matrix could not.
- No JUnit test was added. The change is build configuration, and a test reading `build/libs/arbiter.jar` would need `check` to depend on `shadowJar`.
- Not verified: behaviour on a physical Apple Silicon Mac, and Intel Mac behaviour under the prescribed Zulu JDK+FX.
