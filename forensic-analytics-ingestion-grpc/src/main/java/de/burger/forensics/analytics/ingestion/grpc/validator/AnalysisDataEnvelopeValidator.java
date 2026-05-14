package de.burger.forensics.analytics.ingestion.grpc.validator;

import de.burger.forensics.analytics.ingestion.v1.AnalysisDataEnvelope;
import de.burger.forensics.analytics.ingestion.v1.AnalysisPayloadDescriptor;
import de.burger.forensics.analytics.ingestion.v1.AnalysisPayloadKind;

public final class AnalysisDataEnvelopeValidator {
    public void validate(AnalysisDataEnvelope envelope) {
        RequiredFields.nonBlank(envelope.getSessionId(), "sessionId");
        RequiredFields.nonBlank(envelope.getSchemaVersion(), "schemaVersion");
        RequiredFields.present(envelope.hasPayloadDescriptor(), "payloadDescriptor");
        validate(envelope.getPayloadDescriptor());
        RequiredFields.nonEmpty(envelope.getPayload(), "payload");
    }

    private void validate(AnalysisPayloadDescriptor descriptor) {
        RequiredFields.nonBlank(descriptor.getPayloadId(), "payloadDescriptor.payloadId");
        RequiredFields.nonBlank(descriptor.getContentType(), "payloadDescriptor.contentType");
        if (
            descriptor.getKind() == AnalysisPayloadKind.ANALYSIS_PAYLOAD_KIND_UNSPECIFIED
                || descriptor.getKind() == AnalysisPayloadKind.UNRECOGNIZED
        ) {
            throw new ValidationException("payloadDescriptor.kind must be specified");
        }
        descriptor.getAttributesMap().forEach((key, value) -> {
            RequiredFields.nonBlank(key, "payloadDescriptor.attributes.key");
            RequiredFields.nonBlank(value, "payloadDescriptor.attributes[" + key + "]");
        });
    }
}
