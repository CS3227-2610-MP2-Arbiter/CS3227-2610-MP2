# Developer guide and architecture write-up

Status: Awaiting human verification.

## Original request

- After asking what was left once #37 merged, the owner said "do 41 and 43". This log covers [#43]; [#41] is log 019.

## Follow-ups, corrections, and reflection

- Partway through, the owner approved every plan in this batch and asked for Whimsyturtle to be requested as reviewer once all three tasks were done, which covers delivery.
- The first draft stated four things the code does not do: an integrity check for answers holding exactly one value, a resolver that follows links, source reads always outside the store's action, and a session check on every public service method. Each was caught by reading the code behind the sentence before keeping it. A write-up of "how it works" needs the same verification as code.
- The Pages site uses Jekyll's Cayman theme, which does not render Mermaid, so the diagrams are plain text that reads the same on GitHub and Pages.

## Agent responses and outcomes

- Used `write-plan`, `maintain-docs`, `review` and `log`. The plan is `plans/developer-guide.md`, on branch `docs/developer-guide`.
- `docs/DeveloperGuide.md` now documents the architecture with a screens-to-services diagram, the domain model, authorization and blindness, persistence (snapshot, transaction boundary, integrity, atomic publication, lock, versions and the absence of migrations, source hashing, submission atomicity with a sequence diagram), resolution and export with a checklist for a new output format, and the [#62] scope reduction with the schema implications of another source or task type. The decisions table and testing section are extended.
- `context/architecture.md` gains authorization and persistence rules and links to the guide for reasons.

## Verification

- Every claim about behaviour was checked against the code it names. Links and anchors were checked by script.
- A search finds deferred features only as deferred or future work.
- Documentation only, so no Gradle run is needed. The `Product site` check runs on the pull request.
- The owner approved the plan and delivery on 28 September 2026.

[#41]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/41
[#43]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/43
[#62]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/62
