---
title: User Guide
---

# User Guide

Arbiter has [two roles](index.md#the-two-roles), and you see a different interface depending on which one you have.

- **Annotators:** work assigned splits in a [blind queue](UserFlows.md#1-the-annotator-flow).
- **The adjudicator:** owns the workspace, sets projects up, resolves disagreements and [exports datasets](UserFlows.md#2-the-adjudicator-flow).

## Getting started

1. Launch Arbiter. On first run the workspace wizard asks you to choose the folder both roles will use for the database, source media and exports.
2. During setup, create the workspace's **one fixed adjudicator account**.
3. The adjudicator creates annotator accounts and supplies their usernames/passwords. There is no self-signup or promotion to adjudicator.
4. Sign in with an existing account. Annotators see their assigned splits; the adjudicator sees the project list.

Under the agreed [account rules](UserFlows.md#3-rules-both-tracks-share), an annotator who forgets their password asks the adjudicator to set a replacement, then signs in normally with the same username. There is no reset code or email step. Recovery for a forgotten adjudicator password remains unresolved; this annotator reset flow cannot recover the owner account. Account screens and detailed instructions remain implementation work.

## Quick reference

The behaviour behind each task is specified in [User Flows](UserFlows.md); the issue that implements it is linked below.

| I want to... | Role | Issue |
| --- | --- | --- |
| Label items | Annotator | [#13], [#14], [#15] |
| Report a bad image | Annotator | [#16] |
| Track my split progress and lifetime valid annotations | Annotator | [#18], [#19] |
| Create or deactivate annotator accounts | Adjudicator | [#31] |
| Replace an annotator's forgotten password | Adjudicator | [#23] |
| Register files already inside workspace media, before the first assignment | Adjudicator | [#25] |
| Configure labels before the first assignment | Adjudicator | [#26] |
| Assign annotators | Adjudicator | [#32] |
| Monitor project progress and label agreement | Adjudicator | [#33] |
| Resolve disagreements | Adjudicator | [#27], [#34] |
| Export the dataset | Adjudicator | [#37] |

The agreed [workflow and lifecycle rules](UserFlows.md#3-rules-both-tracks-share) use editable current drafts followed by permanent Submit & next, with no backward review or return-for-rework. Assignments cannot be removed or transferred after creation. Setup freezes after assignment and completed projects are read-only; viewing and exporting remain available.

The agreed import flow is to place images or plain-text files in the workspace's `media/` folder first (for example `ArbiterFolder/media/cat.jpg`), then select those files or a subfolder in the project. Import records the existing files without copying or moving them. Keep registered files at the same locations with the same contents; missing or changed files produce an error rather than a replacement-file picker. Deleting an incomplete project leaves source files in place. See [User Flows rule 21](UserFlows.md#3-rules-both-tracks-share) and [#25] for the accepted contract; screen-specific instructions follow implementation.

The agreed statistics scope keeps session counts/timing and the 14-day activity chart ([#18], [#19]). Adjudicators get single-select label agreement; scales, detection and cases with no eligible items show N/A under [#33].

Reward configuration, earnings and income statements ([#20], [#21]) are deferred beyond v1.0.0.

Step-by-step instructions for each screen will be added as features are released.

[Back to home](index.md)

[#13]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/13
[#14]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/14
[#15]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/15
[#16]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/16
[#18]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/18
[#19]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/19
[#20]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/20
[#21]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/21
[#23]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/23
[#25]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/25
[#26]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/26
[#27]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/27
[#31]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/31
[#32]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/32
[#33]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/33
[#34]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/34
[#37]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/37
