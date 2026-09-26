---
title: User Guide
---

# User Guide

Arbiter has [two roles](index.md#the-two-roles), and you see a different interface depending on which one you have. [User Flows](UserFlows.md) shows what each role does.

## Getting started

1. Launch Arbiter. On first run the workspace wizard asks you to choose a [workspace](Glossary.md) folder.
2. During setup, create the workspace's adjudicator account.
3. The adjudicator creates annotator accounts on the [Accounts](#accounts) screen and gives each annotator their username and password.
4. Sign in. Arbiter opens your role's home ([#5]): [Projects](#projects) for the adjudicator, and [My splits](#my-splits) for annotators.

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
| Split the corpus into batches | Adjudicator | [#28] |
| Assign annotators | Adjudicator | [#32] |
| Monitor basic project progress | Adjudicator | [#33] |
| Resolve disagreements | Adjudicator | [#27], [#34] |
| Export the dataset | Adjudicator | [#37] |

Instructions for each screen are added below as it is released.

## Projects

Projects is the adjudicator's home screen. It lists every [project](Glossary.md) with its taxonomy kind, output format and counts.

- **Create a project.** Choose **New project**, enter a unique name and an optional description, and pick a [taxonomy kind and output format](Glossary.md#fixed-value-sets). Neither can be changed later ([rule 4](UserFlows.md#3-rules-both-tracks-share)).
- **Delete a project.** Choose **Delete** on its row and confirm. You can delete a project only before its first assignment, and its source files in `media/` are kept ([rule 5](UserFlows.md#3-rules-both-tracks-share)).
- **Open a project.** Choose **Open** on its row to see its files and splits. **Back** returns to the list.
- **Register files.** Put the `.txt` files in the workspace's `media/` folder, open the project, choose **Add files...** and select them. Arbiter records where each file is without copying or changing it ([rule 21](UserFlows.md#3-rules-both-tracks-share)). If any file is rejected, none are registered and the message names the file.
- **Unregister a file.** On the project page, choose **Unregister** on its row and confirm. The file itself stays in `media/`, and a split left with no files is deleted.
- **Generate splits.** On the project page, enter the number of files per split, choose **Generate splits** and confirm the sizes shown. Arbiter shuffles the files not yet in a split into new splits, and the **Split** column shows each file's split and position ([rule 7](UserFlows.md#3-rules-both-tracks-share)).
- **Delete a split.** Choose **Delete** on its row and confirm. Its files return to those not yet in a split. You can delete a split only before its first assignment ([rule 14](UserFlows.md#3-rules-both-tracks-share)).
- **Assign annotators.** On the project page, choose **Assign** on a split's row. The form lists the split's annotators and the active annotators you can add, each with their unfinished assignments and files. With the split's first assignment, enter [*k*](Glossary.md), which cannot be changed afterwards. Tick one or more annotators, choose **Assign** and confirm. The **Annotators** column shows how many of the split's *k* places are taken. Assignments cannot be removed or moved, and a deactivated annotator keeps their place ([rule 19](UserFlows.md#3-rules-both-tracks-share)).

You can register or unregister files only before the project's first assignment ([rule 3](UserFlows.md#3-rules-both-tracks-share)). Labels cannot be set up until taxonomy setup ([#26]) is released, and a project assigned before then can never get them.

## My splits

My splits is an annotator's home screen. It lists only your own assignments, unfinished ones first, each with its project and split, its status and how many of its files you have answered.

- **Start or continue a split.** Choose **Start** or **Continue** on its card. The split opens at the first file you have not answered, in its saved order, showing the file's text and its position, such as "File 3 of 10". Restarting Arbiter brings you back to the same file. **Back to My splits** returns to the list. Choosing an answer arrives with [#14], and moving on to the next file with [#17].
- **A file that cannot be read.** If a file is missing or has changed since it was registered, the split shows why instead of its text, and the file cannot be answered until its original contents are restored ([rule 21](UserFlows.md#3-rules-both-tracks-share)).
- **Finished splits** show that every file has your answer and cannot be reopened.

You never see another annotator's assignments, answers or progress ([rule 1](UserFlows.md#3-rules-both-tracks-share)).

## Accounts

Accounts lists every account with its role and [status](Glossary.md#fixed-value-sets), the adjudicator first. Only the adjudicator sees it, and every account they create is an annotator ([rule 12](UserFlows.md#3-rules-both-tracks-share)).

- **Create an annotator.** Choose **New annotator** and enter a username and the password twice. A username already taken in any letter case, even by a disabled account, is refused.
- **Reset a password.** Choose **Reset password** on an active annotator's row and enter the new password twice. The adjudicator's own password cannot be reset.
- **Deactivate an annotator.** Choose **Deactivate** on their row and confirm. They can no longer sign in, and their work and assignments are kept ([rule 19](UserFlows.md#3-rules-both-tracks-share)). Deactivation is permanent.

[Back to home](index.md)

[#5]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/5
[#13]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/13
[#14]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/14
[#17]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/17
[#18]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/18
[#23]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/23
[#24]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/24
[#25]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/25
[#26]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/26
[#27]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/27
[#28]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/28
[#30]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/30
[#31]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/31
[#32]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/32
[#33]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/33
[#34]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/34
[#37]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/37
