package arbiter.workspace;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads and writes the small, fixed-schema JSON documents the workspace keeps.
 *
 * <p>Both documents are flat: an object whose values are strings, numbers or arrays of strings. That
 * is little enough that a dependency would cost more than it saves, and the escaping rules are
 * covered by tests. Anything more complex should use a real parser instead.
 */
final class Json {
    private Json() {
    }

    /**
     * Writes a flat object as JSON, one field per line.
     *
     * @param fields field names mapped to values: {@code String}, {@code Number}, {@code Boolean} or
     *     an {@code Iterable} of them
     * @return the document, ending in a newline
     */
    static String write(Map<String, Object> fields) {
        StringBuilder out = new StringBuilder("{\n");
        int index = 0;
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            out.append("  ").append(quote(entry.getKey())).append(": ").append(value(entry.getValue()));
            if (++index < fields.size()) {
                out.append(',');
            }
            out.append('\n');
        }
        return out.append("}\n").toString();
    }

    /**
     * Reads a flat JSON object.
     *
     * @param text the document
     * @return field names mapped to {@code String}, {@code Long}, {@code Double}, {@code Boolean} or
     *     {@code List<String>}
     * @throws WorkspaceMetadataException if the document is not a flat object
     */
    static Map<String, Object> read(String text) {
        Parser parser = new Parser(text);
        Map<String, Object> result = parser.object();
        parser.skipSpace();
        if (!parser.done()) {
            throw new WorkspaceMetadataException("Unexpected trailing content in JSON");
        }
        return result;
    }

    private static String value(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Iterable<?>) {
            StringBuilder out = new StringBuilder("[");
            boolean first = true;
            for (Object element : (Iterable<?>) value) {
                if (!first) {
                    out.append(", ");
                }
                out.append(quote(String.valueOf(element)));
                first = false;
            }
            return out.append(']').toString();
        }
        return quote(String.valueOf(value));
    }

    private static String quote(String text) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
            case '"':
                out.append("\\\"");
                break;
            case '\\':
                out.append("\\\\");
                break;
            case '\n':
                out.append("\\n");
                break;
            case '\r':
                out.append("\\r");
                break;
            case '\t':
                out.append("\\t");
                break;
            default:
                if (c < 0x20) {
                    out.append(String.format("\\u%04x", (int) c));
                } else {
                    out.append(c);
                }
            }
        }
        return out.append('"').toString();
    }

    /** A minimal recursive-descent reader for the subset of JSON written above. */
    private static final class Parser {
        private final String text;
        private int at;

        Parser(String text) {
            this.text = text;
        }

        boolean done() {
            return at >= text.length();
        }

        void skipSpace() {
            while (at < text.length() && Character.isWhitespace(text.charAt(at))) {
                at++;
            }
        }

        Map<String, Object> object() {
            Map<String, Object> result = new LinkedHashMap<>();
            skipSpace();
            expect('{');
            skipSpace();
            if (peek() == '}') {
                at++;
                return result;
            }
            while (true) {
                skipSpace();
                String key = string();
                skipSpace();
                expect(':');
                skipSpace();
                result.put(key, value());
                skipSpace();
                char c = next();
                if (c == '}') {
                    return result;
                }
                if (c != ',') {
                    throw new WorkspaceMetadataException("Expected ',' or '}' in JSON object");
                }
            }
        }

        private Object value() {
            char c = peek();
            if (c == '"') {
                return string();
            }
            if (c == '[') {
                return array();
            }
            if (c == '{') {
                return object();
            }
            if (text.startsWith("true", at)) {
                at += 4;
                return Boolean.TRUE;
            }
            if (text.startsWith("false", at)) {
                at += 5;
                return Boolean.FALSE;
            }
            if (text.startsWith("null", at)) {
                at += 4;
                return null;
            }
            return number();
        }

        private java.util.List<String> array() {
            java.util.List<String> result = new java.util.ArrayList<>();
            expect('[');
            skipSpace();
            if (peek() == ']') {
                at++;
                return result;
            }
            while (true) {
                skipSpace();
                result.add(string());
                skipSpace();
                char c = next();
                if (c == ']') {
                    return result;
                }
                if (c != ',') {
                    throw new WorkspaceMetadataException("Expected ',' or ']' in JSON array");
                }
            }
        }

        private Object number() {
            int start = at;
            if (peek() == '-' || peek() == '+') {
                at++;
            }
            boolean fractional = false;
            while (at < text.length()) {
                char c = text.charAt(at);
                if (Character.isDigit(c)) {
                    at++;
                } else if (c == '.' || c == 'e' || c == 'E' || c == '-' || c == '+') {
                    fractional = fractional || c == '.' || c == 'e' || c == 'E';
                    at++;
                } else {
                    break;
                }
            }
            String token = text.substring(start, at);
            if (token.isEmpty()) {
                throw new WorkspaceMetadataException("Expected a value in JSON, found '"
                        + text.charAt(at) + "'");
            }
            try {
                return fractional ? (Object) Double.valueOf(token) : (Object) Long.valueOf(token);
            } catch (NumberFormatException e) {
                throw new WorkspaceMetadataException("Not a number in JSON: " + token);
            }
        }

        private String string() {
            expect('"');
            StringBuilder out = new StringBuilder();
            while (true) {
                char c = next();
                if (c == '"') {
                    return out.toString();
                }
                if (c != '\\') {
                    out.append(c);
                    continue;
                }
                char escape = next();
                switch (escape) {
                case '"':
                case '\\':
                case '/':
                    out.append(escape);
                    break;
                case 'n':
                    out.append('\n');
                    break;
                case 'r':
                    out.append('\r');
                    break;
                case 't':
                    out.append('\t');
                    break;
                case 'b':
                    out.append('\b');
                    break;
                case 'f':
                    out.append('\f');
                    break;
                case 'u':
                    if (at + 4 > text.length()) {
                        throw new WorkspaceMetadataException("Truncated \\u escape in JSON");
                    }
                    out.append((char) Integer.parseInt(text.substring(at, at + 4), 16));
                    at += 4;
                    break;
                default:
                    throw new WorkspaceMetadataException("Unknown escape in JSON: \\" + escape);
                }
            }
        }

        private char peek() {
            if (at >= text.length()) {
                throw new WorkspaceMetadataException("Unexpected end of JSON");
            }
            return text.charAt(at);
        }

        private char next() {
            char c = peek();
            at++;
            return c;
        }

        private void expect(char expected) {
            if (next() != expected) {
                throw new WorkspaceMetadataException("Expected '" + expected + "' in JSON");
            }
        }
    }
}
