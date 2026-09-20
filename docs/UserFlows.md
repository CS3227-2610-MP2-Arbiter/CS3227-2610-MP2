---
title: User Flows
---

# Arbiter user flows

Arbiter is an offline desktop app for teams labelling data. This page is the shared context for the project: the shape of the two flows and the rules both tracks must respect. It stays at the level of intent. Screen-by-screen behaviour lives in the GitHub issues, which each step links to. The terms it uses are defined in the [Glossary](Glossary.md).

## Getting started

On first launch a wizard creates the workspace and the first account, which is automatically the adjudicator. Annotator accounts are self-service or created by an adjudicator, but an annotator sees nothing until a split is assigned. Credentials are issued and reset by an adjudicator, with no self-service password reset. See [#9], [#7], [#31], [#23].

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
3. **Annotate the item.** A classification item takes a label from the taxonomy - single-select, multi-select or a numeric scale - plus a rationale. A detection item takes labelled boxes drawn on the image. [#14], [#15]
4. **Flag bad source material.** An unclear flag records why the item is unusable and surfaces it for the adjudicator, while the annotation is still saved. [#16]
5. **Save and submit.** Every answer is written through immediately, so a power cut loses nothing. Submitting a split completes the annotator's side of it. [#17]
6. **Track progress and earnings.** Per-split progress, lifetime totals, and earnings split into released and pending. Submitting does not pay: an adjudicator must mark the whole dataset complete. [#18], [#19], [#20], [#21]

## 2. The adjudicator flow

An adjudicator sets a project up, gets it annotated, and turns the result into a dataset.

```
create project -> import corpus -> define taxonomy -> split -> assign
                                                                |
                              monitor progress -> resolve conflicts -> export
```

1. **Create a project.** Name, task type, source type and output format, all fixed at creation because every downstream record assumes them. [#24], [#29], [#30]
2. **Import a corpus.** One item per supported file, keyed by content hash so re-imports do not duplicate, with a dry-run summary before anything is written. [#25]
3. **Define the taxonomy.** The adjudicator owns the labels: create, rename, reorder and delete. Deleting a label also removes the annotations that used it, and those items return to the same annotator to redo. [#26]
4. **Split the corpus and assign.** Cut splits by count, proportion or by hand, set a per-item reward, then assign annotators and set *k*. Rewards lock once a split is assigned. [#28], [#32], [#31]
5. **Monitor progress.** A dashboard shows items, annotations, agreement and unresolved conflicts per project. [#33]
6. **Resolve disagreements.** Where annotators agreed, the item resolves automatically; where they did not, the adjudicator decides, seeing the competing annotations without names attached. Flagged items are reviewed here too. [#27], [#34], [#35], [#36]
7. **Complete and export.** Marking the dataset complete releases everyone's earnings and is permanent. Export writes CSV, JSON or COCO plus a provenance record explaining every decision. [#46], [#37]

## 3. Rules both tracks share

These cut across both surfaces and are where the two tracks can accidentally contradict each other.

1. **Blindness is absolute.** No annotator-visible screen shows another annotator's annotation, any resolved label, or a per-item agreement stat. Aggregate progress is fine; per-item is not. [#13]
2. **Write-through persistence.** Every annotator action hits the store immediately, never an in-memory queue flushed at the end. [#6]
3. **Deleting a label clears its annotations.** Removing a label deletes the annotations that used it and returns those items to unannotated **for the same annotator to redo**. There is no merge and no split of labels. [#26]
4. **Immutable project shape.** Task type and source type lock at creation.
5. **Soft delete everywhere, except labels.** Accounts and items are deactivated or retired, never purged, so historical annotations stay interpretable. Labels are the exception: deleting one genuinely removes its annotations (rule 3).
6. **Exclusion means excluded.** Flagged-and-excluded items leave both the export and the earnings calculation.
7. **Reproducibility.** Seeded splits and a recorded resolution rule mean a dataset can be regenerated and explained months later.
8. **Everything is local.** No network calls in core flows; the only exceptions are the stretch email features [#39], [#40].
9. **Earnings are released when the dataset completes, and that is final.** An annotator is paid only once an adjudicator marks the whole project `COMPLETE` ([#46]). Submitting a split is necessary but not sufficient, and there is no per-split payout. Marking complete is permanent, so it cannot be undone to withdraw pay. Earnings stay derived, never stored.
10. **A dispute is a missing strict majority.** With *k* annotations, if no label holds a strict majority the item is a dispute, including a flat tie between two labels. Disputes go to [#34].
11. **One shared SQLite file on a shared drive.** The team shares a single `arbiter.db` over a shared drive; there is no package exchange and no merge path. Arbiter takes a workspace lock so only one instance writes at a time ([#1]).
12. **Adjudicators own credentials, and email is deferred.** Login ids and passwords are issued and reset by an adjudicator ([#31], [#23]). [#39] and [#40] are out of scope unless time remains.
13. **The adjudicator has final authority over project data.** They may edit or delete any annotation, remove files entirely, and add files to an existing split - recorded, so provenance still explains every item. This overrides the annotator-side locks in [#17].
14. **Rewards are fixed once a split is assigned.** Nothing about an assigned split changes but its status, because altering a rate after the work would change what someone already earned.
15. **An incomplete export is still a valid export.** A dataset may be exported before it is complete, and the result must parse as a proper file of its format. Unresolved items are absent or explicitly marked, never given a wrong label.

## 4. Where the work lives

The v1.0.0 backlog is on GitHub under the [v1.0.0 milestone](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/milestone/7), and the flows above link to the issues that implement them. zheng-jj owns the annotator track and Whimsyturtle the adjudicator track; the spikes, foundation and delivery issues are shared.

| Area | Owner | Issues |
| --- | --- | --- |
| Spikes | shared | [#1], [#2], [#3] |
| Foundation | shared | [#4] - [#11] |
| Annotator track | zheng-jj | [#12] - [#22] |
| Adjudicator track | Whimsyturtle | [#23] - [#38], [#46] |
| Delivery | zheng-jj | [#39] - [#45] |

[Back to home](index.md)

[#1]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/1
[#2]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/2
[#3]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/3
[#4]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/4
[#6]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/6
[#7]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/7
[#9]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/9
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
[#45]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/45
[#46]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/46
