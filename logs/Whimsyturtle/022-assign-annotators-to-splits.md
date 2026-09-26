# Assign annotators to splits

Status: Human verified.

## Original request

- The human asked the agent to start issue #32 following `context/swe.md`, keep it simple and not overengineer, use the repo's skills as needed and ask about important decisions.

## Follow-ups, corrections, and reflection

- Clarifying #32 found that the `AssignmentService` the architecture context names did not exist, that the taxonomy (#26) was not built, and that the annotator home (#12) and submission (#17) were still open. The human chose the recommended option each time:
  - *k* is a whole number from 1 to the number of active annotators, prefilled with 2.
  - *k* is set on the assign form with the split's first assignment and is read-only afterwards.
  - An **Assign** button and an Annotators column on the splits table open an on-page form with *k*, the annotators and their load, and the current assignees, then a confirmation.
  - Load is an annotator's unfinished assignments across all projects and the files in them.
  - #32 does not check the taxonomy; #26 owns that.
- The human accepted the agent's edge-case assumptions: one commit may assign several annotators, all or nothing; only active annotators not already on the split are offered; disabled assignees keep their places; and the confirmation says assignments are permanent and, on a project's first, that the project freezes.
- The human approved `plans/assign-annotators.md`, choosing a read-only check before the confirmation, as Generate splits previews, and a prefill of 1 when only one annotator is active. They accepted that **Assign** is disabled on a full split and the column reads "None" before the first assignment.
- In acceptance testing the human found the assign confirmation cut off with an ellipsis. They pointed out that owner login (log 014) had the same problem, fixed only on that screen, and asked the agent to update the relevant skill so it does not recur.
- The human then passed acceptance testing, confirmed the wrapping was fixed and asked for this log. The User Guide update and pull request are still to do.
- The human then asked for a thorough review with the `review` skill, focusing on edge cases, missing tests, duplication, misleading names and abstraction. Of the agent's three medium and eight low findings, the human chose to fix nine and leave the `unfinishedFiles` name, the User Guide and the nits. One finding, that this log's status was wrong, was mistaken: the log skill keeps it awaiting verification.
- The human committed the code in four groups the agent proposed, then asked for the User Guide update, and later for a concise pull request without the agent committing.

## Agent responses and outcomes

- The agent updated #32's body with the decisions, citing rules 3 and 14 rather than restating them. The #12 and #17 criteria now say what #32 itself does: an assignment is listed only for its annotator and starts NOT_STARTED. Taxonomy validity is left to #26, and after planning the agent added the prefill of 1.
- The agent created `feat/assign-annotators` from `main`.
- A new assignment service lists a split's options, checks a request and assigns in one write, saving *k* with the first assignment. The project page gained the column, **Assign**, the form and the confirmation, and now works out the freeze from the splits it loads.
- One deviation from the plan: once *k* is saved, checking and assigning ignore the typed *k*, since the form shows it read-only.
- Review found no bugs; the agent fixed one Javadoc that repeated the count rule. Four low-severity points were left, including a duplicated `assigned` field on `SplitSummary` and three wordings of the free-places error.
- The shared `Dialogs` (zheng-jj's UI kit) now lets every dialog grow to fit its message, and the assign form's annotator lines wrap. The architecture context's UI rules now say no text is cut off, and the `review` skill checks this on screen changes and adds a longest-realistic-text human check.
- After the second review, every form scrolls rather than squeezing its text, which had cut off check boxes and hidden **Assign** with about eight annotators. This changed zheng-jj's `Components`, `Styles` and stylesheet, and `Components` gained a wrapping text label.
- `JsonIntegrity` now rejects an assigned split without *k*, more assignments than *k* or the same annotator twice. The test fixture that seeded an assigned split without *k* now saves it.
- The assign form moved into its own `AssignForm` class, the project page shares one error handler for its changes, and `SplitSummary` derives `assigned()` from its count. The form-loading error heading and the hint and prefill with no active annotator were fixed, and the plan and architecture context were updated.
- The User Guide now says how to assign, and that a project assigned before #26 can never get labels.

## Verification

- `AssignmentServiceTest` has 19 tests covering the acceptance criteria and asserting every rejection message; they found no production defects. New integrity tests check each limit `JsonIntegrity` now enforces.
- `.\gradlew.bat check shadowJar --rerun-tasks` passed on JDK 25 with 329 tests, no failures and six skips from existing tests Windows cannot run. It passed again after the `Dialogs` and wrapping fixes, and with 333 tests after the second review's fixes.
- An off-screen layout check confirmed the form overflow, then that forms scroll, keep their text whole and look unchanged.
- The human passed acceptance testing by hand, then repeated the tests the second review affected, including the assign form with many annotators and every other form, and passed them.
- CI passed on the pushed branch. The taxonomy freeze is untested until #26 exists, and "listed only for its original annotator" is checked only by tests until #12.
