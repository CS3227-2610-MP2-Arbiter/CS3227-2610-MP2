# Restore lost flow-level rules

Status: Awaiting human verification.

## Original request

- The user reported that the flow-level rules in `docs/UserFlows.md` do not count properly and skip
  numbers, and asked for an issue, a pull request, a fix, and Whimsyturtle assigned to review.
- Created issue #49 to track it.

## Follow-ups, corrections, and reflection

- The report was accurate and the damage was worse than skipped numbering. Numbers 4, 6 and 10 were
  **missing entirely**, not just absent from the sequence: each rule had lost its opening line to an
  earlier multi-line edit, leaving three orphaned fragments (`calculation.` under rule 5, and a
  `majority the item is a dispute ...` line under rule 9).
- The fix was therefore **restoration, not renumbering**. The surviving numbers 5, 7, 9 and 11 were
  already correct for their positions, so renumbering the list would have moved twenty-odd existing
  "rule N" references for no reason. The lost text was recovered from `git show 12b4ba8` rather than
  rewritten, so the rules say exactly what they said before.
- Rule 4 (`Immutable project shape`) had no surviving fragment at all; it was recovered purely from
  history. Its wording is from the original commit, and `docs/UserFlows.md` 2.1 still describes the
  same behaviour independently, so the two agree.
- Rule 6 was restored verbatim from history, so no wording was invented. A separate stale reference
  to the closed `S2` spike remains in section 2.8 ("per `S2`, out of earnings"); that is outside this
  issue and was left alone.
- The missing blank line before the `---` after rule 15 was also fixed; it was part of the same
  edit damage.

## Agent responses and outcomes

- Restored rules 4, 6 and 10, folding their orphaned fragments back into place.
- Confirmed the list now runs 1-15 with no gaps or duplicates.
- Checked every "rule N" reference in the repository and confirmed none needed updating, because no
  number changed meaning.
- Branch `docs/fix-flow-rule-numbering`, pull request opened and assigned to Whimsyturtle for review.

## Verification

- Rule numbering extracted programmatically and confirmed as `1,2,3,4,5,6,7,8,9,10,11,12,13,14,15`.
- All fifteen rules read as complete statements; no orphaned fragments remain.
- All `rule N` references across `docs/` and `context/` were listed and checked against the restored
  numbering.
- `./gradlew check shadowJar` was not run: documentation-only change with no application code.
- Unverified: whether any rule wording was changed in meaning by the earlier damaged edit, beyond
  the three restored rules. The restoration was taken from history, so this is believed to be exact.
- A human has not yet verified this summary.
