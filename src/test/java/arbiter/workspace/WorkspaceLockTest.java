package arbiter.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import arbiter.data.json.JsonStore;

/** Exercises workspace lock ownership within and across processes. */
public class WorkspaceLockTest {
    private static final int LOCK_OFFSET = 1024 * 1024;

    @TempDir
    Path temporary;

    @Test
    void acquire_sameWorkspaceInUse_reportsContentionWithoutChangingData() throws Exception {
        WorkspacePaths paths = workspace("workspace");
        Files.writeString(paths.dataFile(), "untouched");
        String metadata = Files.readString(paths.metadataFile());
        long metadataSize = Files.size(paths.metadataFile());

        try (WorkspaceLock ignored = WorkspaceLock.acquire(paths)) {
            WorkspaceException failure = assertThrows(WorkspaceException.class, () -> WorkspaceLock.acquire(paths));

            assertTrue(failure.getMessage().contains("already in use"), failure.getMessage());
            assertEquals("untouched", Files.readString(paths.dataFile()));

            Path result = temporary.resolve("probe-result");
            Process probe = startChild(Probe.class, paths, result);
            try {
                assertTrue(probe.waitFor(5, TimeUnit.SECONDS), "Probe process timed out");
                assertEquals(0, probe.exitValue(), new String(probe.getInputStream().readAllBytes()));
                assertEquals("contended", Files.readString(result));
            } finally {
                probe.destroyForcibly();
            }
        }
        assertEquals(metadata, Files.readString(paths.metadataFile()));
        assertEquals(metadataSize, Files.size(paths.metadataFile()));
    }

    @Test
    void acquire_heldMetadata_allowsDataInitializationAndOpen() {
        WorkspacePaths paths = workspace("workspace");

        try (WorkspaceLock ignored = WorkspaceLock.acquire(paths)) {
            JsonStore.initializeNew(paths);
            assertTrue(JsonStore.open(paths).<Boolean>read(session -> session.users().listAll().isEmpty()));
        }
    }

    @Test
    void acquire_metadataDeletedWhileHeld_secondProcessRefused() throws Exception {
        WorkspacePaths paths = workspace("workspace");

        try (WorkspaceLock ignored = WorkspaceLock.acquire(paths)) {
            boolean deleted;
            try {
                Files.delete(paths.metadataFile());
                deleted = true;
            } catch (FileSystemException e) {
                assertTrue(Files.isRegularFile(paths.metadataFile()), e.getMessage());
                deleted = false;
            }

            Path result = temporary.resolve("deleted-metadata-probe-result");
            Process probe = startChild(Probe.class, paths, result);
            try {
                assertTrue(probe.waitFor(5, TimeUnit.SECONDS), "Probe process timed out");
                assertEquals(0, probe.exitValue(), new String(probe.getInputStream().readAllBytes()));
                assertEquals(deleted ? "refused" : "contended", Files.readString(result));
            } finally {
                probe.destroyForcibly();
            }
        }
    }

    @Test
    void close_heldWorkspace_allowsReacquisition() {
        WorkspacePaths paths = workspace("workspace");

        try (WorkspaceLock ignored = WorkspaceLock.acquire(paths)) {
            assertTrue(Files.isRegularFile(paths.metadataFile()));
        }

        try (WorkspaceLock ignored = WorkspaceLock.acquire(paths)) {
            assertTrue(Files.isRegularFile(paths.metadataFile()));
        }
    }

    @Test
    void acquire_metadataMissing_refusedWithoutRecreatingFile() throws IOException {
        WorkspacePaths paths = workspace("workspace");
        Files.delete(paths.metadataFile());

        assertThrows(WorkspaceException.class, () -> WorkspaceLock.acquire(paths));
        assertFalse(Files.exists(paths.metadataFile()));
    }

    @Test
    void acquire_metadataAtLockOffset_allowedWithoutChangingFile() throws IOException {
        WorkspacePaths paths = workspace("workspace");
        Files.write(paths.metadataFile(), new byte[LOCK_OFFSET]);

        try (WorkspaceLock ignored = WorkspaceLock.acquire(paths)) {
            assertEquals(LOCK_OFFSET, Files.size(paths.metadataFile()));
        }
    }

