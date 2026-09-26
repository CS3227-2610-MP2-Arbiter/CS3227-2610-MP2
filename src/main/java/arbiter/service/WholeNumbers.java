package arbiter.service;

import java.util.OptionalInt;
import java.util.regex.Pattern;

/** Parses whole numbers people type, such as a scale range's ends (#26) or an annotator's rating (#14). */
final class WholeNumbers {
    private static final Pattern WHOLE_NUMBER = Pattern.compile("-?[0-9]+");

    private WholeNumbers() {
    }

    /**
     * Returns the whole number this text gives: ASCII digits with an optional leading minus sign, once surrounding
     * whitespace is stripped.
     *
     * @return the number, or empty for any other text, including a number too large for an {@code int}
     */
    static OptionalInt parse(String text) {
        String stripped = text == null ? "" : text.strip();
        if (!WHOLE_NUMBER.matcher(stripped).matches()) {
            return OptionalInt.empty();
        }
        try {
            return OptionalInt.of(Integer.parseInt(stripped));
        } catch (NumberFormatException e) {
            // The pattern leaves only a number too large for an int.
            return OptionalInt.empty();
        }
    }
}
