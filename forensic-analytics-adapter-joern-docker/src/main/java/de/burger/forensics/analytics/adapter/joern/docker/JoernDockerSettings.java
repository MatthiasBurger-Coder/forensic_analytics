package de.burger.forensics.analytics.adapter.joern.docker;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

public record JoernDockerSettings(
    String dockerExecutable,
    JoernDockerImage image,
    Path outputDirectory,
    Duration timeout,
    boolean failOnError
) {
    public JoernDockerSettings {
        if (dockerExecutable == null || dockerExecutable.isBlank()) {
            throw new IllegalArgumentException("docker executable must not be blank");
        }
        Objects.requireNonNull(image, "image must not be null");
        outputDirectory = Objects.requireNonNull(outputDirectory, "outputDirectory must not be null")
            .toAbsolutePath()
            .normalize();
        Objects.requireNonNull(timeout, "timeout must not be null");
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
    }
}
