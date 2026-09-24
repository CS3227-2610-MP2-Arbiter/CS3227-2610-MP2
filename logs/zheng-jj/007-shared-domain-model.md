# Shared domain model and repository interfaces

Status: Human verified.

## Original request

- The user asked to start with the first issue that other work depends on, after the documentation pull requests were merged.
- The dependency graph was computed from the live GitHub issues: only [#4], [#9] and [#44] had no open blocker, and [#4] heads the longest chain at 16 issues ([#4] to [#45]), so it was chosen.

## Follow-ups, corrections, and reflection

- [#4] is a `type/chore` pairing issue rather than a feature, and it is co-owned by both track owners: "Do it together, on one screen". The implementation is therefore written to be reviewed and rejected decision by decision, not merged on the strength of the code alone. The plan lists seven decisions and four open questions explicitly.
- Four decisions go beyond the issue text: adding `Flag` its own table, adding `AccountStatus`, adding `ResolutionRepository` and `FlagRepository`, and putting `TaxonomyKind` on the project. Each is called out in the plan so it can be rejected individually rather than silently inherited.
- One conflict surfaced while reading the specs: `context/architecture.md` says model classes carry ORM mapping annotations, while [#4] says to add them "as plain value objects". The issue was followed, and the conflict is recorded as an open question rather than resolved unilaterally, because the architecture line may also need changing.
- The first import-fixing pass matched model type names as substrings, so `Annotation` matched inside `AnnotationRepository` and every repository lost its own model import. Checkstyle caught it. The second pass computes imports from the interface body only.
- A second pass then stripped the javadoc off the interface declarations while rewriting the import block. Regenerating the repositories from the declaration list rather than patching the files was faster and safer than repairing them.
- Boolean fields initially generated `getX()` getters; these were renamed to `isX()` to match Java convention.
- `git status` showed `arbiter/model` as untracked but did not mention `arbiter/data` at all. The cause was an unanchored `data/` line in `.gitignore`, which matched `src/main/java/arbiter/data/` as well as the intended root-level output folder, so the whole `arbiter.data` package this issue exists to create would have been silently left out of the commit. Both `data/` and `bin/` are now anchored to the repository root. This would have hit anyone adding that package, not just this change.

## Agent responses and outcomes

- Created branch `feat/shared-domain-model` from `main`.
- Wrote `plans/shared-domain-model.md` with the goal, non-goals, the seven decisions, four open questions, the verification table and the risks.
- Added `arbiter.model`: 10 classes (`User`, `Project`, `Label`, `Item`, `Split`, `Assignment`, `Annotation`, `BoundingBox`, `Resolution`, `Flag`) and 9 enums (`Role`, `AccountStatus`, `TaskType`, `SourceType`, `TaxonomyKind`, `OutputFormat`, `AssignmentStatus`, `ResolutionMethod`, `FlagReason`).
- Added `arbiter.data`: 10 repository interfaces with 47 method signatures in total.
- Added no automated tests. The issue ships no behaviour, and [#11] owns the test harness and fixtures.

## Review follow-up

- Whimsyturtle requested changes with 26 inline comments and one structural request. All are applied in the same branch, which was rebased on the docs branch so the stack stays linear.
- The structural request was to split both modules into submodules. `arbiter.model` and `arbiter.data` now each have `user`, `project`, `annotation` and `resolution`, mirroring each other.
- Most comments were decisions rather than defects: eight enum constants and five fields were dropped, the scale bounds moved into a new `TaxonomySettings`, `Resolution.scaleValue` became a `Double`, and collection-returning repositories were renamed from `find*` to `list*`.
- Two comments caught real problems. `Annotation` and `Resolution` could hold a label and a scale value at once with nothing preventing it; their setters now clear the other. And `countByRole` did not do what its comment claimed: it counted disabled accounts, so the last-adjudicator guard did not hold and two adjudicators could disable each other out of the workspace. It is now `countActiveByRole`.
- The fixes ripple into the documentation, since several of them remove documented behaviour. Those edits were applied on the docs branch, which this branch is now stacked on, so the two cannot contradict each other.
- Checkstyle caught two lines over 120 characters that the restructure introduced, both in javadoc, and both were wrapped.
- Several Gradle runs failed with access errors on `build/` while writing class files and reports. This is the sandbox, not the code: each time the folder was cleared after checking it resolved inside the workspace, and the run then succeeded.

## Verification

- `./gradlew check` passes: `compileJava` and `checkstyleMain` both succeed. `test` and `checkstyleTest` report `NO-SOURCE`, since there are no tests in this change.
- Model classes carry no annotations and neither package references SQL, JDBC or any ORM: checked by scanning all 20 files.
- Every model class has a public no-arg constructor, so every one is constructible without a database.
- All nine enums were compared constant by constant against the expected set and match exactly.
- The repository interfaces expose 10 interfaces and 47 signatures; 9 of the 10 were read back to confirm the model import and javadoc survived the regeneration.
- After the `.gitignore` fix, `git status --untracked-files=all` lists all 20 source files, and `git check-ignore` confirms the root-level `bin/` and `data/` directories are still ignored.
- Not verified: whether the seven decisions are the right ones. That is the human review this issue is for.
- The owner marked this summary human verified on 24 September 2026.

[#4]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/4
[#9]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/9
[#11]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/11
[#44]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/44
[#45]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/45
