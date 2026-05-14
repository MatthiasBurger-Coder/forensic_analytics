package de.burger.forensics.analytics.domain.repository;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

public record RepositoryReference(
    String remoteUrl,
    Optional<String> provider,
    Map<String, String> attributes
) {
    public RepositoryReference {
        RequiredRepositoryText.requireText(remoteUrl, "remote url");
        provider = copyOptionalText(provider, "provider");
        attributes = copyAttributes(attributes);
    }

    private static Optional<String> copyOptionalText(Optional<String> value, String fieldName) {
        var copied = Objects.requireNonNull(value, fieldName + " must not be null");
        copied.ifPresent(text -> RequiredRepositoryText.requireText(text, fieldName));
        return copied;
    }

    private static Map<String, String> copyAttributes(Map<String, String> attributes) {
        Objects.requireNonNull(attributes, "attributes must not be null");
        var sorted = new TreeMap<String, String>();
        attributes.forEach((key, value) -> {
            RequiredRepositoryText.requireText(key, "attribute key");
            RequiredRepositoryText.requireText(value, "attribute value");
            sorted.put(key, value);
        });
        return Collections.unmodifiableMap(sorted);
    }
}
