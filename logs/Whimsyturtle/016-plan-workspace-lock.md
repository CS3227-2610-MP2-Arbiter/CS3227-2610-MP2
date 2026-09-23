# Plan workspace single-writer lock

Status: Owner verified.

## Original request

Start issue #61 using the Arbiter process and task skills. Keep the lock simple and ask about important design choices.

## Follow-ups, corrections, and reflection

The owner confirmed the recovery and refusal policy in `plans/workspace-lock.md`, then approved that plan for implementation. Independent review identified a platform-dependent channel-close risk; the implementation and cross-process test were corrected before full verification. During human acceptance, the owner found a manual-deletion bypass and approved a revision to lock required workspace metadata. The owner asked that no references to a separate lock artifact remain.

After the revision, the owner asked why the metadata content does not change, whether a computer crash can leave the lock held, and why `HELD_ROOTS` is a set when the current GUI uses one workspace. The agent explained that the OS tracks the byte-range lock without writing JSON, JVM termination releases it while a still-running hung process retains it, and the set guards repeated acquisition of the same root within one JVM while allowing distinct roots. The owner also noticed two spellings of the 1 MiB offset; the test constant was changed to `1024 * 1024` to match production, keeping an `int` for array sizes and a `long` for the file position.

For draft-PR delivery, the owner asked for relevant issue checkboxes, approved including the stale JSON documentation corrections, confirmed the revised two-instance test on the intended shared disk, and chose to keep log 015 in this PR. The owner-login PR merged into `main` while this branch was in review.

## Agent responses and outcomes

Read `context/swe.md`, issue #61, architecture and startup code. Used `clarify-requirements`, `write-plan`, `implement-feature`, `write-test`, `review`, and `log`. Created `feat/workspace-lock` from `main` in an isolated worktree, then moved the draft plan and log into the primary working folder and switched that folder to the task branch at the owner's request. The owner-login branch and unrelated untracked files were preserved. Implemented the approved plan in the workspace lock and startup flow; see the plan for design decisions and acceptance evidence. Published draft PR #66, updated #61, and added a superseding cross-reference to historical #1. Merged the owner-login branch from `main` and preserved the lock through its authentication screen.

## Verification

Issue #61 was read through the authenticated GitHub CLI after sandboxed network access failed. A focused regression test reproduced the owner's manual-deletion bypass before the revision (expected `contended`, actual `acquired`). The first draft-PR Ubuntu CI run exposed a test that reread workspace metadata under its lock; moving those assertions after release made the next Linux, macOS, and Windows CI runs pass. Independent review then found normal startup also reread metadata after locking, which could silently release the lock on POSIX systems. Lock-aware JSON-store entry points and a child-process probe after initialization/opening fixed that path. The merged branch passed JDK 25 `.\gradlew.bat check shadowJar --offline --no-daemon --console=plain` locally with 77 JUnit tests, both Checkstyle tasks, and the release jar. Final Linux, macOS, Windows, and Apple Silicon CI passed. The owner confirmed the final two-instance retest on the intended shared disk after the startup fix. The sandboxed wrapper could not write `C:\.gradle`; approved escalated execution used the existing cache. Documentation now describes the merged JSON store and current lock.
