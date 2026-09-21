# Shared domain model and repository interfaces

**Issue:** [#4](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/4) - Agree the shared domain model and repository interfaces
**Branch:** `feat/shared-domain-model`
**Status:** Implemented, awaiting human review. The issue is co-owned by zheng-jj and Whimsyturtle and says to agree this "together, on one screen", so the decisions below still need both owners' sign-off.

## Result

`./gradlew check` passes. 12 model classes, 11 enums and 12 repository interfaces (60 method signatures) compile under `arbiter.model` and `arbiter.data`, with no SQL, no ORM annotations and no dependency on a database. `.gitignore` also needed anchoring: its `data/` pattern was matching `src/main/java/arbiter/data/` and would have excluded the package from the commit.

## Goal

Commit the model classes, enums and repository interfaces that both feature tracks code against, so the annotator and adjudicator work can proceed in parallel without constant merge conflicts.

## Non-goals

- Persistence: no SQL, no schema, no migrations. That is [#6](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/6).
- UI, services and any feature behaviour.
- Password hashing ([#7](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/7)).

## Decisions to confirm

The three the issue asks for, plus four that came up while drafting.

1. ***k* defaults to 2, and lives on `Split`.** Revised after review: it was first put on `Assignment`, but *k* describes the work rather than one annotator's link to it, and every annotator on a split sees the same items. The reviewer was right to move it.
2. **`BoundingBox` gets its own table**, not an inline blob on `Annotation`. [#36] needs per-box provenance, [#37] writes per-box records, and [#15]'s box list needs a stable order, so boxes carry an explicit `sequence`.
3. **A `SCALE` answer is stored as a whole number.** `Annotation.scaleValue` is an `Integer`; the range lives on `TaxonomySettings`, not `Project`. `Resolution.scaleValue` is a `Double` to retain fractional arithmetic means. The agreed v1 rule is recorded in the review decisions below and User Flows rule 10.
4. **`Flag` gets its own table** rather than three nullable columns on `Annotation`. [#35] lists flagged items filterable by reason, and [#36] shows flag history, which are both natural queries against a flag table.
5. **Taxonomy kind lives with the project, not on `Label`.** [#29] sets the task type and taxonomy kind at creation and rejects impossible combinations; the scale range is a property of that taxonomy, and now lives in `TaxonomySettings`. Multi-select was dropped in review, so there are no multi-select bounds.
6. **Repositories beyond the seven named in the issue.** `ResolutionRepository` and `FlagRepository`, joined later by `SplitItemRepository` and `TaxonomySettingsRepository`. [#27], [#34], [#35] and [#36] all read and write these directly; without them those issues either add interfaces later, which is the drift this issue exists to prevent, or route through `ItemRepository`, which is worse.
7. **`AccountStatus` added to the enum set.** `User` needs it and [#7](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/7) explicitly requires an account-status extension point. Adding it now avoids [#7] editing a shared model class, which is what this issue exists to prevent.

## Open questions

- **ORM annotations.** `context/architecture.md` says model classes carry ORM mapping annotations, but this issue says to add them "as plain value objects". The draft follows the issue: plain POJOs, no ORMLite annotations, so the mapping is designed in [#6](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/6) alongside the schema instead of being guessed here. If that is wrong, the architecture line needs changing too.
- **Timestamps.** The draft uses `java.time.Instant`. ORMLite has no built-in `Instant` mapping, so [#6](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/6) either registers a persister or this switches to a type it maps natively.
- **`equals` / `hashCode`.** Not defined yet. Entities compare by identity today; say if value equality is wanted.
- **Immutability.** `Project.taskType`, `sourceType` and `outputFormat` are settable, not final, because ORMLite sets fields reflectively. Rule 4 is enforced in `arbiter.service`, per the architecture rule that every rule about the data lives there.

## Proposed changes

- `arbiter.model`: 12 classes (`Annotation`, `Assignment`, `BoundingBox`, `Flag`, `Item`, `Label`, `Project`, `Resolution`, `Split`, `SplitItem`, `TaxonomySettings`, `User`) and 11 enums (`AccountStatus`, `AssignmentStatus`, `FlagDisposition`, `FlagReason`, `OutputFormat`, `ResolutionMethod`, `Role`, `SourceType`, `SplitStrategy`, `TaskType`, `TaxonomyKind`).
- `arbiter.data`: 12 repository interfaces (60 signatures), signatures only.
- No `build.gradle` change.

## Review decisions

Whimsyturtle requested changes on the first revision. The decisions below are his, applied after rebasing on the docs branch; the original numbered list above is kept as the record of what was proposed.

- **Subpackages.** Both `arbiter.model` and `arbiter.data` are split by area - `user`, `project`, `annotation`, `resolution` - mirroring each other.
- **Dropped enum constants.** `AccountStatus.PENDING`, `SourceType.MD`, `TaxonomyKind.MULTI`, `ResolutionMethod.GOLD`, and three of the six `FlagReason` values (`DUPLICATE`, `OFF_TOPIC`, `INSTRUCTIONS_UNCLEAR`). Rationale given: no email verification in v1.0.0, no practical difference between Markdown and plain text, multi-select is extra complexity, and gold standards are unnecessary. `AssignmentStatus.RETURNED` was also dropped here and **has since been restored**: #12 and #17 both require a returned split to be reopened for rework, so it is not optional.
- **Dropped fields.** `User.email` becomes `username`. `Label.name` and `Label.colour` go, leaving `key` plus `description`. `Annotation.timeSpentMillis` goes. `Flag.excluded` goes, because `Item.retired` already covers exclusion.
- **`TaxonomySettings`.** The scale bounds moved out of `Project` into their own class, so a project is not a bag of optional numbers that matter for one taxonomy kind only.
- **Mutual exclusion.** `Annotation` and `Resolution` each store a label or a scale value. Their setters clear the other, **and so do their getters**, because ORMLite sets fields reflectively and a row loaded from the database would otherwise bypass the setters entirely.
- **`Flag` records the disposition.** #35 needs exclude, repair and keep, and #36 needs the disposition shown. Dropping `Flag.excluded` without a replacement left the review queue unable to empty, so `Flag` now carries a `FlagDisposition` and a `reviewedAt`. Excluding still retires the item, so `Item.retired` remains the single place that decides membership of the dataset.
- **A return is recorded.** Rule 13 says returns are recorded, so `Assignment` gained `returnedAt`, `returnedByUserId` and `returnReason`; without them a returned assignment reads as submitted again with no trace it was ever sent back.
- **One direction per relationship.** `Project` no longer holds a `TaxonomySettings` object, since `TaxonomySettings.projectId` already points back and two directions can disagree. `TaxonomySettingsRepository` was also missing, leaving one entity with no way to load or save it.
- **Scales stay in v1 with arithmetic-mean resolution.** On 21 September, Whimsyturtle chose to retain scales and defer tolerance and automatic scale-disagreement detection. `Resolution.scaleValue` remains a `Double` for fractional means. [#27](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/27) and [User Flows rule 10](../docs/UserFlows.md#3-rules-both-tracks-share) define the rule; this model PR does not implement the service.
- **Detection stays in v1 with selection of one complete submitted box set.** Whimsyturtle chose to retain annotator box editing and COCO export while simplifying adjudication: after *k* valid submitted annotations, select one annotator's complete set for that item. No automatic box matching, combining submissions or editing boxes during resolution. [#34](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/34) and [User Flows rule 16](../docs/UserFlows.md#3-rules-both-tracks-share) define the rule. **Model follow-up remains:** `Resolution` currently stores only a label or scale value; add a persistent relationship identifying the selected annotation for detection, retaining unselected submissions for provenance. This documentation decision does not implement that relationship or the service/export behaviour.
- **`Project.outputFormat` is fixed at creation too**, which the review questioned and the docs now state.
- **Repository naming.** Methods returning a collection are `list*`; `findAll()` became `listAll()`, and the same rule applied to every other collection-returning method.
- **`countByRole` was a real bug.** It counted disabled accounts, so the last-adjudicator guard did not hold: two adjudicators could disable each other out of the workspace. It is now `countActiveByRole`, and the comment says why.
- **`Item.path`** documents that the missing-source-file case belongs to media resolution.

### Second review round

- **`Split` gained `SplitItem`, `SplitStrategy` and `seed`.** The reviewer spotted that a split had no way to record which items it held. Membership is now its own record so items can be moved or withdrawn without rewriting the split, and the seed is stored because rule 7 requires a seeded split to be reproducible.
- **`Label.active` was removed.** It made a new label look deleted, and it also contradicted rule 5, which makes labels the one thing that is genuinely deleted rather than retired.
- ***k* moved from `Assignment` to `Split`**, as above.
- **`TaxonomySettings` gained `id` and `projectId`**, without which ORMLite could not persist it at all.
- **`countSubmittedByAnnotator`'s comment was wrong**, in the same way as `countByRole`: earnings are not derived from that count.

## Verification

| Acceptance criterion | How it is verified |
| --- | --- |
| Model classes and interfaces compile under `arbiter.model` and `arbiter.data` | `./gradlew check` compiles them and runs Checkstyle |
| Every agreed enum exists with the agreed constants | Compile plus review of the enum files against `context/architecture.md` and the issues |
| PR description records *k*, box storage, scale storage | PR description |
| Reviewed by both track owners | GitHub review by zheng-jj and Whimsyturtle |

Automated tests are not added: this issue ships no behaviour, and [#11](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/11) owns the test harness and fixtures.

## Risks

- Anything agreed here is expensive to change later, since both tracks build on it. That is the reason to review it carefully now rather than after [#6](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/6).
- Adding `AccountStatus`, `ResolutionRepository` and `FlagRepository` goes slightly beyond the issue text; they are called out above so they can be rejected individually.
