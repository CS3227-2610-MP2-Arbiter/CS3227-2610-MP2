package arbiter.workspace;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Remembers which workspaces exist and which was opened last.
 *
 * <p>This cannot live inside a workspace, because it records which workspaces there are. It sits in a
 * user-level folder instead, whose location is injected so tests never touch the real one.
 *
 * <p>The list is stored newest first, so "last opened" is simply the first entry and there is no
 * separate field to fall out of step with it.
 */
public final class RecentWorkspaces {
    /** Name of the file holding the recent list. */
    public static final String FILE_NAME = "recent-workspaces.json";

    /** How many workspaces are remembered. */
    public static final int LIMIT = 10;

    private static final JsonMapper JSON = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    private final Path file;
    private final List<Path> paths = new ArrayList<>();

    /**
     * Creates a recent list backed by a file.
     *
     * @param file the file to read from and write to
     */
    public RecentWorkspaces(Path file) {
        this.file = file.toAbsolutePath().normalize();
    }

    /** Returns the default location, {@code ~/.arbiter/recent-workspaces.json}. */
    public static RecentWorkspaces atUserHome() {
        return new RecentWorkspaces(
                Path.of(System.getProperty("user.home"), ".arbiter", FILE_NAME));
    }

    /** Returns the remembered workspaces, most recently opened first. */
    public List<Path> getPaths() {
        return Collections.unmodifiableList(paths);
    }

    /** Returns the last opened workspace, if one is remembered. */
    public Path lastOpened() {
        return paths.isEmpty() ? null : paths.get(0);
    }

    /** Reads the list from disk, ignoring a missing or unreadable file. */
    public void load() {
        paths.clear();
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            JsonNode fields = JSON.readTree(Files.readString(file, StandardCharsets.UTF_8));
            for (JsonNode path : fields.path("workspaces")) {
                paths.add(Path.of(path.asString()));
            }
        } catch (IOException | JacksonException e) {
            // A damaged recent list must never stop the app from starting: treat it as empty.
            paths.clear();
        }
    }

    /**
     * Moves a workspace to the front of the list and writes it out.
     *
     * @param workspace the workspace that was just opened
     */
    public void remember(Path workspace) {
        Path normalised = workspace.toAbsolutePath().normalize();
        paths.remove(normalised);
        paths.add(0, normalised);
        while (paths.size() > LIMIT) {
            paths.remove(paths.size() - 1);
        }
        save();
    }

    /** Writes the list to disk, creating the parent folder if needed. */
    public void save() {
        try {
            Files.createDirectories(file.getParent());
            List<String> asText = new ArrayList<>();
            for (Path path : paths) {
                asText.add(path.toString());
            }
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("version", 1);
            fields.put("updated", Instant.now().toString());
            fields.put("workspaces", asText);
            Files.writeString(file, JSON.writeValueAsString(fields), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new WorkspaceException("Could not save the recent workspace list", e);
        }
    }
}
