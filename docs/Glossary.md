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
| **Split** | A batch of items cut from the corpus. |
| **Assignment** | The link between one split and one annotator. One split can go to several annotators. |
| **Annotation** | One annotator's answer for one item: a label, a rationale, optional boxes. |
| **Bounding box** | One labelled rectangle drawn on a detection item, in image coordinates. |
| **Flag** | An annotator's report that an item's source material is unusable, with a reason and an optional comment. A reason is one of the values of `FlagReason`. |
| **Resolved label** | The final answer for an item after adjudication. |
| **Resolution** | The record of how an item was settled: its resolved label, how it was reached, and who decided. How it was reached is one of the values of `ResolutionMethod`. An item with no strict majority stays a dispute until an adjudicator decides it. |
| **Workspace** | The folder a team's data lives in: one `arbiter.db`, plus `media/`, `exports/` and `logs/`. |

*k* is the number of annotators who see each item, set per assignment and defaulting to 2. Every item is annotated independently by *k* annotators, and annotators never see each other's work. That is what makes "resolve by highest count" meaningful, and it is why the queue is blind.

## Fixed value sets

Where a term above has a fixed set of values, they are listed here. Adding a value is a schema change, so these are deliberately short.

| Set | Values | Meaning |
| --- | --- | --- |
| **Role** | `ANNOTATOR`, `ADJUDICATOR` | Which side of the workflow an account works on. |
| **Task type** | `CLASSIFICATION`, `DETECTION` | Whether an annotator picks a label or draws boxes. Fixed when the project is created. |
| **Source type** | `IMAGE`, `TXT` | Whether an item is an image or a text document. Fixed when the project is created. |
| **Taxonomy kind** | `SINGLE`, `SCALE` | Whether a label set is a pick-one list or a numeric scale. |
| **Output format** | `CSV`, `JSON`, `COCO` | The on-disk format the finished dataset is written in. Only the exporter writes these. |
| **Assignment status** | `NOT_STARTED`, `IN_PROGRESS`, `SUBMITTED` | Where an assignment has got to. |
| **Resolution method** | `MAJORITY`, `ADJUDICATED`, `AUTO_SCALE` | How an item's final label was reached, recorded for provenance. |
| **Flag reason** | `CORRUPT_OR_UNREADABLE`, `WRONG_CONTENT`, `TOXIC_OR_SENSITIVE` | Why an annotator reported an item's source material as unusable. |
| **Account status** | `ACTIVE`, `DISABLED` | Whether an account may be used. Deactivation is a soft delete, so a disabled account's annotations remain history. |

See [User Flows](UserFlows.md) for the shape of both flows and the rules that follow from these terms.

[Back to home](index.md)
