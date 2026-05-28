package de.burger.forensics.analytics.services.ingestion.adapter.in.file;

import de.burger.forensics.analytics.services.ingestion.domain.PayloadDescriptor;

import java.nio.file.Path;
import java.util.Objects;

public record EngineIngestionPayloadReference(
    PayloadDescriptor descriptor,
    Path file
) {
    public EngineIngestionPayloadReference {
        Objects.requireNonNull(descriptor, "descriptor must not be null");
        file = Objects.requireNonNull(file, "file must not be null").toAbsolutePath().normalize();
    }
}
