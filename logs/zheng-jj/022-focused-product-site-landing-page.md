# Focused product-site landing page

Status: Awaiting human verification.

## Original request

- After merging PR #92 ([#42]), the owner said "start working on the product landing page" ([#45]).

## Follow-ups, corrections, and reflection

- [#45] lists [#38] and [#44] as blockers, and both are still open. The agent first proposed holding the pull request until they closed. The owner asked why the reflections belonged on the landing page at all, heard that [#45] only requires a link to them, and decided the blockers do not matter because the linked files always exist. They asked for everything to be written, brought up to date and sent for review.
- The first draft said Arbiter "settles the items where those answers agree", which is wrong for `SCALE` projects, whose ratings always resolve to their mean (rule 10). It was corrected before review.
- Whimsyturtle's review of PR #94 found that the steps implied the file format is chosen at export, though it locks at project creation (rule 4); that the page said "label dispute" and "single-label dispute" instead of the Glossary's *dispute*; that "Arbiter resolves automatically and the adjudicator settles disputes" appeared three times; and that the README should say text rather than data.

## Agent responses and outcomes

- Used `write-plan`. The plan is `plans/landing-page.md`, on branch `docs/landing-page` from `main`.
- `docs/index.md` was already the home page, with every link [#45] requires, so it was revised in place: a problem statement, one sentence per role, and a five-step workflow naming `SINGLE`/`SCALE`, **Submit & next**, resolution and CSV/JSON export, each step linking its rule. "Running a release" was added to the links.
- After review: the output format moved to the set-up step and export writes the project's format; the resolve step alone describes automatic resolution and disputes, linking the Glossary; the problem paragraph and the adjudicator's sentence no longer repeat it. The README and the site tagline in `docs/_config.yml` now say text. The agent changed the tagline too, because the site shows it on every page.

## Verification

- A search of `docs/index.md` for detection, images, flags, analytics, agreement, drafts, sealing, completion, browser and COCO finds none.
- Every relative link resolves to a file in `docs/`, the README has the "Running a release" section, and the backlog link is the v1.0.0 milestone.
- After review, a search of `docs/index.md` finds "dispute", "settle" and "automatic" only in the resolve step.
- Pending: the Pages workflow on the pull request, a check of the rendered site, and teammate re-review.

[#38]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/38
[#42]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/42
[#44]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/44
[#45]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/45
