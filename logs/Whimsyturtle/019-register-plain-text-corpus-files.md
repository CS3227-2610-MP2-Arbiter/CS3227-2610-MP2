# Register plain-text corpus files

Status: Human verified.

## Original request

- The human asked the agent to start issue #25 following `context/swe.md`, keep it simple, use the repo's skills as needed and ask about important decisions.

## Follow-ups, corrections, and reflection

- Clarifying #25 found that #24 had excluded opening a project, that the issue both rejected duplicates and said re-registering "creates no duplicate", and that rule 14 contradicted unregister removing split membership. The human decided:
  - An **Open** action on each project row leads to a project page listing its items, with **Add files** and **Unregister**.
  - Any duplicate, whether already registered or repeated in the selection, fails the whole set.
  - Unregister removes one item at a time, after a confirmation naming its file.
  - Rule 14 gains an exception for unregistering an item under rule 5.
- The human approved `plans/corpus-registration.md` as proposed.
- Of the review's findings, the human chose only to reword the message for a file selected twice. They kept all the tests and the stored-path case risk the plan already accepted.
- The human asked for this log before acceptance testing.
- The human then asked for a thorough review of edge cases, tests, duplication, naming and abstractions. It found no data-integrity bugs and ten lesser findings, of which the human chose seven to fix, including rejecting a chosen name in the wrong case, which reverses the earlier acceptance of that risk.
- The human asked what documentation was still missing, then asked for it to be updated.
- The human asked whether `test.txt` and `TEST.txt` were handled, then asked for tests of it.

## Agent responses and outcomes

- The agent updated #25's body with the decisions and created `feat/corpus-registration` from `main`.
- The agent added a corpus service to list, register and unregister a project's items, and a project page opened from the Projects screen. It also updated rule 14, the item delete Javadoc and the architecture context.
- Review found no blocking issues. It suggested trimming about ten redundant tests, which the human declined.
- Selecting the same file twice now reports that it is selected more than once.
- After the second review, the source resolver makes a chosen file's stored path and rejects files outside `media/` or named in the wrong case. One shared check guards the first-assignment freeze, table helpers are shared UI components, and copied test helpers became shared fixtures.
- The project page's stale frozen state, false rejections through linked or mapped paths and a redundant scan were left unfixed.
- The User Guide now says how to open a project and register or unregister files. `plans/source-resolution.md` records the case check and drops the two open questions this task answered. The Developer Guide needed no change.
- Two tests cover names differing only in case: with different content both register, and with the same content the second is rejected.

## Verification

- `CorpusServiceTest` adds 32 tests and five stored-path tests; all pass and found no production defects.
- `.\gradlew.bat check shadowJar` passed on JDK 25 after the message fix. The only skips are four existing `SourceResolverTest` cases that Windows cannot run without symlink privileges.
- The second review's first check reused cached results, so later checks used `--rerun-tasks`. After its fixes, 279 tests passed with the same four skips.
- CI passed on Linux, macOS and Windows for PR #74. The new case tests passed on Linux and were skipped elsewhere, and the resolver's wrong-case refusal passed on macOS.
- The human passed every acceptance scenario by hand. The disabled controls on an assigned project are covered only by tests until #28 and #32.
