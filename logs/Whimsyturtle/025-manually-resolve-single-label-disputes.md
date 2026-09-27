# Manually resolve single-label disputes

Status: Human verified.

## Original request

- The human asked the agent to start issue #34 following `context/swe.md`, keep it simple and not overengineer, use the repo's skills as needed and ask about important decisions.

## Follow-ups, corrections, and reflection

- Clarifying #34 found #27 and #32 closed and the `Resolution` model already able to store a manual decision, with no service or screen. It also found four problems in #34:
  - It said "valid" and "eligible" answers, while rule 10, the Glossary and #27 say "submitted".
  - It restated the dispute definition and rules 13 and 17.
  - "Stale writes" had no concrete meaning under the single-writer lock.
  - Replacing a decision was underspecified.
- The human accepted the agent's edge-case assumptions and decided:
  - A **Disputes** button on the project page opens a per-project list.
  - Only `ADJUDICATED` decisions can be replaced; `MAJORITY` results never change.
  - An unreadable source shows why and refuses a save.
- After planning, the human decided:
  - Items with a strict majority from before #27 are not listed.
  - An unreadable source still shows the answer cards but no picker.
  - A refused save returns to the list.
- The human approved `plans/manual-resolution.md`.
- The human asked for a thorough second review with the `review` skill, focusing on edge cases, missing tests, duplication, misleading names and comments, and abstraction. It found a defect the first review missed, so "no defects" should not be trusted without a fresh read.
- The human asked the agent to fix findings 1, 3, 5, 6 and 7 below and left 2, 4 and 8 open. They later accepted finding 8's behaviour.
- The human staged the changes with the agent's help and committed them as three commits: service, view and wording.
- After their acceptance checks passed, the human asked the agent to update the guides and log and open a concise PR without committing.

## Agent responses and outcomes

- The agent rewrote #34's body with the decisions, "submitted" wording and citations of rules 10, 13, 17 and 21, and created `feat/manual-resolution` and the plan.
- The agent made `ResolutionService` a public service with `disputes`, `comparison` and `adjudicate`, and added `DisputeSummary`, `DisputeComparison` and `DisputesView`. It also reworded the `AnnotationEditor` Javadoc and the architecture context, since the adjudicator now reuses the editor. Deviations from the plan:
  - **Disputes** sits under **Taxonomy**, because the page stacks its controls.
- `ResolutionServiceDisputeTest` covers the list, the comparison, saving and replacing, restart, refusals and role checks. It found no production defects.
- The first review found no defects. It fixed stale Javadoc in `TaxonomySummary` and `CorpusService.taxonomyOf`, and the agent removed an unused `DisputeComparison.itemId`.
- The second review's findings:
  1. Fixed: the list showed the stored sequence as the position, which has gaps after an unregister, so it could disagree with the project page. Positions are now counted in split order, with a regression test.
  2. Open: `disputes` repeats each item's split and taxonomy lookups.
  3. Fixed: the list called `decide` with a null time just to test for a majority. Both now share a private `strictMajority`.
  4. Open: no tests for another project's items, replacing a decision after its source changed, or listing an unassigned split.
  5. Fixed: the "A scale project has none" hint showed on every project. Each kind now gets its own hint, as the plan says.
  6. Fixed: the private `Dispute` record, which also holds decided items, is now `ListedItem`.
  7. Fixed: a comment copied from `ProjectPage` wrongly said the reload shows why a save failed.
  8. Accepted by the human: re-saving the current decision silently updates its decider and time.
- The agent added a "Resolve a dispute" step to the User Guide's Projects section, reworded the Developer Guide's "side by side" line and updated the plan's status.
- The agent opened draft PR #86.
- Pending: teammate review, committing the guides, plan and log, and findings 2 and 4.

## Verification

- After the fixes, `.\gradlew.bat check shadowJar --rerun-tasks` passed with 443 tests, no failures and six skips from existing tests Windows cannot run, including all 15 in `ResolutionServiceDisputeTest`. A plain run had been fully cached, so it proved nothing.
- The new position test failed against the old logic, and no other test did.
- CI passed on the pushed commits.
- The human reported that the plan's acceptance checks passed.
