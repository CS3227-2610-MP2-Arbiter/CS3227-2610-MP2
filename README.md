# Arbiter

Arbiter is an offline Java desktop app for teams that label text. See the [product site](https://cs3227-2610-mp2-arbiter.github.io/CS3227-2610-MP2/) for how it works and its documentation.

## Building

See [Setting up](docs/DeveloperGuide.md#setting-up) in the Developer Guide.

## Running a release

Arbiter needs Java 25 or later. The jar bundles JavaFX, so nothing else needs installing.

1. Get `arbiter.jar`. To build it yourself, see [Setting up](docs/DeveloperGuide.md#setting-up).
2. Start Arbiter by running `java -jar arbiter.jar` from the folder that holds it. Check `java -version` first: a Java older than 25 fails with `UnsupportedClassVersionError` instead of starting.
3. On first run, the workspace wizard opens. Continue with [Getting started](docs/UserGuide.md#getting-started) in the User Guide.

The jar's JavaFX native libraries cover Windows and Linux on x64 and macOS on Apple Silicon. Intel Macs and ARM builds of Windows and Linux are not supported.
