---
title: User Flows
---

# Arbiter user flows

This is the single source of truth for *who does what, in what order*. Each issue links back to the
step it implements. Where the feature list is ambiguous, this document states the interpretation we
agreed on, so the annotator track and the adjudicator track do not drift apart. Items marked
**stretch** are optional and have their own issue.

## Vocabulary

| Term | Meaning |
| --- | --- |
| **Corpus** | The imported collection of source items (images, `.txt`, `.md`). |
| **Item** | One annotatable unit: one image or one text document. |
| **Split** | A batch of items cut from the corpus and assigned to one or more annotators. |
| **Assignment** | The link between one split and one annotator. |
| **Annotation** | One annotator's answer for one item: a label, a rationale, optional boxes. |
| **Resolved label** | The final answer for an item after adjudication. |
| **Adjudicator** | The admin role: imports, splits, assigns, resolves, exports. |

**The key rule:** every item in a split is annotated independently by *k* annotators, and annotators
never see each other's work. That is what makes "resolve by highest count" meaningful and it is why
the queue is blind. Confirm *k* (default 2) during `A0`.

---

## 0. First run and authentication

| Step | Actor | Detail | Issue |
| --- | --- | --- | --- |
| 0.1 | Adjudicator | Launch the app. No workspace yet, so a setup wizard asks for a folder and creates `arbiter.db`, `workspace.json`, `media/`, `exports/`. | `A5` |
| 0.2 | Adjudicator | The first account created is automatically the adjudicator: email, password, display name. | `A3`, `C9` |
| 0.3 | Anyone | Log in with email and password. Passwords are stored as a salted hash, never plaintext. | `A3` |
| 0.4 | Anyone | Menu, Log out: returns to the login screen and clears the in-memory session. | `A3` |
| 0.5 | Annotator | **stretch** Email verification: a 6-digit code is emailed on registration and the account stays `PENDING` until verified. Needs SMTP, so it is the first thing to drop if time runs short. | `D1` |
| 0.6 | Annotator | Forgot password. Offline path: the adjudicator generates a one-time reset code. **stretch** email path. | `C1`, `D2` |

**Self-service registration is annotator-only.** Anyone may create an annotator account, but an
annotator sees nothing until an adjudicator assigns them a split. Nobody may self-register as an
adjudicator; that role is granted by an existing adjudicator (`C9`). Otherwise anyone could export
the corpus.

---

## 1. Annotator flow (owner: zheng-jj)

```
log in -> home (my splits) -> pick a split -> blind queue -> annotate one item -> next
  |                                                              |
  |                                          label + rationale + [boxes] + flag
  |                                                              |
  +--- earnings <--- vests on submit ---------------------------+
```

### 1.1 Home and assigned splits (`B1`)

After login the annotator lands on **My Splits**: a card per assignment showing project, split name,
progress bar, item counts, per-item reward and status.

- Statuses: `NOT_STARTED`, `IN_PROGRESS`, `SUBMITTED`, `RETURNED` (adjudicator sent it back).
- Splits the annotator is not assigned to are absent, not greyed out.
- Sorting: in-progress first, then most recently assigned.

### 1.2 The blind queue (`B2`)

Opening a split enters the queue: one item at a time, no list view, no jumping to arbitrary indexes.

- **One item, one decision.** Next moves forward; previous reopens the annotator's own earlier
  answer for editing and never reveals anyone else's.
- **Never show another annotator's annotation, the resolved label, or any per-item agreement stat.**
  This is the core invariant and `A7` tests it.
- **Resume:** closing the app mid-item restores the exact position and any saved partial input.
- **Skip** with a reason; skipped items return at the end of the split, and a split cannot be
  submitted while items are skipped.
- Header shows `Item 7 of 120` and elapsed time on the current item.

### 1.3 Classification tasks (`B3`)

For a `CLASSIFICATION` project the annotator picks from the taxonomy, which has three shapes:

