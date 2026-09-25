# Create and list projects

Status: Awaiting human verification.

## Original request

- The human asked the agent to start issue #24 following `context/swe.md`: first sync `feat/project-list` with the updated `main`, keep the project simple, use the repo's skills as needed and ask about important decisions.

## Follow-ups, corrections, and reflection

- Clarifying #24 found three gaps: its note about plain JavaFX dialogs was stale because #8 had merged, rule 5 left the delete confirmation's content to #24, and #24 both shipped with and blocked #30. The human decided:
  - #30 ships in the same branch and pull request.
  - The confirmation names only the project: "Delete project X? This cannot be undone."
  - Opening a project page, renaming and editing the description are out of scope.
- The human approved `plans/project-list.md` as proposed, including an inline create form rather than a pop-up and one-line descriptions for the CSV and JSON formats.
- The human asked how to review the changes by staging files in groups. The agent proposed four groups and staged each in turn, and the human reviewed and committed each group.

## Agent responses and outcomes

- The branch had no commits of its own, so the agent fast-forwarded it to `main`, bringing in the merged UI kit (#71) and keeping the human's unrelated local edits.
- The agent updated #24 and #30 with the decisions, citing rules 3 and 5 instead of restating the project lifecycle.
- The agent added a project service to create, list with counts and delete projects until their first assignment, and a Projects screen replacing the placeholder. It also added a small page layout style to zheng-jj's shared UI kit, because layout must live in the stylesheet.
- Review found no blocking bugs or overengineering. The agent removed Javadoc that restated #24's rules and fixed a stale plan status line. Two low-severity limits were reported to the human and left unfixed: an emoji counts as two characters toward the name limit, and a project stored without taxonomy settings, which the app cannot produce, would break the list.
- The agent updated the User Guide with a Projects section and the architecture context with the project service's responsibilities.
- The pull request awaits the human's delivery approval.

## Verification

- `ProjectServiceTest` adds 20 tests derived from the issues; all pass and found no production defects.
- `.\gradlew.bat check shadowJar` passed on JDK 25, including the UI-convention and annotator-blindness tests.
- The human ran the app and confirmed all six acceptance scenarios passed, from the empty state and inline errors to the delete confirmation, creation order after restart and annotators never seeing Projects.
- The disabled Delete on an assigned project is covered only by tests until #28 and #32 let the app create assignments.
