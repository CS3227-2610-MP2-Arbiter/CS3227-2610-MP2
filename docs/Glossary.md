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
| **Split** | A work batch generated automatically from the corpus by item count, which is what gets assigned. Its definition, including membership and *k*, locks permanently on its first assignment. |
| **Assignment** | A permanent link between one split and one annotator: no unassignment, transfer or individual deletion, even before work starts. One split can go to several annotators up to its fixed *k*. The assignment becomes SUBMITTED automatically when all non-retired required items have permanent answer/report submissions; there is no separate batch-submit or return. |
| **Annotation** | One annotator's answer for one item: a label, an integer scale rating or labelled boxes, with a rationale. |
| **Submitted answer** | An annotator's answer permanently recorded by Submit & next, including its rationale and boxes where applicable. The current draft is editable before submission; afterwards neither role can rewrite it. |
| **Bounding box** | One labelled rectangle drawn on a detection item, in image coordinates. |
| **Box set** | All labelled bounding boxes in one annotator's annotation of one image. Detection resolution selects one complete submitted box set as the final answer. |
| **Flag** | An annotator's report of unusable source material, with a reason and optional comment. It may accompany an answer or be a terminal report-only outcome without a valid answer; the latter is never a vote, zero rating or empty detection set. Reporter content locks on submission; an adjudicator later records a keep/exclude disposition. |
| **Resolved answer** | The final answer for an item: a label (the *resolved label*), an averaged numeric rating, or a selected complete box set. |
| **Resolution** | The record of how an item was settled: its final answer, how it was reached, and who decided when applicable. How it was reached is one of the values of `ResolutionMethod`; the rules for each task type and taxonomy kind are in [User Flows](UserFlows.md#3-rules-both-tracks-share). |
| **Label agreement** | For single-select classification, the average across eligible items of the fraction of annotator pairs whose original submitted labels match. It measures label consistency, not correctness or agreement with the resolved answer. Scales and detection show N/A; eligibility is defined in [User Flows rule 20](UserFlows.md#3-rules-both-tracks-share). |
| **Provenance** | Immutable submitted answers/report-only outcomes and their attribution, the current resolution and contributors, and current flags/dispositions/review metadata. Drafts are clearly distinguished; no revision timeline or return metadata is required. See [User Flows](UserFlows.md#3-rules-both-tracks-share). |
| **Workspace** | The ordinary folder both roles use on the same computer or shared disk: one `arbiter.db`, plus `media/`, `exports/` and `logs/`. Source images and text must already be inside `media/` before import. The folder can have any name; `ArbiterFolder` is only an example. |
| **Registration in place** | Recording an existing source file's location and content hash as a project item, without copying, moving or editing the file. V1 accepts only files within the workspace's `media/` folder. |

*k* is the number of annotators who see each item, set on the split before its first assignment and defaulting to 2. Every item is annotated independently by *k* annotators, and annotators never see each other's work. Their submitted answers are the inputs to the resolution rule for the project's task type and taxonomy kind.

## Fixed value sets

Where a term above has a fixed set of values, they are listed here. Adding a value is a schema change, so these are deliberately short.

| Set | Values | Meaning |
| --- | --- | --- |
| **Role** | `ANNOTATOR`, `ADJUDICATOR` | Which side of the workflow an account works on, fixed at creation. The setup account is the workspace's sole adjudicator; every later account is an annotator. |
| **Task type** | `CLASSIFICATION`, `DETECTION` | Whether an annotator picks a label or draws boxes. Fixed when the project is created. |
| **Source type** | `IMAGE`, `TXT` | Whether an item is an image or a text document. Fixed when the project is created. |
| **Taxonomy kind** | `SINGLE`, `SCALE` | Whether a label set is a pick-one list or a numeric scale. |
| **Split strategy** | `BY_COUNT` for v1 | Automatic batching by item count with one seeded shuffle. Proportion, manual and stratified allocation are deferred; obsolete enum values still await model cleanup. |
| **Output format** | `CSV`, `JSON`, `COCO` | The on-disk format the finished dataset is written in. Only the exporter writes these. |
| **Assignment status** | `NOT_STARTED`, `IN_PROGRESS`, `SUBMITTED` | Progress through the forward-only queue; SUBMITTED follows automatically when all non-retired required items are handled. The obsolete `RETURNED` value and return fields still await model cleanup. |
| **Resolution method** | `MAJORITY`, `ADJUDICATED`, `AUTO_SCALE` | How an item's final answer was reached: strict-majority label, adjudicator decision or arithmetic mean of scale ratings, respectively. Recorded for provenance. |
| **Flag reason** | `CORRUPT_OR_UNREADABLE`, `WRONG_CONTENT`, `TOXIC_OR_SENSITIVE` | Why an annotator reported an item's source material as unusable. |
| **Flag disposition** | `PENDING`, `EXCLUDED`, `KEPT` | Current adjudicator review state. Exclusion retires the item from work and export while retaining evidence; KEEP never invents a missing answer or reverses item retirement. The obsolete `REPAIRED` value still awaits model cleanup. |
| **Account status** | `ACTIVE`, `DISABLED` | Whether an account may be used. Annotators may be disabled while retaining their answers and attribution; the sole adjudicator stays active. |

See [User Flows](UserFlows.md) for the shape of both flows and the rules that follow from these terms.

[Back to home](index.md)
