# Resolve text sources and detect source changes

**Issue:** [#10] - Resolve text sources and detect source changes
**Branch:** `feat/source-resolution`
**Status:** Implemented and locally verified; awaiting human review and acceptance.

What the feature does is in [#10], and what each class means is in its Javadoc. This plan records what was decided along the way and what is still open.

## Goal and scope

Read one registered item's source text from the workspace, after proving the stored path still points at the same readable file it was registered from. [#25] will register paths and hashes, [#13]/[#14] will read through this boundary, and [#37] must report its error rather than export stale text.

Non-goals, from [#10]: corpus registration, images, thumbnails, decoding or caching, external sources, copying or moving source files, automatic relinking on rename, and accepting replacement content under an existing item.

## Proposed changes

- **`arbiter.workspace.SourceResolver`** is the one place a registered source is turned into text. It takes `WorkspacePaths` plus the stored path and hash, and returns an immutable `ResolvedSource`.
- **`ResolvedSource` is a record** in the same package, carrying the resolved absolute path, the hash actually read, the decoded text, and the file size and modification time. It exists so later screens can show the file that was read without re-reading it.
- **`SourceException`** carries a user-facing message naming the affected stored path and the reason, and extends `WorkspaceException` so the existing workspace error handling in `arbiter.ui.shared` covers it. Its reason is a small `SourceFailure` enum (`MISSING`, `NOT_A_FILE`, `OUTSIDE_MEDIA`, `UNREADABLE`, `HASH_MISMATCH`, `NOT_TEXT`, `INVALID_TEXT`) so callers and tests distinguish cases without parsing messages.
- **Containment is checked on resolved real paths.** The stored path is joined to the workspace root and normalised, then both it and the media root are resolved with `Path.toRealPath()` where the file exists. The result must be the media root itself or below it, compared as path elements rather than a string prefix, so `media-lookalike/` and `../` cannot pass. Rejection happens before any byte is read.
- **Symlink and junction policy: refuse, do not follow outside the root.** A `.txt` link inside `media/` whose target stays inside `media/` is readable; anything whose real path leaves the media root is `OUTSIDE_MEDIA`. This covers symbolic links and, on Windows, junction points, because `toRealPath()` follows both.
- **The file must be a regular file whose name ends in `.txt`** (case-insensitive), because [#62] made plain text the only v1 source type.
- **Hashing matches the registration side of [#25]:** SHA-256 over the raw bytes, lowercase hex. `SourceResolver.hash` is exposed so [#25] hashes through the same entry point instead of a second implementation. A mismatch raises `HASH_MISMATCH` and never updates the stored path or hash.
- **Text is loaded as strict UTF-8.** Bytes are decoded with `CharsetDecoder` in reporting mode, so invalid or truncated sequences fail as `INVALID_TEXT` rather than becoming replacement characters. Unreadable files (permissions, I/O errors) surface as `UNREADABLE` with the cause attached.
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
- **`toRealPath()` needs the path to exist.** A missing path fails as `MISSING` before containment, so a path that is both missing and outside the root reports `MISSING`. The reason order is stored-path shape, then existence, then containment, then hash, then text.
- **Files changing mid-read.** A file replaced between the hash read and the text read yields whichever bytes were read; the resolver does not lock files, because that would block the user editing their own corpus and [#10] does not ask for it.

## Verification

| Acceptance criterion | How it is verified |
| --- | --- |
| Nested `.txt` files under `media/` resolve from workspace-relative paths; absolute and external paths are rejected | Unit tests: a nested file resolves and returns its text; an absolute path and a sibling `../` path both fail with the stored path in the message |
| Traversal, prefix-lookalike paths and links/junctions cannot expose content outside the resolved media root | Unit tests: `media/../outside.txt`, a sibling `media-lookalike/` file and a symlink to an outside file all fail; a symlink staying inside `media/` resolves |
| Missing, unreadable and hash-mismatched files report the affected path and reason without changing project records | Unit tests for each failure, plus an integration test comparing the whole `arbiter.json` snapshot byte-for-byte before and after a failed resolve |
| Valid UTF-8 text is displayed; invalid or unreadable text fails clearly | Unit tests: ASCII, multi-byte and non-ASCII text resolve; a lone continuation byte, a truncated sequence and a UTF-16 file fail as `INVALID_TEXT` |
| An answer cannot be submitted and an included item cannot be exported after source validation fails | Deferred to [#17] and [#37], which own those gates; this plan makes the failure a checkable exception rather than a silent null |
| Restoring the original bytes at the recorded relative path makes validation succeed again | Unit test: resolve, overwrite the file, see `HASH_MISMATCH`, restore the original bytes, resolve again successfully |
| No image, source-copy, move, replacement or relink behavior is introduced | Review of the diff: the resolver only reads; no file write, copy, move or repository call is added |
| `./gradlew check` passes | `./gradlew check shadowJar` |

## Implementation and verification

- Added `SourceResolver`, `ResolvedSource`, `SourceFailure` and `SourceException` to `arbiter.workspace`; no repository, service, UI or model file changed.
- `SourceResolverTest` covers the partitions in the verification table with 26 cases, and `SourceResolverSnapshotTest` compares `arbiter.json` byte-for-byte around a failed resolve.
- The decoder, the hash helper and the real-path containment check are the three places a wrong choice would silently pass a broken file, so each has direct negative tests.
- JDK 25 `./gradlew check shadowJar --no-daemon` passed: 103 JUnit tests, both Checkstyle tasks, and the release jar.

## Open questions

- **Who owns the `.txt` rule?** This plan enforces the extension in the resolver so [#25] cannot register a file that later fails to read. If the adjudicator track wants the importer to reject first, both may check it, but the resolver stays strict.
- **Should `ResolvedSource` expose size and modification time?** They are included so a later screen can show what was read without a second stat call. They can be dropped if nothing wants them.

[#10]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/10
[#13]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/13
[#14]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/14
[#17]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/17
[#25]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/25
[#37]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/37
[#62]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/62
