package arbiter.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit checks for the one boundary that turns a recorded source path into text. */
class SourceResolverTest {
    private static final String TEXT_PATH = "media/corpus/review.txt";

    @TempDir
    Path temporary;

    private WorkspacePaths paths;
    private SourceResolver resolver;

    @BeforeEach
    void setUp() throws IOException {
        paths = new WorkspacePaths(Files.createDirectories(temporary.resolve("workspace")));
        Files.createDirectories(paths.mediaDirectory());
        resolver = new SourceResolver(paths);
    }

    @Test
    void resolve_nestedTextFile_textAndDetailsReturned() throws IOException {
        byte[] bytes = "The review was positive.".getBytes(StandardCharsets.UTF_8);
        write(TEXT_PATH, bytes);

        ResolvedSource source = resolver.resolve(TEXT_PATH, SourceResolver.hash(bytes));

        assertEquals("The review was positive.", source.text());
        assertEquals(TEXT_PATH, source.storedPath());
        assertEquals(SourceResolver.hash(bytes), source.contentHash());
        assertEquals(bytes.length, source.size());
        assertNotNull(source.modifiedAt());
    }

    @Test
    void resolve_uppercaseExtension_textReturned() throws IOException {
        byte[] bytes = "UpperCase".getBytes(StandardCharsets.UTF_8);
        write("media/REVIEW.TXT", bytes);

        assertEquals("UpperCase", resolver.resolve("media/REVIEW.TXT", SourceResolver.hash(bytes)).text());
    }

    @Test
    void resolve_emptyFile_emptyTextReturned() throws IOException {
        byte[] bytes = new byte[0];
        write(TEXT_PATH, bytes);

        ResolvedSource source = resolver.resolve(TEXT_PATH, SourceResolver.hash(bytes));

        assertEquals("", source.text());
        assertEquals(0, source.size());
    }

    @Test
    void resolve_nonAsciiUtf8_textReturned() throws IOException {
        byte[] bytes = "café 日本語 😀".getBytes(StandardCharsets.UTF_8);
        write(TEXT_PATH, bytes);

        assertEquals("café 日本語 😀", resolver.resolve(TEXT_PATH, SourceResolver.hash(bytes)).text());
    }

    @Test
    void resolve_traversalOutsideMedia_exceptionThrown() throws IOException {
        byte[] bytes = "secret".getBytes(StandardCharsets.UTF_8);
        write("outside.txt", bytes);

        SourceException failure = assertThrows(SourceException.class, () ->
                resolver.resolve("media/../outside.txt", SourceResolver.hash(bytes)));

        assertEquals(SourceFailure.OUTSIDE_MEDIA, failure.reason());
        assertTrue(failure.getMessage().contains("media/../outside.txt"), failure.getMessage());
    }

    @Test
    void resolve_prefixLookalikeDirectory_exceptionThrown() throws IOException {
        byte[] bytes = "secret".getBytes(StandardCharsets.UTF_8);
        write("media-lookalike/other.txt", bytes);

        SourceException failure = assertThrows(SourceException.class, () ->
                resolver.resolve("media-lookalike/other.txt", SourceResolver.hash(bytes)));

        assertEquals(SourceFailure.OUTSIDE_MEDIA, failure.reason());
        assertTrue(failure.getMessage().contains("media-lookalike/other.txt"), failure.getMessage());
    }

    @Test
    void resolve_pathRecordedWithoutMediaPrefix_exceptionThrown() throws IOException {
        byte[] bytes = "secret".getBytes(StandardCharsets.UTF_8);
        write("outside.txt", bytes);

        SourceException failure = assertThrows(SourceException.class, () ->
                resolver.resolve("outside.txt", SourceResolver.hash(bytes)));

        assertEquals(SourceFailure.OUTSIDE_MEDIA, failure.reason());
    }

    @Test
    void resolve_absolutePathRecorded_exceptionThrown() throws IOException {
        byte[] bytes = "text".getBytes(StandardCharsets.UTF_8);
        write(TEXT_PATH, bytes);

        SourceException failure = assertThrows(SourceException.class, () ->
                resolver.resolve(paths.root().resolve(TEXT_PATH).toString(), SourceResolver.hash(bytes)));

        assertEquals(SourceFailure.INVALID_PATH, failure.reason());
    }

    @Test
    void resolve_blankPathRecorded_exceptionThrown() {
        SourceException failure = assertThrows(SourceException.class, () -> resolver.resolve("   ", "hash"));

        assertEquals(SourceFailure.INVALID_PATH, failure.reason());
    }

