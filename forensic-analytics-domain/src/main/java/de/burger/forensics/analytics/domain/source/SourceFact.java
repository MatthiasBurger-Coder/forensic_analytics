package de.burger.forensics.analytics.domain.source;

import java.util.Objects;

public record SourceFact(
    String factType,
    SourceLocation location,
    String signature,
    String summary
) {
    public SourceFact {
        requireText(factType, "fact type");
        Objects.requireNonNull(location, "location must not be null");
        requireText(signature, "signature");
        requireText(summary, "summary");
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
