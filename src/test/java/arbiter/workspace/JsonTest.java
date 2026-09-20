package arbiter.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** Tests the small JSON codec, especially the escaping a Windows path needs. */
class JsonTest {
    @Test
    void writesAndReadsASimpleObject() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("workspaceVersion", 1);
        fields.put("schemaVersion", 2);
        fields.put("created", "2026-09-20T00:00:00Z");

        String text = Json.write(fields);
        Map<String, Object> read = Json.read(text);

        assertEquals(1L, read.get("workspaceVersion"));
        assertEquals(2L, read.get("schemaVersion"));
        assertEquals("2026-09-20T00:00:00Z", read.get("created"));
    }

    @Test
    void aWindowsPathSurvivesARoundTrip() {
        String path = "C:\\Users\\Admin\\OneDrive\\Desktop\\my workspace";
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("path", path);

        assertEquals(path, Json.read(Json.write(fields)).get("path"));
    }

    @Test
    void awkwardCharactersSurviveARoundTrip() {
        String awkward = "quote \" backslash \\ newline \n tab \t unicode \u00e9\u4e2d";
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("text", awkward);

        assertEquals(awkward, Json.read(Json.write(fields)).get("text"));
    }

    @Test
    void readsAnArrayOfStrings() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("workspaces", List.of("C:\\one", "C:\\two"));

        Object read = Json.read(Json.write(fields)).get("workspaces");

        assertTrue(read instanceof List<?>, "should read back as a list");
        assertEquals(List.of("C:\\one", "C:\\two"), read);
    }

    @Test
    void readsBooleansAndNull() {
        String text = "{\"yes\": true, \"no\": false, \"nothing\": null}";

        Map<String, Object> read = Json.read(text);

        assertEquals(Boolean.TRUE, read.get("yes"));
        assertEquals(Boolean.FALSE, read.get("no"));
        assertEquals(null, read.get("nothing"));
    }

    @Test
    void readsFractionalNumbersAsDoubles() {
        Map<String, Object> read = Json.read("{\"reward\": 1.5}");

        assertEquals(1.5d, ((Number) read.get("reward")).doubleValue());
    }

    @Test
    void readsAnEmptyObject() {
        assertTrue(Json.read("{}").isEmpty());
        assertTrue(Json.read("  {  }  ").isEmpty());
    }

    @Test
    void rejectsMalformedDocumentsRatherThanGuessing() {
        assertThrows(WorkspaceMetadataException.class, () -> Json.read("{"));
        assertThrows(WorkspaceMetadataException.class, () -> Json.read("{\"a\" 1}"));
        assertThrows(WorkspaceMetadataException.class, () -> Json.read("{\"a\": }"));
        assertThrows(WorkspaceMetadataException.class, () -> Json.read("{\"a\": 1} trailing"));
        assertThrows(WorkspaceMetadataException.class, () -> Json.read("not json at all"));
    }

    @Test
    void rejectsAnArrayAtTheTopLevel() {
        assertThrows(WorkspaceMetadataException.class, () -> Json.read("[1, 2, 3]"));
    }

    @Test
    void aRoundTripIsStable() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("workspaceVersion", 1);
        fields.put("path", "C:\\a\\b");
        fields.put("names", List.of("one \"quoted\"", "two"));

        String once = Json.write(fields);
        String twice = Json.write(Json.read(once));

        assertEquals(once, twice, "reading and rewriting should change nothing");
    }
}
