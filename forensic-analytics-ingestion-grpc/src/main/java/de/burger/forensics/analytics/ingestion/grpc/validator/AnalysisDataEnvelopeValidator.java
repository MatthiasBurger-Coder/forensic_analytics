package de.burger.forensics.analytics.ingestion.grpc.validator;

import de.burger.forensics.analytics.ingestion.v1.AnalysisDataEnvelope;

public final class AnalysisDataEnvelopeValidator {
    public void validate(AnalysisDataEnvelope envelope) {
        RequiredFields.nonBlank(envelope.getSessionId(), "sessionId");
        RequiredFields.nonBlank(envelope.getPayloadType(), "payloadType");
        RequiredFields.nonBlank(envelope.getSchemaVersion(), "schemaVersion");
        RequiredFields.nonEmpty(envelope.getPayload(), "payload");
    }
}
