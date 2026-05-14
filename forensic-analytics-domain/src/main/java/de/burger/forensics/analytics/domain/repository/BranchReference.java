package de.burger.forensics.analytics.domain.repository;

import java.util.Objects;
import java.util.Optional;

public record BranchReference(Optional<String> name, boolean required) {
    public BranchReference {
        name = copyOptionalText(name, "branch name");
    }

    public boolean isSpecified() {
        return name.isPresent();
    }

    private static Optional<String> copyOptionalText(Optional<String> value, String fieldName) {
        var copied = Objects.requireNonNull(value, fieldName + " must not be null");
        copied.ifPresent(text -> RequiredRepositoryText.requireText(text, fieldName));
        return copied;
    }
}
