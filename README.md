# Arbiter

Arbiter is an offline Java desktop app for teams that label data. See the [product site](https://cs3227-2610-mp2-arbiter.github.io/CS3227-2610-MP2/) for how it works and its documentation.

## Building

See [Setting up](docs/DeveloperGuide.md#setting-up) in the Developer Guide.

## Running a release

Arbiter needs Java 25 or later. The jar bundles JavaFX, so nothing else needs installing.

1. Get `arbiter.zip` and unzip it. Its `arbiter` folder holds `arbiter.jar` and two run scripts. To build the zip yourself, see [Setting up](docs/DeveloperGuide.md#setting-up).
2. Start Arbiter:
   - **macOS or Linux:** run `./arbiter.sh` from the `arbiter` folder.
   - **Windows:** double-click `arbiter.bat`, or run it from a command prompt.

   Each script uses `JAVA_HOME` if it is set, and otherwise the `java` on your `PATH`. If that Java is older than 25, the script says so instead of starting. When Java 25 is already your default, `java -jar arbiter.jar` works too.
3. On first run, the workspace wizard opens. Continue with [Getting started](docs/UserGuide.md#getting-started) in the User Guide.

The jar's JavaFX native libraries cover Windows and Linux on x64 and macOS on Apple Silicon. Intel Macs and ARM builds of Windows and Linux are not supported.
