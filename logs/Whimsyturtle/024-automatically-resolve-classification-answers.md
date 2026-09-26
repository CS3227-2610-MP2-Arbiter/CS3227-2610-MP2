# Automatically resolve classification answers

Status: Human verified.

## Original request

- The human asked the agent to start issue #27 following `context/swe.md`, keep it simple and not overengineer, use the repo's skills as needed and ask about important decisions.

## Follow-ups, corrections, and reflection

- Clarifying #27 found `Resolution` and its storage already in place, but nothing that created a resolution. It also found three contradictions:
  - #27 and the `Resolution` Javadoc said "valid" answers, while rule 10 and the Glossary said "submitted".
  - #27 asked for an "exact" mean, which a `Double` cannot store.
  - The Developer Guide said resolved labels are never loaded on the annotator path, although resolution runs inside `submit`.
- The human raised no objection to the agent's edge-case assumptions and decided:
  - A new `ResolutionService` holds the logic, and `AnnotationService.submit` calls it.
  - Resolution does not check again for duplicate or out-of-range answers, since #17 already refuses them.
  - Items that reached *k* answers before this feature are not resolved later.
  - #27 follows rule 10's wording: "submitted" answers and an unrounded mean.
- The human approved `plans/auto-resolution.md` and told the agent to go ahead.
- After a second review, the human decided a resolution's time follows the plan: the *k*th answer's time, not the latest. They asked the agent to fix four other gaps it found and leave the nits.

## Agent responses and outcomes

- The agent rewrote #27's body. It now cites rule 10 instead of restating it, uses "submitted" and "unrounded", and says annotators see no change.
- The agent created `feat/auto-resolution` and the plan.
- The agent added `ResolutionService` and called it in the same action in which `submit` stores the answer. It also updated the "valid" and "belongs" wording in the `Resolution` and `submit` Javadoc, the architecture context and the blindness test's note.
- `ResolutionServiceTest` covers majorities, ties, scale means, fewer than *k* answers, restart and never overwriting a resolution.
- Review found no defects. It flagged two points:
  - A resolution's time is the latest answer's time, not strictly the *k*th answer's. They differ only when clocks disagree, and the agent recommended keeping it.
  - The Developer Guide's blindness line still needs rewording.
- The agent removed a redundant call from one test.
- The second review found a planned re-run test missing, no restart check for majority resolutions, and outdated Javadoc in `ClassificationWorkflow` and `AnnotationService`. The agent fixed these, made `submit` pass its stored answer so a resolution takes that answer's time, and updated the plan.
- The agent reworded the Developer Guide's blindness line and added a sentence on automatic resolution to the User Guide's Projects section.
- The agent opened draft PR #85.
- Pending: teammate review, and whether "valid" in #34 should also become "submitted". zheng-jj's comment on #27 still has stale references.

## Verification

- `.\gradlew.bat check shadowJar --rerun-tasks` passed with 428 tests, no failures and six skips from existing tests Windows cannot run, including all 12 in `ResolutionServiceTest`. The two new resolution-time tests failed against the old latest-time behaviour.
- The human reported that the plan's acceptance checks passed: a SINGLE majority and dispute, a SCALE mean, and both Unresolved counts after a restart.
