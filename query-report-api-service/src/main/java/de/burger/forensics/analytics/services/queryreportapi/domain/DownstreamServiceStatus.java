package de.burger.forensics.analytics.services.queryreportapi.domain;

import java.util.Objects;

public record DownstreamServiceStatus(
    String name,
    String status
) {
    public DownstreamServiceStatus {
        requireText(name, "service name");
        requireText(status, "status");
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        Objects.requireNonNull(value, name + " must not be null");
    }
}
