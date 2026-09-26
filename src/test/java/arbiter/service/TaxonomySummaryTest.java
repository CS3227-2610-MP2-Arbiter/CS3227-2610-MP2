package arbiter.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

import arbiter.model.project.Label;
import arbiter.model.project.TaxonomyKind;
import arbiter.testing.Records;

/** Unit checks for the answer rules the queue's editor and submission share (#14). */
class TaxonomySummaryTest {
    private static final TaxonomySummary LABELS = new TaxonomySummary(TaxonomyKind.SINGLE,
            List.of(label(10, "positive"), label(11, "negative")), null, null, true);
    private static final TaxonomySummary SCALE = new TaxonomySummary(TaxonomyKind.SCALE, List.of(), -2, 5, true);
    private static final TaxonomySummary NO_LABELS = new TaxonomySummary(TaxonomyKind.SINGLE, List.of(), null, null,
            false);
    private static final TaxonomySummary NO_RANGE = new TaxonomySummary(TaxonomyKind.SCALE, List.of(), null, null,
            false);

    @Test
    void accepts_labelOfTheProject_accepted() {
        assertTrue(LABELS.accepts(new Answer.LabelChoice(10)));
        assertTrue(LABELS.accepts(new Answer.LabelChoice(11)));
    }

    @Test
    void accepts_labelNotInTheProject_refused() {
        assertFalse(LABELS.accepts(new Answer.LabelChoice(12)));
    }

    @Test
    void accepts_answerOfTheOtherKind_refused() {
        assertFalse(LABELS.accepts(new Answer.Rating(1)));
        assertFalse(SCALE.accepts(new Answer.LabelChoice(10)));
    }

    @Test
    void accepts_ratingAtAndJustOutsideTheRange() {
        assertTrue(SCALE.accepts(new Answer.Rating(-2)));
        assertTrue(SCALE.accepts(new Answer.Rating(5)));
        assertFalse(SCALE.accepts(new Answer.Rating(-3)));
        assertFalse(SCALE.accepts(new Answer.Rating(6)));
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
        for (String text : new String[] {"", "   ", "-", "2.5", "abc", "+3", "3-", "1e2", "٣", "- 2"}) {
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
    void parseRating_singleProjectOrNoRange_refused() {
        assertEquals(OptionalInt.empty(), LABELS.parseRating("1"));
        assertEquals(OptionalInt.empty(), NO_RANGE.parseRating("1"));
    }

    @Test
    void answerable() {
        assertTrue(LABELS.answerable());
        assertTrue(SCALE.answerable());
        assertFalse(NO_LABELS.answerable());
        assertFalse(NO_RANGE.answerable());
        assertFalse(NO_RANGE.accepts(new Answer.Rating(1)));
    }

    private static Label label(long id, String key) {
        Label label = Records.label(1, key, (int) id);
        label.setId(id);
        return label;
    }
}
