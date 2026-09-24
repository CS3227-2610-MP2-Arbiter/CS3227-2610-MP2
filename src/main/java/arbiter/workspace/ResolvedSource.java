package arbiter.workspace;

import java.time.Instant;

/**
 * One source file that passed every check and was read.
 *
 * <p>It carries the text itself, not a handle to the file: a later screen shows this text rather
 * than opening the file again, so nothing reaches a source without passing the resolver's checks.
 *
 * @param storedPath the workspace-relative path that was resolved
 * @param contentHash the lowercase hexadecimal SHA-256 hash of the bytes that were read
 * @param text the file's content as UTF-8 text, without a leading byte-order mark
 * @param size the size of the file in bytes
 * @param modifiedAt when the file was last modified
 */
public record ResolvedSource(String storedPath, String contentHash, String text, long size,
        Instant modifiedAt) {
}
