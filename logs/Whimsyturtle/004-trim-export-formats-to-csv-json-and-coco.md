# Trim export formats to CSV, JSON and COCO

Status: Human verified.

## Original request

- While auditing `zheng-jj`'s commits (log `003`), the user found the five export formats in `docs/index.md` too many for a first release. They asked where else the formats appeared, in documents or GitHub issues, so they could trim them consistently.

## Follow-ups, corrections, and reflection

- The agent recommended CSV and JSON, which suit every project, plus COCO as the one image format, offered only for image detection projects. The user chose these three and asked for issue drafts to review before GitHub was edited.
- The agent suggested agreeing the decision with `zheng-jj` first and keeping it off `docs/review-fixes`. The user proceeded on a separate branch and will inform `zheng-jj` separately.
- Reviewing the drafts, the user kept `#30` offering only suitable formats rather than warning about unsuitable ones. They also standardised on "fixed at creation", removing `#30`'s export-time override, which contradicted `docs/UserFlows.md` §2.5.
- The agent advised against editing a teammate's comment, but the user asked for `zheng-jj`'s `#37` comment to be updated too, so only its YOLO clause was removed.

## Agent responses and outcomes

- The formats appeared in `docs/index.md`, `docs/UserFlows.md`, `docs/DeveloperGuide.md`, `context/architecture.md` and issues `#17`, `#24`, `#30` and `#37`. The issues had copied the list as often as the documents, so design changes need checking against both.
- The agent trimmed the documents on `docs/trim-export-formats`, then updated the four issues and the comment with `gh` after the user approved the diffs.

## Verification

- No YOLO or Pascal mentions remain outside `build/`, and `git diff --check` passed. The re-fetched issues and comment match the approved drafts.
- Undecided: whether output format should join the "immutable after creation" lists in `#24` and `docs/UserFlows.md` §2.1.
- The user edited the summary and approved it.
