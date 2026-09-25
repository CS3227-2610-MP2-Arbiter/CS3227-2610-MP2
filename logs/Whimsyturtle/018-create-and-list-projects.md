# Create and list projects

Status: Human verified.

## Original request

- The human asked the agent to start issue #24 following `context/swe.md`: first sync `feat/project-list` with the updated `main`, keep the project simple, use the repo's skills as needed and ask about important decisions.

## Follow-ups, corrections, and reflection

- Clarifying #24 found three gaps: its note about plain JavaFX dialogs was stale because #8 had merged, rule 5 left the delete confirmation's content to #24, and #24 both shipped with and blocked #30. The human decided:
  - #30 ships in the same branch and pull request.
  - The confirmation names only the project: "Delete project X? This cannot be undone."
  - Opening a project page, renaming and editing the description are out of scope.
- The human approved `plans/project-list.md` as proposed, including an inline create form rather than a pop-up and one-line descriptions for the CSV and JSON formats.
- The human asked how to review the changes by staging files in groups. The agent proposed four groups and staged each in turn, and the human reviewed and committed each group.
- The human then asked for a thorough review of edge cases, tests, duplication, naming and abstractions. It found that a project without taxonomy settings would lock the adjudicator out at login, plus several low-severity issues, and the human chose which to fix.
- The human argued the CSV and JSON descriptions should leave the create form under the single-home rule. The agent moved them to the Glossary rather than the User Guide, because AGENTS.md makes it the home of enum meanings.
- The human reviewed and committed the fixes in four staged groups, as before.
- The human asked for project names to reject Unicode like usernames, with both rules in one helper. After the agent advised against also moving the password rule there, the human dropped the helper and asked for a general preference for ASCII in the architecture context instead.

## Agent responses and outcomes

- The branch had no commits of its own, so the agent fast-forwarded it to `main`, bringing in the merged UI kit (#71) and keeping the human's unrelated local edits.
- The agent updated #24 and #30 with the decisions, citing rules 3 and 5 instead of restating the project lifecycle.
- The agent added a project service to create, list with counts and delete projects until their first assignment, and a Projects screen replacing the placeholder. It also added a small page layout style to zheng-jj's shared UI kit, because layout must live in the stylesheet.
- Review found no blocking bugs or overengineering. The agent removed Javadoc that restated #24's rules and fixed a stale plan status line. An emoji counting as two characters toward the name limit was reported to the human and left unfixed.
- The agent updated the User Guide with a Projects section and the architecture context with the project service's responsibilities.
- After the follow-up review, the store rejects a project without exactly one taxonomy settings row, deletion uses a shared first-assignment check, and the Projects screen no longer leaves a stale inline error or reports a failed delete twice. `Arbiter` holds its services as fields, and the project deletion Javadoc no longer implies it checks rule 5.
- At the human's request, the agent opened [#72](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/pull/72) with a concise description, closing #24 and #30.
- Project names now allow only printable ASCII, since the username characters would reject spaces, which ends the emoji length issue. Each service keeps its own name check. The architecture context gained the ASCII rule, the Developer Guide its reasons, and #24, #72 and the plan were updated.

## Verification

- `ProjectServiceTest` adds 20 tests derived from the issues; all pass and found no production defects.
- `.\gradlew.bat check shadowJar` passed on JDK 25, including the UI-convention and annotator-blindness tests.
- The human ran the app and confirmed all six acceptance scenarios passed, from the empty state and inline errors to the delete confirmation, creation order after restart and annotators never seeing Projects.
- The disabled Delete on an assigned project is covered only by tests until #28 and #32 let the app create assignments.
- After the review fixes, `.\gradlew.bat check shadowJar` passed again, with two new tests for the taxonomy settings rule. The screen's error fixes still need a manual check.
- CI passed on the pushed branch; the pull request's own run was pending when it opened.
- After the name change, `.\gradlew.bat check shadowJar` passed with two new name tests.
