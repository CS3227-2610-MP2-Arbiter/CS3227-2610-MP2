package arbiter.data.json;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import arbiter.workspace.WorkspaceMetadata;
import arbiter.workspace.WorkspacePaths;
import arbiter.workspace.WorkspaceService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** Opens one versioned JSON snapshot and commits complete logical actions. */
public final class JsonStore {
    static final JsonMapper JSON = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    private static final ConcurrentHashMap<Path, PathState> PATH_STATES = new ConcurrentHashMap<>();

    private final WorkspacePaths paths;
    private final PathState pathState;
    private final SnapshotPublisher publisher;

    private JsonStore(WorkspacePaths paths, SnapshotPublisher publisher) {
        this.paths = Objects.requireNonNull(paths, "paths");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.pathState = PATH_STATES.computeIfAbsent(paths.dataFile(), ignored -> new PathState());
    }

    /** Initializes the data file for a workspace just created by {@link WorkspaceService}. */
    public static JsonStore initializeNew(WorkspacePaths paths) {
        JsonStore store = new JsonStore(paths, JsonStore::publishAtomically);
        synchronized (store.pathState) {
            WorkspaceService workspaces = new WorkspaceService();
            workspaces.open(paths.root());
            if (workspaces.readMetadata(paths).workspaceVersion()
                    != WorkspaceMetadata.CURRENT_WORKSPACE_VERSION) {
                throw new JsonStoreException("This is not a newly created workspace: " + paths.root());
            }
            if (Files.exists(paths.dataFile())) {
                throw new JsonStoreException("The workspace data file already exists: " + paths.dataFile());
            }
            store.publish(JsonSnapshot.empty());
            store.pathState.requiresReopen = false;
        }
        return store;
    }

    /** Opens an existing snapshot without creating missing data. */
    public static JsonStore open(WorkspacePaths paths) {
        return open(paths, JsonStore::publishAtomically);
    }

    static JsonStore open(WorkspacePaths paths, SnapshotPublisher publisher) {
        JsonStore store = new JsonStore(paths, publisher);
        synchronized (store.pathState) {
            WorkspaceService workspaces = new WorkspaceService();
            workspaces.open(paths.root());
            if (!Files.isRegularFile(paths.dataFile())) {
                throw new JsonStoreException("The workspace data file is missing: " + paths.dataFile());
            }
            store.readSnapshot();
            store.pathState.requiresReopen = false;
        }
        return store;
    }

    /** Reads a fresh committed snapshot through the existing repository interfaces. */
    public <T> T read(Function<RepositorySession, T> action) {
        Objects.requireNonNull(action, "action");
        synchronized (pathState) {
            RepositorySession session = new RepositorySession(readSnapshot(), false);
            try {
                return action.apply(session);
            } finally {
                session.close();
            }
        }
    }

    /** Commits all repository changes in one atomic snapshot replacement. */
    public <T> T write(Function<RepositorySession, T> action) {
        Objects.requireNonNull(action, "action");
        synchronized (pathState) {
            if (pathState.requiresReopen) {
                throw new JsonStoreException("Reopen the workspace data store before another write");
            }
            RepositorySession session = new RepositorySession(readSnapshot(), true);
            try {
                T result = action.apply(session);
                JsonSnapshot next = session.snapshot();
                JsonIntegrity.validate(next);
                publish(next);
                return result;
            } finally {
                session.close();
            }
        }
    }

    private JsonSnapshot readSnapshot() {
        try {
            byte[] bytes = Files.readAllBytes(paths.dataFile());
            JsonNode tree = JSON.readTree(bytes);
            if (tree == null || !tree.isObject()) {
                throw new JsonStoreException("The workspace data file is malformed: " + paths.dataFile());
            }
            JsonNode version = tree.get("schemaVersion");
            if (version == null || !version.isIntegralNumber()) {
                throw new JsonStoreException("The workspace data version is missing or invalid");
            }
            if (version.intValue() > JsonSnapshot.CURRENT_VERSION) {
                throw new JsonStoreException("This workspace was written by a newer version of Arbiter");
            }
            if (version.intValue() != JsonSnapshot.CURRENT_VERSION) {
                throw new JsonStoreException("This workspace data version is unsupported: " + version);
            }
            JsonSnapshot snapshot = JSON.readValue(bytes, JsonSnapshot.class);
            JsonIntegrity.validate(snapshot);
            return snapshot;
        } catch (IOException | JacksonException e) {
            throw new JsonStoreException("Could not read workspace data at " + paths.dataFile(), e);
        }
    }

    private void publish(JsonSnapshot snapshot) {
        try {
            publisher.publish(paths.dataFile(), JSON.writeValueAsBytes(snapshot));
        } catch (IOException e) {
            pathState.requiresReopen = true;
            throw new JsonStoreException("Could not atomically write workspace data at " + paths.dataFile(), e);
        } catch (JacksonException e) {
            throw new JsonStoreException("Could not serialize workspace data at " + paths.dataFile(), e);
        }
    }

    private static void publishAtomically(Path target, byte[] bytes) throws IOException {
        Path temporary = Files.createTempFile(target.getParent(), ".arbiter-", ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            }
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static final class PathState {
        private boolean requiresReopen;
    }
}
