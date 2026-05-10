package de.burger.forensics.analytics.application.ingestion.command;

import java.util.Objects;

public record AbortAnalysisSessionCommand(String sessionId, String reason) {
    public AbortAnalysisSessionCommand {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
    }
}
