package arbiter.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Checks that the link fixtures make a working link, or report that they could not instead of throwing. */
class FileLinksTest {
    @TempDir
    Path temporary;

    @Test
    void createDirectoryLink_existingFolder_linkLeadsToTarget() throws IOException {
        // This link needs no privilege on any platform the build runs on, so a failure here means
        // the tests that skip without a link have silently stopped running.
        Path target = Files.createDirectories(temporary.resolve("target"));
        Path link = temporary.resolve("link");

        assertTrue(FileLinks.createDirectoryLink(link, target));

        Files.writeString(target.resolve("later.txt"), "Written after linking");
        assertEquals("Written after linking", Files.readString(link.resolve("later.txt")));
    }

    @Test
    void createDirectoryLink_linkPathTaken_falseReturned() throws IOException {
        Path target = Files.createDirectories(temporary.resolve("target"));
        Path taken = Files.createDirectories(temporary.resolve("taken"));

        assertFalse(FileLinks.createDirectoryLink(taken, target));
    }

    @Test
    void createSymlink_linkPathTaken_falseReturnedAndFileKept() throws IOException {
        Path target = Files.writeString(temporary.resolve("target.txt"), "Target");
        Path taken = Files.writeString(temporary.resolve("taken.txt"), "Taken");

        assertFalse(FileLinks.createSymlink(taken, target));
        assertEquals("Taken", Files.readString(taken));
    }
}
