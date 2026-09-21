---
title: User Flows
---

# Arbiter user flows

Arbiter is an offline desktop app for teams labelling data. This page is the shared context for the project: the shape of the two flows and the rules both tracks must respect. It stays at the level of intent. Screen-by-screen behaviour lives in the GitHub issues, which each step links to. The terms it uses are defined in the [Glossary](Glossary.md).

## Getting started

On first launch a wizard creates the workspace and the first account, which is automatically the adjudicator. Annotator accounts are self-service or created by an adjudicator, but an annotator sees nothing until a split is assigned. Credentials are a username and password, issued and reset by an adjudicator, with no self-service password reset. See [#9], [#7], [#31], [#23].

---

## 1. The annotator flow

An annotator works assigned splits one item at a time, and never sees another annotator's answer.

```
log in -> my splits -> open a split -> blind queue -> annotate one item -> next
  |                                                                  |
  |                                   label + rationale + [boxes] + flag
  |                                                                  |
  +--- earnings <--- released when the adjudicator marks the dataset COMPLETE ---+
```

1. **Sign in and see my splits.** Each assignment shows its project, progress and status. Splits the annotator is not assigned to are absent, not greyed out. [#12]
2. **Work the blind queue.** One item at a time, with no list view and no jumping to arbitrary indexes. Closing the app restores the exact position and any saved input, and an item can be skipped with a reason. [#13]
3. **Annotate the item.** A classification item takes a label from the taxonomy - single-select, or a numeric scale - plus a rationale. A detection item takes labelled boxes drawn on the image. [#14], [#15]
4. **Flag bad source material.** An unclear flag records why the item is unusable and surfaces it for the adjudicator, while the annotation is still saved. [#16]
5. **Save and submit.** Every answer is written through immediately, so a power cut loses nothing. Submitting a split completes the annotator's side of it, and an adjudicator may send it back for rework. [#17]
6. **Track progress and earnings.** Per-split progress, lifetime totals, and earnings split into released and pending. Submitting does not pay: an adjudicator must mark the whole dataset complete. [#18], [#19], [#20], [#21]

## 2. The adjudicator flow

An adjudicator sets a project up, gets it annotated, and turns the result into a dataset.

```
create project -> import corpus -> define taxonomy -> split -> assign
                                                                |
                              monitor progress -> resolve conflicts -> export
```

1. **Create a project.** Name, task type, source type and output format, all fixed at creation because every downstream record assumes them. [#24], [#29], [#30]
2. **Import a corpus.** One item per supported file - images, or plain text - keyed by content hash so re-imports do not duplicate, with a dry-run summary before anything is written. [#25]
3. **Define the taxonomy.** The adjudicator owns the labels: create, rename, reorder and delete. Deleting a label also removes the annotations that used it, and those items return to the same annotator to redo. [#26]
4. **Split the corpus and assign.** Cut splits by count, proportion or by hand, set a per-item reward, then assign annotators and set *k*. Rewards lock once a split is assigned. [#28], [#32], [#31]
5. **Monitor progress.** A dashboard shows items, annotations, agreement and unresolved conflicts per project. [#33]
6. **Resolve answers.** Single-select labels use strict-majority voting, with disputes decided by an adjudicator who sees the competing annotations without names attached. Numeric scale ratings are averaged automatically. For detection, the adjudicator selects one annotator's complete submitted set of labelled boxes. Flagged items are reviewed here too. [#27], [#34], [#35], [#36]
7. **Complete and export.** Marking the dataset complete releases everyone's earnings and is permanent. Export writes the dataset in the project's chosen format, plus a provenance record explaining every decision. [#46], [#37]

## 3. Rules both tracks share

These cut across both surfaces and are where the two tracks can accidentally contradict each other.

1. **Blindness is absolute.** No annotator-visible screen shows another annotator's annotation, any resolved label, or a per-item agreement stat. Aggregate progress is fine; per-item is not. [#13]
2. **Write-through persistence.** Every annotator action hits the store immediately, never an in-memory queue flushed at the end. [#6]
3. **Deleting a label clears its annotations.** Removing a label deletes the annotations that used it and returns those items to unannotated **for the same annotator to redo**. There is no merge and no split of labels. [#26]
4. **Immutable project shape.** Task type, source type and output format lock at creation. [#24], [#29], [#30]
5. **Soft delete everywhere, except labels.** Accounts and items are deactivated or retired, never purged, so historical annotations stay interpretable. Labels are the exception: deleting one genuinely removes its annotations (rule 3). [#31], [#25]
6. **Exclusion means excluded.** An item an adjudicator retires leaves both the export and the earnings calculation. [#35]
7. **Reproducibility.** Seeded splits and a recorded resolution rule mean a dataset can be regenerated and explained months later. [#28]
8. **Everything is local.** No network calls in core flows; the only exceptions are the stretch email features [#39], [#40].
9. **Earnings are released when the dataset completes, and that is final.** An annotator is paid only once an adjudicator marks the whole project `COMPLETE` ([#46]). Submitting a split is necessary but not sufficient, and there is no per-split payout. Marking complete is permanent, so it cannot be undone to withdraw pay. Earnings stay derived, never stored.
10. **Classification resolution follows the taxonomy kind.** For single-select classification, with *k* valid submitted annotations, a label resolves automatically only if it holds a strict majority; otherwise the item is a dispute, including a flat tie, and goes to [#34]. For scale classification, *k* valid submitted integer ratings within the configured range always resolve to their arithmetic mean, retaining fractional results. Each of the *k* independent annotators contributes one answer; fewer than *k* valid submitted answers leaves the item unresolved. Scale tolerance and automatic scale-disagreement detection are deferred beyond v1.0.0. Averaging does not establish annotator agreement. The contributing annotations and resolution method are retained for provenance. [#27]
11. **One shared SQLite file on a shared drive.** The team shares a single `arbiter.db` over a shared drive; there is no package exchange and no merge path. Arbiter takes a workspace lock so only one instance writes at a time ([#1]).
12. **Adjudicators own credentials, and email is deferred.** Accounts are a username and a password, issued and reset by an adjudicator ([#31], [#23]). Email verification and email reset ([#39], [#40]) are out of scope unless time remains.
13. **The adjudicator has final authority over project data.** They may edit or delete any annotation, remove files entirely, add files to an existing split, and return a submitted split for rework - recorded, so provenance still explains every item. This overrides the annotator-side locks in [#17]. Detection resolution itself is limited to selecting a complete submitted box set (rule 16).
14. **Rewards are fixed once a split is assigned.** Nothing about an assigned split changes but its status, because altering a rate after the work would change what someone already earned. [#28], [#32]
15. **An incomplete export is still a valid export.** A dataset may be exported before it is complete, and the result must parse as a proper file of its format. Unresolved items are absent or explicitly marked, never given a wrong label. [#37]
16. **Detection resolves by selecting one complete submission.** After *k* valid submitted annotations, an adjudicator selects exactly one annotator's complete box set for that item, preserving every box's coordinates and label. There is no automatic box matching, mixing of submissions or box editing during detection resolution, even when submissions appear identical. Until a set is selected, the item remains unresolved. Record the selected annotation, method, decider and timestamp, and retain the other submissions for provenance. Export uses exactly the selected set. [#34], [#36], [#37]

## 4. Where the work lives

The v1.0.0 backlog is on GitHub under the [v1.0.0 milestone](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/milestone/7), and the 46 issues below are all of it. Every one is referenced somewhere in this document.

**Spikes.** Three questions were settled in writing before any feature code, and they are closed: how a team shares data while staying offline ([#1]), when annotator earnings vest ([#2]), and whether the email features are in scope ([#3]).

**Foundation.** Shared groundwork both tracks build on, before either can ship a screen:

- [#4] agrees the model classes and repository interfaces both tracks code against.
- [#6] implements the SQLite schema and repositories, and [#11] the test harness, fixtures and blindness test.
- [#7] covers registration, login and logout, and [#31] the adjudicator's account management.
- [#5] is the app shell and role-based routing, and [#8] the shared UI kit and error-handling convention.
- [#9] sets up the workspace and its layout, and [#10] resolves media paths and decodes images.

**Annotator track (zheng-jj).** The whole of section 1: [#12] the home and assigned splits, [#13] the blind queue, [#14] classification, [#15] detection, [#16] flagging, [#17] autosave and submit, [#18] progress, [#19] lifetime totals, [#20] earnings, [#21] the income statement, and [#22] the annotator guide.

**Adjudicator track (Whimsyturtle).** The whole of section 2: [#24] creating a project, [#29] task type, [#30] output format, [#25] importing a corpus, [#26] the taxonomy, [#28] splitting and rewards, [#32] assigning annotators, [#33] the dashboard, [#27] automatic resolution, [#34] manual resolution, [#35] flagged items, [#36] the per-item breakdown, [#37] export, [#46] marking the dataset complete, and [#38] the adjudicator guide. [#23] adds the offline password reset.

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
