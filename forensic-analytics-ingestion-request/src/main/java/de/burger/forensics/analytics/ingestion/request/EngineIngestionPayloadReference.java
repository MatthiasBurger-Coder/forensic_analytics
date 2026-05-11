package de.burger.forensics.analytics.ingestion.request;

import de.burger.forensics.analytics.domain.ingestion.AnalysisPayloadDescriptor;

import java.nio.file.Path;
import java.util.Objects;

public record EngineIngestionPayloadReference(
    AnalysisPayloadDescriptor descriptor,
    Path file
) {
    public EngineIngestionPayloadReference {
        Objects.requireNonNull(descriptor, "descriptor must not be null");
        file = Objects.requireNonNull(file, "file must not be null").toAbsolutePath().normalize();
    }
}
