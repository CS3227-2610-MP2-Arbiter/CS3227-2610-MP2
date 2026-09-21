---
title: User Flows
---

# Arbiter user flows

Arbiter is an offline desktop app for teams labelling data. This page is the shared context for the project: the shape of the two flows and the rules both tracks must respect. It stays at the level of intent. Screen-by-screen behaviour lives in the GitHub issues, which each step links to. The terms it uses are defined in the [Glossary](Glossary.md).

## Getting started

On first launch a wizard creates the workspace and its sole adjudicator account. That adjudicator creates all later accounts as annotators and supplies their usernames/passwords; an annotator sees nothing until a split is assigned. Forgotten annotator passwords are replaced directly by the adjudicator, with no self-signup or reset-code flow. Rule 12 defines the fixed roles and the unresolved owner-recovery limitation. See [#9], [#7], [#31], [#23].

---

## 1. The annotator flow

An annotator works assigned splits one item at a time, and never sees another annotator's answer.

```
log in -> my splits -> open a split -> blind queue -> annotate one item -> next
                                            |
                     label + rationale + [boxes] + flag
```

1. **Sign in and see my splits.** Each assignment shows its project, progress and status. Splits the annotator is not assigned to are absent, not greyed out. [#12]
2. **Work the blind queue.** One current item at a time in saved order, with no Back, review pass, list view, arbitrary jumps or skip-and-return. Closing the app restores that item's editable draft; reopening never revisits submitted items. [#13]
3. **Annotate the item.** A classification item takes a label from the taxonomy - single-select, or a numeric scale - plus a rationale. A detection item takes labelled boxes drawn on the image. [#14], [#15]
4. **Flag bad source material.** Report unusable material with a reason and optional comment. A terminal report may accompany a valid answer or explicitly contain no answer; an unreadable item never requires an invented label or boxes. Reporter content locks on submission, and report-only outcomes are not votes. [#16]
5. **Save and submit forward-only.** The current draft autosaves and stays editable until an explicit **Submit & next** (or the report-only equivalent) permanently records it and advances. When every non-retired required item is handled, the assignment becomes SUBMITTED automatically. There is no final batch-submit, return, resubmission or direct adjudicator repair. [#17]
6. **Track annotation progress and activity.** Keep per-split progress, session counts and average time per item, lifetime/project valid-annotation totals and the 14-day activity chart. Reports without answers count as handled work, not valid annotations. Reward configuration, earnings and income statements remain deferred beyond v1.0.0. [#18], [#19], [#20], [#21]

## 2. The adjudicator flow

An adjudicator sets a project up, gets it annotated, and turns the result into a dataset.

```
create project -> import corpus -> define taxonomy -> split -> assign
                                                                |
                              monitor progress -> resolve conflicts -> export
```

1. **Create a project.** Name, task type, source type and output format, all fixed at creation because every downstream record assumes them. [#24], [#29], [#30]
2. **Import a corpus.** Place supported images or plain-text files inside the workspace's `media/` folder, then select files or subfolders there before the project's first assignment. Import registers their existing locations and content hashes without copying or moving files, with a dry-run summary and duplicate detection. [#25], [#10]
3. **Define the taxonomy.** Configure label keys/descriptions and order, or the scale range, before the project's first assignment. The taxonomy freezes at that point; later label changes or deletion cannot invalidate existing work. [#26]
4. **Split the corpus and assign.** Choose the number of items per batch, preview the automatically generated sizes, set *k*, then assign annotators. V1 uses one seeded shuffle; percentage sizing, hand-picked batches, stratification and reward configuration are deferred. The first assignment freezes the project's corpus and taxonomy; each split's definition locks on its own first assignment. [#28], [#32], [#31]
5. **Monitor progress.** A dashboard shows items, annotations, unresolved conflicts, flagged items and completion breakdowns. Agreement is measured only for single-select labels; scales and detection show N/A (rule 20). [#33]
6. **Resolve answers.** Single-select labels use strict-majority voting, with disputes decided by an adjudicator who sees the competing annotations without names attached. Numeric scale ratings are averaged automatically. For detection, the adjudicator selects one annotator's complete submitted set of labelled boxes. Flagged items are reviewed here too. [#27], [#34], [#35], [#36]
7. **Complete and export.** Marking the dataset complete permanently seals project data after confirmation of any unfinished work. There is no reopen, and a completed project cannot be deleted. Viewing and export remain available, with provenance explaining each item's current decision. [#46], [#37]

## 3. Rules both tracks share

These cut across both surfaces and are where the two tracks can accidentally contradict each other.

1. **Blindness is absolute.** No annotator-visible screen shows another annotator's annotation, any resolved label, or a per-item agreement stat. Aggregate progress is fine; per-item is not. Agreement statistics are adjudicator-only, including aggregate agreement. [#13], [#18], [#33]
2. **Write-through persistence.** Every annotator action hits the store immediately, never an in-memory queue flushed at the end. [#6]
3. **Corpus and taxonomy freeze at the first assignment.** Before the project's first assignment, configure its source items, labels and scale range. Afterwards there is no import, source replacement, ordinary item removal or taxonomy edit, including label creation/deletion, key/description/order changes, taxonomy-kind changes and scale-range changes. Use a new project for a different corpus or label scheme. The freeze is permanent; account deactivation does not undo it, and assignments cannot be removed or reassigned (rule 19). Flagged-item exclusion before completion is the narrow exception in rule 6. [#25], [#26], [#32]
4. **Immutable project shape.** Task type, source type and output format lock at creation. [#24], [#29], [#30]
5. **Retain records needed to explain work.** Annotator accounts are deactivated, preserving their annotations and attribution; the sole adjudicator cannot be disabled or deleted (rule 12). Items may be retired during setup before the first assignment, or through the flagged-item exclusion exception in rule 6, with their records retained. An unused label may be deleted only before the project's first assignment; deletion never cascades into annotations. An entire incomplete project may still be deleted with the explicit loss confirmation in [#24], but a completed project cannot be deleted. This deletes project-owned database records only; source files in `media/` remain, including files another project may reference (rule 21). [#31], [#25], [#26]
6. **Flagged-item exclusion is allowed only before completion.** Retiring a flagged item excludes it from the dataset and export across its splits, while retaining the item, split memberships, annotations and provenance. It never permits importing/replacing source material or moving items between assigned splits. COMPLETE forbids changing exclusions or any flag disposition. [#35]
7. **Persist the generated work definition.** Create batches automatically by item count using one defined seeded shuffle and stable input ordering. Save the actual seed, requested batch size and resulting membership/order; reopening or assigning a batch uses those saved records. Identical input items, count and seed produce identical batches, but a seed alone does not reconstruct an earlier corpus or deleted batches. Full allocation history is deferred. [#28]
8. **Everything is local.** V1 has no network calls in core flows. Email verification and email reset are deferred beyond v1.0.0 ([#39], [#40]).
9. **Completion permanently seals project data.** COMPLETE blocks all project writes, including configuration, corpus, taxonomy, split/assignment changes, annotations/boxes, flags/exclusion, resolutions and project deletion. Viewing and export remain available. Completion may still be confirmed with unfinished work, which then stays read-only; there is no reopen. Account deactivation preserves attribution. Reward configuration and all earnings behavior are deferred beyond v1.0.0 under [#20] and [#21]; COMPLETE has no payment effect in v1. [#46], [#24]
10. **Classification resolution follows the taxonomy kind.** For single-select classification, with *k* valid submitted annotations, a label resolves automatically only if it holds a strict majority; otherwise the item is a dispute, including a flat tie, and goes to [#34]. For scale classification, *k* valid submitted integer ratings within the configured range always resolve to their arithmetic mean, retaining fractional results. Each of the *k* independent annotators contributes one answer; fewer than *k* valid submitted answers leaves the item unresolved. Scale tolerance and automatic scale-disagreement detection are deferred beyond v1.0.0. Averaging does not establish annotator agreement. The current decision's contributing submissions and resolution method are retained for provenance (rule 17). [#27]
11. **One shared workspace and SQLite file.** Both roles use the same workspace folder, on the same computer or a shared disk, with one `arbiter.db` and the source files under `media/` (rule 21). There is no package exchange and no merge path. Arbiter takes a workspace lock so only one instance writes at a time ([#1]).
12. **One fixed adjudicator per workspace.** The first account created during setup is the sole adjudicator; only that account creates later accounts, all as annotators. Roles stay fixed: no self-signup, promotion/demotion, additional adjudicators or owner transfer/deactivation/deletion. The adjudicator issues annotator usernames/passwords and directly sets replacement annotator passwords ([#7], [#31], [#23]). Replacement preserves identity, status, assignments and attribution; it never reactivates an account or changes submitted work. Passwords are stored only as salted hashes, with no stored-credential display, reset codes or self-service reset. Owner password recovery remains unresolved and is not supplied by the annotator reset flow; bootstrap cannot reopen to bypass owner lockout. Email verification and email reset ([#39], [#40]) are deferred beyond v1.0.0.
13. **Adjudicate without rewriting submissions.** Before COMPLETE, adjudicators resolve answers under [#27]/[#34] and keep or exclude flagged items under [#35]. They cannot edit an annotator's submitted answer, boxes, rationale or report, return work, or supply a replacement submission. Classification decisions remain separate final labels; detection selects one complete submitted set (rule 16). KEEP does not approve a label or fill a missing answer. Exclusion retains evidence but removes the item from active work and export (rule 6). Existing bulk keep/exclude actions remain; repair is deferred.
14. **Each split's definition locks on its first assignment.** Its membership/order, *k* and other v1 setup settings cannot change afterwards. Assignments cannot be removed or reassigned (rule 19). Before COMPLETE, normal assignment/work states may still change, and never-assigned batches may be generated/configured from available items in the frozen corpus under [#28]. Changing membership means deleting never-assigned batches and generating replacements; there is no manual item selection or movement, and retained batches are not changed or duplicated by creation. Retirement after generation retains membership but excludes the item from work and export; assignment uses the remaining non-retired count and rejects batches with none. Flag exclusion follows the same retention rule (rule 6). First-assignment locks must persist with the assignment and survive restart. [#28], [#32]
15. **Export one dataset with provenance.** A dataset may be exported before it is complete, and the result must parse as a proper file of its format. Unresolved items are absent or explicitly marked, never given a wrong label. CSV, JSON and COCO remain supported; training/validation/test partitioning is deferred beyond v1.0.0. [#37]
16. **Detection resolves by selecting one complete submission.** After *k* valid submitted annotations, an adjudicator selects exactly one annotator's complete box set for that item, preserving every box's coordinates and label. There is no automatic box matching, mixing of submissions or box editing during detection resolution, even when submissions appear identical. Until a set is selected, the item remains unresolved. Record the selected annotation, method, decider and timestamp, and retain the other annotators' immutable submitted answers for provenance (rule 17). Export uses exactly the currently selected eligible set. [#34], [#36], [#37]
17. **Provenance keeps immutable submissions and the current decision.** Retain each annotator's one submitted answer or explicit report-only outcome, including unselected evidence, with attribution and submission time; the current resolution and exact contributors/selected detection set, method, applicable decider and time; and current flags/dispositions/review metadata. A draft is not submitted evidence. No retained old submission plus revised draft, withdrawn state, return metadata, answer revisions or historical decision/edit timeline is required. COMPLETE seals the retained records. [#17], [#36], [#37]
18. **Submit and advance atomically.** One transaction validates and locks the current answer/report, records its submission time and advances the queue, marking the assignment SUBMITTED when no non-retired requirement remains. Failure preserves the editable draft; retry after success cannot duplicate work or advance twice. Retirement may remove an outstanding requirement without inventing a submission. Resolution counts valid per-item submissions, even while other items in those assignments remain unfinished; drafts and report-only outcomes never count toward *k*. Submissions cannot later change through either role; exclusion must still remove retired results from active dataset/export reads. No final batch-submit or correction loop exists. [#13], [#16], [#17], [#27], [#34], [#35]
19. **Assignments stay with their original annotators.** Once created, an assignment cannot be unassigned, transferred or individually deleted, even before any work starts. Its split and annotator are fixed; normal forward-only work/status changes remain available before COMPLETE. Previously unused places may still be filled up to the split's fixed *k*, but each existing assignment counts regardless of account status, and the same split/annotator cannot be assigned twice. Disabling an account preserves its draft, submissions and ownership without freeing a replacement place or redistributing unfinished items. Missing answers may leave items unresolved, and the project may still be completed after explicit confirmation of unfinished work. Deleting an entire incomplete project is the separate confirmed action in rule 5, not selective unassignment. [#31], [#32], [#24], [#46]
20. **Keep statistics; measure agreement only for single-select labels.** Progress, session timing and activity charts remain in v1. Label agreement averages each eligible item's proportion of matching annotator pairs, with equal weight per item. Eligible items are non-retired SINGLE items with all required *k* valid submitted answers from distinct assigned annotators and *k* at least 2; drafts, report-only outcomes and incomplete items never supply agreement inputs. Use original labels, not resolved answers, and do not equally average percentages from differently sized splits. Show N/A for an empty eligible set, scales and detection. The precise denominator, examples and display rules are in [#33]. Statistics use retained submissions and submission times; session timing does not require restoring full edit-history logs. [#18], [#19]
21. **Register source files in place under workspace media.** Images and plain-text files must already be inside `<workspace>/media/`; subfolders are allowed. The user places them there, and import records workspace-relative locations and content hashes without copying, moving, renaming or editing source files. External references, optional copying and per-file relocation/relink flows are deferred. Missing, corrupt, changed or out-of-root sources produce a clear error, never a silent replacement, automatic retirement or fabricated answer. Before COMPLETE, the explicit report-only route remains available; source errors do not themselves submit work. The app's seal cannot prevent operating-system edits, so validation remains necessary after completion. Restoring the exact original bytes at their recorded location requires no record changes or reopen. Project deletion never deletes these source files; exports never overwrite them. Detailed path, validation and export-failure rules belong to [#9], [#10], [#25] and [#37].

## 4. Where the work lives

The [v1.0.0 milestone](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/milestone/7) tracks the work scheduled for v1.0.0. The issue map below also keeps deferred and historical decisions visible where they explain the accepted scope.

**Spikes.** Three questions were settled in writing before any feature code, and they are closed: how a team shares data while staying offline ([#1]), when annotator earnings would vest ([#2]), and whether the email features are in scope ([#3]). The [#2] decision is historical and superseded for v1.0.0 because all money tracking is now deferred; the accepted account simplification also defers both email features beyond v1 rather than retaining them as stretch deliverables under [#3].

**Foundation.** Shared groundwork both tracks build on, before either can ship a screen:

- [#4] agrees the model classes and repository interfaces both tracks code against.
- [#6] implements the SQLite schema and repositories, and [#11] the test harness, fixtures and blindness test.
- [#7] covers sole-adjudicator bootstrap, login and logout, and [#31] the adjudicator's creation/deactivation of annotator accounts.
- [#5] is the app shell and role-based routing, and [#8] the shared UI kit and error-handling convention.
- [#9] sets up the workspace and its layout, and [#10] resolves media paths and decodes images.

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
