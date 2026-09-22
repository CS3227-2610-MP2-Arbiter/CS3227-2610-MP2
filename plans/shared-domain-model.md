# Shared domain model and repository interfaces

**Issue:** [#4] - Agree the shared domain model and repository interfaces
**Branch:** `feat/shared-domain-model`
**Status:** Superseded for V1 by the approved scope reduction in [#62]. This file remains a historical record of the earlier model decisions and is not an active implementation contract.

What each class and field means, and why it has that shape, is in its Javadoc; the conventions all model classes follow are in [context/architecture.md](../context/architecture.md#model). This plan records what was decided along the way and what is still open. Most decisions came from Whimsyturtle's review of the first revision.

## Decisions

- ***k* lives on `Split`**, not `Assignment`, as the reviewer asked.
- **`BoundingBox`, `Flag` and `SplitItem` are records of their own.** Boxes need per-box provenance ([#36]), per-box export records ([#37]) and a stable order ([#15]). Flags are listed by reason ([#35]) and shown per item ([#36]). Membership names items rather than copying them.
- **A scale answer is a whole number and a resolved scale is fractional.** `Annotation.scaleValue` is an `Integer` and `Resolution.scaleValue` a `Double`.
- **Taxonomy kind and scale range live in `TaxonomySettings`**, not on `Project` or `Label`. Multi-select was dropped.
- **Extra repositories.** `ResolutionRepository` and `FlagRepository` exist because [#27], [#34], [#35] and [#36] read and write these records directly; adding them later is the drift [#4] exists to prevent, and routing them through `ItemRepository` would be worse. `SplitItemRepository` and `TaxonomySettingsRepository` were added in review, because split membership and taxonomy settings otherwise had no way to be loaded or saved.
- **`AccountStatus` stays** for annotator deactivation.
- **Subpackages by area** (`user`, `project`, `annotation`, `resolution`) in both `arbiter.model` and `arbiter.data`.
- **No ORM annotations** on model classes; the mapping is designed in [#6] alongside the schema.
- **Collection-returning repository methods are named `list*`**; `findAll()` became `listAll()`.
- **Cross-repository transactions belong to [#6]**, so this change adds no transaction API.
- **Aggregate statistics queries are left to [#19] and [#33].** The interfaces have only the lifetime valid-answer count; per-project, per-day and agreement aggregates are added with those features.
- **Later product-scope decisions** (forward-only submission, fixed assignments, count-only batches, workspace media, one fixed adjudicator, deferred money, kept statistics, averaged scales, detection by selection, and the setup freeze and completion seal) are recorded in [User Flows](../docs/UserFlows.md) and the issues it links to. The model changes they required are listed below.

### Removed or replaced during review

- **Enum constants:** `AccountStatus.PENDING`, `SourceType.MD`, `TaxonomyKind.MULTI`, `ResolutionMethod.GOLD`, `FlagReason.DUPLICATE`/`OFF_TOPIC`/`INSTRUCTIONS_UNCLEAR`, `AssignmentStatus.RETURNED` and `FlagDisposition.REPAIRED`.
- **Fields:** `User.email` (now `username`); `Label.name` and `Label.colour`; `Label.active`, which made a new label look deleted; `Label.guideline`, which `description` covers; `Project.rationaleRequired`, since the rationale is always optional; `Annotation.timeSpentMillis`; `Annotation.submitted` (now `submittedAt` and `reportOnly`); `Resolution.unresolved`, since a resolution exists only once an item is settled; `Flag.excluded`, which `Item.retired` covers; the `Assignment` return metadata; `Split.itemReward`; the allocation-strategy field and type; and `Project`'s reference to its `TaxonomySettings`, so the link runs one way.
- **Methods:** `UserRepository.countByRole` became `countActiveByRole` because it counted disabled accounts. `AnnotationRepository.countSubmittedByAnnotator` became `countValidSubmittedByAnnotator`. `SplitItemRepository.listByItem` became `findByItem`, since an item is in at most one split (rule 7), and so `AnnotationRepository.listByItemAndAnnotator` became `findByItemAndAnnotator`. `ResolutionRepository.listUnresolvedByProject` and `countUnresolvedByProject` were removed with that field; [#34] adds its own query for items awaiting the adjudicator. The individual answer, assignment and split-membership deletes were removed, and label deletion no longer cascades.
- **Added:** `SplitItem`, `Split.seed`, `Split.requestedBatchSize`, `TaxonomySettings.id`/`projectId` and its repository, `Resolution.selectedAnnotationId`, and `Flag.disposition`/`reviewedAt`.

## Open questions

- **Timestamps.** The model uses `java.time.Instant`. ORMLite has no built-in `Instant` mapping, so [#6] either registers a persister or this switches to a type it maps natively.
- **`equals` / `hashCode`.** Not defined yet. Entities compare by identity today; say if value equality is wanted.
- **Resume position.** [#13] and [#17] need the queue position to survive a restart, but the model has no cursor. Either derive it from saved split order and submitted outcomes, documented and tested, or add one. No skip history is needed.

## Verification

| Acceptance criterion | How it is verified |
| --- | --- |
| Model classes and interfaces compile under `arbiter.model` and `arbiter.data` | `./gradlew check` compiles them and runs Checkstyle |
| Scalar getters reject conflicting fields without mutating them | Temporary focused JUnit tests covering reflectively loaded conflicting fields and valid label, scale, empty, switching and null-setter cases, since removed at Whimsyturtle's request; restore them with [#11]'s harness |
| Every agreed enum exists with the agreed constants | Review of the enum files against the [Glossary's fixed value sets](../docs/Glossary.md#fixed-value-sets) |
| PR description records *k*, box storage, scale storage | PR description |
| Reviewed by both track owners | GitHub review by zheng-jj and Whimsyturtle |

## Risks

- Anything agreed here is expensive to change later, since both tracks build on it. That is the reason to review it carefully now rather than after [#6].
- Adding `AccountStatus`, `ResolutionRepository` and `FlagRepository` goes slightly beyond the issue text; they are called out above so they can be rejected individually.

[#4]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/4
[#6]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/6
[#11]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/11
[#13]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/13
[#15]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/15
[#17]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/17
[#19]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/19
[#27]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/27
[#33]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/33
[#34]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/34
[#35]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/35
[#36]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/36
[#37]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/37
[#62]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/62
