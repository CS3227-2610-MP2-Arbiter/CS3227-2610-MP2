# Unwrap hard-wrapped markdown prose

Status: Human verified.

## Original request

- The user asked to pull again and clean up stale branches, then reported three problems: GitHub issues refer to `A0`, `D1` and other features by code rather than linking the real issues; `docs/UserFlows.md` should become a higher-level context document instead of the current detailed step list; and the markdown files have "random breaklines" because a line-length limit was applied to them.
- The user then clarified that the line fix and the user flow update must be two separate pull requests.

## Follow-ups, corrections, and reflection

- The pull and branch cleanup landed before the documentation work: `main` was already current after PR #54 merged mid-task, and all stale remote branches had been deleted by the merge automation. Local branches `docs/fix-flow-rule-numbers` and `docs/split-reflections` were removed, and the merged remote `docs/review-fixes` was deleted.
- The line-wrapping fix was initially written as a one-off script, but the first pass misclassified 4-space indented list continuations in `docs/UserFlows.md` section 3 as indented code blocks, so rules 10-15 kept their breaks. Tracking the active list marker indent fixed it.
- A second defect surfaced during verification: the first pass stripped the trailing newline from six files. Both defects were caught by comparing against `HEAD` rather than by reading the diff, which is why the check compares block structure and fenced code rather than trusting the visual result.

## Agent responses and outcomes

- Verified the rewrite was semantics-preserving for all 28 markdown files: prose whitespace-equivalent to `HEAD`, fenced code byte-identical after CRLF normalisation, link targets unchanged, and trailing newlines preserved. 197 lines were joined across `docs/`, `context/`, `logs/` and `reflections/`.
- Recorded the convention so it does not regress: `context/architecture.md` now scopes the 120-character limit to Java, and the Developer Guide gained a **Markdown** paragraph under the process section.
- Confirmed every cross-document anchor into `docs/UserFlows.md` still resolves after unwrapping.
- The issue-code rewrite (codes to issue links) and the UserFlows restructure are deliberately deferred to a second pull request, per the user's instruction.

## Verification

- Structural comparison against `HEAD` for 28 files: 0 differences after excluding the two intended convention additions.
- `git diff --check` reported no whitespace errors.
- Link and anchor check across `docs/`, `README.md` and `context/`: 0 broken links.
- `./gradlew check` was not run because the change is documentation-only and touches no Java source.
- The owner marked this summary human verified on 24 September 2026.
