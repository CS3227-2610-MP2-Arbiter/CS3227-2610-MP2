---
title: User Guide
---

# User Guide

Arbiter has [two roles](index.md#the-two-roles), and you see a different interface depending on which one you have. [User Flows](UserFlows.md) shows what each role does.

## Getting started

1. Launch Arbiter. On first run the workspace wizard asks you to choose a [workspace](Glossary.md) folder.
2. During setup, create the workspace's adjudicator account.
3. The adjudicator creates annotator accounts and gives each annotator their username and password.
4. Sign in. Arbiter opens your role's home ([#5]): [Projects](#projects) for the adjudicator, and a placeholder for annotators until [#12].

If the workspace is already in use, close its other Arbiter instance and try again ([#61](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/61)).

If Arbiter says it could not finish something, the details are in the workspace's `logs/arbiter.0.log`. Include that file when you report the problem; it never contains passwords.

## Quick reference

The accepted V1 behaviour behind each planned task is specified in [User Flows](UserFlows.md); the issue that implements it is linked below. This quick reference does not imply that every screen has been released yet.

| I want to... | Role | Issue |
| --- | --- | --- |
| Label plain-text items | Annotator | [#13], [#14] |
| Track my split progress | Annotator | [#18] |
| Create or deactivate annotator accounts | Adjudicator | [#31] |
| Replace an annotator's forgotten password | Adjudicator | [#23] |
| Create or delete a project | Adjudicator | [#24], [#30] |
| Import a corpus | Adjudicator | [#25] |
| Configure labels | Adjudicator | [#26] |
| Assign annotators | Adjudicator | [#32] |
| Monitor basic project progress | Adjudicator | [#33] |
| Resolve disagreements | Adjudicator | [#27], [#34] |
| Export the dataset | Adjudicator | [#37] |

Instructions for each screen are added below as it is released.

## Projects

Projects is the adjudicator's home screen. It lists every [project](Glossary.md) with its taxonomy kind, output format and counts.

- **Create a project.** Choose **New project**, enter a unique name and an optional description, and pick a [taxonomy kind and output format](Glossary.md#fixed-value-sets). Neither can be changed later ([rule 4](UserFlows.md#3-rules-both-tracks-share)).
- **Delete a project.** Choose **Delete** on its row and confirm. You can delete a project only before its first assignment, and its source files in `media/` are kept ([rule 5](UserFlows.md#3-rules-both-tracks-share)).
- **Open a project.** Choose **Open** on its row to see the files registered to it. **Back** returns to the list.
- **Register files.** Put the `.txt` files in the workspace's `media/` folder, open the project, choose **Add files...** and select them. Arbiter records where each file is without copying or changing it ([rule 21](UserFlows.md#3-rules-both-tracks-share)). If any file is rejected, none are registered and the message names the file.
- **Unregister a file.** On the project page, choose **Unregister** on its row and confirm. The file itself stays in `media/`.

You can register or unregister files only before the project's first assignment ([rule 3](UserFlows.md#3-rules-both-tracks-share)). Labels cannot be set up until taxonomy setup ([#26]) is released.

[Back to home](index.md)

[#5]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/5
[#12]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/12
[#13]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/13
[#14]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/14
[#18]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/18
[#23]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/23
[#24]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/24
[#25]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/25
[#26]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/26
[#27]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/27
[#30]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/30
[#31]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/31
[#32]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/32
[#33]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/33
[#34]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/34
[#37]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/37
