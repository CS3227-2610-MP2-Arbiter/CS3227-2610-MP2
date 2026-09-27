# Export

**Issue:** [#37] - Export CSV or JSON with current-decision provenance
**Branch:** `feat/export`, from `main` after PR #86 merged
**Status:** Approved by the owner on 27 September 2026. Implemented as planned with `ExportServiceTest`, review findings fixed, and `check shadowJar` passes. Accepted by the owner on 27 September 2026, and the guides are updated.

What the feature does, including the decisions on unresolved items, item identity, location and CSV layout, is in [#37], and the rules it relies on are rules 1, 15, 17 and 21 in [UserFlows](../docs/UserFlows.md#3-rules-both-tracks-share). This plan records how the code will meet them and what is decided.

## Goal and scope

From a project's page, at any stage, let the adjudicator preview and then write the project's dataset in its output format to `exports/`, after every item's source passes [#10]'s checks.

Non-goals: everything under "Out" in [#37].

## Decided

Decided by the owner on 27 September 2026, besides the decisions in [#37]:

1. An **Export** button beside **Taxonomy** and **Disputes** on the project page. Its preview is a `Dialogs.confirm` showing the output path and the resolved and unresolved counts, and its report is a `Dialogs.showSuccess`, as **Generate splits** does.
2. Jackson's CSV module (`tools.jackson.dataformat:jackson-dataformat-csv:3.2.3`) writes the CSV.

Assumptions accepted by the owner:

- A SINGLE answer is written as its `Label.key` and a SCALE answer as a number; a mean is the unrounded `double`.
- Times are ISO-8601 in UTC. The decider is blank for an automatic decision, and a deactivated annotator is still named.
- Files are UTF-8 without a BOM, with RFC 4180 quoting. A project with nothing resolved still gets a valid file.
- The first failing source stops the export with the resolver's message, and the previous export and `arbiter.json` are left as they were.
- The export reads stored decisions from one snapshot and never recomputes them.
- The JSON is `{project, taxonomyKind, items: [{path, status, answer, method, decidedBy, decidedAt, submissions: [{annotator, answer, submittedAt}]}]}`. The CSV carries each item's fields, flattened, without `project` and `taxonomyKind`, since the file name identifies the project.
- `project` is the project's name, `status` is `RESOLVED` or `UNRESOLVED`, and people are named by username. A blank field is `null` in JSON and an empty cell in CSV.
- The CSV columns are `path`, `status`, `answer`, `method`, `decided_by` and `decided_at`, then the numbered groups. With nothing resolved there are no numbered columns. Rows end with `\n`, Jackson's default.
- Items are in registration order, as the project page lists them, and each item's submissions by submission time.
- Numbers are written in Java's shortest exact form, so a mean of 3 is `3.0`, one of 7/3 is `2.3333333333333335`, and ratings are whole numbers.
- The dialogs show the file's full path. The counts are read again when exporting, and the report gives the counts written.

## Current state

- `Project.outputFormat` holds `CSV` or `JSON`, fixed at creation ([#30]). Its Javadoc and `OutputFormat`'s say "the finished dataset". The architecture context names `ExportService`, which does not exist yet.
- `WorkspaceService` creates `exports/` and requires it on open. Nothing writes there.
- `JsonStore`'s private `publishAtomically` replaces `arbiter.json` with a forced temporary file and an atomic move.
- `SourceResolver.resolve` runs [#10]'s checks, and `ResolutionService.comparison` calls it outside the store's action.
- A `Resolution` holds its method, label or mean, decider and time, and its contributors are its item's submitted answers, as its Javadoc says. `ProjectService` counts items without one as Unresolved.
- `build.gradle` has `jackson-databind` 3.2.3 and no CSV library.

## Proposed changes

- **`ExportService`** (new, `arbiter.service`) is built from the store, `AuthService` and `WorkspacePaths`, like `ResolutionService`. Each method first requires the adjudicator and refuses an unknown project with a `ProjectException`.
  - `preview(projectId)` reads the project and counts its items with and without a resolution in one read, and returns an `ExportSummary`. It writes nothing.
  - `export(projectId)` builds the dataset in one read from the stored project, labels, items, resolutions, answers and usernames. Outside that read, it passes every item's path and hash to `SourceResolver.resolve` in order and lets the first `SourceException` through. Only then does it write the dataset in the project's format and publish it with `JsonStore.publishAtomically`, turning an `IOException` into a `ProjectException` that names the file and says to close it if another program has it open. It returns an `ExportSummary` of what it wrote.
  - The dataset is held in private records, and both formats are written from them: JSON with a Jackson `JsonMapper`, and CSV with Jackson's `CsvMapper`.
  - It shares `CorpusService`'s registration order and `ProjectService`'s unresolved count, as package-private helpers.
- **`ExportSummary`** (new record, `arbiter.service`) holds the file's path and the resolved and unresolved counts.
- **`JsonStore.publishAtomically`** becomes public, and its Javadoc says a reader sees the old file or the new one, never part of one.
- **`build.gradle`** gains `tools.jackson.dataformat:jackson-dataformat-csv:3.2.3`, matching `jackson-databind` (decision 2).
- **`ProjectPage`** gains **Export**, which previews, confirms, exports and reports. It shows a failure with `Dialogs.showError` and does not reload, since nothing changed. **`ProjectsScreen`** and **`Arbiter`** pass the new service through.
- **Wording:** `OutputFormat` and `Project.outputFormat` say the format a project's dataset is exported in, not "the finished dataset". `ProjectPage`'s Javadoc names export.
- **Documentation**, in the Accept step: the User Guide's Projects section says how to export, and the Developer Guide's acknowledgements say Jackson also writes the exports.

The model, repositories, `JsonIntegrity` and the architecture context do not change.

## Design alternatives considered

| Choice | Alternative rejected | Why |
| --- | --- | --- |
| Jackson's CSV module | Hand-written quoting, or Apache Commons CSV | The architecture forbids hand-writing a solved problem, and the module belongs to the Jackson already used |
| Reuse `JsonStore.publishAtomically` | A copy in `ExportService`, or a new class in `arbiter.workspace` | Atomic replacement keeps one home, with the smallest change |
| Check sources after the store's read | Inside it | As `comparison` does; the read holds the data, not the media |
| One `ExportSummary` for the preview and the report | Two records | They carry the same fields |

## Risks

- **Blindness.** `ExportService` returns every annotator's answers and names, but only to the adjudicator and only from `arbiter.ui.adjudicator`, which `AnnotatorBlindnessTest` excludes, so the test should pass unchanged.
- **Every source is read in full on the JavaFX thread**, up to `SourceResolver.MAX_TEXT_BYTES` each, so a large corpus pauses the page while it exports.
- **A crash mid-write** can leave a `.arbiter-*.tmp` file in `exports/`, but never a file at the export's name.
- **Spreadsheet formulas.** A label key or file name starting with `=` may open as a formula. Values are written unchanged, since changing them would lose data.
- **The page needs JavaFX, which CI lacks,** so it is covered by human checks.

## Verification

Automated coverage goes in a new `ExportServiceTest`, which seeds `ClassificationWorkflow` projects in a `TestWorkspace` and parses each written file back with Jackson. Both methods refuse a signed-out or annotator caller, and every failure leaves the bytes of `arbiter.json` and of any earlier export unchanged.

| Acceptance criterion | How it is verified |
| --- | --- |
| The format from [#30] gives a parseable CSV or JSON dataset; no COCO | Integration: a CSV project writes `exports/project-<id>.csv`, which `CsvMapper` reads back with the expected header and one row per item, and a JSON project writes `.json`, which `JsonMapper` reads back. Review: the export branches only on `OutputFormat`'s two values. Human: the CSV opens in a spreadsheet and the JSON in a JSON viewer |
| SINGLE labels and fractional SCALE results are represented without loss | Integration: SINGLE answers are label keys, including one with a comma and a quote. SCALE means 2.5 and 7/3 parse back to the same `double` in both formats, a mean of 3 is `3.0`, and ratings are whole numbers. A path with a comma and non-ASCII letters reads back unchanged, and neither file starts with a BOM |
| Each resolved item's provenance matches its decision, method, contributors, submitted values and times, and adjudicator and time | Integration: `MAJORITY`, `ADJUDICATED` and `AUTO_SCALE` items each give their stored answer, method and time, with the owner as decider only for `ADJUDICATED`. Each lists its *k* submissions with username, answer and time, including a disabled annotator's and one that disagreed. The CSV header numbers its groups up to the largest *k*, and a resolved item from a split with a smaller *k* leaves the extra groups blank. Human: after a dispute is decided, its row names the adjudicator |
| Every item is exported, and each unresolved item is marked `UNRESOLVED` and never given an invented answer | Integration: an item outside any split, one with fewer than *k* answers, an undecided dispute, and one with a majority but no resolution, as before [#27], are each `UNRESOLVED` with blank fields, no submissions and blank numbered columns. A resolution whose label is not the project's stops the export rather than writing a blank answer |
| A preview shows the location and counts before writing, then a report | Integration: `preview` returns the path under `exports/` and the counts of items with and without a resolution, and `exports/` stays empty. `export` returns the same. Human: **Export** shows the path and counts, **Cancel** writes nothing, and confirming shows the report |
| Publication is atomic | Integration: a second export replaces the first whole, after a decision has changed it. When the target cannot be replaced, because a non-empty folder has its name, export refuses and leaves nothing else in `exports/`. Review: it publishes through the same temporary file and atomic move as `arbiter.json`. Human: on Windows, exporting again while the earlier CSV is open in Excel keeps the earlier file and says to close it |
| Missing, unreadable, changed or outside-root sources are reported through [#10] before publication, and records stay unchanged | Integration: with an unresolved item's source deleted, and separately edited, export throws the resolver's `SourceException` naming the file, and the earlier export, `arbiter.json` and the sources are unchanged. `SourceResolverTest` covers the other failures, since every item goes through `resolve`. Human: renaming a source makes **Export** name the file, and restoring it lets the export succeed |
| Export is read-only and available whenever requested, with no COMPLETE state | Integration: a project with no items, and one with nothing resolved, each get a valid file. `arbiter.json`'s bytes are unchanged after `preview` and `export`. Review: no project state is added. Human: **Export** works on a new project |
| No detection, flag, historical-provenance or partition output | Review of the diff: only the fields above, with no split |
| `./gradlew check` passes | JDK 25 `./gradlew check shadowJar`, including `UiConventionTest` and `AnnotatorBlindnessTest` |
| No text is cut off (`review` skill) | Human: a long nested path shows in full in the preview, report and error dialogs |

[#10]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/10
[#27]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/27
[#30]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/30
[#37]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/37
