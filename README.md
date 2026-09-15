# Arbiter

Arbiter is an offline Java desktop app for teams that label data. It has two roles with separate interfaces:

- **Annotators** log in and work a blind queue: one item at a time, select a label, add a rationale, and flag anything unclear. They never see each other's answers.
- **Adjudicators** import a corpus, split it into batches, assign annotators, and resolve the items where annotators disagreed. They own the label taxonomy and export the finished dataset with provenance for every decision.

## Documentation

- [User Guide](docs/UserGuide.md)
- [Developer Guide](docs/DeveloperGuide.md)
- [Reflections](docs/Reflections.md)

## Building

Requires JDK 25. The Gradle wrapper downloads everything else.

| Task | Command |
| --- | --- |
| Run the app | `./gradlew run` |
| Tests + Checkstyle | `./gradlew check` |
| Build `build/libs/arbiter.jar` | `./gradlew shadowJar` |

On Windows use `.\gradlew.bat` instead of `./gradlew`.
