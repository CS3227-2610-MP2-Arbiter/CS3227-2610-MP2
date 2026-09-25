package arbiter.ui.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.junit.jupiter.api.Test;

import arbiter.data.json.JsonStoreException;
import arbiter.service.AuthException;
import arbiter.workspace.SourceException;
import arbiter.workspace.SourceFailure;

class ErrorMessagesTest {
    @Test
    void of_arbiterException_ownMessageShown() {
        assertEquals("Account is disabled", ErrorMessages.of(new AuthException("Account is disabled")));
        assertEquals("Could not read workspace data",
                ErrorMessages.of(new JsonStoreException("Could not read workspace data")));
    }

    @Test
    void of_subclassOfArbiterException_ownMessageShown() {
        SourceException missing = new SourceException("media/a.txt", SourceFailure.MISSING, "media/a.txt is missing");

        assertEquals("media/a.txt is missing", ErrorMessages.of(missing));
    }

    @Test
    void of_arbiterExceptionWithoutUsableMessage_genericMessageShown() {
        assertEquals(ErrorMessages.UNEXPECTED, ErrorMessages.of(new AuthException(null)));
        assertEquals(ErrorMessages.UNEXPECTED, ErrorMessages.of(new AuthException("")));
        assertEquals(ErrorMessages.UNEXPECTED, ErrorMessages.of(new AuthException("  ")));
    }

    @Test
    void of_jdkException_genericMessageShown() {
        assertEquals(ErrorMessages.UNEXPECTED, ErrorMessages.of(new IllegalStateException("index 3 of 2")));
    }

    @Test
    void of_jdkExceptionCausedByArbiterException_genericMessageShown() {
        UncheckedIOException wrapped = new UncheckedIOException("disk", new IOException(
                new AuthException("Invalid username or password")));

        assertEquals(ErrorMessages.UNEXPECTED, ErrorMessages.of(wrapped));
    }

    @Test
    void isUserFacing() {
        assertTrue(ErrorMessages.isUserFacing(new AuthException("Account is disabled")));
        assertFalse(ErrorMessages.isUserFacing(new NullPointerException("user")));
    }
}
