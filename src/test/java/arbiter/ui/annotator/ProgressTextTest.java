package arbiter.ui.annotator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import arbiter.model.project.AssignmentStatus;
import arbiter.service.AssignmentProgress;

class ProgressTextTest {
    @Test
    void of_submittedTotalAndRemaining() {
        assertEquals("0 of 3 files submitted, 3 remaining",
                ProgressText.of(new AssignmentProgress(1, "P", "S", AssignmentStatus.NOT_STARTED, 0, 3)));
        assertEquals("3 of 3 files submitted, 0 remaining",
                ProgressText.of(new AssignmentProgress(1, "P", "S", AssignmentStatus.SUBMITTED, 3, 3)));
    }
}
