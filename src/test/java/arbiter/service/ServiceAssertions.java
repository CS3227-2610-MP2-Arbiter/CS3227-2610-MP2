package arbiter.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.function.Executable;

/** Assertions about the rejections the services report, shared by their tests. */
final class ServiceAssertions {
    private ServiceAssertions() {
    }

    /** Asserts that the call is rejected with a {@link ProjectException} whose message can be shown. */
    static void assertRejected(Executable call) {
        ProjectException rejection = assertThrows(ProjectException.class, call);
        assertFalse(rejection.getMessage() == null || rejection.getMessage().isBlank());
    }
}
