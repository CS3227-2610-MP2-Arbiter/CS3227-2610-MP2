---
name: review
description: Verify an Arbiter change by running relevant automated checks and reviewing correctness, design, tests and documentation. Also use for test-running requests.
---

# review

**Input:** A change or revision to verify, its acceptance criteria and any existing verification results.

1. Inspect the diff and relevant surrounding code against the requirements. Review correctness, role isolation, data integrity, shared design, test coverage, documentation, style and unused code. For screen changes, check the UI rules in `context/architecture.md`, including that no text can be cut off, and add a check with the longest realistic text to the remaining human checks.
2. For code changes, use JDK 25 and run `./gradlew check shadowJar` (Windows: `.\gradlew.bat check shadowJar`). For focused test requests or documentation-only changes, run the applicable checks and state the scope.
3. Inspect command results and test reports. Distinguish passing tests from skipped tasks, cached results, `NO-SOURCE` and checks that could not run. Never hide failures or weaken tests to pass.
4. Report findings with severity, file locations and concrete impact. For a review-only request, leave edits to the user; during an authorized implementation, fix issues within scope and rerun affected checks.

**Done when:** The report identifies the revision or working-tree changes reviewed, commands and actual results, unresolved findings and remaining human checks. If no findings were found, state that along with verification limits.
