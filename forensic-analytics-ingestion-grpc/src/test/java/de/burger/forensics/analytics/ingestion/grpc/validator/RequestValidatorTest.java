package de.burger.forensics.analytics.ingestion.grpc.validator;

import com.google.protobuf.ByteString;
import de.burger.forensics.analytics.ingestion.v1.AbortAnalysisSessionRequest;
import de.burger.forensics.analytics.ingestion.v1.AnalysisDataEnvelope;
import de.burger.forensics.analytics.ingestion.v1.AnalysisPayloadDescriptor;
import de.burger.forensics.analytics.ingestion.v1.AnalysisPayloadKind;
import de.burger.forensics.analytics.ingestion.v1.BuildIdentity;
import de.burger.forensics.analytics.ingestion.v1.CompleteAnalysisSessionRequest;
import de.burger.forensics.analytics.ingestion.v1.PluginIdentity;
import de.burger.forensics.analytics.ingestion.v1.StartAnalysisSessionRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestValidatorTest {
    @Test
    void startSessionAcceptsRequiredFields() {
        var request = StartAnalysisSessionRequest.newBuilder()
            .setBuildIdentity(validBuildIdentity())
            .setPluginIdentity(validPluginIdentity())
            .setSchemaVersion("schema-v1")
            .build();

        assertDoesNotThrow(() -> new StartAnalysisSessionRequestValidator().validate(request));
    }

    @Test
    void startSessionRejectsMissingBuildId() {
        var request = StartAnalysisSessionRequest.newBuilder()
            .setBuildIdentity(validBuildIdentity().toBuilder().clearBuildId())
            .setPluginIdentity(validPluginIdentity())
            .setSchemaVersion("schema-v1")
            .build();

        assertThrows(ValidationException.class, () -> new StartAnalysisSessionRequestValidator().validate(request));
    }

    @Test
    void startSessionRejectsMissingBuildIdentity() {
        var request = StartAnalysisSessionRequest.newBuilder()
            .setPluginIdentity(validPluginIdentity())
            .setSchemaVersion("schema-v1")
            .build();

        assertThrows(ValidationException.class, () -> new StartAnalysisSessionRequestValidator().validate(request));
    }

    @Test
    void startSessionRejectsMissingPluginIdentity() {
        var request = StartAnalysisSessionRequest.newBuilder()
            .setBuildIdentity(validBuildIdentity())
            .setSchemaVersion("schema-v1")
            .build();

        assertThrows(ValidationException.class, () -> new StartAnalysisSessionRequestValidator().validate(request));
    }

    @Test
    void uploadEnvelopeAcceptsRequiredFields() {
        var envelope = AnalysisDataEnvelope.newBuilder()
            .setSessionId("session-1")
            .setSchemaVersion("schema-v1")
            .setPayloadDescriptor(validPayloadDescriptor())
            .setPayload(ByteString.copyFromUtf8("{}"))
            .build();

        assertDoesNotThrow(() -> new AnalysisDataEnvelopeValidator().validate(envelope));
    }

    @Test
    void uploadEnvelopeRejectsMissingPayloadDescriptor() {
        var envelope = AnalysisDataEnvelope.newBuilder()
            .setSessionId("session-1")
            .setSchemaVersion("schema-v1")
            .setPayload(ByteString.copyFromUtf8("{}"))
            .build();

        assertThrows(ValidationException.class, () -> new AnalysisDataEnvelopeValidator().validate(envelope));
    }

    @Test
    void uploadEnvelopeRejectsEmptyPayload() {
        var envelope = AnalysisDataEnvelope.newBuilder()
            .setSessionId("session-1")
            .setSchemaVersion("schema-v1")
            .setPayloadDescriptor(validPayloadDescriptor())
            .build();

        assertThrows(ValidationException.class, () -> new AnalysisDataEnvelopeValidator().validate(envelope));
    }

    @Test
    void uploadEnvelopeRejectsUnspecifiedPayloadKind() {
        var envelope = AnalysisDataEnvelope.newBuilder()
            .setSessionId("session-1")
            .setSchemaVersion("schema-v1")
            .setPayloadDescriptor(validPayloadDescriptor().toBuilder()
                .setKind(AnalysisPayloadKind.ANALYSIS_PAYLOAD_KIND_UNSPECIFIED))
            .setPayload(ByteString.copyFromUtf8("{}"))
            .build();

        assertThrows(ValidationException.class, () -> new AnalysisDataEnvelopeValidator().validate(envelope));
    }

    @Test
    void uploadEnvelopeRejectsIncompletePayloadDescriptor() {
        var missingPayloadId = AnalysisDataEnvelope.newBuilder()
            .setSessionId("session-1")
            .setSchemaVersion("schema-v1")
            .setPayloadDescriptor(validPayloadDescriptor().toBuilder().clearPayloadId())
            .setPayload(ByteString.copyFromUtf8("{}"))
            .build();
        var missingContentType = missingPayloadId.toBuilder()
            .setPayloadDescriptor(validPayloadDescriptor().toBuilder().clearContentType())
            .build();
        var blankAttributeKey = missingPayloadId.toBuilder()
            .setPayloadDescriptor(validPayloadDescriptor().toBuilder().clearAttributes().putAttributes("", "value"))
            .build();
        var blankAttributeValue = missingPayloadId.toBuilder()
            .setPayloadDescriptor(validPayloadDescriptor().toBuilder().clearAttributes().putAttributes("name", " "))
            .build();

        var validator = new AnalysisDataEnvelopeValidator();
        assertThrows(ValidationException.class, () -> validator.validate(missingPayloadId));
        assertThrows(ValidationException.class, () -> validator.validate(missingContentType));
        assertThrows(ValidationException.class, () -> validator.validate(blankAttributeKey));
        assertThrows(ValidationException.class, () -> validator.validate(blankAttributeValue));
    }

    @Test
    void completeSessionRequiresSessionId() {
        var request = CompleteAnalysisSessionRequest.newBuilder().build();

        assertThrows(ValidationException.class, () -> new CompleteAnalysisSessionRequestValidator().validate(request));
    }

    @Test
    void completeSessionAcceptsSessionId() {
        var request = CompleteAnalysisSessionRequest.newBuilder()
            .setSessionId("session-1")
            .build();

        assertDoesNotThrow(() -> new CompleteAnalysisSessionRequestValidator().validate(request));
    }

    @Test
    void abortSessionRequiresReason() {
        var request = AbortAnalysisSessionRequest.newBuilder()
            .setSessionId("session-1")
            .build();

        assertThrows(ValidationException.class, () -> new AbortAnalysisSessionRequestValidator().validate(request));
    }

    @Test
    void abortSessionAcceptsSessionIdAndReason() {
        var request = AbortAnalysisSessionRequest.newBuilder()
            .setSessionId("session-1")
            .setReason("cancelled")
            .build();

        assertDoesNotThrow(() -> new AbortAnalysisSessionRequestValidator().validate(request));
    }

    private BuildIdentity validBuildIdentity() {
        return BuildIdentity.newBuilder()
            .setProjectId("project-a")
            .setRepositoryUrl("https://example.invalid/repo.git")
            .setBranchName("main")
            .setCommitHash("abcdef")
            .setBuildId("build-1")
            .setScanTimestamp("2026-05-09T12:00:00Z")
            .build();
    }

    private PluginIdentity validPluginIdentity() {
        return PluginIdentity.newBuilder()
            .setPluginName("forensic-plugin")
            .setPluginVersion("0.1.0")
            .build();
    }

    private AnalysisPayloadDescriptor validPayloadDescriptor() {
        return AnalysisPayloadDescriptor.newBuilder()
            .setPayloadId("payload-1")
            .setKind(AnalysisPayloadKind.ANALYSIS_PAYLOAD_KIND_SOURCE_FACTS)
            .setContentType("application/json")
            .putAttributes("schema", "source-facts-v1")
            .build();
    }
}
