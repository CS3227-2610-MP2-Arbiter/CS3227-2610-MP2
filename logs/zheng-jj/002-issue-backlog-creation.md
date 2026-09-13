# Turning the brainstorm into the 45-issue backlog

Status: Awaiting human verification.

## Original request

- The user dumped a rough brainstorm of Arbiter as unstructured notes: a Java desktop labelling app
  with two roles, annotators working a blind queue and adjudicators importing, splitting, assigning,
  resolving and exporting. The notes ended with a loose bullet list per role (login, logout, annotate,
  classification, detection, view progress, flag low-quality material, export income statement,
  upload dataset, split, assign, choose output format, define labels, resolve conflicts, manage
  accounts) plus a bare `ORMs for storage` line.
- The user asked the agent to write and clean up the requirements: turn the brainstorm into clear
  requirements, split the result into GitHub issues, and add them to the repository.
- The bulleted items marked with an asterisk in the brainstorm (`*email verification`,
  `*reset password`) were the user's own signal that those two were optional.

## Follow-ups, corrections, and reflection

- The original conversation for this task is no longer available. This log was written afterwards and
  reconstructed from the GitHub issue metadata, the issue bodies, and `docs/UserFlows.md`. The
  prompts, corrections and agent decisions below are inferred from those artefacts rather than
  quoted, and the user has not yet confirmed them.
- The brainstorm listed capabilities without ordering, ownership or acceptance criteria, so it could
  not be turned into issues directly. The agent wrote `docs/UserFlows.md` first as the intermediate
  step, then derived the issues from it, so every issue could link back to the flow step it
  implements and the two role tracks could not drift apart.
- Ambiguities in the brainstorm were not silently resolved. Three of them became explicit spike
  issues ending in a written decision rather than code: how a team shares data while staying offline
  (offline premise versus team use, `#1`), when annotator earnings vest (the reward features depend
  on it, `#2`), and whether the starred email features ship at all (they need a network, `#3`). The
  two email items were kept as `#39` and `#40` with a `stretch` label and blocked on `#3`, instead of
  being either dropped or built.
- `#1` recorded both candidate models with their trade-offs (shared SQLite file risks corruption from
  concurrent writers; package exchange is safe but needs a merge path) rather than picking one
  prematurely.
- The trailing `ORMs for storage` note was interpreted in `#6` as a constraint rather than a
  requirement: Hibernate was judged overkill for an offline desktop app, with jOOQ or plain
  row-mapping preferred and all SQL kept inside the repository classes.
- Money was treated as derived from completed annotations and rewards rather than stored, so no
  balance column was added, and soft delete was specified so historical annotations stay
  interpretable after users, labels or items are retired.
- Ordering was recorded as dependencies, not just labels: the foundation spikes and shared contract
  work gate the two role tracks, and the annotator and adjudicator tracks then proceed in parallel
  under separate owners.

## Agent responses and outcomes

- Created all 45 issues on `CS3227-2610-MP2-Arbiter/CS3227-2610-MP2`, authored by `zheng-jj`, all
  under the **v1.0.0** milestone (`milestone/7`), all still open.
- The creation timestamps run `2026-09-12T12:43:33Z` to `12:44:31Z` at roughly one issue per second,
  which indicates the issues were filed by a scripted sequence of `gh issue create` calls rather than
  one by one.
- Each issue follows the same template: `## Summary`, `## Context` (which frequently quotes the
  brainstorm and records the interpretation chosen), `## Scope` with explicit in/out lists, `## Tasks`
  as checkboxes, `## Constraints`, `## Acceptance criteria` tied to the flow rules and
  `./gradlew check`, and `## Dependencies` naming blocking and blocked issues.
- Issues carry a consistent four-axis label taxonomy: `area/*` (foundation 13, adjudicator 16,
  annotator 11, docs 6), `type/*` (feature 32, chore 10, spike 3), `priority/*` (p0 22, p1 16, p2 7),
  `size/*` (S 18, M 17, L 10), plus `stretch` on the two email issues.
- The backlog is numbered to mirror `docs/UserFlows.md`: `S*` spikes (`#1`-`#3`), `A*` foundation
  (`#4`-`#11`), `B*` annotator track (`#12`-`#22`), `C*` adjudicator track (`#23`-`#38`), `D*` docs
  and delivery (`#39`-`#45`).
- Ownership follows the role split: `zheng-jj` 23 issues (annotator track, media, test harness, docs,
  packaging, spike `#3`), `Whimsyturtle` 18 (adjudicator track, schema, workspace), and 4 shared
  (`#1`, `#2`, `#4`, `#7`) where a single answer has to be agreed before parallel work is safe.
- No log was written at the time, which is why this entry is filed a day after the issues themselves.
  `logs/zheng-jj/001-user-flow-and-issue-backlog.md` covers the later pull and documentation commit,
  and lists the existence of these 45 issues as unverified because GitHub was unreachable then.

## Verification

- Confirmed against the live GitHub API: 45 issues, all open, all on milestone `v1.0.0`
  (`milestone/7`), authored by `zheng-jj` with the timestamps above; the label counts, ownership
  split and issue ordering match `docs/UserFlows.md` section 5.
- This also resolves the two items that `001` flagged as unverified: the claim that all 45 issues sit
  under the v1.0.0 milestone is correct, and the hardcoded `.../milestone/7` link in `docs/index.md`
  points at the right milestone.
- Still unverified: `docs/UserFlows.md` ends by pointing at `issues/README.md` for build order,
  labels and the definition of done, and that file still does not exist.
- Unverified: whether the issue bodies and acceptance criteria match what the user actually agreed
  in the original conversation, since that history is unavailable.
- No Gradle checks apply; this task produced GitHub issues and documentation only.
- A human has not yet verified this summary.