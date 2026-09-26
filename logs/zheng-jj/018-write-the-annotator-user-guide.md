# Write the annotator user guide

Status: Awaiting human verification.

## Original request

- After [#18] merged, the owner asked to "work through what's unblocked", which for the annotator track was [#22].

## Follow-ups, corrections, and reflection

- Most of [#22] already existed, because each annotator issue had added to the User Guide as it landed. The work was the gaps: credentials, screenshots, a plain statement of blindness, and linking terms and rules rather than restating them.
- The screenshots were rendered from the app's real screens rather than drawn as mockups. A throwaway program seeded a workspace with the #11 fixtures and realistic texts, built the real `AppShell`, `MySplitsScreen` and `QueueScreen`, and saved JavaFX snapshots as PNGs. This worked although the session cannot capture the screen, so the images cannot drift from the implementation the way hand-made mockups can.
- The first render failed to find the rating field, because a scroll pane attaches its content only during a layout pass. Running a layout pass before the lookup fixed it.
- The work stays uncommitted under the delivery rule.

## Agent responses and outcomes

- Used `write-plan`, `maintain-docs`, `review` and `log`. The plan is `plans/annotator-guide.md`, on branch `docs/annotator-guide` from `main` at 429b030.
- Updated Getting started, the quick reference and My splits in `docs/UserGuide.md`, and added three screenshots in `docs/images/`.

## Verification

- A search of the guide finds no deferred feature: detection, images as input, flags, rationale, shortcuts, timers, analytics, agreement, review navigation or earnings.
- Every issue reference in the guide has a matching link definition, and every image path exists.
- Not run locally: the Jekyll build, which is not installed here. The Pages workflow builds the site on the pull request.
- The owner approved delivery, and a review request, on 26 September 2026.
- Pending: human acceptance (comparing the screenshots with the running app), teammate review and the Pages build.

[#11]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/11
[#18]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/18
[#22]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/22
