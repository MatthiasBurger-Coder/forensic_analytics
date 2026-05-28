package de.burger.forensics.analytics.services.repositoryanalysis.adapter.out.git;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

public record GitCommand(Path workingDirectory, Duration timeout, List<String> arguments) {
    public GitCommand {
        Objects.requireNonNull(workingDirectory, "working directory must not be null");
        Objects.requireNonNull(timeout, "timeout must not be null");
        arguments = List.copyOf(arguments);
        if (arguments.isEmpty()) {
            throw new IllegalArgumentException("git arguments must not be empty");
        }
    }
}
