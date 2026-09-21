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
5. **Save and submit.** Every answer is written through immediately, so a power cut loses nothing. Submitting a split completes the annotator's side of it, and an adjudicator may send it back for rework before the project is marked complete. [#17]
6. **Track progress and earnings.** Per-split progress, lifetime totals, and earnings split into released and pending. Submitting does not pay: an adjudicator must mark the whole dataset complete. [#18], [#19], [#20], [#21]

## 2. The adjudicator flow

An adjudicator sets a project up, gets it annotated, and turns the result into a dataset.

```
create project -> import corpus -> define taxonomy -> split -> assign
                                                                |
                              monitor progress -> resolve conflicts -> export
```

1. **Create a project.** Name, task type, source type and output format, all fixed at creation because every downstream record assumes them. [#24], [#29], [#30]
2. **Import a corpus.** Before the project's first assignment, register one item per supported file - images, or plain text - keyed by content hash so re-imports do not duplicate, with a dry-run summary before anything is written. [#25]
3. **Define the taxonomy.** Configure label keys/descriptions and order, or the scale range, before the project's first assignment. The taxonomy freezes at that point; deleting labels can no longer erase existing annotations or trigger rework. [#26]
4. **Split the corpus and assign.** Cut splits by count, proportion or by hand, set a per-item reward and *k*, then assign annotators. The first assignment freezes the project's corpus and taxonomy; each split's definition locks on its own first assignment. [#28], [#32], [#31]
5. **Monitor progress.** A dashboard shows items, annotations, agreement and unresolved conflicts per project. [#33]
6. **Resolve answers.** Single-select labels use strict-majority voting, with disputes decided by an adjudicator who sees the competing annotations without names attached. Numeric scale ratings are averaged automatically. For detection, the adjudicator selects one annotator's complete submitted set of labelled boxes. Flagged items are reviewed here too. [#27], [#34], [#35], [#36]
7. **Complete and export.** Marking the dataset complete permanently seals project data and releases earnings for eligible submitted work, after confirmation of any unfinished work. Viewing and export remain available, with a provenance record explaining every decision. [#46], [#37]

## 3. Rules both tracks share

These cut across both surfaces and are where the two tracks can accidentally contradict each other.

1. **Blindness is absolute.** No annotator-visible screen shows another annotator's annotation, any resolved label, or a per-item agreement stat. Aggregate progress is fine; per-item is not. [#13]
2. **Write-through persistence.** Every annotator action hits the store immediately, never an in-memory queue flushed at the end. [#6]
3. **Corpus and taxonomy freeze at the first assignment.** Before the project's first assignment, configure its source items, labels and scale range. Afterwards there is no import, source replacement, ordinary item removal or taxonomy edit, including label creation/deletion, key/description/order changes, taxonomy-kind changes and scale-range changes. Use a new project for a different corpus or label scheme. The freeze is permanent even if all current assignments are removed. Flagged-item exclusion before completion is the narrow exception in rule 6. [#25], [#26], [#32]
4. **Immutable project shape.** Task type, source type and output format lock at creation. [#24], [#29], [#30]
5. **Retain records needed to explain work.** Accounts are deactivated, preserving their annotations and attribution. Items may be retired during setup before the first assignment, or through the flagged-item exclusion exception in rule 6, with their records retained. An unused label may be deleted only before the project's first assignment; deletion never cascades into annotations. An entire incomplete project may still be deleted with the explicit loss confirmation in [#24], but a completed project cannot be deleted. [#31], [#25], [#26]
6. **Flagged-item exclusion is allowed only before completion.** Retiring a flagged item excludes it from the dataset, export and earnings across its splits, while retaining the item, split memberships, annotations and provenance. It never permits importing/replacing source material or moving items between assigned splits. COMPLETE forbids changing exclusions or any flag disposition. [#35]
7. **Reproducibility.** Seeded splits and a recorded resolution rule mean a dataset can be regenerated and explained months later. [#28]
8. **Everything is local.** No network calls in core flows; the only exceptions are the stretch email features [#39], [#40].
9. **Completion seals project data and releases earnings permanently.** Earnings for eligible submitted work are released only when an adjudicator marks the whole project `COMPLETE` ([#46]); drafts or unsubmitted work do not become payable merely because the project completes. COMPLETE blocks all project writes, including configuration, corpus, taxonomy, split/assignment changes, annotations/boxes, returns, flags/exclusion/repair, resolutions and project deletion. Viewing and export remain available. Completion may still be confirmed with unfinished work, which then stays read-only; there is no reopen. Earnings remain derived from the sealed inputs, never stored, and account deactivation cannot remove attribution or released earnings. [#20], [#24]
10. **Classification resolution follows the taxonomy kind.** For single-select classification, with *k* valid submitted annotations, a label resolves automatically only if it holds a strict majority; otherwise the item is a dispute, including a flat tie, and goes to [#34]. For scale classification, *k* valid submitted integer ratings within the configured range always resolve to their arithmetic mean, retaining fractional results. Each of the *k* independent annotators contributes one answer; fewer than *k* valid submitted answers leaves the item unresolved. Scale tolerance and automatic scale-disagreement detection are deferred beyond v1.0.0. Averaging does not establish annotator agreement. The contributing annotations and resolution method are retained for provenance. [#27]
11. **One shared SQLite file on a shared drive.** The team shares a single `arbiter.db` over a shared drive; there is no package exchange and no merge path. Arbiter takes a workspace lock so only one instance writes at a time ([#1]).
12. **Adjudicators own credentials, and email is deferred.** Accounts are a username and a password, issued and reset by an adjudicator ([#31], [#23]). Email verification and email reset ([#39], [#40]) are out of scope unless time remains.
13. **Adjudicator corrections respect lifecycle locks.** Before COMPLETE, adjudicators may return submitted assignments for annotator correction under [#17], resolve answers under [#27]/[#34], and review or repair flagged annotations under [#35]. These actions do not unlock the corpus, taxonomy or assigned split definition. Detection resolution remains limited to selecting a complete submitted box set (rule 16); repairs cannot replace frozen source material or taxonomy. Return/repair invalidation of affected resolutions and account replacement still need explicit rules in their owning issues; this decision does not implement them.
14. **Each split's definition locks on its first assignment.** Its membership/order, *k*, reward and other setup settings cannot change afterwards, even if current assignments are removed. Before COMPLETE, normal assignment/work states may still change, and never-assigned splits may be configured from the frozen corpus without changing assigned splits. Flag exclusion retains the existing membership records (rule 6). First-assignment locks must persist with the assignment and survive restart. [#28], [#32]
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
