# Complete the issue references in the flow document

Status: Awaiting human verification.

## Original request

- The user reported that some things are not referenced in `docs/UserFlows.md` and asked for a check.
- The check was run first and the findings reported; the user then said to fix them.

## Follow-ups, corrections, and reflection

- The check found four separate problems, not one. Seven of the 46 backlog issues were unreferenced, six of the fifteen rules carried no issue link, the glossary was missing four terms the code defines, and section 4 grouped issues by numeric range rather than by their actual labels. The user asked for all of them to be fixed.
- The range notation was the root cause of the missing references. `[#4] - [#11]` reads as a link pair but says nothing about the seven issues in between, so [#5], [#8] and [#10] had no presence in the document at all; `[#39] - [#45]` hid four more, and [#45] appeared only as a range endpoint.
- Rewriting section 4 as prose rather than a table was a correction to the first attempt. The table had no room to say what each issue is for, which is what made the range shorthand attractive in the first place.
- Two mistakes were caught and fixed while making the changes: rule 14 lost the word "what" from "change what someone already earned" during a scripted edit, and the glossary gained a `Resolution` entry that duplicated `Resolved label`. The second was rewritten to describe the resolution record rather than the label.
- The claim "nothing is tracked outside these 46 issues" was wrong on first writing: the milestone holds 47 issues, because the closed [#49] is in it too. It was corrected before commit rather than left as an assertion nobody would re-check.

## Agent responses and outcomes

- Verified first, then fixed. The check covered every issue reference in both directions, rule numbering, every `rule N` citation across the docs and issues, the glossary against the model classes, and the section 4 grouping against the real `area/*` labels.
- All 46 backlog issues are now referenced by number somewhere in the document, and the definition block holds exactly the 46 that are used.
- Every one of the 15 rules now links the issue that implements it. Rules 4, 5, 6, 7, 14 and 15 previously linked nothing.
- The glossary gained `Bounding box`, `Flag`, `Resolution` and `Workspace`, taking it from 8 terms to 12.
- Section 4 is now prose grouped by track, so each issue is named with what it does rather than hidden inside a range.

## Review follow-up

- Whimsyturtle requested changes on the companion PR #58, and several comments are really spec decisions rather than code-style points. They are applied here, because this page is the shared context the issues hang off.
- Removed from the documented behaviour: multi-select taxonomies, `.md` as a distinct source type, the `RETURNED` assignment status, gold-standard reservation, the `PENDING` account status, three flag reasons, plaintext email as a credential, and any notion of time spent per item.
- The glossary gained a **Fixed value sets** section listing every enum and its values, which is what the reviewer asked for on `ResolutionMethod` and `FlagReason` and makes the remaining values discoverable in one place.
- Rule 4 now says the output format locks at creation alongside task type and source type, which the reviewer questioned. Rule 6 describes exclusion as retiring the item rather than as a separate flag state, matching the removal of `Flag.excluded`.
- `context/architecture.md` was edited to describe the new subpackages and then reverted: that file documents code structure, and the structure ships in #58, so describing it here would put the docs ahead of the code. It is being updated in #58 instead.

## Verification

- 46 of 46 backlog issues referenced; the definition block matches the used set exactly, with nothing undefined and nothing unused.
- All 15 rules carry an issue link; numbering is still contiguous 1 to 15.
- Every `rule N` citation in `context/architecture.md`, `docs/DeveloperGuide.md` and the issues resolves to the intended rule; the closed [#49] still cites the old section 3 numbering, which is historical and was left alone.
- Cross-document links across ten files: 0 broken.
- Section 4's grouping was checked against the actual `area/*` labels, and the ownership statement against the real assignees. It was also checked against the assignees rather than the older conventions: [#39] and [#40] carry `area/foundation` but are delivery work, and are now described that way.
- Documentation only: no Java was touched, so `./gradlew check` was not run.
- A human has not yet verified this summary.

[#49]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/49
