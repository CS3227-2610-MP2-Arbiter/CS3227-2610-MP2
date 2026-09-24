# Resolve text sources and detect source changes

**Issue:** [#10] - Resolve text sources and detect source changes
**Branch:** `feat/source-resolution`
**Status:** Implemented, locally verified and accepted by the owner on 24 September 2026; teammate review pending.

What the feature does is in [#10], and what each class means is in its Javadoc. This plan records what was decided along the way and what is still open.

## Goal and scope

Read one registered item's source text from the workspace, after proving the stored path still points at the same readable file it was registered from. [#25] will register paths and hashes, [#13]/[#14] will read through this boundary, and [#37] must report its error rather than export stale text.

Non-goals, from [#10]: corpus registration, images, thumbnails, decoding or caching, external sources, copying or moving source files, automatic relinking on rename, and accepting replacement content under an existing item.

## Proposed changes

- **`arbiter.workspace.SourceResolver`** is the one place a registered source is turned into text. It takes `WorkspacePaths` plus the stored path and hash, and returns an immutable `ResolvedSource`.
- **`ResolvedSource` is a record** in the same package, carrying the stored path, the hash actually read, the decoded text, and the file size and modification time. It carries the text rather than a file handle, so nothing reaches a source without passing the resolver's checks.
- **`SourceException`** carries a user-facing message naming the affected stored path and the reason, and extends `WorkspaceException` so the existing workspace error handling in `arbiter.ui.shared` covers it. Its reason is a small `SourceFailure` enum (`INVALID_PATH`, `MISSING`, `NOT_A_FILE`, `NOT_TEXT`, `TOO_LARGE`, `OUTSIDE_MEDIA`, `UNREADABLE`, `HASH_MISMATCH`, `INVALID_TEXT`) so callers and tests distinguish cases without parsing messages.
- **A stored path has one portable shape, checked before the disk is touched.** It is relative, uses `/` between segments, starts with `media/`, and contains no empty, `.` or `..` segment. A `\` or `:` is refused outright, because both are separators or drive syntax on Windows and a path that resolved only on a case-insensitive filesystem would fail on another. Case is never folded, so the path is stored and compared exactly as written.
- **Containment is decided again on the real path.** Only a real path shows where a link points, so `Path.toRealPath()` is compared against the resolved media root afterwards, and a symlink or junction that leaves it is `OUTSIDE_MEDIA`.
- **Symlink and junction policy: refuse, do not follow outside the root.** A `.txt` link inside `media/` whose target stays inside `media/` is readable; anything whose real path leaves the media root is `OUTSIDE_MEDIA`. This covers symbolic links and, on Windows, junction points, because `toRealPath()` follows both.
- **`resolveForImport` applies the same checks at registration.** [#25] calls it instead of reading the file itself, so a file it accepts is one this resolver can read later; the hash comparison is skipped because nothing has been recorded yet, and the returned hash is the exact value to store.
- **The file must be a regular file whose name ends in `.txt`** (case-insensitive), because [#62] made plain text the only v1 source type, and no larger than `MAX_TEXT_BYTES` (10 MiB), the size this version reads, hashes and decodes in memory.
- **Hashing matches the registration side of [#25]:** SHA-256 over the raw bytes, lowercase hex. `SourceResolver.hash` is exposed so [#25] hashes through the same entry point instead of a second implementation. A mismatch raises `HASH_MISMATCH` and never updates the stored path or hash.
- **Text is loaded as strict UTF-8.** Bytes are decoded with `CharsetDecoder` in reporting mode, so invalid or truncated sequences fail as `INVALID_TEXT` rather than becoming replacement characters. Unreadable files (permissions, I/O errors) surface as `UNREADABLE` with the cause attached. A leading byte-order mark is accepted and stripped from the text, because it is invisible and would otherwise show as an unexplained character; the hash still covers the bytes as written.
- **Nothing is stored.** Every call re-reads and re-hashes the file; there is no cache, so restoring the exact bytes restores access with no database change. No repository is touched on any path, including the failure paths, which is what keeps the "never mutates a resolution or advances the queue" criterion testable.
- **No UI or service wiring yet.** [#14] and [#17] own the screens and the submit gate, and [#37] the export gate; this plan supplies the boundary they must call. There is no registered file to read until [#25] lands, so a screen now would only report errors over an empty workspace.

## Design alternatives considered

| Choice | Alternative rejected | Why |
| --- | --- | --- |
| Resolve real paths and compare path elements | Compare `startsWith` on normalised strings | `media-lookalike` and mixed separators pass a string check |
| Refuse links leaving the root | Follow the link and read it | [#10] requires the target to stay under the resolved media root |
| Strict UTF-8 decode | `String(bytes, UTF_8)` | Lenient decoding inserts `U+FFFD` silently instead of failing clearly |
| SHA-256 hex helper owned by the resolver | Hash in each caller | [#25], [#17] and [#37] must agree on one algorithm and encoding |

## Risks and open decisions

- **The hash contract is shared with [#25].** SHA-256 lowercase hex is this plan's choice; if the adjudicator track prefers another encoding, that must be settled before [#25] lands, because both sides must match exactly.
- **`.txt` matching is by file name, not content sniffing.** A binary file renamed to `.txt` fails as `INVALID_TEXT`; that is intended, and no MIME detection is added.
- **Reason order** is stored-path shape, containment, existence, file type, size, hash, then text.
- **Files changing mid-read.** A file replaced between the hash read and the text read yields whichever bytes were read; the resolver does not lock files, because that would block the user editing their own corpus and [#10] does not ask for it.

## Verification

| Acceptance criterion | How it is verified |
| --- | --- |
| Nested `.txt` files under `media/` resolve from workspace-relative paths; absolute and external paths are rejected | Unit tests: a nested file resolves and returns its text; an absolute path and a sibling `../` path both fail with the stored path in the message |
| Traversal, prefix-lookalike paths and links/junctions cannot expose content outside the resolved media root | Unit tests: `media/../outside.txt`, a sibling `media-lookalike/` file, a `\` or `:` path and a link to an outside file all fail; a link staying inside `media/` resolves. The backslash, UNC and drive-absolute cases run on every platform, and a junction is created on Windows and a directory link elsewhere |
| Missing, unreadable and hash-mismatched files report the affected path and reason without changing project records | Unit tests for each failure, plus an integration test comparing the whole `arbiter.json` snapshot byte-for-byte before and after a failed resolve |
| Valid UTF-8 text is displayed; invalid or unreadable text fails clearly | Unit tests: ASCII, multi-byte and non-ASCII text resolve, with and without a leading byte-order mark; a lone continuation byte, a truncated sequence and a UTF-16 file fail as `INVALID_TEXT` |
| An answer cannot be submitted and an included item cannot be exported after source validation fails | Deferred to [#17] and [#37], which own those gates; this plan makes the failure a checkable exception rather than a silent null |
| A file cannot be registered unless it can later be read | `resolveForImport` runs the same path, existence, containment, plain-text, size and UTF-8 checks; tests cover each failing case and a round trip from import to a later read |
| Restoring the original bytes at the recorded relative path makes validation succeed again | Unit test: resolve, overwrite the file, see `HASH_MISMATCH`, restore the original bytes, resolve again successfully |
| No image, source-copy, move, replacement or relink behavior is introduced | Review of the diff: the resolver only reads; no file write, copy, move or repository call is added |
| `./gradlew check` passes | `./gradlew check shadowJar` on Linux, macOS and Windows |

## Implementation and verification

- Added `SourceResolver`, `ResolvedSource`, `SourceFailure` and `SourceException` to `arbiter.workspace`; no repository, service, UI or model file changed.
- A second review round tightened the stored-path grammar to one portable shape, added `resolveForImport` so registration applies the same checks, and added junction coverage. The Windows-only tests became cross-platform, because the grammar check rejects those paths on every system, and 134 tests now pass with none skipped.
- Review follow-up on PR #68 changed the design in four places: `ResolvedSource` no longer carries the resolved file, so consumers show the text the resolver already read; containment is now decided before the disk is touched and again on the real path; a `TOO_LARGE` failure bounds the file at `MAX_TEXT_BYTES`; and a leading byte-order mark is accepted and stripped. The hash comparison is now an exact match, as `RepositorySession` compares it, and Windows-only escape cases were added for the Windows CI job.
- `SourceResolverTest` covers the partitions in the verification table with 26 cases, and `SourceResolverSnapshotTest` compares `arbiter.json` byte-for-byte around a failed resolve.
- The decoder, the hash helper and the real-path containment check are the three places a wrong choice would silently pass a broken file, so each has direct negative tests.
- JDK 25 `./gradlew cleanTest check shadowJar --no-daemon` passed: 134 JUnit tests, none skipped, both Checkstyle tasks, and the release jar.

## Open questions

- **Who owns the `.txt` rule?** This plan enforces the extension in the resolver so [#25] cannot register a file that later fails to read. If the adjudicator track wants the importer to reject first, both may check it, but the resolver stays strict.
- **Should `ResolvedSource` expose size and modification time?** They are included so a later screen can show what was read without a second stat call. They can be dropped if nothing wants them.
- **Should the stored-path grammar live in one shared place?** It is enforced in `SourceResolver`, which both registration and reads call, so [#25] gets it through `resolveForImport`. If the importer ever validates a path before the file exists, that shape check would need to be callable on its own.
- **Is 10 MiB the right source limit?** It bounds memory without being smaller than any plausible plain-text corpus item. It is a constant on `SourceResolver`, so raising it is a one-line change, but the number itself is a product decision.

[#10]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/10
[#13]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/13
[#14]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/14
[#17]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/17
[#25]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/25
[#37]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/37
[#62]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/62
