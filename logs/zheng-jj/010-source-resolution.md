# Resolve text sources and detect source changes

Status: Awaiting human verification.

## Original request

- Work on issues #10 and #11 and create two pull requests, following the Arbiter process and skills.

## Follow-ups, corrections, and reflection

- Issue #10 was unblocked (#9 is closed) and issue #11 was unblocked (#6 is closed), so both could proceed without a further scope decision from the owner.
- `gh` could not reach `api.github.com` inside the sandbox; the authenticated CLI was used with approved escalation for issue reads.
- The plan for #10 sets the SHA-256 lowercase-hex hash contract that #25 must use for registration. It is called out as the one shared decision that must be settled before #25 lands, because both sides must agree exactly.

## Agent responses and outcomes

- Read `context/swe.md`, both skills sets, `context/architecture.md`, the workspace, JSON store, model, repository and existing test code before planning.
- Used `clarify-requirements`, `write-plan`, `implement-feature`, `write-test`, `review`, `log` and `create-pull-request`.
- Created `feat/source-resolution` from `main` and wrote `plans/source-resolution.md`.
- Implemented `arbiter.workspace.SourceResolver` with `ResolvedSource`, `SourceFailure` and `SourceException`. It resolves a stored workspace-relative path, proves the target stays under the resolved `media/` folder using real paths, requires a regular `.txt` file, re-hashes the bytes with SHA-256 and decodes strict UTF-8, and only reads: no repository, no cache, no file mutation.
- Reason order is stored-path shape, existence, containment, hash, then text, which is recorded in the plan because a path that is both missing and outside the root reports `MISSING`.

## Verification

- JDK 25 `./gradlew check shadowJar --no-daemon` passed with 103 JUnit tests, both Checkstyle tasks, and the release jar.
- `SourceResolverTest` has 26 passing cases covering nested and non-ASCII text, absolute and traversal paths, a `media-lookalike` sibling, symlinks leaving and staying inside `media/`, a dangling symlink, a directory, a non-`.txt` file, a missing media folder, an unreadable file, hash mismatch, restored bytes, lone continuation byte, truncated sequence and UTF-16.
- `SourceResolverSnapshotTest` passes: a failed resolve leaves `arbiter.json` byte-identical and the stored path and hash unchanged, and restoring the original bytes resolves again with no store change.
- Two Checkstyle failures in the new tests were fixed before the final run: a wrapped lambda argument had to start on the previous line.
- The submit gate (#17) and export gate (#37) are not covered here; they own those checks and #14/#17/#37 must call this boundary.
- A human has not yet verified this summary.

[#10]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/10
[#17]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/17
[#25]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/25
[#37]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/37
