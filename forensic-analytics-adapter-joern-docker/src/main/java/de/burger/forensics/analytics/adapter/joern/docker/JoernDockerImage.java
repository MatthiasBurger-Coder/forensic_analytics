package de.burger.forensics.analytics.adapter.joern.docker;

import java.util.regex.Pattern;

public record JoernDockerImage(String reference) {
    private static final Pattern SHA256_PINNED_IMAGE = Pattern.compile(".+@sha256:[0-9a-fA-F]{64}");

    public JoernDockerImage {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("Joern Docker image reference must not be blank");
        }
        if (!SHA256_PINNED_IMAGE.matcher(reference).matches()) {
            throw new IllegalArgumentException("Joern Docker image must be pinned by sha256 digest");
        }
    }
}
