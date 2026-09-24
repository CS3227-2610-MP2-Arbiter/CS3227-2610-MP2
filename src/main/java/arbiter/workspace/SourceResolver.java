package arbiter.workspace;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * Reads one registered source file and proves it is still the file that was registered.
 *
 * <p>This is the only place a stored item path becomes content (rule 21). It refuses, before it
 * touches the disk, a stored path that would leave the workspace's {@code media} folder; then it
 * requires a file to exist there, checks again on the resolved location so a link cannot escape,
 * requires a regular plain-text file no larger than {@link #MAX_TEXT_BYTES}, checks that the bytes
 * still match the hash recorded at registration and decodes them as UTF-8.
 *
 * <p>It only reads. It never writes, copies, moves, relinks or stores anything, and it never changes
 * the recorded path or hash, so a failure leaves every project record as it was and restoring the
 * original bytes restores access.
 *
 * <p>Nothing is cached, so each call sees the file as it is now.
 */
public final class SourceResolver {
    /** File extension of the plain-text sources this version accepts, including the leading dot. */
    public static final String TEXT_EXTENSION = ".txt";

    /**
     * Largest source this version reads. The whole file is read, hashed and decoded in memory, so a
     * much larger one would be refused rather than exhaust the heap.
     */
    public static final long MAX_TEXT_BYTES = 10L * 1024L * 1024L;

    /** Name of the hash algorithm recorded for each item at registration. */
    public static final String HASH_ALGORITHM = "SHA-256";

    /** Byte-order mark a UTF-8 file may start with, which is not part of the text. */
    private static final char BYTE_ORDER_MARK = '\uFEFF';

    private final WorkspacePaths paths;

    /** Creates a resolver for one workspace. */
    public SourceResolver(WorkspacePaths paths) {
        this.paths = Objects.requireNonNull(paths, "paths");
    }

    /**
     * Returns the lowercase hexadecimal SHA-256 hash of these bytes.
     *
     * @param bytes the file's bytes
     * @return the hash recorded for a registered item
     */
    public static String hash(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance(HASH_ALGORITHM).digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(HASH_ALGORITHM + " is unavailable in this Java runtime", e);
        }
    }

    /**
     * Reads the source registered at a workspace-relative path.
     *
     * @param storedPath the item's stored workspace-relative path, such as {@code media/corpus/review.txt}
     * @param contentHash the hash recorded for this item at registration
     * @return the text that was read and the details of the file it came from
     * @throws SourceException if the path is unusable, it leaves the workspace's {@code media} folder,
     *     the file is missing or unreadable, it is not a plain-text file, it is larger than
     *     {@link #MAX_TEXT_BYTES}, its bytes no longer match the recorded hash, or its bytes are not
     *     valid UTF-8
     */
    public ResolvedSource resolve(String storedPath, String contentHash) {
        Path candidate = locate(storedPath);
        if (contentHash == null || contentHash.isBlank()) {
            throw new SourceException(storedPath, SourceFailure.HASH_MISMATCH,
                    "No hash was recorded for " + storedPath + ", so its content cannot be trusted.");
        }
        Path file = resolvedInsideMedia(storedPath, candidate);
        if (!Files.isRegularFile(file)) {
            throw new SourceException(storedPath, SourceFailure.NOT_A_FILE,
                    "The recorded source is not a file: " + storedPath);
        }
        if (!hasTextExtension(file)) {
            throw new SourceException(storedPath, SourceFailure.NOT_TEXT,
                    "The recorded source is not a plain-text file: " + storedPath + " (expected "
                            + TEXT_EXTENSION + ")");
        }
        byte[] bytes = readAll(storedPath, file);
        String actualHash = hash(bytes);
        if (!actualHash.equals(contentHash)) {
            throw new SourceException(storedPath, SourceFailure.HASH_MISMATCH,
                    "The source file has changed since it was registered: " + storedPath
                            + ". Restore the original file to use this item.");
        }
        String text = decode(storedPath, bytes);
        return new ResolvedSource(storedPath, actualHash, text, bytes.length,
                lastModified(storedPath, file));
    }

    private Path locate(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            throw new SourceException(storedPath, SourceFailure.INVALID_PATH,
                    "This item has no recorded source path.");
        }
        Path recorded;
        try {
            recorded = Path.of(storedPath);
        } catch (InvalidPathException e) {
            throw new SourceException(storedPath, SourceFailure.INVALID_PATH,
                    "The recorded source path cannot be used: " + storedPath, e);
        }
        if (recorded.isAbsolute()) {
            throw new SourceException(storedPath, SourceFailure.INVALID_PATH,
                    "Sources are recorded as workspace-relative paths under "
                            + WorkspacePaths.MEDIA_DIRECTORY + "/, but this item records an absolute path: "
                            + storedPath);
        }
        Path candidate = paths.root().resolve(recorded).normalize();
        // Containment is decided before the disk is touched, so a path that leaves media/ is reported
        // as outside it whether or not anything exists at the far end. Links are checked again below,
        // because only the real path can show where they point.
        requireInsideMedia(storedPath, candidate, paths.mediaDirectory());
        return candidate;
    }

    private Path resolvedInsideMedia(String storedPath, Path candidate) {
        Path mediaRoot = resolveReal(storedPath, paths.mediaDirectory(), SourceFailure.MISSING,
                "The workspace's " + WorkspacePaths.MEDIA_DIRECTORY + "/ folder is missing, so "
                        + storedPath + " cannot be read.");
        Path file = resolveReal(storedPath, candidate, SourceFailure.MISSING,
                "The recorded source file is missing: " + storedPath);
        requireInsideMedia(storedPath, file, mediaRoot);
        return file;
    }

    private static void requireInsideMedia(String storedPath, Path path, Path mediaRoot) {
        if (!path.startsWith(mediaRoot)) {
            throw new SourceException(storedPath, SourceFailure.OUTSIDE_MEDIA,
                    "The recorded source points outside the workspace's " + WorkspacePaths.MEDIA_DIRECTORY
                            + "/ folder: " + storedPath);
        }
    }

    private static Path resolveReal(String storedPath, Path path, SourceFailure missingReason,
            String missingMessage) {
        try {
            // Files.exists does not follow links, so a link to a missing file is reported as missing.
            if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                throw new SourceException(storedPath, missingReason, missingMessage);
            }
            return path.toRealPath();
        } catch (NoSuchFileException | NotDirectoryException e) {
            throw new SourceException(storedPath, missingReason, missingMessage, e);
        } catch (IOException | SecurityException e) {
            throw new SourceException(storedPath, SourceFailure.UNREADABLE,
                    "The recorded source cannot be read: " + storedPath, e);
        }
    }

    private static boolean hasTextExtension(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(TEXT_EXTENSION) && name.length() > TEXT_EXTENSION.length();
    }

    private static byte[] readAll(String storedPath, Path file) {
        try (InputStream input = Files.newInputStream(file)) {
            // Reading one byte past the limit keeps a file that grew since the size check bounded.
            byte[] bytes = input.readNBytes(Math.toIntExact(MAX_TEXT_BYTES + 1));
            if (bytes.length > MAX_TEXT_BYTES) {
                throw new SourceException(storedPath, SourceFailure.TOO_LARGE,
                        "The recorded source is larger than the " + MAX_TEXT_BYTES + "-byte limit: "
                                + storedPath);
            }
            return bytes;
        } catch (IOException | SecurityException e) {
            throw new SourceException(storedPath, SourceFailure.UNREADABLE,
                    "The recorded source cannot be read: " + storedPath, e);
        }
    }

    private static String decode(String storedPath, byte[] bytes) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            String text = decoder.decode(ByteBuffer.wrap(bytes)).toString();
            // A byte-order mark is invisible, so showing it would put an unexplained character in
            // front of the item. The hash is taken over the bytes, so the file must keep it.
            return text.startsWith(String.valueOf(BYTE_ORDER_MARK)) ? text.substring(1) : text;
        } catch (CharacterCodingException e) {
            throw new SourceException(storedPath, SourceFailure.INVALID_TEXT,
                    "The recorded source is not valid UTF-8 text: " + storedPath, e);
        }
    }

    private static Instant lastModified(String storedPath, Path file) {
        try {
            return Files.readAttributes(file, BasicFileAttributes.class).lastModifiedTime().toInstant();
        } catch (IOException | SecurityException e) {
            throw new SourceException(storedPath, SourceFailure.UNREADABLE,
                    "The recorded source cannot be read: " + storedPath, e);
        }
    }
}
