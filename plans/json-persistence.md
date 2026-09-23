# JSON persistence for the shared workspace

**Issue:** [#6](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/6)
**Branch:** `feat/sqlite-persistence` (reused for the same issue)
**Status:** Approved by the owner on 23 September 2026 and revised to drop legacy database handling. Local verification passed; human acceptance remains pending. The owner subsequently authorized the GitHub edits, which are published in [#6](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/6) and [#9](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/9).

## Goal and scope

Replace the unimplemented SQLite design for #6 with a Jackson-backed workspace data store while preserving the shared repository interfaces and the one-action commit boundary required by rule 2. Keep the #62 feature scope. Feature services, role UI, the #61 workspace lock, and the #11 fixtures and blindness test remain with their owning issues.

## Proposed design

- Use one `arbiter.json` snapshot for the current model's records, ID allocation state, and data format version, as the owner chose. A commit that changes several repositories then has one file replacement as its publication point.
- Keep `workspace.json` for workspace layout metadata. The owner chose `arbiter.json` as the sole data schema-version authority. The first JSON data schema and current workspace layout both use version 1.
- Build one data-store entry point that opens a workspace, exposes all existing `arbiter.data` repository interfaces, and runs a write action with all repositories against one private snapshot. Load the committed snapshot for reads; never expose mutable stored objects to callers. Save, delete, and insertion operations inside the action only change the private snapshot. On action failure, discard it.
- At commit, validate the storage-level constraints retained from #6, serialize with Jackson to a temporary file beside `arbiter.json`, flush the file, and atomically replace the committed snapshot. Publish successful state only after replacement. Never fall back to a regular move or remove the old file first. If replacement fails with an uncertain outcome, stop writes and reopen/inspect the committed snapshot before retrying.
- Initialize `arbiter.json` for a new workspace from the wizard. Opening a workspace requires an existing valid snapshot. The owner explicitly removed legacy database recognition and migration; an older workspace without `arbiter.json` fails as an incomplete data store.
- Implement the existing repository query and mutation contracts without adding deferred model types or service-level business rules. Keep repository-side enforcement aligned with #6's agreed storage constraints; services will own authorization, freezes, and submission uniqueness inside the shared transaction.
- Retain the shared-workspace, single-writer design from rule 11 and #61. This issue supplies the transaction boundary; #61 supplies interprocess writer exclusion. In-process access must be serialized so a transaction cannot publish over another transaction's work.

## Verification plan

| #6 acceptance area | Evidence to produce during implementation |
| --- | --- |
| All current repository interfaces | Integration tests using temporary workspaces cover persistence, IDs across reopen, each query, ordering, and deletes for the current model. |
| Open, versioning, and integrity | Tests cover new and reopened JSON workspaces, supported-format reads, malformed/missing/newer snapshots left unchanged, required values, references, and case-insensitive username uniqueness. |
| One action across repositories | Tests exercise representative #7, #17, #27, and #32 write shapes, reopening after success and after an injected failure before replacement to check the entire before or after state. |
| Interrupted write artifacts | A same-directory orphan temporary file never replaces the committed snapshot; retry behavior is tested without weakening immutable submissions. |
| Scope and build | Review that no #62-deferred contract was added; run JDK 25 `.\gradlew.bat check shadowJar` and inspect test and Checkstyle results. |

## Risks and open decisions

- A separate file per repository cannot publish a cross-repository action atomically with simple renames; it requires a manifest or journal. The owner chose the single snapshot.
- Java's `ATOMIC_MOVE` support and replacement behavior depend on the workspace filesystem. The approved policy is to fail clearly without a non-atomic fallback when replacement is unavailable. `FileChannel.force(true)` improves local durability, but the JDK does not promise the same power-loss durability for a remote drive. A workspace lock remains necessary for concurrent Arbiter instances.
- The owner's follow-up removes compatibility with the previous database placeholder. This application has no production data to migrate; the JSON snapshot begins at schema version 1.
- The historical ORMLite comment on #6 remains as context, with a superseding comment pointing to the updated issue body. The owner later requested that the stale SQLite descriptions in the guides be updated to JSON; human review of those edits remains pending.

## Implementation and verification

- Added `arbiter.data.json` with a versioned snapshot, the existing repository interfaces, storage integrity validation, and one callback-based write action. The workspace wizard initializes or opens it after the workspace layout step.
- `WorkspaceService` now writes layout version 1 metadata without a duplicate data schema version. The wizard initializes the JSON snapshot when creating a workspace and validates it when opening one.
- Added temporary-workspace integration tests for repository contracts, failure and success across repositories, snapshot versioning, malformed and missing files, and orphan temporary files.
- JDK 25 `.\gradlew.bat check shadowJar --offline --no-daemon` passed again after the review refactors: JUnit, both Checkstyle tasks, and the release jar. The sandboxed rerun could not create the Gradle cache lock, so the approved unsandboxed execution was used.
- The owner authorized the GitHub issue updates; #6 has eight checked acceptance criteria and #9 has seven. Human acceptance remains: create and reopen a JSON workspace from the wizard, and check the visible error for a missing or malformed snapshot. Documentation review remains pending under the owner's process.
- At the owner's request, `docs/UserFlows.md`, `docs/Glossary.md`, `docs/DeveloperGuide.md`, and `context/architecture.md` were revised to describe the merged JSON store and distinguish its in-process serialization from the planned #61 lock.
