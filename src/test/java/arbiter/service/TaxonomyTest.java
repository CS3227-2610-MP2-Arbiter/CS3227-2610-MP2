package arbiter.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

import arbiter.model.project.TaxonomyKind;

/** Unit checks for the answer rules the queue's editor and submission share (#14). */
class TaxonomyTest {
    private static final Taxonomy LABELS = new Taxonomy(TaxonomyKind.SINGLE,
            List.of(new LabelOption(10, "positive", "Clearly favourable"), new LabelOption(11, "negative", null)),
            null, null);
    private static final Taxonomy SCALE = new Taxonomy(TaxonomyKind.SCALE, List.of(), -2, 5);

    @Test
    void accepts_labelOfTheProject_accepted() {
        assertTrue(LABELS.accepts(10));
        assertTrue(LABELS.accepts(11));
    }

    @Test
    void accepts_labelNotInTheProject_refused() {
        assertFalse(LABELS.accepts(12));
    }

    @Test
    void accepts_scaleProject_noLabelAccepted() {
        assertFalse(SCALE.accepts(10));
    }

    @Test
    void parseRating_rangeEndsAndInside_accepted() {
        assertEquals(OptionalInt.of(-2), SCALE.parseRating("-2"));
        assertEquals(OptionalInt.of(0), SCALE.parseRating("0"));
        assertEquals(OptionalInt.of(5), SCALE.parseRating("5"));
    }

    @Test
    void parseRating_justOutsideRange_refused() {
        assertEquals(OptionalInt.empty(), SCALE.parseRating("-3"));
        assertEquals(OptionalInt.empty(), SCALE.parseRating("6"));
    }

    @Test
    void parseRating_surroundingWhitespace_stripped() {
        assertEquals(OptionalInt.of(3), SCALE.parseRating("  3 \t"));
    }

    @Test
    void parseRating_notAWholeNumber_refused() {
        for (String text : new String[] {"", "   ", "2.5", "abc", "+3", "3-", "1e2", "٣", "- 2"}) {
            assertEquals(OptionalInt.empty(), SCALE.parseRating(text), "\"" + text + "\"");
        }
        assertEquals(OptionalInt.empty(), SCALE.parseRating(null));
    }

    @Test
    void parseRating_tooLargeForAnInt_refusedWithoutFailing() {
        assertEquals(OptionalInt.empty(), SCALE.parseRating("99999999999"));
        assertEquals(OptionalInt.empty(), SCALE.parseRating("-99999999999"));
    }

    @Test
    void parseRating_singleProject_refused() {
        assertEquals(OptionalInt.empty(), LABELS.parseRating("1"));
    }

    @Test
    void answerable() {
        assertTrue(LABELS.answerable());
        assertTrue(SCALE.answerable());
        assertFalse(new Taxonomy(TaxonomyKind.SINGLE, List.of(), null, null).answerable());
        assertFalse(new Taxonomy(TaxonomyKind.SCALE, List.of(), null, null).answerable());
        assertEquals(OptionalInt.empty(), new Taxonomy(TaxonomyKind.SCALE, List.of(), null, null).parseRating("1"));
    }

    @Test
    void acceptsAnswer_rightKindAndValue_accepted() {
        assertTrue(LABELS.accepts(Answer.label(10)));
        assertTrue(SCALE.accepts(Answer.rating(-2)));
        assertTrue(SCALE.accepts(Answer.rating(5)));
    }

    @Test
    void acceptsAnswer_wrongKindOrValue_refused() {
        assertFalse(LABELS.accepts(Answer.rating(1)));
        assertFalse(LABELS.accepts(Answer.label(12)));
        assertFalse(SCALE.accepts(Answer.label(10)));
        assertFalse(SCALE.accepts(Answer.rating(6)));
        assertFalse(new Taxonomy(TaxonomyKind.SCALE, List.of(), null, null).accepts(Answer.rating(1)));
    }

    @Test
    void answer_exactlyOneOfLabelOrRating() {
        assertEquals(10L, Answer.label(10).labelId());
        assertEquals(3, Answer.rating(3).rating());
        assertThrows(IllegalArgumentException.class, () -> new Answer(null, null));
        assertThrows(IllegalArgumentException.class, () -> new Answer(10L, 3));
    }
}
