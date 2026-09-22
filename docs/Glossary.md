---
title: Glossary
---

# Glossary

The terms Arbiter's documentation and issues rely on. Each term uses only the terms above it. Arbiter has [two roles](index.md#the-two-roles), annotator and adjudicator, which [the home page](index.md) defines. The rules that follow from these terms are in [User Flows](UserFlows.md#3-rules-both-tracks-share).

| Term | Meaning |
| --- | --- |
| **Project** | One labelling job, with a task type, source type and output format. |
| **Item** | One annotatable unit: one image, or one plain-text document. |
| **Corpus** | The collection of items imported into a project. |
| **Label** | One answer an annotator can choose. A project's labels form its *taxonomy*, which the adjudicator defines. |
| **Split** | A batch of corpus items, which is what gets assigned. |
| **Assignment** | The link between one split and one annotator who works it. |
| **Annotation** | One annotator's answer for one item: a label, an integer scale rating or labelled boxes, with a rationale. |
| **Submitted answer** | An annotation permanently recorded by Submit & next, including its rationale and any boxes. Before that it is a draft. |
| **Bounding box** | One labelled rectangle drawn on a detection item, in image coordinates. |
| **Box set** | All labelled bounding boxes in one annotator's annotation of one image. |
| **Flag** | An annotator's report of unusable source material, with a reason and optional comment. It either accompanies an answer or is submitted alone as a *report-only outcome*, which holds no answer. |
| **Resolved answer** | The final answer for an item: a label (the *resolved label*), an averaged numeric rating, or a selected complete box set. |
| **Resolution** | The record of how an item was settled: its final answer, how it was reached, and who decided when applicable. |
| **Label agreement** | For single-select classification, the average across eligible items of the fraction of annotator pairs whose original submitted labels match. It measures label consistency, not correctness. [#33](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/33) defines eligibility and display. |
| **Provenance** | The evidence behind an item's current decision: each submitted answer or report-only outcome with its annotator and submission time, the current resolution and its contributors, and current flags with their dispositions. |
| **Workspace** | The folder both roles use, holding one `arbiter.db` plus `media/`, `exports/` and `logs/`. |
| **Registration in place** | Recording an existing source file's location and content hash as a project item, without copying, moving or editing the file. |

*k* is the number of annotators who independently annotate each item in a split. It defaults to 2.

## Fixed value sets

Where a term above has a fixed set of values, they are listed here. Adding a value is a schema change, so these are deliberately short.

| Set | Values | Meaning |
| --- | --- | --- |
| **Role** | `ANNOTATOR`, `ADJUDICATOR` | Which side of the workflow an account works on. |
| **Task type** | `CLASSIFICATION`, `DETECTION` | Whether an annotator picks a label or draws boxes. |
| **Source type** | `IMAGE`, `TXT` | Whether an item is an image or a text document. |
| **Taxonomy kind** | `SINGLE`, `SCALE` | Whether a label set is a pick-one list or a numeric scale. |
| **Output format** | `CSV`, `JSON`, `COCO` | The format the finished dataset is written in. |
| **Assignment status** | `NOT_STARTED`, `IN_PROGRESS`, `SUBMITTED` | Progress through an assignment's queue. `SUBMITTED` means every non-retired item in it has a submitted answer or report-only outcome. |
| **Resolution method** | `MAJORITY`, `ADJUDICATED`, `AUTO_SCALE` | How an item's final answer was reached: strict-majority label, adjudicator decision or arithmetic mean of scale ratings, respectively. |
| **Flag reason** | `CORRUPT_OR_UNREADABLE`, `WRONG_CONTENT`, `TOXIC_OR_SENSITIVE` | Why an annotator reported an item's source material as unusable. |
| **Flag disposition** | `PENDING`, `EXCLUDED`, `KEPT` | The adjudicator's decision on a flag: not yet reviewed, item excluded, or item kept. |
| **Account status** | `ACTIVE`, `DISABLED` | Whether an account may be used. |

[Back to home](index.md)
