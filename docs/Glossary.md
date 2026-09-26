---
title: Glossary
---

# Glossary

The terms Arbiter's documentation and issues rely on. Each term uses only the terms above it. Arbiter has [two roles](index.md#the-two-roles), annotator and adjudicator, which [the home page](index.md) defines. The rules that follow from these terms are in [User Flows](UserFlows.md#3-rules-both-tracks-share).

| Term | Meaning |
| --- | --- |
| **Project** | One plain-text classification job, with a taxonomy kind and output format. |
| **Item** | One annotatable plain-text document. |
| **Corpus** | The collection of items imported into a project. |
| **Label** | One answer an annotator can choose. A `SINGLE` project's labels, or a `SCALE` project's range, form its *taxonomy*, which the adjudicator defines. |
| **Split** | A batch of corpus items, which is what gets assigned. |
| **Assignment** | The link between one split and one annotator who works it. |
| **Annotation** | One annotator's submitted answer for one item: a label or an integer scale rating. |
| **Submitted answer** | An annotation permanently recorded by **Submit & next**. An unsubmitted choice exists only in the current screen and is not an annotation. |
| **Resolved answer** | The final answer for an item: a label (the *resolved label*) or an averaged numeric rating. |
| **Unresolved item** | An item with no resolved answer yet, for any reason. |
| **Dispute** | An unresolved single-select item that has *k* submitted answers but no strict-majority label, so it awaits the adjudicator ([rule 10](UserFlows.md#3-rules-both-tracks-share)). |
| **Resolution** | The record of how an item was settled: its final answer, how it was reached, and who decided when applicable. |
| **Provenance** | The evidence exported with an item's current decision: its immutable submitted answers with annotator and submission time, plus the current resolution and its contributors. |
| **Workspace** | The folder both roles use for shared data and source files ([rule 11](UserFlows.md#3-rules-both-tracks-share)). |
| **Registration in place** | Recording an existing source file's location and content hash as a project item, without copying, moving or editing the file. |

*k* is the number of annotators who independently annotate each item in a split. It defaults to 2.

## Fixed value sets

Where a term above has a fixed set of values, they are listed here. Adding a value is a schema change, so these are deliberately short.

| Set | Values | Meaning |
| --- | --- | --- |
| **Role** | `ANNOTATOR`, `ADJUDICATOR` | Which side of the workflow an account works on. |
| **Taxonomy kind** | `SINGLE`, `SCALE` | Whether a label set is a pick-one list or a numeric scale. |
| **Output format** | `CSV`, `JSON` | The format the dataset is written in: a table that opens in a spreadsheet, or structured records for scripts and other tools, respectively. |
| **Assignment status** | `NOT_STARTED`, `IN_PROGRESS`, `SUBMITTED` | Progress through an assignment's queue. `SUBMITTED` means every item in it has a submitted answer. |
| **Resolution method** | `MAJORITY`, `ADJUDICATED`, `AUTO_SCALE` | How an item's final answer was reached: strict-majority label, adjudicator decision or arithmetic mean of scale ratings, respectively. |
| **Account status** | `ACTIVE`, `DISABLED` | Whether an account may be used. |

[Back to home](index.md)
