package de.burger.forensics.analytics.observability;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public record CorrelationId(String value) {
    private static final Pattern UNSAFE_EXTERNAL_CHARACTER = Pattern.compile("[^A-Za-z0-9_.:-]");

    public CorrelationId {
        value = Objects.requireNonNull(value, "value must not be null").strip();
        if (value.isBlank()) {
            throw new IllegalArgumentException("correlation id must not be blank");
        }
    }

    public static CorrelationId generate() {
        return new CorrelationId(UUID.randomUUID().toString());
    }

    public static CorrelationId fromExternal(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return generate();
        }
        var sanitized = UNSAFE_EXTERNAL_CHARACTER.matcher(rawValue.strip()).replaceAll("_");
        return new CorrelationId(sanitized);
    }
}
