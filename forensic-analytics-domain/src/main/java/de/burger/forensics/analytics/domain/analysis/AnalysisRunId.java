package de.burger.forensics.analytics.domain.analysis;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

public record AnalysisRunId(String value) {
    public AnalysisRunId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("analysis run id must not be blank");
        }
    }

    public static AnalysisRunId random() {
        return new AnalysisRunId(UUID.randomUUID().toString());
    }

    public static AnalysisRunId deterministic(String seed) {
        Objects.requireNonNull(seed, "seed must not be null");
        return new AnalysisRunId(UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString());
    }
}
