# Focused product-site landing page

**Issue:** [#45] - Focused product-site landing page
**Branch:** `docs/landing-page` (from `main` after [#42] merged)
**Status:** Drafted with the page at the owner's request ("start working on the product landing page") on 28 September 2026. The owner approved delivery and review the same day. Awaiting the Pages workflow, a check of the rendered site and teammate review.

## Goal and scope

Make the docs home page, `docs/index.md`, the landing page a marker sees first: the problem Arbiter solves, the two roles, the focused v1 workflow and links to the rest of the documentation.

Non-goals, from [#45]: the detailed guides themselves.

## What already existed

`docs/index.md` is already the site's home and the home of what Arbiter is and what each role does. It had every link [#45] asks for. The gaps against [#45] were:
- no statement of the problem Arbiter solves
- two sentences per role rather than one
- a workflow that did not name `SINGLE` and `SCALE`, immutable submission, or CSV and JSON

## Changes

- **The problem:** one paragraph on why blind, independent answers and adjudication are needed.
- **The two roles:** one sentence each. Blindness links rule 1 rather than restating it.
- **How it works:** five numbered steps (set up, assign, submit, resolve, export). Each is one sentence and links the rule that holds its detail (rules 10, 15 and 18).
- **Documentation:** adds the README's "Running a release" first, since a marker needs it before the guides.

## Design alternatives considered

| Choice | Alternative rejected | Why |
| --- | --- | --- |
| Revise the existing home page | A new landing page beside it | The home page already holds what Arbiter is and what each role does; a second page would duplicate it |
| Link rules for workflow detail | Describe resolution and export fully | Section 3 of User Flows is the home of those rules |

## Open decisions

None. On 28 September 2026 the owner decided that [#38] and [#44] do not gate this page, since its links go to files that always exist, and [#45]'s dependencies were updated to match.

## Verification

| Acceptance criterion | How it is verified |
| --- | --- |
| Explains the problem, with one concise sentence per role | Review of the opening paragraph and "The two roles" |
| Shows plain-text `SINGLE`/`SCALE` classification through assignment, immutable submission, resolution and CSV/JSON export | Review of "How it works" |
| Advertises no detection, images, flags, advanced analytics, persisted drafts, completion sealing or provenance browser | A search of the page for those terms finds none |
| Links the user guide, developer guide, reflections, user flows, glossary, repository and issue backlog | Review of "Documentation"; the backlog link is the v1.0.0 milestone |
| The Pages workflow passes and the site renders | CI on the pull request, and a human check of the rendered page |

[#38]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/38
[#42]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/42
[#44]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/44
[#45]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/45
