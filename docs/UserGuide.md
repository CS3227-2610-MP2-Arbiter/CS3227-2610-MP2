---
title: User Guide
---

# User Guide

Arbiter has [two roles](index.md#the-two-roles), and you see a different interface depending on which one you have.

- **Annotators:** work assigned splits in a [blind queue](UserFlows.md#1-the-annotator-flow).
- **Adjudicators:** set projects up, resolve disagreements and [export datasets](UserFlows.md#2-the-adjudicator-flow).

## Getting started

1. Launch Arbiter. On first run the workspace wizard asks you to choose a folder where your database, imported media and exports will live.
2. Create the first account. **The first account created is the adjudicator.**
3. Log in. Annotators see their assigned splits; adjudicators see their projects.

## Quick reference

The behaviour behind each task is specified in [User Flows](UserFlows.md); the issue that implements it is linked below.

| I want to... | Role | Issue |
| --- | --- | --- |
| Label items | Annotator | [#13], [#14], [#15] |
| Report a bad image | Annotator | [#16] |
| Track my split progress and lifetime valid annotations | Annotator | [#18], [#19] |
| Import a corpus before the first assignment | Adjudicator | [#25] |
| Configure labels before the first assignment | Adjudicator | [#26] |
| Assign annotators | Adjudicator | [#32] |
| Resolve disagreements | Adjudicator | [#27], [#34] |
| Export the dataset | Adjudicator | [#37] |

The agreed [workflow and lifecycle rules](UserFlows.md#3-rules-both-tracks-share) use editable current drafts followed by permanent Submit & next, with no backward review or return-for-rework. Assignments cannot be removed or transferred after creation. Setup freezes after assignment and completed projects are read-only; viewing and exporting remain available.

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
[#25]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/25
[#26]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/26
[#27]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/27
[#32]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/32
[#34]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/34
[#37]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/37
