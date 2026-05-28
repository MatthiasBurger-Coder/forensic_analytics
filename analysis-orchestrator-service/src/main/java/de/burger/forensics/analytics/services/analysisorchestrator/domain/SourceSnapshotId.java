package de.burger.forensics.analytics.services.analysisorchestrator.domain;

public record SourceSnapshotId(String value) {
    public SourceSnapshotId {
        value = SafeMetadata.requireOpaqueId(value, "sourceSnapshotId");
    }
}
