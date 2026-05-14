package de.burger.forensics.analytics.adapter.repository.source;

import de.burger.forensics.analytics.application.ingestion.RepositoryCheckoutException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

final class GitCommandRunner {
    GitCommandResult run(Path workingDirectory, Duration timeout, List<String> command) {
        Objects.requireNonNull(workingDirectory, "workingDirectory must not be null");
        Objects.requireNonNull(timeout, "timeout must not be null");
        var commandLine = List.copyOf(Objects.requireNonNull(command, "command must not be null"));
        if (timeout.isNegative() || timeout.isZero()) {
            throw new RepositoryCheckoutException("Git command timeout must be positive");
        }
        try {
            var process = new ProcessBuilder(commandLine)
                .directory(workingDirectory.toFile())
                .redirectErrorStream(true)
                .start();
            var completed = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new RepositoryCheckoutException("Git command timed out: " + redactedCommand(commandLine));
            }
            var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                throw new RepositoryCheckoutException(
                    "Git command failed: " + redactedCommand(commandLine) + System.lineSeparator() + output.strip()
                );
            }
            return new GitCommandResult(output);
        } catch (IOException e) {
            throw new RepositoryCheckoutException("Failed to execute Git command: " + redactedCommand(commandLine), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RepositoryCheckoutException("Git command interrupted: " + redactedCommand(commandLine), e);
        }
    }

    private static String redactedCommand(List<String> commandLine) {
        if (commandLine.contains("clone")) {
            return "git clone <repository-url> <workspace>";
        }
        return String.join(" ", commandLine);
    }
}
