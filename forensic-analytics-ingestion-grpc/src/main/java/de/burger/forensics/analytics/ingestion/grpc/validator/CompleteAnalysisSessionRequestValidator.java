package de.burger.forensics.analytics.ingestion.grpc.validator;

import de.burger.forensics.analytics.ingestion.v1.CompleteAnalysisSessionRequest;

public final class CompleteAnalysisSessionRequestValidator {
    public void validate(CompleteAnalysisSessionRequest request) {
        RequiredFields.nonBlank(request.getSessionId(), "sessionId");
    }
}
