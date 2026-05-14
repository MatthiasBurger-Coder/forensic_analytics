package de.burger.forensics.analytics.domain.ingestion;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IngestionDomainModelTest {
    @Test
    void ingestionPayloadDefensivelyCopiesBytes() {
        var bytes = new byte[] {1, 2, 3};
        var payload = new IngestionPayload("session-1", "module-a", "schema-v1", payloadDescriptor(), bytes);
        bytes[0] = 9;

        assertArrayEquals(new byte[] {1, 2, 3}, payload.payload());
        var returned = payload.payload();
        returned[1] = 9;
        assertArrayEquals(new byte[] {1, 2, 3}, payload.payload());
    }

    @Test
    void ingestionPayloadRejectsMissingFields() {
        var descriptor = payloadDescriptor();

        assertThrows(NullPointerException.class, () -> new IngestionPayload(null, "module", "schema", descriptor, new byte[] {1}));
        assertThrows(NullPointerException.class, () -> new IngestionPayload("session", null, "schema", descriptor, new byte[] {1}));
        assertThrows(NullPointerException.class, () -> new IngestionPayload("session", "module", null, descriptor, new byte[] {1}));
        assertThrows(NullPointerException.class, () -> new IngestionPayload("session", "module", "schema", null, new byte[] {1}));
        assertThrows(NullPointerException.class, () -> new IngestionPayload("session", "module", "schema", descriptor, null));
    }

    @Test
    void analysisPayloadDescriptorKeepsStablePayloadMetadata() {
        var attributes = new LinkedHashMap<String, String>();
        attributes.put("schema", "source-facts-v1");
        attributes.put("origin", "plugin");

        var descriptor = new AnalysisPayloadDescriptor(
            "payload-1",
            AnalysisPayloadKind.SOURCE_FACTS,
            "application/json",
            attributes
        );
        attributes.put("later", "ignored");

        assertEquals("payload-1", descriptor.payloadId());
        assertEquals(AnalysisPayloadKind.SOURCE_FACTS, descriptor.kind());
        assertEquals("application/json", descriptor.contentType());
        assertEquals(Map.of("origin", "plugin", "schema", "source-facts-v1"), descriptor.attributes());
        assertThrows(UnsupportedOperationException.class, () -> descriptor.attributes().put("x", "y"));
    }

    @Test
    void analysisPayloadDescriptorRejectsIncompleteMetadata() {
        assertThrows(IllegalArgumentException.class, () -> new AnalysisPayloadDescriptor(
            " ",
            AnalysisPayloadKind.SOURCE_FACTS,
            "application/json",
            Map.of()
        ));
        assertThrows(NullPointerException.class, () -> new AnalysisPayloadDescriptor(
            "payload-1",
            null,
            "application/json",
            Map.of()
        ));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisPayloadDescriptor(
            "payload-1",
            AnalysisPayloadKind.SOURCE_FACTS,
            " ",
            Map.of()
        ));
        assertThrows(NullPointerException.class, () -> new AnalysisPayloadDescriptor(
            "payload-1",
            AnalysisPayloadKind.SOURCE_FACTS,
            "application/json",
            null
        ));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisPayloadDescriptor(
            "payload-1",
            AnalysisPayloadKind.SOURCE_FACTS,
            "application/json",
            Map.of(" ", "value")
        ));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisPayloadDescriptor(
            "payload-1",
            AnalysisPayloadKind.SOURCE_FACTS,
            "application/json",
            Map.of("name", " ")
        ));
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

    private AnalysisPayloadDescriptor payloadDescriptor() {
        return new AnalysisPayloadDescriptor(
            "payload-1",
            AnalysisPayloadKind.SOURCE_FACTS,
            "application/json",
            Map.of("schema", "source-facts-v1")
        );
    }
}
