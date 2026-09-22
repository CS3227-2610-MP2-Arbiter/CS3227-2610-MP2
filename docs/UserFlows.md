---
title: User Flows
---

# Arbiter user flows

This page is the shared context for the project: the shape of the two flows and the rules both tracks must respect. It stays at the level of intent. Screen-by-screen behaviour lives in the GitHub issues, which each step links to. The terms it uses are defined in the [Glossary](Glossary.md).

## Getting started

On first launch a wizard creates the workspace and its adjudicator account, who then creates the annotator accounts (rule 12). See [#9], [#7], [#31], [#23].

---

## 1. The annotator flow

An annotator works assigned splits one item at a time, and never sees another annotator's answer.

```
log in -> my splits -> open a split -> blind queue -> annotate one item -> next
                                            |
                   label + [rationale] + [boxes] + [flag]
```

1. **Sign in and see my splits.** Each assignment shows its project, progress and status. [#12]
2. **Work the blind queue.** One item at a time in saved order, moving only forward (rule 18). Closing the app restores the current draft. [#13]
3. **Annotate the item.** A classification item takes a label from the taxonomy - single-select, or a numeric scale. A detection item takes labelled boxes drawn on the image. Either can carry an optional rationale. [#14], [#15]
4. **Flag bad source material.** Report unusable material with a reason and optional comment, alongside an answer or instead of one. [#16]
5. **Save and submit.** The current draft autosaves until **Submit & next**, or **Flag & next** for a report-only outcome, permanently records it and advances (rule 18). [#17]
6. **Track annotation progress and activity.** Per-split progress, session statistics and lifetime totals (rule 20). [#18], [#19]

## 2. The adjudicator flow

An adjudicator sets a project up, gets it annotated, and turns the result into a dataset.

```
create project -> import corpus -> define taxonomy -> split -> assign
                                                                |
                              monitor progress -> resolve conflicts -> export
```

1. **Create a project.** Name, task type, source type, taxonomy kind and output format (rule 4). [#24], [#29], [#30]
2. **Import a corpus.** Register images or plain-text files already in the workspace's `media/` folder, with a dry-run summary and duplicate detection (rule 21). [#25], [#10]
3. **Define the taxonomy.** Configure the labels, or the scale range, before the first assignment (rule 3). [#26]
4. **Split the corpus and assign.** Choose the number of items per batch, preview the generated batches, set *k*, then assign annotators (rules 7, 14, 19). [#28], [#32], [#31]
5. **Monitor progress.** A dashboard shows progress, disputes, flagged items and label agreement (rule 20). [#33]
6. **Resolve answers.** Settle each item under rules 10 and 16, and keep or exclude flagged items (rule 6). [#27], [#34], [#35], [#36]
7. **Complete and export.** Marking the dataset complete seals the project (rule 9); export writes the dataset with provenance (rule 15). [#46], [#37]

## 3. Rules both tracks share

These cut across both surfaces and are where the two tracks can accidentally contradict each other. Feature-specific detail is in the linked issues.

1. **Blindness is absolute.** An annotator sees only their own work and progress, never a resolved label. Anything about other annotators, such as their annotations, progress or agreement, is adjudicator-only, even as totals. [#12], [#13], [#18], [#33]
2. **Write-through persistence.** Every annotator action hits the store immediately, never an in-memory queue flushed at the end. [#6]
3. **Corpus and taxonomy freeze at the first assignment.** The project's first assignment permanently freezes its corpus and taxonomy: no import, source replacement, ordinary item removal or taxonomy edit, including label creation/deletion, key/description/order changes and scale-range changes. Use a new project for a different corpus or label scheme. Flagged-item exclusion (rule 6) is the only exception. [#25], [#26], [#32]
4. **Immutable project shape.** Task type, source type, taxonomy kind and output format lock at creation. [#24], [#29], [#30]
5. **Retain records needed to explain work.** Annotator accounts are deactivated and items retired, never deleted, so their annotations and attribution remain. Items may be retired only during setup or through the exclusion in rule 6. An unused label may be deleted only before the project's first assignment, and deletion never cascades into annotations. An entire incomplete project may also be deleted, with the loss confirmation in [#24]; this removes its database records, not its source files (rule 21). [#31], [#25], [#26]
6. **Flagged-item exclusion is allowed only before completion.** Retiring a flagged item excludes it from the dataset and export, while retaining the item, its split membership, annotations and provenance. It never permits importing/replacing source material or moving items between assigned splits. [#35]
7. **Persist the generated work definition.** Batches are generated automatically by item count from a seeded shuffle, and each item belongs to at most one split. The saved membership and order are the work definition: reopening or assigning a batch reads them and never reruns the allocation. [#28]
8. **Everything is local.** V1 has no network calls in core flows.
9. **Completion permanently seals project data.** COMPLETE blocks all project writes, including configuration, corpus, taxonomy, split/assignment changes, annotations/boxes, flags/exclusion, resolutions and project deletion. Viewing and export remain available. Completion may still be confirmed with unfinished work, which then stays read-only; there is no reopen. [#46], [#24]
10. **Classification resolution follows the taxonomy kind.** For single-select classification, with *k* valid submitted annotations, a label resolves automatically only if it holds a strict majority; otherwise the item is a dispute, including a flat tie, and goes to [#34]. For scale classification, *k* valid submitted integer ratings within the configured range always resolve to their arithmetic mean, retaining fractional results. Each of the *k* independent annotators contributes one answer; fewer than *k* valid submitted answers leaves the item unresolved. [#27]
11. **One shared workspace and SQLite file.** Both roles use the same workspace folder, on the same computer or a shared disk, with one `arbiter.db` and the source files under `media/` (rule 21). There is no package exchange and no merge path. Arbiter takes a workspace lock so only one instance writes at a time. [#1], [#61]
12. **One fixed adjudicator per workspace.** The first account created during setup is the sole adjudicator; only that account creates later accounts, all as annotators. Roles stay fixed: no self-signup, promotion/demotion, additional adjudicators or owner transfer/deactivation/deletion. The adjudicator issues annotator usernames/passwords and directly sets replacement annotator passwords. Owner password recovery remains unresolved. [#7], [#31], [#23]
13. **Adjudicate without rewriting submissions.** Adjudicators resolve answers under [#27]/[#34] and keep or exclude flagged items under [#35], but never edit an annotator's submitted answer, boxes, rationale or report, return work, or supply a replacement submission. A resolution is a separate record, and KEEP does not approve a label or fill a missing answer.
14. **Each split's definition locks on its first assignment.** Its membership/order, *k* and other setup settings cannot change afterwards. Before then, changing membership means deleting the batch and generating a replacement; there is no manual item selection or movement. Retiring an item after generation keeps its membership. [#28], [#32]
15. **Export one dataset with provenance.** A dataset may be exported before or after completion, and the result must parse as a proper file of its format. Unresolved items are absent or explicitly marked, never given a wrong label. [#37]
16. **Detection resolves by selecting one complete submission.** After *k* valid submitted annotations, an adjudicator selects exactly one annotator's complete box set for that item. There is no automatic box matching, mixing of submissions or box editing during detection resolution, even when submissions appear identical; until a set is selected, the item remains unresolved. [#34]
17. **Provenance keeps immutable submissions and the current decision.** Retain each item's [provenance](Glossary.md), including unselected submissions. A draft is not submitted evidence, and no revision history, return metadata or decision timeline is required. [#17], [#36], [#37]
18. **Submit and advance atomically.** Submit & next (or Flag & next for a report-only outcome) validates and permanently records the current answer or report and advances the queue in one step; failure leaves the draft editable, and a retry cannot submit twice. Submissions never change afterwards through either role, and there is no Back, return, resubmission or batch-submit. Resolution counts valid per-item submissions, even while other items in those assignments remain unfinished; drafts and report-only outcomes never count toward *k*. [#13], [#16], [#17], [#27], [#34], [#35]
19. **Assignments stay with their original annotators.** Once created, an assignment cannot be unassigned, transferred or individually deleted, even before any work starts. Unused places may still be filled up to the split's fixed *k*, but every existing assignment counts regardless of account status, and the same split/annotator cannot be assigned twice. Disabling an account preserves its work and ownership without freeing its place or redistributing its items, so some items may stay unresolved. [#31], [#32]
20. **Keep statistics; measure agreement only for single-select labels.** Progress, session timing and activity charts remain in v1, counting submissions by their submission time and needing no edit-history log. [Label agreement](Glossary.md) uses original submitted labels, never resolved answers, and shows N/A for scales and detection. [#18], [#19], [#33]
21. **Register source files in place under workspace media.** Images and plain-text files must already be inside `<workspace>/media/`; import records their workspace-relative locations and content hashes without copying them. Arbiter never moves, edits, deletes or overwrites source files, including on project deletion or export. A missing, corrupt or changed source produces a clear error, never a silent replacement, automatic retirement or fabricated answer. [#9], [#10], [#25], [#37]

## 4. Where the work lives

The [v1.0.0 milestone](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/milestone/7) tracks the work scheduled for v1.0.0. The issue map below also keeps deferred and historical decisions visible where they explain the accepted scope.

**Spikes.** Three questions were settled in writing before any feature code, and they are closed: how a team shares data while staying offline ([#1]), when annotator earnings would vest ([#2]), and whether the email features are in scope ([#3]). The [#2] decision is historical and superseded for v1.0.0 because all money tracking is now deferred; the accepted account simplification also defers both email features beyond v1 rather than retaining them as stretch deliverables under [#3].

**Foundation.** Shared groundwork both tracks build on, before either can ship a screen:

- [#4] agrees the model classes and repository interfaces both tracks code against.
- [#6] implements the SQLite schema and repositories, and [#11] the test harness, fixtures and blindness test.
- [#7] covers sole-adjudicator bootstrap, login and logout, and [#31] the adjudicator's creation/deactivation of annotator accounts.
- [#5] is the app shell and role-based routing, and [#8] the shared UI kit and error-handling convention.
- [#9] sets up the workspace and its layout, [#61] locks it to a single writer, and [#10] resolves media paths and decodes images.

**Annotator track (zheng-jj).** The v1 work in section 1: [#12] the home and assigned splits, [#13] the blind queue, [#14] classification, [#15] detection, [#16] flagging, [#17] autosave and submit, [#18] progress, [#19] lifetime valid-annotation totals, and [#22] the annotator guide. Earnings [#20] and the income statement [#21] are explicitly deferred beyond v1.0.0.

**Adjudicator track (Whimsyturtle).** The whole of section 2: [#24] creating a project, [#29] task type, [#30] output format, [#25] importing a corpus, [#26] the taxonomy, [#28] count-based splitting, [#32] assigning annotators, [#33] the dashboard, [#27] automatic resolution, [#34] manual resolution, [#35] flagged items, [#36] the per-item breakdown, [#37] export, [#46] marking the dataset complete, and [#38] the adjudicator guide. [#23] adds direct replacement of an annotator's password.

**Delivery (zheng-jj).** Everything that turns the code into a submitted project: [#39] and [#40] are the deferred email features, [#41] the demo corpus and smoke checklist, [#42] packaging, [#43] the developer guide, [#44] the reflections, and [#45] the product site.

[Back to home](index.md)

[#1]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/1
[#2]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/2
[#3]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/3
[#4]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/4
[#5]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/5
[#6]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/6
[#7]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/7
[#8]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/8
[#9]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/9
[#10]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/10
[#11]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/11
[#12]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/12
[#13]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/13
[#14]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/14
[#15]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/15
[#16]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/16
[#17]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/17
[#18]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/18
[#19]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/19
[#20]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/20
[#21]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/21
[#22]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/22
[#23]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/23
[#24]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/24
[#25]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/25
[#26]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/26
[#27]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/27
[#28]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/28
[#29]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/29
[#30]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/30
[#31]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/31
[#32]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/32
[#33]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/33
[#34]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/34
[#35]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/35
[#36]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/36
[#37]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/37
[#38]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/38
[#39]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/39
[#40]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/40
[#41]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/41
[#42]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/42
[#43]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/43
[#44]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/44
[#45]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/45
[#46]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/46
[#61]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/61