    @Test
    void resolve_nullPathRecorded_exceptionThrown() {
        SourceException failure = assertThrows(SourceException.class, () -> resolver.resolve(null, "hash"));

        assertEquals(SourceFailure.INVALID_PATH, failure.reason());
    }

    @Test
    void resolve_fileMissing_exceptionThrown() {
        SourceException failure = assertThrows(SourceException.class, () -> resolver.resolve(TEXT_PATH, "hash"));

        assertEquals(SourceFailure.MISSING, failure.reason());
        assertEquals(TEXT_PATH, failure.storedPath());
        assertTrue(failure.getMessage().contains(TEXT_PATH), failure.getMessage());
    }

    @Test
    void resolve_mediaFolderMissing_exceptionThrown() throws IOException {
        Files.delete(paths.mediaDirectory());

        SourceException failure = assertThrows(SourceException.class, () -> resolver.resolve(TEXT_PATH, "hash"));

        assertEquals(SourceFailure.MISSING, failure.reason());
    }

    @Test
    void resolve_directoryInsteadOfFile_exceptionThrown() throws IOException {
        Files.createDirectories(paths.root().resolve("media/folder.txt"));

        SourceException failure = assertThrows(SourceException.class, () ->
                resolver.resolve("media/folder.txt", "hash"));

        assertEquals(SourceFailure.NOT_A_FILE, failure.reason());
    }

    @Test
    void resolve_nonTextExtension_exceptionThrown() throws IOException {
        byte[] bytes = "not text".getBytes(StandardCharsets.UTF_8);
        write("media/review.pdf", bytes);

        SourceException failure = assertThrows(SourceException.class, () ->
                resolver.resolve("media/review.pdf", SourceResolver.hash(bytes)));

        assertEquals(SourceFailure.NOT_TEXT, failure.reason());
        assertTrue(failure.getMessage().contains("media/review.pdf"), failure.getMessage());
    }

    @Test
    void resolve_hashMismatch_exceptionThrown() throws IOException {
        byte[] original = "original".getBytes(StandardCharsets.UTF_8);
        write(TEXT_PATH, original);
        write(TEXT_PATH, "replaced".getBytes(StandardCharsets.UTF_8));

        SourceException failure = assertThrows(SourceException.class, () ->
                resolver.resolve(TEXT_PATH, SourceResolver.hash(original)));

        assertEquals(SourceFailure.HASH_MISMATCH, failure.reason());
        assertTrue(failure.getMessage().contains(TEXT_PATH), failure.getMessage());
    }

    @Test
    void resolve_hashNotRecorded_exceptionThrown() throws IOException {
        write(TEXT_PATH, "text".getBytes(StandardCharsets.UTF_8));

        SourceException failure = assertThrows(SourceException.class, () -> resolver.resolve(TEXT_PATH, " "));

        assertEquals(SourceFailure.HASH_MISMATCH, failure.reason());
    }

    @Test
    void resolve_hashRecordedWithDifferentCase_exceptionThrown() throws IOException {
        // Registration stores one exact hash, as RepositorySession compares it, so a hash that
        // differs only in case is not the recorded one.
        byte[] bytes = "text".getBytes(StandardCharsets.UTF_8);
        write(TEXT_PATH, bytes);

        SourceException failure = assertThrows(SourceException.class, () ->
                resolver.resolve(TEXT_PATH, SourceResolver.hash(bytes).toUpperCase()));

        assertEquals(SourceFailure.HASH_MISMATCH, failure.reason());
    }

    @Test
    void resolve_originalBytesRestored_validationSucceedsAgain() throws IOException {
        byte[] original = "original".getBytes(StandardCharsets.UTF_8);
        write(TEXT_PATH, original);
        String recordedHash = SourceResolver.hash(original);
        resolver.resolve(TEXT_PATH, recordedHash);
        write(TEXT_PATH, "replaced".getBytes(StandardCharsets.UTF_8));
        assertThrows(SourceException.class, () -> resolver.resolve(TEXT_PATH, recordedHash));

        write(TEXT_PATH, original);

        assertEquals("original", resolver.resolve(TEXT_PATH, recordedHash).text());
    }

    @Test
    void resolve_loneContinuationByte_exceptionThrown() throws IOException {
        byte[] bytes = {(byte) 0x80, 'a'};
        write(TEXT_PATH, bytes);

        SourceException failure = assertThrows(SourceException.class, () ->
                resolver.resolve(TEXT_PATH, SourceResolver.hash(bytes)));

        assertEquals(SourceFailure.INVALID_TEXT, failure.reason());
        assertTrue(failure.getMessage().contains(TEXT_PATH), failure.getMessage());
    }

