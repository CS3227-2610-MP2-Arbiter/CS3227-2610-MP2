package arbiter.workspace;

import java.io.IOException;
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
 * <p>This is the only place a stored item path becomes content (rule 21). It checks, in order, that
 * the stored path is a usable workspace-relative path, that a file exists there, that the resolved
 * location stays beneath the resolved {@code media} folder, that the file is a plain-text file, that
 * its bytes still match the hash recorded at registration and that those bytes are valid UTF-8.
 *
 * <p>It only reads. It never writes, copies, moves, relinks or stores anything, and it never changes
 * the recorded path or hash, so a failure leaves every project record as it was and restoring the
 * original bytes restores access.
 *
 * <p>Nothing is cached, so each call sees the file as it is now.
 */
public final class SourceResolver {
    /** File extension, without the dot, of the plain-text sources this version accepts. */
    public static final String TEXT_EXTENSION = ".txt";

    /** Name of the hash algorithm recorded for each item at registration. */
    public static final String HASH_ALGORITHM = "SHA-256";

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
     * @return the text and details of the file that was read
     * @throws SourceException if the path is unusable, the file is missing or unreadable, it resolves
     *     outside the workspace's {@code media} folder, it is not a plain-text file, its bytes no
     *     longer match the recorded hash, or its bytes are not valid UTF-8
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
        if (!actualHash.equalsIgnoreCase(contentHash.trim())) {
            throw new SourceException(storedPath, SourceFailure.HASH_MISMATCH,
                    "The source file has changed since it was registered: " + storedPath
                            + ". Restore the original file to use this item.");
        }
        String text = decode(storedPath, bytes);
        return new ResolvedSource(storedPath, file, actualHash, text, bytes.length, lastModified(storedPath, file));
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
        return paths.root().resolve(recorded).normalize();
    }

    private Path resolvedInsideMedia(String storedPath, Path candidate) {
        Path mediaRoot = resolveReal(storedPath, paths.mediaDirectory(), SourceFailure.MISSING,
                "The workspace's " + WorkspacePaths.MEDIA_DIRECTORY + "/ folder is missing, so "
                        + storedPath + " cannot be read.");
        Path file = resolveReal(storedPath, candidate, SourceFailure.MISSING,
                "The recorded source file is missing: " + storedPath);
        if (!file.startsWith(mediaRoot)) {
            throw new SourceException(storedPath, SourceFailure.OUTSIDE_MEDIA,
                    "The recorded source points outside the workspace's " + WorkspacePaths.MEDIA_DIRECTORY
                            + "/ folder: " + storedPath);
        }
        return file;
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
        try {
            return Files.readAllBytes(file);
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
            return decoder.decode(ByteBuffer.wrap(bytes)).toString();
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
