# Apple Silicon support for the release jar

- **Issue:** [#52](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/52)
- **Branch:** `build/apple-silicon-native-support`
- **Status:** Approved by the human on the approach and CI scope.

## Goal

`java -jar arbiter.jar` opens the Arbiter window on an Apple Silicon Mac running a plain JDK 25, and
CI fails if that stops being true.

## Non-goals

- Intel Macs on a plain JDK. They keep working under the prescribed Zulu JDK+FX only.
- Linux on ARM, `win-x86`, installers, code signing, run scripts (issue #42 owns packaging).
- A universal jar carrying both macOS architectures. Considered and rejected below.

## Proposed changes

1. **`build.gradle`** - resolve macOS JavaFX with the `mac-aarch64` classifier instead of `mac`.
2. **`.github/workflows/gradle.yml`** - add an `apple-silicon-jar` job on `macos-latest` using a
   plain Temurin JDK 25 (deliberately not `jdk+fx`) that builds `shadowJar`, asserts the bundled
   `.dylib` files are arm64, and smoke-launches the jar.
3. **`docs/DeveloperGuide.md`** - mention the new job in the CI paragraph. A "Supported platforms"
   section was written and then dropped at the human's request as unnecessary; the constraint is
   recorded in the `build.gradle` comment and in issue #52.

## Design decision

`javafx-graphics-*-mac.jar` and `*-mac-aarch64.jar` store their natives under identical paths at the
jar root (`libglass.dylib`, `libprism_common.dylib`, ...), so a shaded jar can carry only one macOS
architecture. Windows and Linux do not collide, because their natives use `.dll` and `.so`.

A universal jar is possible: strip natives from the jar root, re-add them under `native/<os>-<arch>/`
and have `Launcher` extract the matching set and prepend it to `java.library.path`, which JavaFX's
`NativeLibLoader` reads at load time via `System.getProperty`. It was rejected as disproportionate
for one platform gap, and because it puts every platform on a custom code path.

Swapping the classifier is a one-line change with no new code and no new failure modes. The cost is
Intel Macs on a plain JDK, which the prescribed JDK+FX distribution already covers.

## Verification

| Acceptance criterion | How it is verified |
| --- | --- |
| Window opens on Apple Silicon with a plain JDK 25 | New `apple-silicon-jar` CI job smoke-launches the jar |
| macOS natives are arm64 | Same job asserts `lipo -archs` reports `arm64` for every bundled `.dylib` |
| Windows and Linux x86_64 unaffected | Existing `build` matrix runs `./gradlew check shadowJar` |
| Intel Mac trade-off is recorded | `build.gradle` comment and issue #52 |

No JUnit test is added. The change is build configuration, and a test that unzips
`build/libs/arbiter.jar` would need `check` to depend on `shadowJar`, which inverts the task order CI
relies on. The CI job is the regression guard.

## Risks

- The smoke launch needs a window server on the macOS runner. If GitHub's runner cannot provide one,
  the launch step is replaced by loading the natives directly and the arm64 assertion stays as the
  hard gate. Confirm against the first CI run.
- `macos-latest` must actually be arm64. The job asserts `uname -m` is `arm64` so the check cannot
  pass vacuously.