    @Test
    void acquire_metadataBeyondLockOffset_refused() throws IOException {
        WorkspacePaths paths = workspace("workspace");
        Files.write(paths.metadataFile(), new byte[LOCK_OFFSET + 1]);

        assertThrows(WorkspaceException.class, () -> WorkspaceLock.acquire(paths));
    }

    @Test
    void acquire_distinctWorkspaces_bothAvailable() {
        WorkspacePaths first = workspace("first");
        WorkspacePaths second = workspace("second");

        try (WorkspaceLock firstLock = WorkspaceLock.acquire(first);
                WorkspaceLock secondLock = WorkspaceLock.acquire(second)) {
            assertEquals(first, firstLock.paths());
            assertEquals(second, secondLock.paths());
        }
    }

    @Test
    void acquire_holderProcessKilled_allowsReacquisition() throws Exception {
        WorkspacePaths paths = workspace("workspace");
        Path ready = temporary.resolve("holder-ready");
        Process holder = startHolder(paths, ready);
        try {
            awaitReady(holder, ready);
            WorkspaceException failure = assertThrows(WorkspaceException.class, () -> WorkspaceLock.acquire(paths));
            assertTrue(failure.getMessage().contains("already in use"), failure.getMessage());

            holder.destroyForcibly();
            assertTrue(holder.waitFor(5, TimeUnit.SECONDS), "Holder process did not stop");
            try (WorkspaceLock ignored = WorkspaceLock.acquire(paths)) {
                assertTrue(Files.isRegularFile(paths.metadataFile()));
            }
        } finally {
            holder.destroyForcibly();
            holder.waitFor(5, TimeUnit.SECONDS);
        }
    }

    private WorkspacePaths workspace(String name) {
        return new WorkspaceService().create(temporary.resolve(name));
    }

    private static Process startHolder(WorkspacePaths paths, Path ready)
            throws IOException, URISyntaxException {
        return startChild(Holder.class, paths, ready);
    }

    private static Process startChild(Class<?> mainClass, WorkspacePaths paths, Path result)
            throws IOException, URISyntaxException {
        String extension = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")
                ? ".exe" : "";
        String java = Path.of(System.getProperty("java.home"), "bin", "java" + extension).toString();
        String testClasses = Path.of(WorkspaceLockTest.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI()).toString();
        String mainClasses = Path.of(WorkspaceLock.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI()).toString();
        String classpath = testClasses + File.pathSeparator + mainClasses;
        return new ProcessBuilder(java, "-cp", classpath, mainClass.getName(),
                paths.root().toString(), result.toString()).redirectErrorStream(true).start();
    }

    private static void awaitReady(Process holder, Path ready) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (!Files.exists(ready) && holder.isAlive() && System.nanoTime() < deadline) {
            Thread.sleep(25);
        }
        if (Files.exists(ready)) {
            return;
        }
        String detail = holder.isAlive() ? "process still running"
                : new String(holder.getInputStream().readAllBytes());
        fail("Holder did not acquire lock: " + detail);
    }

    /** Holds a lock in another JVM until that process is stopped. */
    public static final class Holder {
        private Holder() {
        }

        /** Starts the lock holder. */
        public static void main(String[] args) throws IOException {
            WorkspacePaths paths = new WorkspacePaths(Path.of(args[0]));
            try (WorkspaceLock ignored = WorkspaceLock.acquire(paths)) {
                Files.writeString(Path.of(args[1]), "ready");
                System.in.read();
            }
        }
    }

    /** Attempts a lock in another JVM and records whether contention was enforced. */
    public static final class Probe {
        private Probe() {
        }

        /** Starts the one-shot probe. */
        public static void main(String[] args) throws IOException {
            WorkspacePaths paths = new WorkspacePaths(Path.of(args[0]));
            Path result = Path.of(args[1]);
            try (WorkspaceLock ignored = WorkspaceLock.acquire(paths)) {
                Files.writeString(result, "acquired");
            } catch (WorkspaceException e) {
                String outcome = e.getMessage().contains("already in use") ? "contended" : "refused";
                Files.writeString(result, outcome);
            }
        }
    }
}
