package de.burger.forensics.analytics.ingestion.grpc.mapper;

import com.google.protobuf.ByteString;
import de.burger.forensics.analytics.ingestion.v1.AnalysisDataEnvelope;
import de.burger.forensics.analytics.ingestion.v1.AnalysisPayloadDescriptor;
import de.burger.forensics.analytics.ingestion.v1.BuildIdentity;
import de.burger.forensics.analytics.ingestion.v1.ModuleIdentity;
import de.burger.forensics.analytics.ingestion.v1.PluginIdentity;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnalysisDataEnvelopeMapperTest {
    private final AnalysisDataEnvelopeMapper mapper = new AnalysisDataEnvelopeMapper(
        new BuildIdentityMapper(),
        new ModuleIdentityMapper(),
        new PluginIdentityMapper()
    );

    @Test
    void mapsEnvelopeToApplicationCommandWithoutChangingPayloadBytes() {
        var payload = new byte[] {1, 2, 3};
        var command = mapper.toCommand(AnalysisDataEnvelope.newBuilder()
            .setSessionId("session-1")
            .setBuildIdentity(BuildIdentity.newBuilder()
                .setProjectId("project-a")
                .setRepositoryUrl("https://example.invalid/repo.git")
                .setBranchName("main")
                .setCommitHash("abcdef")
                .setBuildId("build-1")
                .setScanTimestamp("2026-05-09T12:00:00Z"))
            .setModuleIdentity(ModuleIdentity.newBuilder()
                .setModuleName("module-a")
                .setModulePath(":module-a"))
            .setPluginIdentity(PluginIdentity.newBuilder()
                .setPluginName("forensic-plugin")
                .setPluginVersion("0.1.0"))
            .setSchemaVersion("schema-v1")
            .setPayloadDescriptor(payloadDescriptor(
                de.burger.forensics.analytics.ingestion.v1.AnalysisPayloadKind.ANALYSIS_PAYLOAD_KIND_SOURCE_FACTS
            ))
            .setPayload(ByteString.copyFrom(payload))
            .build());

        assertEquals("session-1", command.sessionId());
        assertEquals("project-a", command.buildIdentity().projectId());
        assertEquals("module-a", command.moduleIdentity().moduleName());
        assertEquals("forensic-plugin", command.pluginIdentity().pluginName());
        assertEquals("schema-v1", command.schemaVersion());
        assertEquals("payload-1", command.payloadDescriptor().payloadId());
        assertEquals(
            de.burger.forensics.analytics.domain.ingestion.AnalysisPayloadKind.SOURCE_FACTS,
            command.payloadDescriptor().kind()
        );
        assertEquals("application/json", command.payloadDescriptor().contentType());
        assertEquals(Map.of("schema", "source-facts-v1"), command.payloadDescriptor().attributes());
        assertArrayEquals(payload, command.payload());
    }

    @Test
    void mapsStableProtoPayloadKindsToDomainKinds() {
        assertEquals(
            de.burger.forensics.analytics.domain.ingestion.AnalysisPayloadKind.SOURCE_FACTS,
            mapKind(de.burger.forensics.analytics.ingestion.v1.AnalysisPayloadKind.ANALYSIS_PAYLOAD_KIND_SOURCE_FACTS)
        );
        assertEquals(
            de.burger.forensics.analytics.domain.ingestion.AnalysisPayloadKind.SEMANTIC_ARTIFACTS,
            mapKind(de.burger.forensics.analytics.ingestion.v1.AnalysisPayloadKind.ANALYSIS_PAYLOAD_KIND_SEMANTIC_ARTIFACTS)
        );
        assertEquals(
            de.burger.forensics.analytics.domain.ingestion.AnalysisPayloadKind.RULE_ARTIFACTS,
            mapKind(de.burger.forensics.analytics.ingestion.v1.AnalysisPayloadKind.ANALYSIS_PAYLOAD_KIND_RULE_ARTIFACTS)
        );
        assertEquals(
            de.burger.forensics.analytics.domain.ingestion.AnalysisPayloadKind.RUNTIME_TRACE,
            mapKind(de.burger.forensics.analytics.ingestion.v1.AnalysisPayloadKind.ANALYSIS_PAYLOAD_KIND_RUNTIME_TRACE)
        );
        assertEquals(
            de.burger.forensics.analytics.domain.ingestion.AnalysisPayloadKind.DIAGNOSTIC_REPORT,
            mapKind(de.burger.forensics.analytics.ingestion.v1.AnalysisPayloadKind.ANALYSIS_PAYLOAD_KIND_DIAGNOSTIC_REPORT)
        );
    }

    @Test
    void rejectsUnsupportedProtoPayloadKind() {
        var envelope = validEnvelope()
            .setPayloadDescriptor(payloadDescriptor(
                de.burger.forensics.analytics.ingestion.v1.AnalysisPayloadKind.ANALYSIS_PAYLOAD_KIND_UNSPECIFIED
            ))
            .build();

        assertThrows(IllegalArgumentException.class, () -> mapper.toCommand(envelope));
    }

    private de.burger.forensics.analytics.domain.ingestion.AnalysisPayloadKind mapKind(
        de.burger.forensics.analytics.ingestion.v1.AnalysisPayloadKind kind
    ) {
        return mapper.toCommand(validEnvelope()
            .setPayloadDescriptor(payloadDescriptor(kind))
            .build()).payloadDescriptor().kind();
    }

    private AnalysisDataEnvelope.Builder validEnvelope() {
        return AnalysisDataEnvelope.newBuilder()
            .setSessionId("session-1")
            .setBuildIdentity(BuildIdentity.newBuilder()
                .setProjectId("project-a")
                .setRepositoryUrl("https://example.invalid/repo.git")
                .setBranchName("main")
                .setCommitHash("abcdef")
                .setBuildId("build-1")
                .setScanTimestamp("2026-05-09T12:00:00Z"))
            .setModuleIdentity(ModuleIdentity.newBuilder()
                .setModuleName("module-a")
                .setModulePath(":module-a"))
            .setPluginIdentity(PluginIdentity.newBuilder()
                .setPluginName("forensic-plugin")
                .setPluginVersion("0.1.0"))
            .setSchemaVersion("schema-v1")
            .setPayload(ByteString.copyFrom(new byte[] {1, 2, 3}));
    }

    private AnalysisPayloadDescriptor payloadDescriptor(
        de.burger.forensics.analytics.ingestion.v1.AnalysisPayloadKind kind
    ) {
        return AnalysisPayloadDescriptor.newBuilder()
            .setPayloadId("payload-1")
            .setKind(kind)
            .setContentType("application/json")
            .putAttributes("schema", "source-facts-v1")
            .build();
    }
}
