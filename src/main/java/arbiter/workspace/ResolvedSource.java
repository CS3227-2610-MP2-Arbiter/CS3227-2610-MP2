package arbiter.workspace;

import java.nio.file.Path;
import java.time.Instant;

/**
 * One source file that passed every check and was read.
 *
 * @param storedPath the workspace-relative path that was resolved
 * @param file the absolute file that was read, with links and {@code ..} already resolved
 * @param contentHash the lowercase hexadecimal SHA-256 hash of the bytes that were read
 * @param text the file's content as UTF-8 text
 * @param size the size of the file in bytes
 * @param modifiedAt when the file was last modified
 */
public record ResolvedSource(String storedPath, Path file, String contentHash, String text, long size,
        Instant modifiedAt) {
}
