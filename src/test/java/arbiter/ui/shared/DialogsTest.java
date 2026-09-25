package arbiter.ui.shared;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Covers the part of {@link Dialogs} that runs without JavaFX; the dialogs themselves need a display. */
class DialogsTest {
    @TempDir
    Path temporary;

    @AfterEach
    void detach() {
        DiagnosticLog.detach();
    }

    @Test
    void showUncaught_javaFxNotRunning_recordedAtOnceWithoutThrowing() throws IOException {
        Path logs = Files.createDirectories(temporary.resolve("logs"));
        DiagnosticLog.attach(logs);

        Dialogs.showUncaught(null, new IllegalStateException("layout failed"));

        String log = Files.readString(logs.resolve(DiagnosticLog.FILE_PATTERN.replace("%g", "0")));
        assertTrue(log.contains("SEVERE: Failed: Something went wrong"), log);
        assertTrue(log.contains("java.lang.IllegalStateException: layout failed"), log);
    }
}
