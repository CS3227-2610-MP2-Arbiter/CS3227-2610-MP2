# Focused product-site landing page

Status: Awaiting human verification.

## Original request

- After merging PR #92 ([#42]), the owner said "start working on the product landing page" ([#45]).

## Follow-ups, corrections, and reflection

- [#45] lists [#38] and [#44] as blockers, and both are still open. The agent first proposed holding the pull request until they closed. The owner asked why the reflections belonged on the landing page at all, heard that [#45] only requires a link to them, and decided the blockers do not matter because the linked files always exist. They asked for everything to be written, brought up to date and sent for review.
- The first draft said Arbiter "settles the items where those answers agree", which is wrong for `SCALE` projects, whose ratings always resolve to their mean (rule 10). It was corrected before review.

## Agent responses and outcomes

- Used `write-plan`. The plan is `plans/landing-page.md`, on branch `docs/landing-page` from `main`.
- `docs/index.md` was already the home page, with every link [#45] requires, so it was revised in place: a problem statement, one sentence per role, and a five-step workflow naming `SINGLE`/`SCALE`, **Submit & next**, resolution and CSV/JSON export, each step linking its rule. "Running a release" was added to the links.

## Verification

- A search of `docs/index.md` for detection, images, flags, analytics, agreement, drafts, sealing, completion, browser and COCO finds none.
- Every relative link resolves to a file in `docs/`, the README has the "Running a release" section, and the backlog link is the v1.0.0 milestone.
- Pending: the Pages workflow on the pull request, a check of the rendered site, and teammate review.

[#38]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/38
[#42]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/42
[#44]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/44
[#45]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/45
