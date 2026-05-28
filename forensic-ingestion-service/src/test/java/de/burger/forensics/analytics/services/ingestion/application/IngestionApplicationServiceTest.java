package de.burger.forensics.analytics.services.ingestion.application;

import de.burger.forensics.analytics.services.ingestion.adapter.out.memory.InMemoryIngestionSessionRepository;
import de.burger.forensics.analytics.services.ingestion.application.command.StartAnalysisSessionCommand;
import de.burger.forensics.analytics.services.ingestion.application.command.UploadAnalysisDataCommand;
import de.burger.forensics.analytics.services.ingestion.application.port.AcceptedIngestionHandoffPort;
import de.burger.forensics.analytics.services.ingestion.domain.AnalysisPayloadKind;
import de.burger.forensics.analytics.services.ingestion.domain.BuildIdentity;
import de.burger.forensics.analytics.services.ingestion.domain.ModuleIdentity;
import de.burger.forensics.analytics.services.ingestion.domain.PayloadDescriptor;
import de.burger.forensics.analytics.services.ingestion.domain.PluginIdentity;
import de.burger.forensics.analytics.services.ingestion.domain.RawIngestionPayload;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IngestionApplicationServiceTest {
    private final InMemoryIngestionSessionRepository sessions = new InMemoryIngestionSessionRepository();
    private final RecordingHandoff handoff = new RecordingHandoff();
    private final IngestionApplicationService service = new IngestionApplicationService(sessions, handoff);

    @Test
    void startsSessionUploadsUniquePayloadsAndCompletes() {
        var start = service.start(new StartAnalysisSessionCommand(
            buildIdentity(),
            pluginIdentity(),
            "schema-v1"
        ));

        var firstUpload = service.upload(new UploadAnalysisDataCommand(start.sessionId(), payload("payload-a")));
        var duplicateUpload = service.upload(new UploadAnalysisDataCommand(start.sessionId(), payload("payload-a")));
        var complete = service.complete(start.sessionId());

        assertFalse(start.sessionId().isBlank());
        assertTrue(firstUpload.acceptedNewPayload());
        assertFalse(duplicateUpload.acceptedNewPayload());
        assertEquals(1, duplicateUpload.receivedItems());
        assertEquals(1, handoff.acceptedPayloads);
        assertEquals(start.sessionId(), complete.sessionId());
        assertEquals("Analysis session completed", complete.message());
    }

    @Test
    void rejectsUploadToMissingSession() {
        var command = new UploadAnalysisDataCommand("missing-session", payload("payload-a"));

        assertThrows(IngestionSessionNotFoundException.class, () -> service.upload(command));
    }

    @Test
    void rejectsUploadWithMismatchedSessionProvenance() {
        var start = service.start(new StartAnalysisSessionCommand(buildIdentity(), pluginIdentity(), "schema-v1"));

        assertThrows(IllegalArgumentException.class, () -> service.upload(new UploadAnalysisDataCommand(
            start.sessionId(),
            payload("payload-a", buildIdentity("other-project"), pluginIdentity(), "schema-v1", "{}")
        )));
        assertThrows(IllegalArgumentException.class, () -> service.upload(new UploadAnalysisDataCommand(
            start.sessionId(),
            payload("payload-a", buildIdentity(), new PluginIdentity("other-plugin", "0.1.0"), "schema-v1", "{}")
        )));
        assertThrows(IllegalArgumentException.class, () -> service.upload(new UploadAnalysisDataCommand(
            start.sessionId(),
            payload("payload-a", buildIdentity(), pluginIdentity(), "schema-v2", "{}")
        )));
    }

    @Test
    void rejectsConflictingDuplicatePayloadId() {
        var start = service.start(new StartAnalysisSessionCommand(buildIdentity(), pluginIdentity(), "schema-v1"));
        service.upload(new UploadAnalysisDataCommand(start.sessionId(), payload("payload-a", buildIdentity(), pluginIdentity(), "schema-v1", "{}")));

        var conflictingUpload = new UploadAnalysisDataCommand(
            start.sessionId(),
            payload("payload-a", buildIdentity(), pluginIdentity(), "schema-v1", "[]")
        );

        assertThrows(IllegalArgumentException.class, () -> service.upload(conflictingUpload));
    }

    @Test
    void rejectsBlankSessionIdsBeforeRepositoryLookup() {
        assertThrows(IllegalArgumentException.class, () -> service.complete(null));
        assertThrows(IllegalArgumentException.class, () -> service.complete(" "));
        assertThrows(IllegalArgumentException.class, () -> service.abort(" ", "operator aborted"));
    }

    @Test
    void abortedSessionCannotAcceptMorePayloads() {
        var start = service.start(new StartAnalysisSessionCommand(buildIdentity(), pluginIdentity(), "schema-v1"));
        service.abort(start.sessionId(), "operator aborted");

        var command = new UploadAnalysisDataCommand(start.sessionId(), payload("payload-a"));

        assertThrows(IllegalStateException.class, () -> service.upload(command));
    }

    private static RawIngestionPayload payload(String payloadId) {
        return payload(payloadId, buildIdentity(), pluginIdentity(), "schema-v1", "{}");
    }

    private static RawIngestionPayload payload(
        String payloadId,
        BuildIdentity buildIdentity,
        PluginIdentity pluginIdentity,
        String schemaVersion,
        String bytes
    ) {
        return new RawIngestionPayload(
            buildIdentity,
            new ModuleIdentity("module-a", ":module-a"),
            pluginIdentity,
            schemaVersion,
            new PayloadDescriptor(payloadId, AnalysisPayloadKind.SOURCE_FACTS, "application/json", Map.of("kind", "test")),
            bytes.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static BuildIdentity buildIdentity() {
        return buildIdentity("project-a");
    }

    private static BuildIdentity buildIdentity(String projectId) {
        return new BuildIdentity(
            projectId,
            "https://example.invalid/repo.git",
            "main",
            "abcdef",
            "build-1",
            "2026-05-16T00:00:00Z"
        );
    }

    private static PluginIdentity pluginIdentity() {
        return new PluginIdentity("forensic-plugin", "0.1.0");
    }

    private static final class RecordingHandoff implements AcceptedIngestionHandoffPort {
        private int acceptedPayloads;

        @Override
        public void accepted(String sessionId, RawIngestionPayload payload) {
            acceptedPayloads++;
        }
    }
}
