package de.burger.forensics.analytics.adapter.joern.docker;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

public record JoernDockerCommand(
    List<String> arguments,
    Duration timeout,
    Path workingDirectory
) {
    public JoernDockerCommand {
        arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments must not be null"));
        Objects.requireNonNull(timeout, "timeout must not be null");
        Objects.requireNonNull(workingDirectory, "workingDirectory must not be null");
    }
}
