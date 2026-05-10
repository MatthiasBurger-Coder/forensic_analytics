package de.burger.forensics.analytics.adapter.joern.docker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessJoernDockerCommandRunnerTest {
    @TempDir
    Path tempDir;

    private final ProcessJoernDockerCommandRunner runner = new ProcessJoernDockerCommandRunner();

    @Test
    void executesLocalProcessAndCapturesOutput() {
        var result = runner.run(new JoernDockerCommand(
            List.of(javaExecutable().toString(), "-version"),
            Duration.ofSeconds(10),
            tempDir
        ));

        assertTrue(result.successful());
        assertFalse((result.stdout() + result.stderr()).isBlank());
    }

    @Test
    void timesOutLongRunningProcess() throws Exception {
        var source = tempDir.resolve("SleepCommand.java");
        Files.writeString(
            source,
            "public class SleepCommand { public static void main(String[] args) throws Exception { Thread.sleep(5000); } }",
            StandardCharsets.UTF_8
        );

        var result = runner.run(new JoernDockerCommand(
            List.of(javaExecutable().toString(), source.toString()),
            Duration.ofMillis(200),
            tempDir
        ));

        assertEquals(124, result.exitCode());
        assertEquals("command timed out", result.stderr());
    }

    private static Path javaExecutable() {
        var executable = System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home"), "bin", executable);
    }
}
