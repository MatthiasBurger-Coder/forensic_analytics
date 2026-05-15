package de.burger.forensics.analytics.persistence;

import de.burger.forensics.analytics.domain.ingestion.AnalysisPayloadDescriptor;
import de.burger.forensics.analytics.domain.ingestion.AnalysisPayloadKind;
import de.burger.forensics.analytics.domain.ingestion.IngestionPayload;
import de.burger.forensics.analytics.domain.ingestion.IngestionSession;
import de.burger.forensics.analytics.domain.ingestion.IngestionSessionState;
import de.burger.forensics.analytics.observability.OperationLogger;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    @Test
    void logsWriteOperations() {
        var logger = new RecordingOperationLogger();
        var repository = new InMemoryIngestionSessionRepository(logger);
        var session = IngestionSession.start("session-1", "project-a", "schema-v1");

        repository.save(session);
        repository.appendPayload(payload());
        repository.update(session.complete().withReceivedItems(1));

        assertEquals(
            List.of(
                "started:persistence.ingestion-session.save",
                "succeeded:persistence.ingestion-session.save",
                "started:persistence.ingestion-payload.append",
                "succeeded:persistence.ingestion-payload.append",
                "started:persistence.ingestion-session.update",
                "succeeded:persistence.ingestion-session.update"
            ),
            logger.events()
        );
    }

    private IngestionPayload payload() {
        return new IngestionPayload("session-1", "module-a", "schema-v1", payloadDescriptor(), new byte[] {1});
    }

    private AnalysisPayloadDescriptor payloadDescriptor() {
        return new AnalysisPayloadDescriptor(
            "payload-1",
            AnalysisPayloadKind.SOURCE_FACTS,
            "application/json",
            Map.of("schema", "source-facts-v1")
        );
    }

    private static final class RecordingOperationLogger implements OperationLogger {
        private final List<String> events = new ArrayList<>();

        @Override
        public void started(String operation) {
            events.add("started:" + operation);
        }

        @Override
        public void succeeded(String operation, long durationMillis) {
            events.add("succeeded:" + operation);
        }

        @Override
        public void failed(String operation, long durationMillis, Throwable error) {
            events.add("failed:" + operation);
        }

        private List<String> events() {
            return List.copyOf(events);
        }
    }
}
