package de.burger.forensics.analytics.services.analysisstore.domain;

public record ArtifactReference(
    String path,
    String type,
    String sha256,
    long sizeBytes
) {
    public ArtifactReference {
        path = RequiredText.require(path, "artifact.path");
        type = RequiredText.require(type, "artifact.type");
        sha256 = RequiredText.require(sha256, "artifact.sha256");
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("artifact.sizeBytes must not be negative");
        }
    }
}
