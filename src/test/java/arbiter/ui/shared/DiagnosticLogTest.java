package arbiter.ui.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import arbiter.service.AuthException;
import arbiter.service.AuthService;
import arbiter.testing.ClassificationWorkflow;
import arbiter.testing.TestWorkspace;

class DiagnosticLogTest {
    @TempDir
    Path temporary;

    @AfterEach
    void detach() {
        DiagnosticLog.detach();
    }

    @Test
    void record_userFacingFailure_warningWithTypeMessageAndStackInWorkspaceLog() throws IOException {
        Path logs = logsFolder("workspace");
        DiagnosticLog.attach(logs);

        DiagnosticLog.record("Log in", new AuthException("Account is disabled"));

        String log = read(logs);
        assertTrue(log.contains("WARNING: Failed: Log in"), log);
        assertTrue(log.contains("arbiter.service.AuthException: Account is disabled"), log);
        assertTrue(log.contains("DiagnosticLogTest"), "the stack trace should be recorded:\n" + log);
    }

    @Test
    void record_unexpectedFailure_severe() throws IOException {
        Path logs = logsFolder("workspace");
        DiagnosticLog.attach(logs);

        DiagnosticLog.record("Show the queue", new IllegalStateException("index 3 of 2"));

        String log = read(logs);
        assertTrue(log.contains("SEVERE: Failed: Show the queue"), log);
        assertTrue(log.contains("java.lang.IllegalStateException: index 3 of 2"), log);
    }

    @Test
    void record_failedLoginAndSetup_passwordsNeverWritten() throws IOException {
        TestWorkspace workspace = TestWorkspace.create(temporary.resolve("workspace"));
        ClassificationWorkflow.single("positive").seed(workspace);
        DiagnosticLog.attach(workspace.paths().logsDirectory());
        AuthService auth = new AuthService(workspace.store());
        AuthService setup = new AuthService(TestWorkspace.create(temporary.resolve("new")).store());

        AuthException wrongPassword = assertThrows(AuthException.class, () -> auth.login("owner", "wrongSecret123"));
        AuthException badSetupPassword = assertThrows(AuthException.class, () -> setup.bootstrapOwner("owner",
                "short-secret!"));
        DiagnosticLog.record("Log in", wrongPassword);
        DiagnosticLog.record("Create the adjudicator account", badSetupPassword);

        String log = read(workspace.paths().logsDirectory());
        assertTrue(log.contains("Failed: Log in"), log);
        assertFalse(log.contains("wrongSecret123"), log);
        assertFalse(log.contains("short-secret!"), log);
        assertFalse(log.contains(TestWorkspace.PASSWORD), log);
    }

    @Test
    void detach_afterAttach_nothingMoreWrittenAndFileReleased() throws IOException {
        Path logs = logsFolder("workspace");
        DiagnosticLog.attach(logs);
        DiagnosticLog.record("Before", new AuthException("first"));

        DiagnosticLog.detach();
        DiagnosticLog.record("After", new AuthException("second"));

        String log = read(logs);
        assertTrue(log.contains("Failed: Before"), log);
        assertFalse(log.contains("Failed: After"), log);
        try (Stream<Path> files = Files.list(logs)) {
            assertEquals(0, files.filter(file -> file.toString().endsWith(".lck")).count());
        }
    }

    @Test
    void attach_secondWorkspace_recordsMoveToIt() throws IOException {
        Path first = logsFolder("first");
        Path second = logsFolder("second");
        DiagnosticLog.attach(first);

        DiagnosticLog.attach(second);
        DiagnosticLog.record("Open a workspace", new AuthException("moved"));

        assertFalse(read(first).contains("moved"));
        assertTrue(read(second).contains("moved"));
    }

    @Test
    void attach_percentInFolderName_fileCreatedInThatFolder() throws IOException {
        Path logs = logsFolder("100% done");

        DiagnosticLog.attach(logs);
        DiagnosticLog.record("Open a workspace", new AuthException("percent"));

        assertTrue(read(logs).contains("percent"));
    }

    private Path logsFolder(String workspace) throws IOException {
        return Files.createDirectories(temporary.resolve(workspace).resolve("logs"));
    }

    private static String read(Path logs) throws IOException {
        Path current = logs.resolve(DiagnosticLog.FILE_PATTERN.replace("%g", "0"));
        return Files.exists(current) ? Files.readString(current) : "";
    }
}
