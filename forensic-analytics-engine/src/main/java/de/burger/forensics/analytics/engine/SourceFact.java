package de.burger.forensics.analytics.engine;

import java.util.Objects;

public record SourceFact(String factType, String sourcePath, String summary) {
    public SourceFact {
        Objects.requireNonNull(factType, "factType must not be null");
        Objects.requireNonNull(sourcePath, "sourcePath must not be null");
        Objects.requireNonNull(summary, "summary must not be null");
    }
}
