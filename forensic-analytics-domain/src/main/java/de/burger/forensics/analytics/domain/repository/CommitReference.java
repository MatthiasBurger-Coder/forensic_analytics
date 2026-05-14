package de.burger.forensics.analytics.domain.repository;

import java.util.Objects;
import java.util.Optional;

public record CommitReference(Optional<String> hash, boolean required) {
    public CommitReference {
        hash = copyOptionalText(hash, "commit hash");
    }

    public boolean isSpecified() {
        return hash.isPresent();
    }

    private static Optional<String> copyOptionalText(Optional<String> value, String fieldName) {
        var copied = Objects.requireNonNull(value, fieldName + " must not be null");
        copied.ifPresent(text -> RequiredRepositoryText.requireText(text, fieldName));
        return copied;
    }
}
