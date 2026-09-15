# Review of zheng-jj documentation commits

Status: Human verified.

## Original request

- The user wanted to review a large batch of commits by teammate `zheng-jj` and asked how, and in what order, to review them, after first checking the Git history.

## Follow-ups, corrections, and reflection

- The user asked whether to fix the problems on a new branch and tag `zheng-jj` to review the pull request. The agent recommended splitting the work: mechanical fixes go in a pull request now, while contradictions and product decisions are agreed with `zheng-jj` first. 
- A pull request also restores the teammate review required by `context/swe.md`. The direct pushes to `main` had skipped it, which is why this review happened only after the work had landed. The agent suggested enforcing reviews through branch protection on `main`. The user followed the suggestion and added a `Protect main branch` ruleset. The GitHub API confirms it is active, requires a pull request with one approving review, and blocks force pushes and deletion of `main`.
- The user then asked for a branch for their own changes and for this log. The first draft of the log added standalone lesson bullets. The user pointed out that these did not match their earlier logs, so each lesson was folded into the event it came from.
- While reviewing, the user noticed drift between `README.md` and `docs/index.md`, which both defined Arbiter and its roles. They proposed keeping the definition only in `docs/index.md`, with README linking to the product site. The agent agreed: the Pages workflow builds only from `docs/`, so README cannot be the source, and the two versions differed only in wording, which made this a mechanical fix rather than a decision to agree with `zheng-jj` first.
- The agent found the same problem elsewhere: `docs/UserGuide.md` restated the roles, and README's build commands repeated the Developer Guide's "Setting up" section. It also pointed out that step 3 of `maintain-docs`, "Keep shared facts consistent across affected pages", assumed facts would be copied between pages, so the agent would reintroduce the drift. The user accepted every suggestion and specified that README keep its current sentence, which they judged more accurate than `docs/index.md`'s, with `docs/index.md` changed to match.
- The agent had described the introduction of `context/architecture.md` as a one-sentence summary, but it is two sentences and the second summarises both roles. It was initially left unchanged as unpublished agent context, and the new rule was scoped around it.
- The user asked what the closing line of `docs/UserGuide.md` meant. The agent explained that `B11` and `C16` are the user-guide issues `#22` and `#38`. The line claimed that the guides were "being written", but both issues are blocked by features not yet built, and the codes mean nothing to readers of the published site. The user chose to replace the line with a plain note for readers.
- The user later revisited `context/architecture.md`: it still defined the roles, its ASCII diagrams did not suit an agent audience, and it overlapped heavily with `docs/DeveloperGuide.md`. The agent agreed and added that MP2 requires the Developer Guide itself to describe the design, which it deferred to the unpublished architecture file instead.
- The agent proposed that the Developer Guide hold the design and its rationale, and `context/architecture.md` only the code-level rules. The user accepted. They also asked for step 3 of `maintain-docs` and `AGENTS.md` to be split into bullets.

## Agent responses and outcomes

- `git fetch --all --prune` showed no other branches. `main` has a linear history of 24 commits, and `zheng-jj` authored the latest 11 (`12b4ba8` to `9d02ead`) without pull requests. The net diff `git diff 06c6733 9d02ead` covers 9 files, with 1,128 lines added and 9 removed. All of it is Markdown apart from a `bin/` line in `.gitignore`, and every changed document was previously a `TODO` stub or did not exist.
- Six commits re-edit `docs/UserFlows.md`, and `1a26d95` and `2539621` cancel out, leaving `README.md` and `.codex/skills/log/SKILL.md` unchanged. The agent therefore recommended reviewing the final files rather than individual commits, in this order:
  1. Spikes `#1`-`#3` on GitHub
  2. `docs/UserFlows.md`, especially sections 2 and 3
  3. `context/architecture.md`
  4. The user's issues that `zheng-jj` edited (`#6`, `#7`, `#25`, `#26`, `#28`, `#31`, `#37`, `#46`)
  5. `docs/DeveloperGuide.md`
  6. `docs/Reflections.md`
  7. `docs/UserGuide.md`, `docs/index.md` and `.gitignore`
  8. `zheng-jj`'s logs, which are optional
- The agent created the local branch `docs/review-fixes` from `9d02ead` for the user's fixes.
- On `docs/review-fixes`, the agent applied the agreed changes to `README.md`, `docs/index.md`, `docs/UserGuide.md` and `maintain-docs`. It also removed README's documentation list, which duplicated `docs/index.md`'s and already lacked User Flows, and carried README's general Windows note into `docs/DeveloperGuide.md`, which had covered Windows only for `run`.
- The agent reworded that line of `docs/UserGuide.md` to "Step-by-step instructions for each screen will be added as features are released."
- The agent cut `context/architecture.md` from 257 to about 90 lines: an audience line, a package table with layers in place of the diagram, grouped rules, the model and the services. It moved the rationale and the decisions-and-costs table into the Developer Guide's "Design" section, pointed `AGENTS.md` to `docs/index.md` and `docs/UserFlows.md`, and made that section a third home in `maintain-docs`, whose rule now covers `context/`.
- The diagram had put migrations in `arbiter.workspace` but the package table in `arbiter.data.sqlite`; both documents now follow the table, pending `zheng-jj`'s confirmation. The agent also aligned the soft-delete rule with rule 5 and dropped the guide's claim of fifteen rules in UserFlows section 3, which has twelve plus two orphaned fragments left for `zheng-jj`.
- After the user approved a draft, the agent edited issue `#43` (`D5`) to add `context/architecture.md` to its scope and acceptance criteria.

## Verification

- Observed: the Git history, the net diff and the final contents of every changed file. Read-only `gh` calls confirmed that 46 issues exist and that the spikes are closed with their decision comments, and showed the ORMLite wording in `#6` and that `#22` and `#38` are open.
- Not checked: the bodies of the other issues `zheng-jj` edited.
- For the documentation fixes, the live product site returned HTTP 200 after a successful deployment of `9d02ead`. It has the `the-two-roles` and `setting-up` anchors, rewrites relative `.md#section` links with the section intact, and resolves `index.md` to the site root. The final `git diff` was reviewed.
- The site was not built locally; the product-site workflow builds it on the pull request. Gradle checks were not run because only Markdown changed.
- For the architecture follow-up, `git diff --stat` showed four changed files, `config/checkstyle/checkstyle.xml` confirmed the 120-character limit, and `gh issue view 43` supplied the current issue body. After the edit, the body of `#43` matched the approved draft. The site was not built locally.
- The user edited the summary and approved it.
