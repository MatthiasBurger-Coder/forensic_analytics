package de.burger.forensics.analytics.services.ingestion.adapter.in.grpc;

import com.google.protobuf.ByteString;
import de.burger.forensics.analytics.ingestion.v1.AnalysisDataEnvelope;
import de.burger.forensics.analytics.ingestion.v1.AnalysisPayloadDescriptor;
import de.burger.forensics.analytics.ingestion.v1.AnalysisPayloadKind;
import de.burger.forensics.analytics.ingestion.v1.BuildIdentity;
import de.burger.forensics.analytics.ingestion.v1.ModuleIdentity;
import de.burger.forensics.analytics.ingestion.v1.PluginIdentity;
import de.burger.forensics.analytics.ingestion.v1.StartAnalysisSessionRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ForensicIngestionRequestValidatorTest {
    private final ForensicIngestionRequestValidator validator = new ForensicIngestionRequestValidator();

    @Test
    void acceptsCompleteStartRequest() {
        var request = StartAnalysisSessionRequest.newBuilder()
            .setBuildIdentity(buildIdentity())
            .setPluginIdentity(pluginIdentity())
            .setSchemaVersion("schema-v1")
            .build();

        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    void rejectsUploadWithoutProvenanceIdentity() {
        var envelope = envelope("session-1", "payload-a").toBuilder()
            .clearBuildIdentity()
            .build();

        assertThrows(ValidationException.class, () -> validator.validate(envelope));
    }

    @Test
    void rejectsLegacyPayloadTypeWithoutDescriptor() {
        var envelope = envelope("session-1", "payload-a").toBuilder()
            .clearPayloadDescriptor()
            .setPayloadType("SOURCE_FACTS")
            .build();

        assertThrows(ValidationException.class, () -> validator.validate(envelope));
    }

    @Test
    void rejectsUnspecifiedPayloadKindAndBlankAttributes() {
        var unspecifiedKind = envelope("session-1", "payload-a").toBuilder()
            .setPayloadDescriptor(payloadDescriptor("payload-a").toBuilder()
                .setKind(AnalysisPayloadKind.ANALYSIS_PAYLOAD_KIND_UNSPECIFIED))
            .build();
        var blankAttribute = envelope("session-1", "payload-a").toBuilder()
            .setPayloadDescriptor(payloadDescriptor("payload-a").toBuilder()
                .putAttributes("source", " "))
            .build();

        assertThrows(ValidationException.class, () -> validator.validate(unspecifiedKind));
        assertThrows(ValidationException.class, () -> validator.validate(blankAttribute));
    }

    @Test
    void rejectsUploadWithMissingPayloadBodyAndIdentityDetails() {
        var emptyPayload = envelope("session-1", "payload-a").toBuilder()
            .clearPayload()
            .build();
        var blankModule = envelope("session-1", "payload-a").toBuilder()
            .setModuleIdentity(ModuleIdentity.newBuilder()
                .setModuleName(" ")
                .setModulePath(":module-a"))
            .build();
        var blankBuild = envelope("session-1", "payload-a").toBuilder()
            .setBuildIdentity(buildIdentity().toBuilder()
                .setBuildId(" "))
            .build();
        var blankPlugin = envelope("session-1", "payload-a").toBuilder()
            .setPluginIdentity(pluginIdentity().toBuilder()
                .setPluginVersion(" "))
            .build();

        assertThrows(ValidationException.class, () -> validator.validate(emptyPayload));
        assertThrows(ValidationException.class, () -> validator.validate(blankModule));
        assertThrows(ValidationException.class, () -> validator.validate(blankBuild));
        assertThrows(ValidationException.class, () -> validator.validate(blankPlugin));
    }

    @Test
    void validatesCompleteAndAbortRequiredText() {
        assertDoesNotThrow(() -> validator.validateComplete("session-1"));
        assertDoesNotThrow(() -> validator.validateAbort("session-1", "operator aborted"));

        assertThrows(ValidationException.class, () -> validator.validateComplete(null));
        assertThrows(ValidationException.class, () -> validator.validateComplete(" "));
        assertThrows(ValidationException.class, () -> validator.validateAbort("session-1", " "));
    }

    static AnalysisDataEnvelope envelope(String sessionId, String payloadId) {
        return AnalysisDataEnvelope.newBuilder()
            .setSessionId(sessionId)
            .setBuildIdentity(buildIdentity())
            .setModuleIdentity(ModuleIdentity.newBuilder()
                .setModuleName("module-a")
                .setModulePath(":module-a"))
            .setPluginIdentity(pluginIdentity())
            .setSchemaVersion("schema-v1")
            .setPayloadDescriptor(payloadDescriptor(payloadId))
            .setPayload(ByteString.copyFromUtf8("{}"))
            .build();
    }

    static BuildIdentity buildIdentity() {
        return BuildIdentity.newBuilder()
            .setProjectId("project-a")
            .setRepositoryUrl("https://example.invalid/repo.git")
            .setBranchName("main")
            .setCommitHash("abcdef")
            .setBuildId("build-1")
            .setScanTimestamp("2026-05-16T00:00:00Z")
            .build();
    }

    static PluginIdentity pluginIdentity() {
        return PluginIdentity.newBuilder()
            .setPluginName("forensic-plugin")
            .setPluginVersion("0.1.0")
            .build();
    }

    static AnalysisPayloadDescriptor payloadDescriptor(String payloadId) {
        return AnalysisPayloadDescriptor.newBuilder()
            .setPayloadId(payloadId)
            .setKind(AnalysisPayloadKind.ANALYSIS_PAYLOAD_KIND_SOURCE_FACTS)
            .setContentType("application/json")
            .putAttributes("source", "test")
            .build();
    }

}
