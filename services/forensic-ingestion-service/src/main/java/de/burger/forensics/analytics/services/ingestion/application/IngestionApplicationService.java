package de.burger.forensics.analytics.services.ingestion.application;

import de.burger.forensics.analytics.services.ingestion.application.command.StartAnalysisSessionCommand;
import de.burger.forensics.analytics.services.ingestion.application.command.UploadAnalysisDataCommand;
import de.burger.forensics.analytics.services.ingestion.application.port.AcceptedIngestionHandoffPort;
import de.burger.forensics.analytics.services.ingestion.application.port.IngestionSessionRepository;
import de.burger.forensics.analytics.services.ingestion.application.result.AbortAnalysisSessionResult;
import de.burger.forensics.analytics.services.ingestion.application.result.CompleteAnalysisSessionResult;
import de.burger.forensics.analytics.services.ingestion.application.result.StartAnalysisSessionResult;
import de.burger.forensics.analytics.services.ingestion.application.result.UploadAnalysisDataResult;
import de.burger.forensics.analytics.services.ingestion.domain.IngestionSession;
import de.burger.forensics.analytics.services.ingestion.domain.IngestionStatus;

import java.util.Objects;
import java.util.UUID;

public final class IngestionApplicationService {
    private final IngestionSessionRepository sessions;
    private final AcceptedIngestionHandoffPort handoff;

    public IngestionApplicationService(IngestionSessionRepository sessions, AcceptedIngestionHandoffPort handoff) {
        this.sessions = Objects.requireNonNull(sessions, "sessions must not be null");
        this.handoff = Objects.requireNonNull(handoff, "handoff must not be null");
    }

    public StartAnalysisSessionResult start(StartAnalysisSessionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        var session = IngestionSession.accepted(
            UUID.randomUUID().toString(),
            command.buildIdentity(),
            command.pluginIdentity(),
            command.schemaVersion()
        );
        sessions.save(session);
        return new StartAnalysisSessionResult(session.sessionId(), IngestionStatus.ACCEPTED, "Analysis session accepted");
    }

    public UploadAnalysisDataResult upload(UploadAnalysisDataCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        var session = session(command.sessionId());
        var acceptance = session.accept(command.payload());
        sessions.save(acceptance.session());
        if (acceptance.acceptedNewPayload()) {
            handoff.accepted(command.sessionId(), command.payload());
        }
        return new UploadAnalysisDataResult(
            acceptance.session().sessionId(),
            IngestionStatus.ACCEPTED,
            acceptance.session().receivedItems(),
            acceptance.acceptedNewPayload(),
            acceptance.acceptedNewPayload() ? "Analysis payload accepted" : "Duplicate payload ignored"
        );
    }

    public CompleteAnalysisSessionResult complete(String sessionId) {
        var completed = session(sessionId).completed();
        sessions.save(completed);
        return new CompleteAnalysisSessionResult(
            completed.sessionId(),
            IngestionStatus.COMPLETED,
            "Analysis session completed"
        );
    }

    public AbortAnalysisSessionResult abort(String sessionId, String reason) {
        var aborted = session(sessionId).aborted(reason);
        sessions.save(aborted);
        return new AbortAnalysisSessionResult(
            aborted.sessionId(),
            IngestionStatus.ABORTED,
            "Analysis session aborted"
        );
    }

    private IngestionSession session(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
        return sessions.findById(sessionId.strip())
            .orElseThrow(() -> new IngestionSessionNotFoundException(sessionId.strip()));
    }
}
