# Write the annotator user guide

**Issue:** [#22] - Write the annotator user guide
**Branch:** `docs/annotator-guide` (from `main` after [#18] merged)
**Status:** Written at the owner's request to "work through what's unblocked", and delivery approved on 26 September 2026. Human acceptance and teammate review are pending.

## Goal and scope

Make the annotator's part of `docs/UserGuide.md` complete for the released workflow ([#12], [#13], [#14], [#17], [#18]): credentials, My splits, the blind queue, SINGLE and SCALE answers, Submit & next, restart and progress, with screenshots of the real screens.

Non-goals, from [#22]: developer documentation, the landing page ([#45]), and anything [#62] deferred.

## What already existed

Each annotator issue added to the "My splits" section as it landed, so most of the content was already there. The gaps against [#22] were:
- credentials in Getting started
- screenshots
- a direct statement of blindness
- a quick reference that still pointed only at issues

## Changes

- **Getting started** says the adjudicator issues annotator credentials and sets a forgotten annotator password directly, with no self-signup or email, and that the adjudicator's own password cannot be recovered (rule 12). It promises no owner recovery.
- **My splits** is reorganized around what an annotator does: start or continue, choose an answer, Submit & next, closing and coming back, unreadable files, and projects not yet set up. It links "file" to the Glossary's *item* and *assignment*, and links rules 1, 13, 18 and 21 rather than restating them.
- **What you never see** explains blindness: no other annotator's work, no resolved answers, and no file names or folders.
- **Three screenshots** in `docs/images/` (My splits, a SINGLE file with a label chosen, a SCALE file with a rating entered).
  - They were rendered from the real `AppShell`, `MySplitsScreen` and `QueueScreen` over a seeded workspace, so they match the implemented screens exactly.
  - They were made with JavaFX snapshots by a throwaway program kept outside the repository.
- **The quick reference** points the annotator rows at the My splits section, with their issues in brackets.

## Verification

| Acceptance criterion | How it is verified |
| --- | --- |
| Annotator sections complete and linked from the docs home | The home page links the User Guide, whose Getting started links My splits. Review |
| Getting started explains credentials and replacement passwords, without promising self-signup, email or owner recovery | Review of step 3 |
| Covers the queue, SINGLE and SCALE controls, immutable Submit & next, resume and progress | Review of My splits |
| Explains blindness and that an unsent choice may be lost on close | "What you never see" and "Closing and coming back" |
| No detection, images, flags, rationale, shortcuts, timers, analytics, agreement, review or Back navigation, or earnings | A search of the guide finds none. The only "Back" is the Back to My splits button |
| Screenshots match the implemented workflow | Rendered from the real screens. Human check against the running app |
| Terminology matches the glossary and rules without duplicating feature detail | Links to the Glossary and rules. Review |
| The Pages workflow passes | CI on the pull request |

[#12]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/12
[#13]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/13
[#14]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/14
[#17]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/17
[#18]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/18
[#22]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/22
[#45]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/45
[#62]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/62
