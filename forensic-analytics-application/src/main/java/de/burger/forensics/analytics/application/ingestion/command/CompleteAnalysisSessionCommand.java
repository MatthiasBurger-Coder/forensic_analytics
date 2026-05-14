package de.burger.forensics.analytics.application.ingestion.command;

import java.util.Objects;

public record CompleteAnalysisSessionCommand(String sessionId) {
    public CompleteAnalysisSessionCommand {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
    }
}
