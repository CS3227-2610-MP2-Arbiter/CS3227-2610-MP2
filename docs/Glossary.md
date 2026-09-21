---
title: Glossary
---

# Glossary

The terms Arbiter's documentation and issues rely on. Each term uses only the terms above it. Arbiter has [two roles](index.md#the-two-roles), annotator and adjudicator, which [the home page](index.md) defines.

| Term | Meaning |
| --- | --- |
| **Project** | One labelling job, whose task type, source type and output format are fixed at creation. |
| **Item** | One annotatable unit: one image, or one plain-text document. |
| **Corpus** | The collection of items imported into a project. |
| **Label** | One answer an annotator can choose. A project's labels form its *taxonomy*, which the adjudicator defines. |
| **Split** | A batch of items cut from the corpus, which is what gets assigned. Its definition, including membership, *k* and reward, locks permanently on its first assignment. |
| **Assignment** | The link between one split and one annotator. One split can go to several annotators. An assignment is submitted by the annotator and may be returned for rework before project completion. |
| **Annotation** | One annotator's answer for one item: a label, an integer scale rating or labelled boxes, with a rationale. |
| **Submitted answer** | The retained snapshot of an annotator's latest submitted annotation. During returned work it remains visible as withdrawn/ineligible while the working draft is edited separately; valid resubmission replaces the snapshot. |
| **Bounding box** | One labelled rectangle drawn on a detection item, in image coordinates. |
| **Box set** | All labelled bounding boxes in one annotator's annotation of one image. Detection resolution selects one complete submitted box set as the final answer. |
| **Flag** | An annotator's report that an item's source material is unusable, with a reason and an optional comment. A reason is one of the values of `FlagReason`, and an adjudicator later gives it a `FlagDisposition`. |
| **Resolved answer** | The final answer for an item: a label (the *resolved label*), an averaged numeric rating, or a selected complete box set. |
| **Resolution** | The record of how an item was settled: its final answer, how it was reached, and who decided when applicable. How it was reached is one of the values of `ResolutionMethod`; the rules for each task type and taxonomy kind are in [User Flows](UserFlows.md#3-rules-both-tracks-share). |
| **Provenance** | The latest submitted answers and their eligibility/attribution, the current resolution and its contributors, current flags/dispositions and latest return metadata. V1 does not retain a full revision timeline; see [User Flows](UserFlows.md#3-rules-both-tracks-share). |
| **Workspace** | The folder a team's data lives in: one `arbiter.db`, plus `media/`, `exports/` and `logs/`. |

*k* is the number of annotators who see each item, set on the split before its first assignment and defaulting to 2. Every item is annotated independently by *k* annotators, and annotators never see each other's work. Their submitted answers are the inputs to the resolution rule for the project's task type and taxonomy kind.

## Fixed value sets

Where a term above has a fixed set of values, they are listed here. Adding a value is a schema change, so these are deliberately short.

| Set | Values | Meaning |
| --- | --- | --- |
| **Role** | `ANNOTATOR`, `ADJUDICATOR` | Which side of the workflow an account works on. |
| **Task type** | `CLASSIFICATION`, `DETECTION` | Whether an annotator picks a label or draws boxes. Fixed when the project is created. |
| **Source type** | `IMAGE`, `TXT` | Whether an item is an image or a text document. Fixed when the project is created. |
| **Taxonomy kind** | `SINGLE`, `SCALE` | Whether a label set is a pick-one list or a numeric scale. |
| **Split strategy** | `BY_COUNT`, `BY_PROPORTION`, `MANUAL` | How a split's items were chosen. Kept so a seeded split can be reproduced. |
| **Output format** | `CSV`, `JSON`, `COCO` | The on-disk format the finished dataset is written in. Only the exporter writes these. |
| **Assignment status** | `NOT_STARTED`, `IN_PROGRESS`, `SUBMITTED`, `RETURNED` | Where an assignment has got to. `RETURNED` means an adjudicator sent it back for rework before project completion. |
| **Resolution method** | `MAJORITY`, `ADJUDICATED`, `AUTO_SCALE` | How an item's final answer was reached: strict-majority label, adjudicator decision or arithmetic mean of scale ratings, respectively. Recorded for provenance. |
| **Flag reason** | `CORRUPT_OR_UNREADABLE`, `WRONG_CONTENT`, `TOXIC_OR_SENSITIVE` | Why an annotator reported an item's source material as unusable. |
| **Flag disposition** | `PENDING`, `EXCLUDED`, `REPAIRED`, `KEPT` | What an adjudicator decided about a flag. `PENDING` is the review queue; `EXCLUDED` retires the item before project completion, so it leaves the export and earnings while its records remain. |
| **Account status** | `ACTIVE`, `DISABLED` | Whether an account may be used. Deactivation is a soft delete, so a disabled account's retained answers and attribution remain available. |

See [User Flows](UserFlows.md) for the shape of both flows and the rules that follow from these terms.

[Back to home](index.md)
