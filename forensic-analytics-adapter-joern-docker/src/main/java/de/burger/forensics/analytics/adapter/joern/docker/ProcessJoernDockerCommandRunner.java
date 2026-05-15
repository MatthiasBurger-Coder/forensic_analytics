package de.burger.forensics.analytics.adapter.joern.docker;

import de.burger.forensics.analytics.observability.OperationLogger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class ProcessJoernDockerCommandRunner implements JoernDockerCommandRunner {
    private final OperationLogger operationLogger;

    public ProcessJoernDockerCommandRunner() {
        this(OperationLogger.system(ProcessJoernDockerCommandRunner.class));
    }

    ProcessJoernDockerCommandRunner(OperationLogger operationLogger) {
        this.operationLogger = Objects.requireNonNull(operationLogger, "operationLogger must not be null");
    }

    @Override
    public JoernDockerCommandResult run(JoernDockerCommand command) {
        var verifiedCommand = Objects.requireNonNull(command, "command must not be null");
        return operationLogger.logged("adapter.joern-docker.command", () -> runVerified(verifiedCommand));
    }

    private JoernDockerCommandResult runVerified(JoernDockerCommand command) {
        Path stdout = null;
        Path stderr = null;
        try {
            stdout = Files.createTempFile(command.workingDirectory(), "joern-docker-stdout-", ".log");
            stderr = Files.createTempFile(command.workingDirectory(), "joern-docker-stderr-", ".log");
            var process = new ProcessBuilder(command.arguments())
                .directory(command.workingDirectory().toFile())
                .redirectOutput(stdout.toFile())
                .redirectError(stderr.toFile())
                .start();
            var completed = process.waitFor(command.timeout().toMillis(), TimeUnit.MILLISECONDS);
            if (!completed) {
                process.destroyForcibly();
                process.waitFor();
                return new JoernDockerCommandResult(124, "", "command timed out");
            }
            return new JoernDockerCommandResult(
                process.exitValue(),
                Files.readString(stdout, StandardCharsets.UTF_8),
                Files.readString(stderr, StandardCharsets.UTF_8)
            );
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to execute Joern Docker command.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while executing Joern Docker command.", e);
        } finally {
            deleteIfPresent(stdout);
            deleteIfPresent(stderr);
        }
    }

    private static void deleteIfPresent(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // Best-effort cleanup. The command result must not be hidden by platform-specific temp-file locks.
        }
    }
}
