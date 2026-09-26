# Configure single-label or scale taxonomy

Status: Human verified.

## Original request

- The human asked the agent to start issue #26 following `context/swe.md`, keep it simple and not overengineer, use the repo's skills as needed and ask about important decisions.

## Follow-ups, corrections, and reflection

- Clarifying #26 found #6 and #24 closed and the label and range model already stored, with no service or UI. It also found two contradictions: the Glossary and `Label` Javadoc say labels form the taxonomy, ignoring SCALE, and `Label.key` calls keys stable although they can be edited before the freeze.
- The human raised no objection to the agent's edge-case assumptions and decided:
  - The taxonomy is edited in an on-page view, as Assign is.
  - The first assignment is refused until a SINGLE taxonomy has at least two labels or a SCALE range has min below max.
  - Label keys follow the project-name rule and are unique ignoring case.
- The human asked why the scale bounds could not be smaller, such as −5 to 5. The agent explained that this rules out 1–7 and 0–10 scales, and the human chose −10 to 10.
- The human approved `plans/taxonomy.md` and the proposed wording fixes but asked the agent not to implement until told to go.
- The human decided that the Key and Description columns wrap rather than cut text off, which no table in the app had done before.
- Of review's two UI findings, the human decided that wrapped labels keep their full height, so a table shrinks instead in small windows, and that the labels table does not sort. The height fix also reaches `ProjectPage`, so zheng-jj should review it as a shared change.
- The human said zheng-jj had merged to `main` and asked the agent to bring it in and check whether anything needed changing.
- The human asked for a second, thorough review, then chose eight of its findings to fix.
- The human asked the agent to stage the changes in groups for the human to commit, then to move the fixtures' second labels into their own commit, apart from the readiness check.

## Agent responses and outcomes

- The agent created `feat/taxonomy` and the plan, putting the work in `CorpusService` as the architecture context already says. It updated #26's criteria on the scale bounds and the first-assignment rule.
- The agent implemented the plan and gave existing fixtures with one label or a 3-to-3 range a valid taxonomy. One deviation: save errors show inline in the view.
- The shared `Components` gained a wrapping table column.
- Review found no defects. The agent fixed where a new label goes, now after the highest stored position, and a wrong range Javadoc.
- Merging `main` after PR #79 brought in #78 and #79 cleanly, but 10 of zheng-jj's new `AnnotationServiceTest` tests seeded one-label projects with assignees, which #26 now refuses. The agent gave those fixtures a second label without changing any assertion.
- The second review found no data or freeze defects, but the edit form cut off a long description, and some tests and code were missing or duplicated. The agent:
  - Made the description a wrapping text box.
  - Split moving a label into separate up and down methods.
  - Shared the name-rule message, the form error handling and the tests' project and split setup.
  - Corrected the `TaxonomySummary` Javadoc.
  - Added tests for renumbering after a move and for the fixture's new refusals.
- Left from that review: typed form text is lost on Up, Down or Delete, a failed delete drops the edit, and four minor wording and layout points.
- The agent staged the code in five groups: the integrity checks, the service, the fixtures' second labels, the readiness check and the view. `AssignmentServiceTest` and `ClassificationWorkflowTest` held both fixture changes and new tests, so the agent staged each part with its own group.
- After acceptance, the human asked the agent to update the docs and open the pull request without committing. The User Guide now explains taxonomy setup, and the Glossary follows decision 2 of the plan. The agent opened PR #83.

## Verification

- 30 new tests cover the taxonomy writes, the readiness check and the integrity checks.
- `.\gradlew.bat check shadowJar --rerun-tasks` passed with 363 tests, no failures and six skips from existing tests Windows cannot run. After merging `main` it passed with 382, and after the second review's fixes with 386.
- The human reported that every acceptance check passed: the plan's Verification table, the longest-text check, the annotator's screens at the minimum window size, the description box's wrapping, line breaks, Enter and Tab, Save and Cancel at 760×520, and the Generate splits and Assign errors.
