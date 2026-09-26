# Text classification annotation UI

**Issue:** [#14] - Text classification annotation UI
**Branch:** `feat/annotation-ui` (from `main` after [#13] merged)
**Status:** Plan approved by the owner on 26 September 2026, after it was implemented at their request to "start what you can", before [#26] exists. Verified locally; human acceptance and teammate review are pending.

What the feature does is in [#14], and what each class means is in its Javadoc. This plan records what was decided and what is still open.

## Goal and scope

Under the queue's current file ([#13]), let the annotator choose one answer from the project's frozen taxonomy: one label for SINGLE, or one whole number within the range for SCALE. Keep **Submit & next** disabled until the choice is valid. The choice lives only on screen.

Non-goals, from [#14]: storing the answer and moving forward ([#17]), multi-select, flags, rationale and drafts.

## Building before [#26]

[#26] is the adjudicator's screen for setting labels and the scale range. It writes them into `Label` (key, description, order) and `TaxonomySettings` (kind, minimum, maximum), which already exist, and #14 only reads those. So #14 is built and tested against taxonomies the #11 fixtures seed. The only thing that waits for [#26] is using it in the running app. Until then, a project has no labels or range, and the editor says so instead of offering an answer.

## Proposed changes

- **`Taxonomy`** in `arbiter.service` is a record of the project's kind, its labels in order (`LabelOption`: identifier, key, description) and its scale range. It owns the answer rules, so the screen and [#17]'s enforcement apply the same ones:
  - `accepts(labelId)` is true only for one of the project's labels.
  - `parseRating(text)` accepts ASCII digits with an optional leading minus sign, once surrounding whitespace is stripped, within the inclusive range. It returns empty for anything else, including a number too large for an `int`. ASCII only follows the architecture rule for text the code compares.
  - `answerable()` is false for SINGLE with no labels, or SCALE with no range.
- **`QueueView`** gains the `Taxonomy`, read with the assignment in the same snapshot. Nothing about a taxonomy is private to an annotator.
- **`Answer`** is a record of the chosen label or rating: exactly one of the two.
- **`AnnotationEditor`** in `arbiter.ui.shared`, where the architecture puts it, builds the controls from a `Taxonomy`:
  - SINGLE: one radio button per label, showing its key and, when present, its description. The text wraps, per the "no text is cut off" rule.
  - SCALE: a whole-number field with its range as a hint, and an inline message when the text is not a valid rating.
  - It exposes whether the choice is valid and the current `Answer`, and holds nothing else. Nothing is written until [#17] submits. It takes plain values, so the adjudicator's dispute screen ([#34]) can reuse the label picker.
- **`QueueScreen`** shows the editor under the file, and a **Submit & next** button bound to the editor's validity. The button is also disabled for an unreadable file or an unanswerable taxonomy. Until [#17], choosing it shows a notice that submitting arrives with [#17].

## Design alternatives considered

| Choice | Alternative rejected | Why |
| --- | --- | --- |
| Answer rules in `Taxonomy`, in the service layer | Validating in the editor | Rules about data live in `arbiter.service`, and [#17] must enforce exactly what the screen allowed |
| Text field with validation for SCALE | A spinner | A spinner cannot show an invalid entry, but [#14] asks that invalid inputs be handled, and a field also works with a range of 21 values |
| Taxonomy read with the queue | A separate call | One snapshot keeps the file, position and taxonomy consistent |

## Risks and open decisions

- **No labels until [#26].** In the running app, every project currently has an unanswerable taxonomy, so the editor shows its "not set up yet" message. That can be tested properly only once [#26] lands, or with a seeded workspace.
- **Submit & next is a placeholder until [#17].** If [#17] lands before this merges, the placeholder never ships.

## Verification

| Acceptance criterion | How it is verified |
| --- | --- |
| SINGLE shows one selectable label and accepts exactly one label from the frozen taxonomy | `TaxonomyTest`: a listed label is accepted, and an unlisted one or another project's is refused. The editor uses one toggle group. Human check |
| SCALE accepts one integer within the frozen range | `TaxonomyTest`: the minimum, the maximum and negative values are accepted; one below, one above, fractions, letters, blanks and overflow are refused |
| Label descriptions appear when present | `AnnotationServiceTest`: the queue's taxonomy carries descriptions in label order. Human check |
| Submit & next is disabled until the answer is valid | `QueueScreen` binds the button to the editor's validity. Human check |
| No annotation record exists before submission | `AnnotationServiceTest`: opening the queue leaves `arbiter.json` byte-for-byte unchanged |
| A submitted answer cannot be edited or reopened; no Back, review or resubmission | [#13]'s queue never shows an answered file, and this adds no such control. Review |
| Unit tests cover both kinds and invalid inputs | `TaxonomyTest` |
| `./gradlew check` passes | JDK 25 `./gradlew check shadowJar` |

## Implementation and verification

- Added `Taxonomy` (with its answer rules), `LabelOption` and `Answer` in `arbiter.service`, the taxonomy in `QueueView` (read in the queue's snapshot), `AnnotationEditor` in `arbiter.ui.shared`, and the editor and **Submit & next** in `QueueScreen`. Until [#17], choosing **Submit & next** says submitting is not available yet and that nothing was saved.
- The blindness test needed no change: it walks `AnnotationEditor`, which takes only the taxonomy's plain values.
- `TaxonomyTest` (11 tests) covers labels of the project and outside it, the range ends and one past each, negative ratings, whitespace, fractions, letters, a plus sign, non-ASCII digits, blanks, `null`, overflow, unanswerable taxonomies and `Answer`'s one-of rule. Accepting any rating and accepting any label each made their tests fail.
- `AnnotationServiceTest` adds 4 tests: SINGLE labels in order with descriptions, a SCALE range, a project with no labels yet, and opening the queue leaving `arbiter.json` byte-for-byte unchanged.
- `docs/UserGuide.md` describes choosing an answer.
- JDK 25 `./gradlew cleanTest check shadowJar --no-daemon` passed: 367 JUnit tests, with 3 existing case-sensitivity tests skipped on macOS.

### Human acceptance checks

These need a project with labels or a range. Until [#26], they need a workspace seeded by a test fixture.

1. SINGLE: each label shows as a radio button, its description beneath it, with long descriptions wrapping rather than cut off. **Submit & next** stays disabled until one label is chosen, and only one can be chosen.
2. SCALE: the range shows as a hint. Typing a number outside it, a fraction or letters shows the inline message and keeps **Submit & next** disabled; a valid number enables it.
3. A project with no labels yet says its files cannot be answered, and **Submit & next** stays disabled.
4. An unreadable file shows no answer controls, and **Submit & next** stays disabled.

[#11]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/11
[#13]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/13
[#14]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/14
[#17]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/17
[#26]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/26
[#34]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/34
