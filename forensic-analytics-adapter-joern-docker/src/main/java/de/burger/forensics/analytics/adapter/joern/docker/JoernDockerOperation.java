package de.burger.forensics.analytics.adapter.joern.docker;

import java.util.Objects;

public record JoernDockerOperation(String name, JoernDockerCommand command) {
    public JoernDockerOperation {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("operation name must not be blank");
        }
        Objects.requireNonNull(command, "command must not be null");
    }
}