- **Single-select** (`SINGLE`): radio buttons.
- **Multi-select** (`MULTI`): checkboxes, with optional min/max.
- **Numeric scale** (`SCALE`): slider or spinner over an integer range with optional end labels,
  e.g. `1 = clearly safe` to `5 = clearly unsafe`.

Plus a **rationale** box (required when the adjudicator marked it so) and an **unclear** flag.

### 1.4 Detection tasks (`B4`)

For a `DETECTION` project the annotator draws boxes on the image.

- Draw by drag; boxes snap to the image and are clamped to its bounds.
- Select a box to change its label, edit via handles or arrow keys, `Delete` to remove.
- Zoom and pan; box coordinates must stay correct at any zoom level.
- A box list panel shows every box with its label in a deterministic order.
- Every box carries a label from the taxonomy.
- Optional, only if time allows: copy the previous item's boxes forward for near-identical items
  such as video frames.

### 1.5 Flagging bad source material (`B5`)

The **unclear** flag is the feedback channel for low-quality source material. Flagging opens a
required reason dropdown:

`CORRUPT_OR_UNREADABLE`, `WRONG_CONTENT`, `DUPLICATE`, `OFF_TOPIC`, `TOXIC_OR_SENSITIVE`,
`INSTRUCTIONS_UNCLEAR`

plus a free-text comment. Flagged items are still submitted and the annotation is still saved, but
they surface in the adjudicator's **Flagged items** tab (`C13`) for exclude/repair. The flag
propagates into the export so training-set builders can drop those rows.

### 1.6 Saving and submitting (`B6`)

- **Every answer autosaves** on change. Save is not a manual action; a power cut must not lose work.
  This is the whole reason for the write-through store in `A2`.
- A per-item **Submit** advances the queue. Submitted items lock unless the adjudicator returns them.
- **Submit split** appears once every item is annotated or flagged. It sets `SUBMITTED`, stops the
  item counting as remaining, and vests the earnings.
- Annotators persist the **canonical** annotation only; they never choose a file format. The
  adjudicator's exporter (`C15`) turns canonical records into COCO/YOLO/Pascal/CSV/JSON. Building
  formatting logic twice is the one mistake to avoid here.

### 1.7 Progress, earnings, income statement (`B7`-`B10`)

- **Current progress** (`B7`): on each split card and in a session strip: done/total, remaining,
  flagged count, count this session, average seconds per item.
- **Total completed** (`B8`): a lifetime counter on the home screen broken down by project, plus a
  per-day bar for the last 14 days.
- **Money made** (`B9`): earnings = sum of accepted items x per-item reward, vesting **on submit**.
  Shown as total earned, pending (submitted, not yet resolved), and this period. Money is a
  **derived** value, never a stored balance, recomputed from submitted annotations so it cannot drift.
- **Export income statement** (`B10`): a CSV with one row per submitted annotation (date, project,
  split, item, reward, status) plus a summary by project and a total. Written to a user-chosen path
  with a sensible default filename.
---

## 2. Adjudicator flow (owner: Whimsyturtle)

```
create project -> import corpus -> define taxonomy -> split -> assign
                                                                |
                              monitor progress -> resolve conflicts -> export
```

### 2.1 Create a project (`C2`)

Name, description, **task type** (`CLASSIFICATION` or `DETECTION`), **source type** (`IMAGE`, `TXT`,
`MD`) and **output format**.

Task type and source type are immutable after creation because every downstream record assumes them.
The project is the unit everything hangs off: splits, taxonomy, assignments, exports.

### 2.2 Import the corpus (`C3`)

Pick a folder or individual files; Arbiter walks it and registers one item per supported file,
storing a content hash so re-imports do not duplicate.

- Supported: images (`png`, `jpg`, `jpeg`, `bmp`, `gif`) and text (`txt`, `md`).
- Preview and a dry-run summary (found / supported / skipped / duplicated) **before** anything is
  written. Importing 10 000 files the wrong way is unrecoverable in a UI with no undo.
