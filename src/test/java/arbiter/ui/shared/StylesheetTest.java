package arbiter.ui.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import javafx.css.CssParser;
import javafx.css.Rule;
import javafx.css.Selector;
import javafx.css.Stylesheet;

class StylesheetTest {
    private static final Pattern STYLE_CLASS = Pattern.compile("\\.([a-z][a-z-]*)");

    @Test
    void stylesheet_parsesWithoutErrors() throws IOException {
        CssParser.errorsProperty().clear();

        new CssParser().parse(Styles.stylesheet());

        assertEquals(List.of(), CssParser.errorsProperty().stream().map(Object::toString).toList());
    }

    @Test
    void stylesheet_hasOneRuleSetPerDeclaredClassAndNoOthers() throws IOException {
        Stylesheet stylesheet = new CssParser().parse(Styles.stylesheet());
        Set<String> selected = new TreeSet<>();
        for (Rule rule : stylesheet.getRules()) {
            for (Selector selector : rule.getSelectors()) {
                Matcher match = STYLE_CLASS.matcher(selector.toString());
                while (match.find()) {
                    selected.add(match.group(1));
                }
            }
        }

        Set<String> expected = new TreeSet<>(Styles.CLASSES);
        expected.add("root");
        assertEquals(expected, selected);
    }
}
