# Shared domain model and repository interfaces

**Issue:** [#4](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/4) - Agree the shared domain model and repository interfaces
**Branch:** `feat/shared-domain-model`
**Status:** Initial models/interfaces implemented, awaiting human review; accepted detection, lifecycle, provenance, count-only allocation and forward-only submission follow-ups remain unimplemented. The issue is co-owned by zheng-jj and Whimsyturtle; this plan records accepted scope alongside the remaining model cleanup.

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
4. **`Flag` gets its own table** rather than three nullable columns on `Annotation`. [#35] lists flagged items filterable by reason, and [#36] shows current flags and dispositions, which are both natural queries against a flag table.
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
- **Dropped enum constants.** Earlier review removed `AccountStatus.PENDING`, `SourceType.MD`, `TaxonomyKind.MULTI`, `ResolutionMethod.GOLD`, and `FlagReason.DUPLICATE`/`OFF_TOPIC`/`INSTRUCTIONS_UNCLEAR`. `AssignmentStatus.RETURNED` was removed then restored to satisfy the requirements at that time. The later accepted forward-only decision supersedes those return requirements; removing RETURNED again is now pending model cleanup, together with `FlagDisposition.REPAIRED`.
- **Dropped fields.** `User.email` becomes `username`. `Label.name` and `Label.colour` go, leaving `key` plus `description`. `Annotation.timeSpentMillis` goes. `Flag.excluded` goes, because `Item.retired` already covers exclusion.
- **`TaxonomySettings`.** The scale bounds moved out of `Project` into their own class, so a project is not a bag of optional numbers that matter for one taxonomy kind only.
- **Mutual exclusion.** `Annotation` and `Resolution` each store a label or a scale value. Their setters clear the other, **and so do their getters**, because ORMLite sets fields reflectively and a row loaded from the database would otherwise bypass the setters entirely.
- **`Flag` records the disposition.** Earlier #35 required exclude/repair/keep, so the branch gained `FlagDisposition` and review time. The later forward-only decision defers repair and retains keep/exclude; the current adjudicator disposition/reviewer/time is separate from immutable reporter content. `Item.retired` still determines exclusion from active work/export/earnings.
- **Return metadata is now obsolete.** `Assignment.returnedAt`, `returnedByUserId` and `returnReason` satisfied the earlier rework requirement. The accepted forward-only decision removes that workflow; delete these fields/contracts during model cleanup.
- **One direction per relationship.** `Project` no longer holds a `TaxonomySettings` object, since `TaxonomySettings.projectId` already points back and two directions can disagree. `TaxonomySettingsRepository` was also missing, leaving one entity with no way to load or save it.
- **Scales stay in v1 with arithmetic-mean resolution.** On 21 September, Whimsyturtle chose to retain scales and defer tolerance and automatic scale-disagreement detection. `Resolution.scaleValue` remains a `Double` for fractional means. [#27](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/27) and [User Flows rule 10](../docs/UserFlows.md#3-rules-both-tracks-share) define the rule; this model PR does not implement the service.
- **Detection stays in v1 with selection of one complete submitted box set.** Whimsyturtle chose to retain annotator box editing and COCO export while simplifying adjudication: after *k* valid submitted annotations, select one annotator's complete set for that item. No automatic box matching, combining submissions or editing boxes during resolution. [#34](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/34) and [User Flows rule 16](../docs/UserFlows.md#3-rules-both-tracks-share) define the rule. **Model follow-up remains:** `Resolution` currently stores only a label or scale value; add a persistent relationship identifying the selected annotation for detection, retaining unselected submissions for provenance. This documentation decision does not implement that relationship or the service/export behaviour.
- **Freeze setup after assignment and seal completed projects.** [User Flows rules 3/6/9/13/14](../docs/UserFlows.md#3-rules-both-tracks-share) freeze corpus/taxonomy at the project's first assignment, lock each assigned split's definition and make COMPLETE seal all project data/earnings inputs with no reopen or completed-project deletion. Before completion, forward-only submissions, separate resolution and keep/exclude review continue; the later decision removes returns and repair. Exclusion retains membership/evidence. Never-assigned batches follow count-only creation rules; unassignment never unfreezes setup.
- **Lifecycle implementation follow-up.** Persist first-assignment evidence and enforce locks/sealing transactionally in services/persistence. Mutable POJOs and repository interfaces do not supply those guarantees. Update stale membership/deletion contracts; label deletion cannot cascade into answers. Account replacement and by-reference source integrity remain follow-ups. Replacement cannot edit/reassign the authorship of permanent submissions or create extra independent votes.
- **Keep submitted evidence and the current decision.** The earlier current-only provenance decision remains, simplified by one-way submission: retain every annotator's immutable submitted answer or explicit report-only outcome, unselected evidence, current resolution/contributors or selected detection set/method/decider/time, and current flags/dispositions/review metadata. No withdrawn/replacement submissions, return metadata, answer revisions or historical timelines are required. COMPLETE seals unfinished drafts as drafts.
- **Provenance implementation follow-up.** The former requirement to keep an old submission beside a revised draft is superseded; a current draft becomes one immutable terminal submission. Agree the answer-versus-report-only representation and submission timestamp, preserve original authorship, and identify exact resolution contributors. No source implementation is added by this decision.
- **`Project.outputFormat` is fixed at creation too**, which the review questioned and the docs now state.
- **Repository naming.** Methods returning a collection are `list*`; `findAll()` became `listAll()`, and the same rule applied to every other collection-returning method.
- **`countByRole` was a real bug.** It counted disabled accounts, so the last-adjudicator guard did not hold: two adjudicators could disable each other out of the workspace. It is now `countActiveByRole`, and the comment says why.
- **`Item.path`** documents that the missing-source-file case belongs to media resolution.
- **Count-only work batches; defer training/validation/test partitioning.** Whimsyturtle chose automatic batches by items-per-batch count, using one defined seeded shuffle and stable input ordering. Persist the actual seed, requested batch size and generated membership/order, which remains authoritative on reopen. Identical inputs/count/seed reproduce an allocation; rebuilding past corpus states or deleted batches is outside v1. [#28](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/28) defines creation from available items and replacement of never-assigned batches, with no percentage sizing, manual picking/moving or stratification. [#37](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/37) retains CSV/JSON/COCO export with current provenance but defers training/validation/test subsets. **Implementation follow-up remains:** remove the obsolete `BY_PROPORTION`/`MANUAL` model contract (or simplify away the now-single-mode strategy field), represent requested batch size, and update `Split`/`SplitItem` comments. No allocator, persistence or exporter behavior is implemented by this documentation change.

- **Forward-only submission replaces review/rework.** Whimsyturtle accepted editable autosaved current drafts followed by explicit Submit & next, permanently locking the item and advancing. No Back, review pass, arbitrary jump, skip-and-return, final batch-submit, return/resubmission or direct adjudicator repair. Assignment SUBMITTED follows automatically when all non-retired requirements are handled, including after exclusion removes the final outstanding item. [#13](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/13), [#16](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/16) and [#17](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/17) own the queue/outcome contract. Keep bulk keep/exclude; it was not cut. **Implementation follow-up:** remove obsolete return/repair values/fields, represent a terminal report without a valid answer, persist submitted time and resume position, and provide atomic/idempotent submission/advancement/completion. A cursor or a proven deterministic resume derivation is still needed; no skip history is required. Existing getters/model defects remain independent work.

### Second review round

- **`Split` gained `SplitItem`, `SplitStrategy` and `seed`.** The reviewer spotted that a split had no way to record which items it held. Membership is now its own record; later accepted decisions limit creation to automatic count-based batches and replacement to never-assigned batches. Flag exclusion preserves those records. The later rule 7 clarification makes saved membership/order authoritative and bounds seed reproducibility to identical inputs/count/seed; the original multi-strategy contract is pending cleanup.
- **`Label.active` was removed.** It made a new label look deleted, and it also contradicted rule 5, which permits deletion of unused setup labels. The later lifecycle decision prohibits label deletion after assignment and removes the annotation cascade.
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