- Files are registered by reference plus content hash rather than copied by default, so a 20 GB
  corpus does not double. Offer "copy into workspace" as a checkbox for portability. `A6` owns the
  path handling that keeps this working when a source file later moves.
- Unsupported files are listed, not silently dropped.

### 2.3 Define and evolve the taxonomy (`C4`)

The adjudicator owns the labels: create, rename, reorder, retire, **merge** and **split**.

- A label has a name, a short key, a colour, and optional description and guideline text.
- **Merge** two labels into one and **split** one into several both show an impact preview
  ("14 annotations will be remapped") and require confirmation. Existing annotations are remapped,
  never orphaned.
- Retire rather than delete when annotations reference a label, so history stays readable.
- Taxonomy edits are recorded so an export can explain what a label meant at the time.

### 2.4 Split the corpus and set rewards (`C6`)

- **By count** (N per split), **by proportion** (70/20/10) or **manual** (drag items between splits).
- **Random or stratified** allocation with an optional seed, so a split is reproducible.
- Optional but cheap once splitting exists: reserve items duplicated across splits as **gold
  standards**, the cleanest way to measure annotator quality.
- Per-item reward per split, defaulting from the project. Splits can be added, renamed, emptied and
  deleted before assignment; after assignment they lock except for the reward.

### 2.5 Task type and output format (`C7`, `C8`)

- **Task type** (`C7`): `CLASSIFICATION` or `DETECTION`, plus the taxonomy input shape
  (`SINGLE` / `MULTI` / `SCALE`). Validate the combination at creation time: a `SCALE` taxonomy with
  a `DETECTION` project is rejected then, not at export time.
- **Output format** (`C8`): `COCO`, `YOLO`, `Pascal VOC`, `CSV` or `JSON`, stored on the project and
  used by `C15`. The exporter must refuse gracefully when the data cannot be represented, for
  example detection boxes in a pure-CSV classification export, and say *why* rather than write a
  broken file.

### 2.6 Manage accounts and assign (`C9`, `C10`)

- **Accounts** (`C9`): create annotators directly or approve self-registered ones, promote to
  adjudicator, deactivate. Deactivation is a soft delete so their annotations remain history.
- **Assign** (`C10`): pick a split, pick annotators, set **annotations per item** (*k*, default 2).
  Every item in the split is then queued independently for each of the *k* annotators. This is what
  makes blind agreement measurable and `C5` possible.
- Warn when assigning a split whose items already belong to the same annotator: duplicates produce
  fake agreement.
- Show each annotator's current load before confirming, so nobody gets 3 000 items by accident.
- Unassign only while the annotator has submitted nothing.

### 2.7 Monitor and resolve (`C11`-`C14`, `C5`)

- **Dashboard** (`C11`): per-project progress: items, annotations, agreement rate, unresolved
  conflicts, per-annotator completion.
- **Auto-resolution** (`C5`): for each item with at least two annotations, accept a label with a
  strict majority; ties stay `UNRESOLVED` for manual review. For `SCALE`, accept when the spread is
  within a tolerance (default 1) and average it, otherwise unresolved. Record which rule fired so
  the export's provenance is explainable.
- **Manual resolution** (`C12`): a queue of unresolved items showing the competing annotations side
  by side **without revealing which annotator gave which**, with the item, rationale text and boxes.
  The adjudicator picks a winner or supplies their own label. Keeping the no-names rule is what lets
  annotators write honest rationales.
- **Flagged items** (`C13`): everything annotators flagged, filterable by reason, with exclude /
  repair / keep actions. Excluded items drop out of the export and, per `S2`, out of earnings.
- **Per-item breakdown** (`C14`): the full history of one item: every annotation, who made it
  (adjudicator-only), timestamps, time spent, flag, resolution, and the rule or person that resolved
  it. This is the provenance record.

### 2.8 Export (`C15`)

