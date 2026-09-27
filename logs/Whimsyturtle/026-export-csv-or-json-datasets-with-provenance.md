# Export CSV or JSON datasets with provenance

Status: Human verified.

## Original request

- The human asked the agent to start issue #37 following `context/swe.md`, keep it simple and not overengineer, use the repo's skills as needed and ask about important decisions.

## Follow-ups, corrections, and reflection

- Clarifying #37 found its dependencies #10, #27, #30 and #34 closed. The human raised no objection to the agent's assumptions: an **Export** button on the project page, dialogs for the preview and report, and the proposed format details. They decided:
  - Every item is exported, and unresolved items are marked `UNRESOLVED` with their sources still checked. The agent had recommended leaving them out.
  - Each item gives its source path only, with no text. The agent had recommended both.
  - The output is a fixed `exports/project-<id>.csv` or `.json`, replaced atomically.
  - The CSV has one row per item, with numbered provenance columns.
- After planning, the human chose Jackson's CSV module over hand-written quoting, and approved `plans/export.md` once the repeated `project` and `taxonomy_kind` columns were dropped from the CSV.
- The human asked for a thorough second review with the `review` skill, focusing on edge cases, missing tests, duplication, misleading names and comments, and abstraction. It found no correctness bugs, but trying the export against a locked file found a failure the first review missed.
- The human asked the agent to fix findings 1 to 10 below.

## Agent responses and outcomes

- The agent added a "Decisions" section to #37, reworded its unresolved-items criterion, and created `feat/export` and the plan.
- The agent added `ExportService`, `ExportSummary`, the **Export** button and the `jackson-dataformat-csv` dependency. It made `JsonStore.publishAtomically` public and reworded the `OutputFormat` and `Project.outputFormat` Javadoc.
- `ExportServiceTest` covers both formats, provenance, unresolved and empty projects, replacement, failed sources and role checks. It found no production defects.
- The first review found no major problems. Its three minor findings were fixed:
  - The **Export** button separated **Disputes** from its hint, so the buttons were reordered.
  - A test checked for a BOM with an invisible character instead of `\uFEFF`.
  - The plan's status was out of date.
- The second review's findings, all fixed:
  1. Exporting again while Excel had the earlier CSV open failed without saying why. The message now says to close it.
  2. No test covered SCALE in CSV, a whole-number mean or a resolved row with fewer submissions than the header. A new test covers all three.
  3. The CSV's column names and values were matched by position in two lists. Each column now declares both.
  4. The status was a plain string. It is now a private enum.
  5. A test comment contradicted itself, and the Javadoc said answers were in the order submitted, though they are sorted by submission time.
  6. The Javadoc said sources are checked "first" rather than before anything is written.
  7. Some variables shared a method's name, such as the `export` button and `export()`. These were renamed.
  8. A label from another project was written as a blank answer. The export now fails, with a test.
  9. The registration order and unresolved count were duplicated. `CorpusService` and `ProjectService` now share them.
  10. The dialogs read as if files were copied. They now say the export lists them.
- The agent reworded the plan's submission-order assumption to "by submission time" to match the tests, and added a human check for re-exporting while Excel has the file open.
- The agent grouped the changes for staging, and the human committed them as the wording, the service with its tests, and the button.
- After acceptance, the agent added the export step to the User Guide and Jackson's export role to the Developer Guide's acknowledgements.
- Pending: committing the docs, plan and log, and the pull request.

## Verification

- After the fixes, `.\gradlew.bat check shadowJar --rerun-tasks` passed with 454 tests, no failures and six skips from existing tests Windows cannot run, including all 11 in `ExportServiceTest`. A plain run had been fully cached, so it proved nothing.
- The first final run failed because two Gradle builds ran at once in the same checkout; a rerun passed. Builds should not run in parallel in one checkout.
- The human reported that the acceptance checks passed: CSV and JSON export, cancel, an empty project, a renamed source, re-exporting while Excel has the file open, long text and a narrow window.
