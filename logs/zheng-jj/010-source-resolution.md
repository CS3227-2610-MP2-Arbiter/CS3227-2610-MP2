# Resolve text sources and detect source changes

Status: Human verified.

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

## Review follow-up

- Whimsyturtle reviewed PR #68 and raised seven points. Six changed the code: `ResolvedSource` dropped the resolved `Path` so nothing opens the source file outside the resolver; containment moved before the disk check, so a path that leaves `media/` is no longer reported as `MISSING`; a `TOO_LARGE` failure with a 10 MiB `MAX_TEXT_BYTES` bound was added; a leading UTF-8 byte-order mark is now accepted and stripped; the hash comparison is an exact match, matching `RepositorySession`; and the extension constant's Javadoc no longer says "without the dot" while holding one.
- The seventh point was the Windows gap. Escape protection was only exercised on POSIX, so three Windows-only cases now cover a backslash traversal, a UNC path and a drive-absolute path. They skip on macOS and Linux and run in the Windows CI job.
- The exact hash comparison is a behaviour change: a stored hash differing only in case is now a `HASH_MISMATCH` rather than accepted. This is what the reviewer asked for and matches how the store compares hashes.

## Second review round

- Whimsyturtle raised three more points, all structural. Stored paths now have one portable shape: relative, `/`-separated, starting with `media/`, with no empty, `.` or `..` segment, and `\` and `:` refused as `INVALID_PATH`. That replaced the platform-specific escape handling, so the Windows-only tests became cross-platform and nothing skips now.
- `resolveForImport` was added so [#25] applies exactly the read checks at registration and only accepts a file it can read later. Its returned hash is the exact value to store, because registration and later reads compare hashes exactly.
- Junction coverage was missing. A directory link is now created on every platform, with a real junction on Windows through `mklink /J`, and both the escaping and the staying-inside cases are tested.
- The junction test initially failed because its target folder did not exist; that was a fixture error, fixed in the test rather than the resolver.

## Verification

- Before the review follow-up: JDK 25 `./gradlew check shadowJar --no-daemon` passed with 103 JUnit tests, both Checkstyle tasks, and the release jar.
- After the review follow-up: JDK 25 `./gradlew cleanTest check shadowJar --no-daemon` passed with 113 JUnit tests, both Checkstyle tasks, and the release jar. Three Windows-only cases skip on macOS and run on Windows CI.
- After the second review round: JDK 25 `./gradlew cleanTest check shadowJar --no-daemon` passed with 134 JUnit tests, none skipped, both Checkstyle tasks, and the release jar.
- `SourceResolverTest` has 51 cases covering nested and non-ASCII text, a leading byte-order mark, the size limit and one byte over it, absolute and traversal paths, a `media-lookalike` sibling, symlinks leaving and staying inside `media/`, a dangling symlink, a directory, a non-`.txt` file, a missing media folder, an unreadable file, hash mismatch, restored bytes, lone continuation byte, truncated sequence and UTF-16.
- `SourceResolverSnapshotTest` passes: a failed resolve leaves `arbiter.json` byte-identical and the stored path and hash unchanged, and restoring the original bytes resolves again with no store change.
- Two Checkstyle failures in the new tests were fixed before the final run: a wrapped lambda argument had to start on the previous line.
- The submit gate (#17) and export gate (#37) are not covered here; they own those checks and #14/#17/#37 must call this boundary.
- The owner marked this summary human verified on 24 September 2026.

[#10]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/10
[#17]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/17
[#25]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/25
[#37]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/37