- Writes the correct on-disk structure for the format: COCO's single JSON with `images`/
  `annotations`/`categories`; YOLO's one `.txt` per image plus `classes.txt`; Pascal's one XML per
  image; CSV/JSON as flat tables.
- **Provenance for every decision**: each row carries the resolved label, how it was resolved
  (`MAJORITY`, `ADJUDICATED`, `GOLD`, `AUTO_SCALE`), the timestamp, the adjudicator, and every
  contributing annotation with its rationale and flag. Ship this as a `provenance.csv`/JSON side
  file regardless of format; it is what makes the dataset defensible.
- Options: include unresolved, include flagged, train/val/test split of the resolved set.
- Preview the output tree and item counts, then write to a chosen folder and report what was written
  and what was skipped.

---

## 3. Flow-level rules

These cut across both surfaces and are where the two tracks can accidentally contradict each other.

1. **Blindness is absolute.** No annotator-visible screen shows another annotator's annotation, any
   resolved label, or a per-item agreement stat. Aggregate progress is fine; per-item is not.
2. **Write-through persistence.** Every annotator action hits the store immediately, never an
   in-memory queue flushed at the end. `A2`.
3. **Taxonomy changes propagate.** Merging or splitting a label remaps existing annotations and
   records the change. `C4`.
4. **Immutable project shape.** Task type and source type lock at creation.
5. **Soft delete everywhere.** Accounts, labels and items are deactivated or retired, never purged,
   so historical annotations stay interpretable.
6. **Exclusion means excluded.** Flagged-and-excluded items leave both the export and the earnings
   calculation.
7. **Reproducibility.** Seeded splits and a recorded resolution rule mean a dataset can be
   regenerated and explained months later.
8. **Everything is local.** No network calls in core flows; the only exceptions are the stretch
   email features `D1` and `D2`.

---

## 4. Screen map

| Screen | Role | Issue |
| --- | --- | --- |
| Workspace setup wizard | first run | `A5` |
| Login | all | `A3` |
| Register (annotator self-service) | all | `A3` |
| Annotator home: My Splits | annotator | `B1` |
| Annotation workspace (classification) | annotator | `B3` |
| Annotation workspace (detection) | annotator | `B4` |
| Progress panel | annotator | `B7` |
| Earnings and income statement | annotator | `B9`, `B10` |
| Project list / create | adjudicator | `C2` |
| Import corpus | adjudicator | `C3` |
| Taxonomy editor | adjudicator | `C4` |
| Split manager | adjudicator | `C6` |
| Annotator management | adjudicator | `C9` |
| Assignment dialog | adjudicator | `C10` |
| Project dashboard | adjudicator | `C11` |
| Conflict resolution queue | adjudicator | `C12` |
| Flagged items | adjudicator | `C13` |
| Item detail / provenance | adjudicator | `C14` |
| Export dialog | adjudicator | `C15` |

[Back to home](index.md)

## 5. Issues

All 45 are on GitHub under the **v1.0.0** milestone. Owners: `B*` is zheng-jj, `C*` is
Whimsyturtle, and `A*`/`S*`/`D*` are shared.

