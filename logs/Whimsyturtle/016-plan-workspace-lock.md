# Plan workspace single-writer lock

Status: Awaiting human verification.

## Original request

Start issue #61 using the Arbiter process and task skills. Keep the lock simple and ask about important design choices.

## Follow-ups, corrections, and reflection

The owner confirmed the recovery and refusal policy in `plans/workspace-lock.md`, then approved that plan for implementation. Independent review identified a platform-dependent channel-close risk; the implementation and cross-process test were corrected before full verification. During human acceptance, the owner found a manual-deletion bypass and approved a revision to lock required workspace metadata. The owner asked that no references to a separate lock artifact remain.

After the revision, the owner asked why the metadata content does not change, whether a computer crash can leave the lock held, and why `HELD_ROOTS` is a set when the current GUI uses one workspace. The agent explained that the OS tracks the byte-range lock without writing JSON, JVM termination releases it while a still-running hung process retains it, and the set guards repeated acquisition of the same root within one JVM while allowing distinct roots. The owner also noticed two spellings of the 1 MiB offset; the test constant was changed to `1024 * 1024` to match production, keeping an `int` for array sizes and a `long` for the file position.

## Agent responses and outcomes

Read `context/swe.md`, issue #61, architecture and startup code. Used `clarify-requirements`, `write-plan`, `implement-feature`, `write-test`, `review`, and `log`. Created `feat/workspace-lock` from `main` in an isolated worktree, then moved the draft plan and log into the primary working folder and switched that folder to the task branch at the owner's request. The owner-login branch and unrelated untracked files were preserved. Implemented the approved plan in the workspace lock and startup flow; see the plan for design decisions and acceptance evidence.

## Verification

Issue #61 was read through the authenticated GitHub CLI after sandboxed network access failed. A focused regression test reproduced the owner's manual-deletion bypass before the revision (expected `contended`, actual `acquired`). JDK 25 `.\gradlew.bat check shadowJar --offline --no-daemon --console=plain` passed again after the test-constant edit: 60 JUnit tests, both Checkstyle tasks, and the release jar. The sandboxed wrapper could not write `C:\.gradle`; approved escalated execution used the existing cache. Independent review found no remaining blocker. The owner confirmed the revised two-instance behavior on the intended shared disk. Documentation was corrected from the old SQLite description to the merged JSON store and current lock.