    @Test
    void resolve_truncatedMultiByteSequence_exceptionThrown() throws IOException {
        byte[] bytes = {'a', (byte) 0xE2, (byte) 0x82};
        write(TEXT_PATH, bytes);

        SourceException failure = assertThrows(SourceException.class, () ->
                resolver.resolve(TEXT_PATH, SourceResolver.hash(bytes)));

        assertEquals(SourceFailure.INVALID_TEXT, failure.reason());
    }

    @Test
    void resolve_utf16Text_exceptionThrown() throws IOException {
        byte[] bytes = "text".getBytes(StandardCharsets.UTF_16);
        write(TEXT_PATH, bytes);

        SourceException failure = assertThrows(SourceException.class, () ->
                resolver.resolve(TEXT_PATH, SourceResolver.hash(bytes)));

        assertEquals(SourceFailure.INVALID_TEXT, failure.reason());
    }

    @Test
    void resolve_symlinkLeavingMedia_exceptionThrown() throws IOException {
        byte[] bytes = "secret".getBytes(StandardCharsets.UTF_8);
        write("outside.txt", bytes);
        Path link = paths.mediaDirectory().resolve("link.txt");
        assumeTrue(createSymlink(link, paths.root().resolve("outside.txt")), "symbolic links unavailable");

        SourceException failure = assertThrows(SourceException.class, () ->
                resolver.resolve("media/link.txt", SourceResolver.hash(bytes)));

        assertEquals(SourceFailure.OUTSIDE_MEDIA, failure.reason());
    }

    @Test
    void resolve_danglingSymlink_exceptionThrown() throws IOException {
        Path link = paths.mediaDirectory().resolve("link.txt");
        assumeTrue(createSymlink(link, paths.root().resolve("media/gone.txt")), "symbolic links unavailable");

        SourceException failure = assertThrows(SourceException.class, () -> resolver.resolve("media/link.txt", "hash"));

        assertEquals(SourceFailure.MISSING, failure.reason());
    }

    @Test
    void resolve_symlinkInsideMedia_textReturned() throws IOException {
        byte[] bytes = "linked".getBytes(StandardCharsets.UTF_8);
        write(TEXT_PATH, bytes);
        Path link = paths.mediaDirectory().resolve("link.txt");
        assumeTrue(createSymlink(link, paths.root().resolve(TEXT_PATH)), "symbolic links unavailable");

        assertEquals("linked", resolver.resolve("media/link.txt", SourceResolver.hash(bytes)).text());
    }

    @Test
    void resolve_unreadableFile_exceptionThrown() throws IOException {
        Path file = write(TEXT_PATH, "text".getBytes(StandardCharsets.UTF_8));
        assumeTrue(Files.getFileStore(file).supportsFileAttributeView("posix"), "POSIX permissions unavailable");
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("---------"));
        assumeTrue(!Files.isReadable(file), "the test cannot make a file unreadable");

        SourceException failure = assertThrows(SourceException.class, () -> resolver.resolve(TEXT_PATH, "hash"));

