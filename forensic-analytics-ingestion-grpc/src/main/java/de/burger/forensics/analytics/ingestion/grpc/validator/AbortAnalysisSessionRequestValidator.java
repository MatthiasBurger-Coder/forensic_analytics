package de.burger.forensics.analytics.ingestion.grpc.validator;

import de.burger.forensics.analytics.ingestion.v1.AbortAnalysisSessionRequest;

public final class AbortAnalysisSessionRequestValidator {
    public void validate(AbortAnalysisSessionRequest request) {
        RequiredFields.nonBlank(request.getSessionId(), "sessionId");
        RequiredFields.nonBlank(request.getReason(), "reason");
    }
}
