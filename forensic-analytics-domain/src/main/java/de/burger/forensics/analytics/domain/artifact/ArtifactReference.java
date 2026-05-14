package de.burger.forensics.analytics.domain.artifact;

public record ArtifactReference(
    String path,
    String type,
    String sha256,
    long sizeBytes
) {
    public ArtifactReference {
        requireText(path, "artifact path");
        requireText(type, "artifact type");
        requireText(sha256, "artifact checksum");
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("artifact size must not be negative");
        }
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
