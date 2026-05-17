package de.burger.forensics.analytics.services.analysisstore.domain;

public record SourceSnapshotId(String value) {
    public SourceSnapshotId {
        value = RequiredText.require(value, "sourceSnapshotId");
    }
}
