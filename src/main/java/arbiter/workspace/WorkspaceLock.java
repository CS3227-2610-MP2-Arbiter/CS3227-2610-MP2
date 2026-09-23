package arbiter.workspace;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

/**
 * Holds an operating-system byte-range lock on workspace metadata until the workspace closes.
 * On some systems, closing another channel to the metadata in this JVM releases this lock.
 */
public final class WorkspaceLock implements AutoCloseable {
    // The workspace metadata is small; locking beyond its contents also permits reads on Windows.
    private static final long LOCK_BYTE = 1024L * 1024L;
    private static final Set<Path> HELD_ROOTS = new HashSet<>();

    private final WorkspacePaths paths;
    private final Path realRoot;
    private final FileChannel channel;
    private final FileLock lock;
    private boolean closed;

    private WorkspaceLock(WorkspacePaths paths, Path realRoot, FileChannel channel, FileLock lock) {
        this.paths = paths;
        this.realRoot = realRoot;
        this.channel = channel;
        this.lock = lock;
    }

    /** Takes the workspace lock without waiting for another instance. */
    public static WorkspaceLock acquire(WorkspacePaths paths) {
        Path realRoot;
        try {
            realRoot = paths.root().toRealPath();
        } catch (IOException | SecurityException e) {
            throw new WorkspaceException("Could not lock the workspace at " + paths.root(), e);
        }
        synchronized (HELD_ROOTS) {
            if (HELD_ROOTS.contains(realRoot)) {
                throw inUse(paths);
            }
            WorkspaceLock held = takeFileLock(paths, realRoot);
            HELD_ROOTS.add(realRoot);
            return held;
        }
    }

    private static WorkspaceLock takeFileLock(WorkspacePaths paths, Path realRoot) {
        FileChannel channel;
        try {
            channel = FileChannel.open(paths.metadataFile(), StandardOpenOption.WRITE);
        } catch (IOException | SecurityException e) {
            throw new WorkspaceException("Could not lock the workspace at " + paths.root(), e);
        }
        try {
            if (channel.size() > LOCK_BYTE) {
                throw new WorkspaceException("The workspace metadata is too large to lock safely: "
                        + paths.metadataFile());
            }
            FileLock lock = channel.tryLock(LOCK_BYTE, 1L, false);
            if (lock == null) {
                throw inUse(paths);
            }
            return new WorkspaceLock(paths, realRoot, channel, lock);
        } catch (IOException | RuntimeException e) {
            try {
                channel.close();
            } catch (IOException closeFailure) {
                e.addSuppressed(closeFailure);
            }
            if (e instanceof OverlappingFileLockException) {
                throw inUse(paths);
            }
            if (e instanceof WorkspaceException workspaceException) {
                throw workspaceException;
            }
            throw new WorkspaceException("Could not lock the workspace at " + paths.root(), e);
        }
    }

    /** Returns the workspace whose lock is held. */
    public WorkspacePaths paths() {
        return paths;
    }

    /** Releases this workspace's metadata lock. */
    @Override
    public void close() {
        synchronized (HELD_ROOTS) {
            if (closed) {
                return;
            }
            try {
                channel.close();
            } catch (IOException e) {
                if (!lock.isValid()) {
                    HELD_ROOTS.remove(realRoot);
                    closed = true;
                }
                throw new WorkspaceException("Could not release the workspace lock at " + paths.root(), e);
            }
            HELD_ROOTS.remove(realRoot);
            closed = true;
        }
    }

    private static WorkspaceException inUse(WorkspacePaths paths) {
        return new WorkspaceException("That workspace is already in use by another Arbiter instance: "
                + paths.root());
    }
}
