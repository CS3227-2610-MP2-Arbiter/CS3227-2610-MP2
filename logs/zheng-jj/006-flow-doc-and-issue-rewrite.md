# Rewrite the flow document and issues as context

Status: Human verified.

## Original request

- The user asked to pull again and clean up stale branches, then raised three problems: the GitHub issues refer to `A0`, `D1` and other features by short codes rather than linking the real issues; `docs/UserFlows.md` is too detailed and should become a higher-level context document, with the issues trimmed the same way; and the markdown files have "random breaklines" because a line-length limit was applied to them.
- The user then clarified that the line fix and the user flow update must be two separate pull requests.

## Follow-ups, corrections, and reflection

- The line-wrapping fix shipped first as its own pull request, since the user asked for two. This log covers the second: the flow document and issue rewrite.
- Verified all 46 code-to-issue mappings against the live GitHub titles before rewriting anything, and found no mismatches. The mapping table in the old `docs/UserFlows.md` section 5 was accurate; the problem was that the codes were used as the primary reference instead of the issue numbers.
- Condensing the issues risked silently changing the dependency graph. Rather than trust the rewrite, the original `Blocked by` and `Blocks` clauses were parsed from each issue and compared against the new bodies: 8 of 46 diverged on the first pass and were regenerated from the original graph. The final check reports 0 mismatches across all 46.
- Twelve issues carried a byte-order mark that made their `## Summary` heading render as literal text rather than a heading. The rewrite drops the BOM.
- Codes referenced outside the dependency section were deliberately not all preserved as links: many were self-references or implementation trivia that the condensation removes. A check confirmed no bare codes and no broken links remain in any issue.
- The stale remote branch `docs/review-fixes` was deleted, and PR #54 merged mid-task, so `main` was re-pulled and the work branch recreated from the new head.

## Agent responses and outcomes

- Rewrote `docs/UserFlows.md` as a context document: vocabulary, the shape of both flows as numbered summaries, the 15 shared rules, and a short table of where the work lives. It links to the real issues throughout and dropped from 344 to about 140 lines, most of which are link definitions. The step-by-step screen detail now lives only in the issues.
- Issue links use reference-style definitions so the prose stays readable rather than carrying full URLs inline.
- Repointed every cross-document link: `docs/UserGuide.md` gained real issue links in its quick-reference table, and `AGENTS.md`, `docs/index.md`, `docs/DeveloperGuide.md` and `context/architecture.md` now describe the document as product context rather than the behaviour spec.
- Replaced the backticked codes in `context/architecture.md` (18) and `docs/DeveloperGuide.md` (9) with links to the real issues.
- Rewrote all 46 issues: codes became issue links, and each body was condensed from Summary, Context, Scope, Tasks, Acceptance criteria and Dependencies to Summary, Scope, Acceptance criteria and Dependencies. Total body text fell from about 96,000 to about 60,000 characters, a 37% reduction.

## Review fixes

- Whimsyturtle reviewed PR #56 and raised two points. Both are addressed in a follow-up commit on the same branch.
- **Stale code style.** `context/architecture.md` still labelled two package owners `B*` and `C*`, and `docs/UserFlows.md` used the same style in its ownership sentence. He asked for a sweep rather than a spot fix, so the whole current documentation set was scanned for the pattern; those were the only two, and both now name the track or the owner in words. Historical `logs/` and `reflections/` were left alone, since they are a record of what was believed at the time.
- **Vocabulary as a glossary.** The vocabulary table moved out of `docs/UserFlows.md` into a new `docs/Glossary.md`, which `docs/index.md`, `AGENTS.md` and the `maintain-docs` skill now point at. `docs/index.md` also lists it under Documentation. This gives the domain terms one home instead of burying them inside the flows page.

## Verification

- Dependency graph: 0 mismatches of 46 against the original `Blocked by` and `Blocks` clauses.
- All issues: 0 backticked codes, 0 bare codes, 0 links to non-existent issues, 0 self-references.
- Rule citations in the rewritten issues (rules 9, 11, 12) all resolve to the intended rule in the new section 3.
- Cross-document anchor check: 0 broken links across `docs/`, `README.md`, `AGENTS.md` and `context/`, including the new glossary page.
- After the review fixes: every reference-style issue link still resolves with no undefined or unused definitions, and no stale `B*`-style code remains in the current documentation.
- `./gradlew check` was not run because the change is documentation-only and touches no Java source.
- The owner marked this summary human verified on 24 September 2026.
