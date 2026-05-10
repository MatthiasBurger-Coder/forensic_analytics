package de.burger.forensics.analytics.domain.ingestion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IngestionDomainModelTest {
    @Test
    void ingestionPayloadDefensivelyCopiesBytes() {
        var bytes = new byte[] {1, 2, 3};
        var payload = new IngestionPayload("session-1", "module-a", "schema-v1", "source-facts", bytes);
        bytes[0] = 9;

        assertArrayEquals(new byte[] {1, 2, 3}, payload.payload());
        var returned = payload.payload();
        returned[1] = 9;
        assertArrayEquals(new byte[] {1, 2, 3}, payload.payload());
    }

    @Test
    void ingestionPayloadRejectsMissingFields() {
        assertThrows(NullPointerException.class, () -> new IngestionPayload(null, "module", "schema", "type", new byte[] {1}));
        assertThrows(NullPointerException.class, () -> new IngestionPayload("session", null, "schema", "type", new byte[] {1}));
        assertThrows(NullPointerException.class, () -> new IngestionPayload("session", "module", null, "type", new byte[] {1}));
        assertThrows(NullPointerException.class, () -> new IngestionPayload("session", "module", "schema", null, new byte[] {1}));
        assertThrows(NullPointerException.class, () -> new IngestionPayload("session", "module", "schema", "type", null));
    }

    @Test
    void ingestionSessionTracksStateTransitions() {
        var active = IngestionSession.start("session-1", "project-a", "schema-v1");

        assertTrue(active.acceptsPayload());
        assertEquals(0L, active.receivedItems());
        assertEquals(3L, active.withReceivedItems(3L).receivedItems());
        assertFalse(active.complete().acceptsPayload());
        assertFalse(active.abort().acceptsPayload());
    }

    @Test
    void ingestionSessionRejectsInvalidState() {
        assertThrows(NullPointerException.class, () -> new IngestionSession(null, "project", "schema", IngestionSessionState.ACTIVE, 0L));
        assertThrows(NullPointerException.class, () -> new IngestionSession("session", null, "schema", IngestionSessionState.ACTIVE, 0L));
        assertThrows(NullPointerException.class, () -> new IngestionSession("session", "project", null, IngestionSessionState.ACTIVE, 0L));
        assertThrows(NullPointerException.class, () -> new IngestionSession("session", "project", "schema", null, 0L));
        assertThrows(IllegalArgumentException.class, () -> new IngestionSession("session", "project", "schema", IngestionSessionState.ACTIVE, -1L));
    }
}