| # | Issue | Owner | Blocked by |
| --- | --- | --- | --- |
| 1 | `S1` Spike: decide how a team shares data while staying offline | shared | — |
| 2 | `S2` Spike: decide when annotator earnings vest | shared | — |
| 3 | `S3` Spike: decide whether the email features are in scope | zheng-jj | — |
| 4 | `A0` Agree the shared domain model and repository interfaces | shared | — |
| 5 | `A1` App shell, navigation and role-based routing | zheng-jj | `A3` |
| 6 | `A2` SQLite schema and repository implementations | Whimsyturtle | `A0`, `A5` |
| 7 | `A3` Register, log in and log out | shared | `A2` |
| 8 | `A4` Shared UI kit and error-handling convention | zheng-jj | `A1` |
| 9 | `A5` Workspace setup and file layout | Whimsyturtle | — |
| 10 | `A6` Media and asset resolution | zheng-jj | `A5` |
| 11 | `A7` Test harness, seed fixtures and blindness test | zheng-jj | `A2` |
| 12 | `B1` Annotator home: view assigned splits | zheng-jj | `A1`, `A3`, `C10` |
| 13 | `B2` Blind annotation queue | zheng-jj | `A2`, `A7`, `B1`, `A6` |
| 14 | `B3` Classification annotation UI | zheng-jj | `B2`, `C4`, `C7` |
| 15 | `B4` Detection annotation UI (bounding boxes) | zheng-jj | `B2`, `A6`, `C4`, `C7` |
| 16 | `B5` Flag low-quality source material | zheng-jj | `B2` |
| 17 | `B6` Autosave annotations and submit a split | zheng-jj | `B2`, `B3`, `B4`, `B5` |
| 18 | `B7` View current progress | zheng-jj | `B1`, `B6` |
| 19 | `B8` View total annotations completed | zheng-jj | `B6` |
| 20 | `B9` View money made | zheng-jj | `B6`, `S2`, `C6` |
| 21 | `B10` Export income statement as CSV | zheng-jj | `B9`, `S2` |
| 22 | `B11` Write the annotator user guide | zheng-jj | `B1`, `B2`, `B3`, `B4`, `B6`, `B9`, `B10` |
| 23 | `C1` Offline password reset by one-time code | Whimsyturtle | `A3`, `C9` |
| 24 | `C2` Create and configure a project | Whimsyturtle | `A1`, `A3` |
| 25 | `C3` Import a corpus | Whimsyturtle | `C2`, `A5`, `A6`, `S1` |
| 26 | `C4` Label taxonomy: create, merge and split labels | Whimsyturtle | `C2`, `A2` |
| 27 | `C5` Automatic conflict resolution | Whimsyturtle | `C10`, `C4`, `C6`, `A7` |
| 28 | `C6` Split the corpus and set rewards | Whimsyturtle | `C2`, `C3` |
| 29 | `C7` Choose task type | Whimsyturtle | `C2` |
| 30 | `C8` Choose output format | Whimsyturtle | `C2` |
| 31 | `C9` Manage annotator accounts | Whimsyturtle | `A3` |
| 32 | `C10` Assign annotators to splits | Whimsyturtle | `C6`, `C9`, `A0` |
| 33 | `C11` Project dashboard and results | Whimsyturtle | `C5`, `C10`, `C12`, `C13` |
| 34 | `C12` Manual conflict resolution | Whimsyturtle | `C5`, `C10` |
| 35 | `C13` Review flagged items | Whimsyturtle | `B5`, `C3` |
| 36 | `C14` Per-item annotation breakdown | Whimsyturtle | `C12`, `C4`, `C13` |
| 37 | `C15` Export the dataset with provenance | Whimsyturtle | `C5`, `C8`, `C12`, `C13`, `C14` |
| 38 | `C16` Write the adjudicator user guide | Whimsyturtle | `C2`, `C3`, `C4`, `C6`, `C10`, `C12`, `C15` |
| 39 | `D1` *Email verification | zheng-jj | `S3`, `A3` |
| 40 | `D2` *Email password reset | zheng-jj | `S3`, `A3`, `C1` |
| 41 | `D3` Demo corpus and smoke-test checklist | zheng-jj | `C15`, `B10` |
| 42 | `D4` Packaging: shadowJar and run scripts | zheng-jj | `D3` |
| 43 | `D5` Developer guide and architecture write-up | zheng-jj | `A0`, `A2`, `C15` |
| 44 | `D6` Agentic SE reflections | zheng-jj | — |
| 45 | `D7` Product site landing page | zheng-jj | `B11`, `C16`, `D5`, `D6` |

All issues live on [GitHub](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/milestone/7); this document is the index, and GitHub is the only copy of the backlog.

[Back to home](index.md)
