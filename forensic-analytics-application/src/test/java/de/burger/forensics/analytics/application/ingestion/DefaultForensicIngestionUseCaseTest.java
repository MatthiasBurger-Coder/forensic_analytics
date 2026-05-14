package de.burger.forensics.analytics.application.ingestion;

import de.burger.forensics.analytics.application.ingestion.command.AbortAnalysisSessionCommand;
import de.burger.forensics.analytics.application.ingestion.command.BuildIdentityCommand;
import de.burger.forensics.analytics.application.ingestion.command.CompleteAnalysisSessionCommand;
import de.burger.forensics.analytics.application.ingestion.command.ModuleIdentityCommand;
import de.burger.forensics.analytics.application.ingestion.command.PluginIdentityCommand;
import de.burger.forensics.analytics.application.ingestion.command.StartAnalysisSessionCommand;
import de.burger.forensics.analytics.application.ingestion.command.UploadAnalysisDataCommand;
import de.burger.forensics.analytics.application.ingestion.port.IngestionSessionRepository;
import de.burger.forensics.analytics.application.ingestion.result.IngestionStatus;
import de.burger.forensics.analytics.domain.ingestion.AnalysisPayloadDescriptor;
import de.burger.forensics.analytics.domain.ingestion.AnalysisPayloadKind;
import de.burger.forensics.analytics.domain.ingestion.IngestionPayload;
import de.burger.forensics.analytics.domain.ingestion.IngestionSession;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultForensicIngestionUseCaseTest {
    private final RecordingRepository repository = new RecordingRepository();
    private final DefaultForensicIngestionUseCase useCase = new DefaultForensicIngestionUseCase(repository);

    @Test
    void startCreatesActiveSession() {
        var result = useCase.start(startCommand());

        assertEquals(IngestionStatus.ACCEPTED, result.status());
        assertEquals(1, repository.sessions.size());
        assertEquals(result.sessionId(), repository.sessions.get(result.sessionId()).sessionId());
    }

    @Test
    void uploadCountsPayloadsForActiveSession() {
        var session = useCase.start(startCommand());

        var first = useCase.upload(uploadCommand(session.sessionId()));
        var second = useCase.upload(uploadCommand(session.sessionId()));

        assertEquals(1, first.receivedItems());
        assertEquals(2, second.receivedItems());
        assertEquals(AnalysisPayloadKind.SOURCE_FACTS, repository.lastPayload.payloadDescriptor().kind());
        assertEquals("payload-1", repository.lastPayload.payloadDescriptor().payloadId());
    }

    @Test
    void uploadRejectsCompletedSession() {
        var session = useCase.start(startCommand());
        useCase.complete(new CompleteAnalysisSessionCommand(session.sessionId()));

        assertThrows(IngestionSessionException.class, () -> useCase.upload(uploadCommand(session.sessionId())));
    }

    @Test
    void abortMarksExistingSessionAsAborted() {
        var session = useCase.start(startCommand());

        var result = useCase.abort(new AbortAnalysisSessionCommand(session.sessionId(), "cancelled"));

        assertEquals(IngestionStatus.ABORTED, result.status());
        assertEquals("ABORTED", repository.sessions.get(session.sessionId()).state().name());
    }

    private StartAnalysisSessionCommand startCommand() {
        return new StartAnalysisSessionCommand(buildIdentity(), pluginIdentity(), "schema-v1");
    }

    private UploadAnalysisDataCommand uploadCommand(String sessionId) {
        return new UploadAnalysisDataCommand(
            sessionId,
            buildIdentity(),
            new ModuleIdentityCommand("module-a", ":module-a"),
            pluginIdentity(),
            "schema-v1",
            payloadDescriptor(),
            new byte[] {1, 2, 3}
        );
    }

    private AnalysisPayloadDescriptor payloadDescriptor() {
        return new AnalysisPayloadDescriptor(
            "payload-1",
            AnalysisPayloadKind.SOURCE_FACTS,
            "application/json",
            Map.of("schema", "source-facts-v1")
        );
    }

    private BuildIdentityCommand buildIdentity() {
        return new BuildIdentityCommand(
            "project-a",
            "https://example.invalid/repo.git",
            "main",
            "abcdef",
            "build-1",
            "2026-05-09T12:00:00Z"
        );
    }

    private PluginIdentityCommand pluginIdentity() {
        return new PluginIdentityCommand("forensic-plugin", "0.1.0");
    }

    private static final class RecordingRepository implements IngestionSessionRepository {
        private final Map<String, IngestionSession> sessions = new HashMap<>();
        private final Map<String, Long> payloadCounts = new HashMap<>();
        private IngestionPayload lastPayload;

        @Override
        public void save(IngestionSession session) {
            sessions.put(session.sessionId(), session);
            payloadCounts.put(session.sessionId(), 0L);
        }

        @Override
        public Optional<IngestionSession> findById(String sessionId) {
            return Optional.ofNullable(sessions.get(sessionId));
        }

        @Override
        public void update(IngestionSession session) {
            sessions.put(session.sessionId(), session);
        }

        @Override
        public long appendPayload(IngestionPayload payload) {
            lastPayload = payload;
            var nextCount = payloadCounts.getOrDefault(payload.sessionId(), 0L) + 1;
            payloadCounts.put(payload.sessionId(), nextCount);
            return nextCount;
        }
    }
}
