package de.burger.forensics.analytics.services.ingestion.adapter.out.memory;

import de.burger.forensics.analytics.services.ingestion.application.port.AcceptedIngestionHandoffPort;
import de.burger.forensics.analytics.services.ingestion.domain.RawIngestionPayload;

public final class NoOpAcceptedIngestionHandoffPort implements AcceptedIngestionHandoffPort {
    @Override
    public void accepted(String sessionId, RawIngestionPayload payload) {
        // Slice 04 keeps canonical fact handoff behind a port until Analysis Store contracts are implemented.
    }
}
