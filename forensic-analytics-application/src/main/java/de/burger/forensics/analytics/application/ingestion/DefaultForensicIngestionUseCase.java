package de.burger.forensics.analytics.application.ingestion;

import de.burger.forensics.analytics.application.ingestion.command.AbortAnalysisSessionCommand;
import de.burger.forensics.analytics.application.ingestion.command.CompleteAnalysisSessionCommand;
import de.burger.forensics.analytics.application.ingestion.command.StartAnalysisSessionCommand;
import de.burger.forensics.analytics.application.ingestion.command.UploadAnalysisDataCommand;
import de.burger.forensics.analytics.application.ingestion.port.IngestionSessionRepository;
import de.burger.forensics.analytics.application.ingestion.result.AbortAnalysisSessionResult;
import de.burger.forensics.analytics.application.ingestion.result.CompleteAnalysisSessionResult;
import de.burger.forensics.analytics.application.ingestion.result.IngestionStatus;
import de.burger.forensics.analytics.application.ingestion.result.StartAnalysisSessionResult;
import de.burger.forensics.analytics.application.ingestion.result.UploadAnalysisDataResult;
import de.burger.forensics.analytics.domain.ingestion.IngestionPayload;
import de.burger.forensics.analytics.domain.ingestion.IngestionSession;

import java.util.Objects;
import java.util.UUID;

public final class DefaultForensicIngestionUseCase implements ForensicIngestionUseCase {
    private final IngestionSessionRepository repository;

    public DefaultForensicIngestionUseCase(IngestionSessionRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public StartAnalysisSessionResult start(StartAnalysisSessionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        var sessionId = UUID.randomUUID().toString();
        var session = IngestionSession.start(
            sessionId,
            command.buildIdentity().projectId(),
            command.schemaVersion()
        );

        repository.save(session);
        return new StartAnalysisSessionResult(sessionId, IngestionStatus.ACCEPTED, "Analysis session accepted");
    }

    @Override
    public UploadAnalysisDataResult upload(UploadAnalysisDataCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        var session = activeSession(command.sessionId());
        var payload = new IngestionPayload(
            command.sessionId(),
            command.moduleIdentity().moduleName(),
            command.schemaVersion(),
            command.payloadDescriptor(),
            command.payload()
        );

        var receivedItems = repository.appendPayload(payload);
        repository.update(session.withReceivedItems(receivedItems));
        return new UploadAnalysisDataResult(
            command.sessionId(),
            IngestionStatus.ACCEPTED,
            receivedItems,
            "Analysis data accepted"
        );
    }

    @Override
    public CompleteAnalysisSessionResult complete(CompleteAnalysisSessionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        var session = activeSession(command.sessionId());
        repository.update(session.complete());
        return new CompleteAnalysisSessionResult(
            command.sessionId(),
            IngestionStatus.COMPLETED,
            "Analysis session completed"
        );
    }

    @Override
    public AbortAnalysisSessionResult abort(AbortAnalysisSessionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        var session = repository.findById(command.sessionId())
            .orElseThrow(() -> IngestionSessionException.missing(command.sessionId()));
        repository.update(session.abort());
        return new AbortAnalysisSessionResult(
            command.sessionId(),
            IngestionStatus.ABORTED,
            "Analysis session aborted"
        );
    }

    private IngestionSession activeSession(String sessionId) {
        var session = repository.findById(sessionId)
            .orElseThrow(() -> IngestionSessionException.missing(sessionId));
        if (!session.acceptsPayload()) {
            throw IngestionSessionException.closed(sessionId);
        }
        return session;
    }
}
