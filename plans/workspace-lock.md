# Workspace single-writer lock

**Issue:** [#61](https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/61)

**Branch:** `feat/workspace-lock` (from `main`; now checked out in the primary working folder)

**Status:** Approved by the owner on 23 September 2026; revised metadata-lock implementation, local verification, and human shared-disk retest passed after an acceptance correction.

## Goal and boundary

Implement rule 11's single-writer workspace access for Arbiter instances. Keep workspace layout, media resolution, multi-writer support, and read-only mode outside this issue. The current UI has no workspace-switch command; the lock's close operation will be the release point when a switch path is added.

## Decisions and proposed design

- Use a JDK `FileChannel.tryLock()` on one byte at offset 1 MiB of required `workspace.json`. This stable metadata file is not rewritten by Arbiter. The byte range lies beyond its normal content so Java can still read the metadata on Windows; refuse metadata larger than the offset with a clear error. Never lock `arbiter.json`, which the JSON store replaces during a commit. No separate lock artifact is created.
- The owner chose to refuse a second instance entirely with a clear message. A failed or unsupported file lock fails closed. The OS releases the held lock if the process exits or is killed; do not add PID files, timestamps, stale-lock detection, retries, or a read-only branch.
- Add a small `WorkspaceLock` handle in `arbiter.workspace` that owns its channel and lock and implements `AutoCloseable`. Treat `tryLock()` returning null and `OverlappingFileLockException` as contention, and close the channel on every failed acquisition. Acquire after workspace layout validation and before `JsonStore.open()` or `JsonStore.initializeNew()` in the startup wizard.
- Guard canonical workspace roots held within the current JVM before opening another channel. The JDK warns that closing a second channel to the same file may release the first channel's lock on some systems.
- Pass the live handle from the wizard to `Arbiter`; hold it for the active stage and close it on normal application exit and any startup failure after acquisition. The existing wizard/store handoff may continue to validate and reopen the snapshot while the same handle stays held. There is no new lock state in the JSON snapshot or dependency.
- `WorkspaceService.create()` writes the layout for a new folder under #9; the new lock is acquired before initialization publishes `arbiter.json`. Existing workspace opens acquire before data-store access or any application write.

## Verification plan

| #61 acceptance area | Evidence |
| --- | --- |
| Lock before data writes; release on close or switch | Focused tests acquire, contend, close and reacquire the same workspace; review startup ordering and the application's exit/failure cleanup. #9 creates new-workspace layout before locking; no switch command exists in the current UI. |
| Second instance cannot write; clear message | A temporary-workspace test checks a contended open leaves `arbiter.json` unchanged and reports the workspace as in use. Bounded subprocess tests check interprocess contention, denial after manual metadata deletion, and reacquisition after killing the holder. |
| Independent workspaces | A focused test checks two different workspace roots can be held at once. |
| Build | Use JDK 25 to run focused JUnit tests and `.\gradlew.bat check shadowJar`; inspect actual JUnit and Checkstyle results. |

## Risks and human acceptance

- The branch starts from merged `main`; the open owner-login PR also edits `Arbiter`. If it merges first, rebase and resolve the startup/lifecycle overlap before merging this PR.
- File-lock behavior on shared disks depends on the filesystem and OS. Fail clearly if a lock cannot be obtained; the owner tested two Arbiter processes against the same shared workspace on the intended disk.
- The owner confirmed that two Arbiter instances passed the shared-disk retest after the revision. Deliberate deletion followed by recreation of the required metadata file is outside the cooperative-lock guarantee.
- The owner approved the simple recovery and refusal policy and this plan.

## Implementation and verification

- Added the `WorkspaceLock` handle on required metadata. The startup wizard acquires the lock before opening or initializing the JSON snapshot, passes the live handle to `Arbiter`, and releases it after validation failure or application close.
- Independent review found that closing a second channel after same-JVM contention could release the first lock on some systems. Added a canonical-root guard before opening the channel, and extended the subprocess test to check that the first process remains excluded.
- The revised implementation passed focused workspace tests and `checkstyleTest`, then JDK 25 `.\gradlew.bat check shadowJar --offline --no-daemon --console=plain`: JUnit, both Checkstyle tasks, and the release jar. Tests cover cross-process contention, deletion of metadata while held, crash recovery, the metadata size boundary, and JSON initialization/open under the lock. The sandboxed Gradle wrapper could not write its redirected `C:\.gradle` cache, so verification used the existing cache with approved access. Independent review found no remaining blocker.

## Acceptance correction

- The owner found that removing the first design's coordination path while Arbiter was running let another instance acquire a new file at that path. A subprocess regression test reproduced the bypass on Windows/JDK 25. The owner approved the metadata-lock revision, which eliminates that separate path. A two-process probe confirmed that `workspace.json` remains readable while locked and a second JVM cannot acquire the same byte-range lock.