        assertEquals(SourceFailure.UNREADABLE, failure.reason());
    }

    @Test
    void resolve_fileAtSizeLimit_textReturned() throws IOException {
        byte[] bytes = new byte[(int) SourceResolver.MAX_TEXT_BYTES];
        java.util.Arrays.fill(bytes, (byte) 'a');
        write(TEXT_PATH, bytes);

        ResolvedSource source = resolver.resolve(TEXT_PATH, SourceResolver.hash(bytes));

        assertEquals(bytes.length, source.text().length());
        assertEquals(bytes.length, source.size());
    }

    @Test
    void resolve_fileOneByteOverSizeLimit_exceptionThrown() throws IOException {
        byte[] bytes = new byte[(int) SourceResolver.MAX_TEXT_BYTES + 1];
        java.util.Arrays.fill(bytes, (byte) 'a');
        write(TEXT_PATH, bytes);

        SourceException failure = assertThrows(SourceException.class, () ->
                resolver.resolve(TEXT_PATH, SourceResolver.hash(bytes)));

        assertEquals(SourceFailure.TOO_LARGE, failure.reason());
        assertTrue(failure.getMessage().contains(TEXT_PATH), failure.getMessage());
    }

    @Test
    void resolve_byteOrderMark_textReturnedWithoutIt() throws IOException {
        // A UTF-8 file may open with a byte-order mark. It is hashed as written but not shown.
        byte[] bytes = "\uFEFFThe review was positive.".getBytes(StandardCharsets.UTF_8);
        write(TEXT_PATH, bytes);

        ResolvedSource source = resolver.resolve(TEXT_PATH, SourceResolver.hash(bytes));

        assertEquals("The review was positive.", source.text());
        assertEquals(bytes.length, source.size());
        assertEquals(SourceResolver.hash(bytes), source.contentHash());
    }

    @Test
    void resolve_byteOrderMarkInsideText_textReturnedUnchanged() throws IOException {
        byte[] bytes = "a\uFEFFb".getBytes(StandardCharsets.UTF_8);
        write(TEXT_PATH, bytes);

        assertEquals("a\uFEFFb", resolver.resolve(TEXT_PATH, SourceResolver.hash(bytes)).text());
    }

    @Test
    void resolve_backslashTraversalOnWindows_exceptionThrown() throws IOException {
        assumeTrue(java.io.File.separatorChar == '\\', "Windows-only path shape");
        byte[] bytes = "secret".getBytes(StandardCharsets.UTF_8);
        write("outside.txt", bytes);

        SourceException failure = assertThrows(SourceException.class, () ->
                resolver.resolve("media\\..\\outside.txt", SourceResolver.hash(bytes)));

        assertEquals(SourceFailure.OUTSIDE_MEDIA, failure.reason());
    }

    @Test
    void resolve_uncPathOnWindows_exceptionThrown() {
        assumeTrue(java.io.File.separatorChar == '\\', "Windows-only path shape");

        SourceException failure = assertThrows(SourceException.class, () ->
                resolver.resolve("\\\\server\\share\\review.txt", "hash"));

        assertEquals(SourceFailure.INVALID_PATH, failure.reason());
    }

    @Test
    void resolve_absoluteWindowsPath_exceptionThrown() throws IOException {
        assumeTrue(java.io.File.separatorChar == '\\', "Windows-only path shape");
        write(TEXT_PATH, "text".getBytes(StandardCharsets.UTF_8));

        SourceException failure = assertThrows(SourceException.class, () ->
                resolver.resolve("C:\\outside\\review.txt", "hash"));

        assertEquals(SourceFailure.INVALID_PATH, failure.reason());
    }

    @Test
    void hash_knownInputs_expectedDigestReturned() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                SourceResolver.hash(new byte[0]));
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                SourceResolver.hash("abc".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void hash_sameBytesTwice_sameDigestReturned() {
        byte[] bytes = "abc".getBytes(StandardCharsets.UTF_8);

        assertEquals(SourceResolver.hash(bytes), SourceResolver.hash(bytes));
    }

    @Test
    void resolve_modifiedLaterThanRegistration_stillResolved() throws IOException {
        byte[] bytes = "text".getBytes(StandardCharsets.UTF_8);
        Path file = write(TEXT_PATH, bytes);
        Files.setLastModifiedTime(file, java.nio.file.attribute.FileTime.from(Instant.parse("2020-01-01T00:00:00Z")));

        ResolvedSource source = resolver.resolve(TEXT_PATH, SourceResolver.hash(bytes));

        assertEquals(Instant.parse("2020-01-01T00:00:00Z"), source.modifiedAt());
    }

    @Test
    void resolve_siblingItemUnchangedByFailure() throws IOException {
        byte[] bytes = "text".getBytes(StandardCharsets.UTF_8);
        write(TEXT_PATH, bytes);
        write("media/other.txt", "other".getBytes(StandardCharsets.UTF_8));
        resolver.resolve("media/other.txt", SourceResolver.hash("other".getBytes(StandardCharsets.UTF_8)));

        assertThrows(SourceException.class, () -> resolver.resolve(TEXT_PATH, "wrong-hash"));

        List<Path> filesInsideMedia = List.of(paths.mediaDirectory().resolve("corpus/review.txt"),
                paths.mediaDirectory().resolve("other.txt"));
        assertTrue(filesInsideMedia.stream().allMatch(Files::isRegularFile));
        assertEquals("text", Files.readString(filesInsideMedia.getFirst()));
    }

    private Path write(String relativePath, byte[] bytes) throws IOException {
        Path file = paths.root().resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.write(file, bytes);
        return file;
    }

    private static boolean createSymlink(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
            return true;
        } catch (IOException | UnsupportedOperationException e) {
            return false;
        }
    }
}
