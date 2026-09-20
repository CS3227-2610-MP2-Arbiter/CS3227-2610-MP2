# Shared domain model and repository interfaces

Status: Awaiting human verification.

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

## Verification

- `./gradlew check` passes: `compileJava` and `checkstyleMain` both succeed. `test` and `checkstyleTest` report `NO-SOURCE`, since there are no tests in this change.
- Model classes carry no annotations and neither package references SQL, JDBC or any ORM: checked by scanning all 20 files.
- Every model class has a public no-arg constructor, so every one is constructible without a database.
- All nine enums were compared constant by constant against the expected set and match exactly.
- The repository interfaces expose 10 interfaces and 47 signatures; 9 of the 10 were read back to confirm the model import and javadoc survived the regeneration.
- After the `.gitignore` fix, `git status --untracked-files=all` lists all 20 source files, and `git check-ignore` confirms the root-level `bin/` and `data/` directories are still ignored.
- Not verified: whether the seven decisions are the right ones. That is the human review this issue is for.
- A human has not yet verified this summary.

[#4]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/4
[#9]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/9
[#11]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/11
[#44]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/44
[#45]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/45
