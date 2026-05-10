package de.burger.forensics.analytics.persistence;

import de.burger.forensics.analytics.domain.ingestion.IngestionPayload;
import de.burger.forensics.analytics.domain.ingestion.IngestionSession;
import de.burger.forensics.analytics.domain.ingestion.IngestionSessionState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryIngestionSessionRepositoryTest {
    @Test
    void savesUpdatesAndCountsPayloads() {
        var repository = new InMemoryIngestionSessionRepository();
        var session = IngestionSession.start("session-1", "project-a", "schema-v1");

        repository.save(session);
        var firstCount = repository.appendPayload(payload());
        var secondCount = repository.appendPayload(payload());
        repository.update(session.complete().withReceivedItems(secondCount));

        var stored = repository.findById("session-1");
        assertTrue(stored.isPresent());
        assertEquals(1, firstCount);
        assertEquals(2, secondCount);
        assertEquals(IngestionSessionState.COMPLETED, stored.orElseThrow().state());
        assertEquals(2, stored.orElseThrow().receivedItems());
    }

    private IngestionPayload payload() {
        return new IngestionPayload("session-1", "module-a", "schema-v1", "generic", new byte[] {1});
    }
}
