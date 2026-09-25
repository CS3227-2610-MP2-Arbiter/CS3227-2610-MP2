# Generate count-based work batches

Status: Human verified.

## Original request

- The human asked what to work on once #25 merged. The agent recommended #28, since #28 and #31 lead to #32, which blocks zheng-jj's annotator track.
- The human asked the agent to start #28 following `context/swe.md` on a new branch stacked on #25's branch, keep it simple, use the repo's skills as needed and ask about important decisions.

## Follow-ups, corrections, and reflection

- Clarifying #28 found that one criterion forbade "replacement-batch behavior" although its contract allowed regenerating a deleted split's items, and that it left split naming, the seed, the preview's detail and splits emptied by unregister open. The human chose the recommended option each time:
  - The preview is a confirmation showing only the split sizes, refused if the available items changed since.
  - The app picks and records a random seed; there is no seed field.
  - Splits are named "Split n" automatically, with no rename.
  - The project page gains a splits table and a column giving each item's split and position.
- The human approved `plans/split-generation.md`, including removing the unused `Split.assigned` flag so whether a split is assigned lives only in its assignments.
- Of the review's low-severity findings, the human chose to:
  - summarise the sizes in the confirmation (e.g. "2 splits of 50 files and 1 of 20.") rather than list each, which could push the dialog off-screen. Asked why the text did not simply wrap, the agent explained that it already wrapped, which is what made a long list overflow.
  - say "files" rather than "items" throughout the split UI and its messages, matching the existing page and User Guide.
- The human asked why a count above the available files was not an error. The plan listed one split for such a count as an accepted assumption, but the agent had never put it to the human directly. The human chose to reject it ("Only N files are available to split"); a count equal to them still makes one split.
- The human asked why the malformed-count message still said "from 1 to 2147483647", and the bound was dropped.
- The human asked for this log before acceptance testing.
- The human then asked whether a split left empty by unregistering all its files was a bug. #28 allowed it, again from an assumption the agent had not raised, and nothing would have stopped #32 assigning it. The human chose to delete a split along with its last file.
- The human asked for a second, thorough review against the plan and chose to fix most of its findings. Left open: the preview shuffling only to get sizes, repeated scans on large projects, a known-answer shuffle test, a confirm-twice test and small UI points (Enter key, text sorting of split names).
- The human said `BigInteger` should never be needed and asked the agent to check for other uses; there were none.
- The human found `ProjectPage.show()` too long and asked the agent to split it.
- The human asked what was left on the branch, then passed acceptance testing and asked the agent to update the User Guide, tick #28's criteria, update this log and open the pull request, leaving commits to them.

## Agent responses and outcomes

- The agent created `feat/split-generation` on `feat/corpus-registration` and updated #28's body with the decisions, rewording the contradictory criterion to "post-assignment refill".
- The corpus service can list, preview, generate and delete splits, refusing to delete one with an assignment. The project page gained a count field, **Generate splits** with a size confirmation, a splits table with **Delete**, and a Split column.
- `Split.assigned` and the fixture lines setting it were removed, and the architecture context says a split's lock is checked against its assignments.
- Review found no correctness, rule, authorisation or blindness bugs. An unreachable endless loop for a count of 0 was left as is.
- #74 had merged meanwhile, so with the human's agreement the agent fast-forwarded the branch onto `main`.
- Counts above the available files are rejected, and #28's first criterion and the plan say so.
- Malformed counts now say "Files per split must be a positive whole number", and a well-formed count too large for an int gets the "Only N files are available" error.
- Unregistering a split's last file now deletes the split too, and #28 and the plan say so. #25's empty-split test was merged into #28's unregister test.
- After the second review, `FirstAssignment.reachedSplit` is the one split-lock check, and the architecture context says services use `FirstAssignment` for a split as for a project.
- The count is parsed with `Integer.parseInt` instead of `BigInteger`, so a million-digit paste no longer freezes the page for about 12 s; too-large counts keep the "Only N files" error.
- The count is now `itemsPerSplit` in code, `availableItemIds` is `requireAvailableItemIds`, and the unused `SplitItemRepository.deleteBySplit`, which skipped the assignment check, is gone. Two rule citations and two Javadocs were corrected, and the plan was updated.
- `ProjectPage.show()` now builds its tables with `itemTable` and `splitTable`, and `places` gives each file's split and position.
- The User Guide now says how to generate and delete splits and lists #28 in its quick reference. The Developer Guide needed no change.
- The agent ticked #28's criteria and opened PR #75.

## Verification

- `CorpusServiceTest` gains 14 tests and extends five; they found no production defects. After the second review, the split rejection tests also check each reason, including the singular "Only 1 file", and the assignment helper moved to `TestWorkspace.assignSplit` with its own test.
- `.\gradlew.bat check shadowJar --rerun-tasks` passed on JDK 25 after each change, and `git diff --check` was clean. The final run had 295 tests with no failures; its six skips are existing tests that need a case-sensitive file system or symlinks, which Windows lacks.
- The first run after the second review's fixes failed Checkstyle on a missing space after `=`; the rerun passed.
- The first run after rejecting oversized counts failed on an existing test that regenerated a deleted one-file split at count 2; at count 1 everything passed.
- Deleting an assigned split, and deactivation or restart after assignment, are covered only by tests on seeded data until #31 and #32.
- CI passed on Linux, macOS and Windows for the pushed branch.
- The human passed every acceptance scenario by hand.
