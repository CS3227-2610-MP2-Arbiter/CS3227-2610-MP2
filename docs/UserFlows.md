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
                                     label or scale
```

1. **Sign in and see my splits.** Each assignment shows its project, progress and status. [#12]
2. **Work the blind queue.** One plain-text item at a time in saved order, moving only forward (rule 18). Restarting resumes at the first item without a submitted answer. [#13]
3. **Classify and submit.** Choose one label or an integer scale rating, then use **Submit & next** to permanently record that answer and advance (rule 18). [#14], [#17]
4. **Track progress.** Each assignment shows the basic count and status needed to finish its queue. [#18]

## 2. The adjudicator flow

An adjudicator sets a project up, gets it annotated, and turns the result into a dataset.

```
create project -> import corpus -> define taxonomy -> split -> assign
                                                                |
                              monitor progress -> resolve conflicts -> export
```

1. **Create a project.** Name, description, taxonomy kind and output format; each project is a plain-text classification job (rule 4). [#24], [#30]
2. **Import a corpus.** Synchronously register selected `.txt` files already in the workspace's `media/` folder (rule 21). [#25], [#10]
3. **Define the taxonomy.** Configure the labels, or the scale range, before the first assignment (rule 3). [#26]
4. **Split the corpus and assign.** Choose the number of items per batch, preview the generated batches, set *k*, then assign annotators (rules 7, 14, 19). [#28], [#32], [#31]
5. **Monitor progress.** A dashboard shows basic assignment and resolution counts and statuses. [#33]
6. **Resolve answers.** Review the submitted classifications for each disputed single-select item and record a separate final label under rule 10. [#27], [#34]
7. **Export.** Write the current dataset as CSV or JSON with compact provenance (rule 15). [#37]

## 3. Rules both tracks share

These cut across both surfaces and are where the two tracks can accidentally contradict each other. Feature-specific detail is in the linked issues.

1. **Blindness is absolute.** An annotator sees only their own work and progress, never a resolved label. Another annotator's answers or progress are adjudicator-only, even as totals. [#12], [#13], [#18], [#33]
2. **Persist completed actions.** Each completed logical action commits immediately. In particular, **Submit & next** stores an immutable answer and advances the queue in one transaction; an unsubmitted choice is screen state, not a stored record. [#6], [#17]
3. **Corpus and taxonomy freeze at the first assignment.** The project's first assignment permanently freezes its corpus and taxonomy: no import, source replacement, item removal or taxonomy edit, including label creation/deletion, key/description/order changes and scale-range changes. Use a new project for a different corpus or label scheme. [#25], [#26], [#32]
4. **Keep the project shape fixed.** Taxonomy kind and output format lock at creation, and plain-text classification is V1's only project shape. Labels or the configured scale range freeze under rule 3. [#24], [#26], [#30]
5. **Retain records needed to explain submitted work.** Annotator accounts are deactivated, never deleted, so their annotations and attribution remain. Before the first assignment, an item may be unregistered and an unused label may be deleted; neither operation touches its source file. A project may be deleted only before its first assignment, with the loss confirmation in [#24]; this removes its stored records, not its source files (rule 21). [#31], [#25], [#26]
6. **Deferred for V1.** [#62] is the scope decision for this reserved rule number.
7. **Persist the generated work definition.** Batches are generated automatically by item count from a seeded shuffle, and each item belongs to at most one split. The saved membership and order are the work definition: reopening or assigning a batch reads them and never reruns the allocation. [#28]
8. **Everything is local.** V1 has no network calls in core flows.
9. **Deferred for V1.** [#62] is the scope decision for this reserved rule number.
10. **Classification resolution follows the taxonomy kind.** For single-select classification, with *k* submitted annotations, a label resolves automatically only if it holds a strict majority; otherwise the item is a dispute, including a flat tie, and goes to [#34]. For scale classification, *k* submitted integer ratings within the configured range always resolve to their arithmetic mean, retaining fractional results. Each of the *k* independent annotators contributes one answer; fewer than *k* submitted answers leaves the item unresolved. [#27]
11. **One shared workspace and JSON snapshot.** Both roles use the same workspace folder, on the same computer or a shared disk, with one `arbiter.json` and the source files under `media/` (rule 21). There is no package exchange and no merge path. Arbiter takes a workspace lock so only one instance writes at a time. [#1], [#61]
12. **One fixed adjudicator per workspace.** The first account created during setup is the sole adjudicator; only that account creates later accounts, all as annotators. Roles stay fixed: no self-signup, promotion/demotion, additional adjudicators or owner transfer/deactivation/deletion. The adjudicator issues annotator usernames/passwords and directly sets replacement annotator passwords. Owner password recovery remains unresolved. [#7], [#31], [#23]
13. **Adjudicate without rewriting submissions.** Adjudicators resolve answers under [#27]/[#34], but never edit an annotator's submitted answer, return work, or supply a replacement submission. A resolution is a separate record.
14. **Each split's definition locks on its first assignment.** Its membership/order, *k* and other setup settings cannot change afterwards. Before then, changing membership means deleting the batch and generating a replacement, apart from unregistering an item under rule 5; there is no manual item selection or movement. [#28], [#32]
15. **Export one current dataset with provenance.** Each export must parse as a proper CSV or JSON file and include compact [provenance](Glossary.md) for the current decision. Unresolved items are absent or explicitly marked, never given a wrong answer. [#37]
16. **Deferred for V1.** [#62] is the scope decision for this reserved rule number.
17. **Provenance keeps immutable submissions and the current decision.** Retain the evidence needed for automatic and manual resolution, including submissions that did not determine the current result. Export exposes the compact provenance defined in the [Glossary](Glossary.md); no revision history or decision timeline is required. [#17], [#34], [#37]
18. **Submit and advance atomically.** **Submit & next** validates and permanently records the current answer and advances the queue in one step; failure leaves the current choice editable, and a retry cannot submit twice. Submissions never change afterwards through either role, and there is no Back, return, resubmission or batch-submit. Closing the app discards only the unsubmitted choice; restart derives the next position from saved split order and submitted answers. Resolution counts per-item submissions even while other items in those assignments remain unfinished. [#13], [#17], [#27], [#34]
19. **Assignments stay with their original annotators.** Once created, an assignment cannot be unassigned, transferred or individually deleted, even before any work starts. Unused places may still be filled up to the split's fixed *k*, but every existing assignment counts regardless of account status, and the same split/annotator cannot be assigned twice. Disabling an account preserves its work and ownership without freeing its place or redistributing its items, so some items may stay unresolved. [#31], [#32]
20. **Show only operational progress.** V1 exposes the counts and statuses needed to perform assignments, identify unresolved items and finish the workflow. [#12], [#18], [#33]
21. **Register source files in place under workspace media.** Selected `.txt` files must already be inside `<workspace>/media/`; import records their workspace-relative locations and content hashes without copying them. Arbiter never moves, edits, deletes or overwrites source files, including on project deletion or export. A missing, unreadable or changed source produces a clear error, never a silent replacement or fabricated answer. [#9], [#10], [#25], [#37]

## 4. Where the work lives

The [v1.0.0 milestone](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/milestone/7) tracks the work scheduled for v1.0.0. [#62] records the approved deadline-driven scope reduction; closed feature issues and historical logs are not active V1 requirements.

**Spikes.** Three questions were settled in writing before any feature code, and they are closed: how a team shares data while staying offline ([#1]), when annotator earnings would vest ([#2]), and whether the email features are in scope ([#3]). The [#2] decision is historical and superseded for v1.0.0 because all money tracking is now deferred; the accepted account simplification also defers both email features beyond v1 rather than retaining them as stretch deliverables under [#3].

**Foundation.** Shared groundwork both tracks build on, before either can ship a screen:

- [#4] agrees the model classes and repository interfaces both tracks code against.
- [#6] implements the JSON snapshot and repositories, and [#11] the test harness, fixtures and blindness test.
- [#7] covers sole-adjudicator bootstrap, login and logout, and [#31] the adjudicator's creation/deactivation of annotator accounts.
- [#5] is the app shell and role-based routing, and [#8] the shared UI kit and error-handling convention.
- [#9] sets up the workspace and its layout, [#61] locks it to a single writer, and [#10] resolves and validates source paths.

**Annotator track (zheng-jj).** The V1 work in section 1: [#12] the home and assigned splits, [#13] the blind queue, [#14] classification, [#17] atomic submission and queue advancement, [#18] basic progress, and [#22] the annotator guide. Earnings [#20] and the income statement [#21] are explicitly deferred beyond V1.

**Adjudicator track (Whimsyturtle).** The V1 work in section 2: [#24] creating a project, [#30] output format, [#25] importing a corpus, [#26] the taxonomy, [#28] count-based splitting, [#32] assigning annotators, [#33] basic progress, [#27] automatic resolution, [#34] manual label resolution, [#37] export, and [#38] the adjudicator guide. [#23] adds direct replacement of an annotator's password.

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
[#17]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/17
[#18]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/18
[#20]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/20
[#21]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/21
[#22]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/22
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
[#38]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/38
[#39]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/39
[#40]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/40
[#41]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/41
[#42]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/42
[#43]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/43
[#44]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/44
[#45]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/45
[#61]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/61
[#62]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/62
