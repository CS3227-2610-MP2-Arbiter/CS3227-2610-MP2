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
import java.util.StringJoiner;

/**
 * Reads one source file and proves it is the file that was registered.
 *
 * <p>This is the only place a stored item path becomes content (rule 21). It refuses, before it
 * touches the disk, any stored path outside the portable shape it accepts; then it requires a file
 * to exist there, checks the resolved location so a link or junction cannot escape, requires a
 * regular plain-text file no larger than {@link #MAX_TEXT_BYTES}, checks the bytes against the hash
 * recorded at registration and decodes them as UTF-8.
 *
 * <p>A stored path is relative, uses {@code /} between segments, starts with {@code media/} and
 * keeps the case it was registered with. Rejecting {@code \} and {@code :} keeps the form portable,
 * because both are separators or drive syntax on Windows. Reads do not check the case, because
 * whether a name resolves regardless of case is a property of the file system, not of the path.
 * Instead, {@link #resolveForImport} refuses a file chosen by a name spelled differently from the
 * one on disk, so a stored path matches the disk from registration ([#25]) on.
 *
 * <p>A malformed path is {@code INVALID_PATH}; a well-formed path whose target has been replaced by
 * a link or junction is {@code OUTSIDE_MEDIA}.
 *
 * <p>{@link #resolve} reads a registered source. {@link #resolveForImport} makes a chosen file's
 * stored path and then applies exactly the same checks, so [#25] cannot accept a file that could
 * not be read afterwards. It only reads: nothing is written, copied, moved, relinked or stored, so a
 * failure leaves every project record as it was and restoring the original bytes restores access.
 *
 * <p>Nothing is cached, so each call sees the file as it is now.
 *
 * [#25]: https://github.com/CS3227-2610-MP2-Arbiter/CS3227-2610-MP2/issues/25
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
        if (contentHash == null || contentHash.isBlank()) {
            locate(storedPath);
            throw new SourceException(storedPath, SourceFailure.HASH_MISMATCH,
                    "No hash was recorded for " + storedPath + ", so its content cannot be trusted.");
        }
        return read(storedPath, contentHash);
    }

    /**
     * Checks a file chosen for registration, and returns what to record for it.
     *
     * <p>Registration ([#25]) calls this instead of reading the file itself. The file must be inside
     * {@code media/} and chosen by the names on disk, compared without following links so a link
     * inside {@code media/} keeps its own name. Its stored path is its path below the workspace with
     * {@code /} between segments, and the same checks as {@link #resolve} then run on it, except the
     * ones that compare against a recorded hash. The returned stored path and hash are the ones to
     * store, exactly as they are returned, because registration and later reads compare them exactly.
     *
     * @param file the chosen file, made absolute against the working directory if it is relative
     * @return the stored path, the file's text and the hash to record for it
     * @throws SourceException if the file is outside {@code media/} or chosen by a name spelled
     *     differently from the one on disk, or for the same reasons as {@link #resolve} except that no
     *     recorded hash is needed
     */
    public ResolvedSource resolveForImport(Path file) {
        Path chosen = Objects.requireNonNull(file, "file").toAbsolutePath().normalize();
        if (!chosen.startsWith(paths.mediaDirectory())) {
            throw new SourceException(null, SourceFailure.OUTSIDE_MEDIA, "Only files inside the workspace's "
                    + WorkspacePaths.MEDIA_DIRECTORY + "/ folder can be registered: " + chosen);
        }
        Path relative = paths.root().relativize(chosen);
        String storedPath = storedForm(relative);
        requireNamesOnDisk(storedPath, chosen, relative.getNameCount());
        return read(storedPath, null);
    }

    /**
     * Refuses a chosen file whose stored path is spelled differently from the names on disk, which a
     * case-insensitive file system would otherwise find. A missing file is left for the read checks to
     * report.
     */
    private static void requireNamesOnDisk(String storedPath, Path chosen, int segments) {
        if (!Files.exists(chosen, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        Path onDisk = resolveReal(storedPath, chosen, SourceFailure.MISSING,
                "The recorded source file is missing: " + storedPath, LinkOption.NOFOLLOW_LINKS);
        // Compared as strings: on Windows, Path.equals ignores case.
        String spelledOnDisk = storedForm(onDisk.subpath(onDisk.getNameCount() - segments, onDisk.getNameCount()));
        if (!spelledOnDisk.equals(storedPath)) {
            throw new SourceException(storedPath, SourceFailure.INVALID_PATH, "The chosen file is named "
                    + spelledOnDisk + " on disk, not " + storedPath + ". Choose it by the name on disk.");
        }
    }

    /** Joins a workspace-relative path's segments with {@code /}, the separator a stored path uses. */
    private static String storedForm(Path relative) {
        StringJoiner stored = new StringJoiner("/");
        for (Path segment : relative) {
            stored.add(segment.toString());
        }
        return stored.toString();
    }

    private ResolvedSource read(String storedPath, String expectedHash) {
        Path candidate = locate(storedPath);
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
        if (expectedHash != null && !actualHash.equals(expectedHash)) {
            throw new SourceException(storedPath, SourceFailure.HASH_MISMATCH,
                    "The source file has changed since it was registered: " + storedPath
                            + ". Restore the original file to use this item.");
        }
        String text = decode(storedPath, bytes);
        return new ResolvedSource(storedPath, actualHash, text, bytes.length,
                lastModified(storedPath, file));
    }

    private Path locate(String storedPath) {
        String shape = "A stored source path is relative, uses / between segments, keeps the case it was "
                + "registered with, and starts with " + WorkspacePaths.MEDIA_DIRECTORY + "/.";
        if (storedPath == null || storedPath.isBlank()) {
            throw new SourceException(storedPath, SourceFailure.INVALID_PATH,
                    "This item has no recorded source path. " + shape);
        }
        if (storedPath.indexOf('\\') >= 0 || storedPath.indexOf(':') >= 0) {
            throw new SourceException(storedPath, SourceFailure.INVALID_PATH,
                    "The recorded source path uses a Windows separator or drive: " + storedPath
                            + ". " + shape);
        }
        String[] segments = storedPath.split("/", -1);
        boolean startsWithMedia = segments.length > 1
                && WorkspacePaths.MEDIA_DIRECTORY.equals(segments[0]);
        for (String segment : segments) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw new SourceException(storedPath, SourceFailure.INVALID_PATH,
                        "The recorded source path has an empty or relative segment: " + storedPath
                                + ". " + shape);
            }
        }
        if (!startsWithMedia) {
            throw new SourceException(storedPath, SourceFailure.INVALID_PATH,
                    "The recorded source path is not inside " + WorkspacePaths.MEDIA_DIRECTORY
                            + "/: " + storedPath + ". " + shape);
        }
        try {
            return paths.root().resolve(storedPath);
        } catch (InvalidPathException e) {
            throw new SourceException(storedPath, SourceFailure.INVALID_PATH,
                    "The recorded source path cannot be used: " + storedPath + ". " + shape, e);
        }
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
            String missingMessage, LinkOption... options) {
        try {
            // Files.exists does not follow links, so a link to a missing file is reported as missing.
            if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                throw new SourceException(storedPath, missingReason, missingMessage);
            }
            return path.toRealPath(options);
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
