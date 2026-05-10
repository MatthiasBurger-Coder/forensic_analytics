package de.burger.forensics.analytics.ingestion.grpc.mapper;

import com.google.protobuf.ByteString;
import de.burger.forensics.analytics.ingestion.v1.AnalysisDataEnvelope;
import de.burger.forensics.analytics.ingestion.v1.BuildIdentity;
import de.burger.forensics.analytics.ingestion.v1.ModuleIdentity;
import de.burger.forensics.analytics.ingestion.v1.PluginIdentity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
            .setPayloadType("generic")
            .setPayload(ByteString.copyFrom(payload))
            .build());

        assertEquals("session-1", command.sessionId());
        assertEquals("project-a", command.buildIdentity().projectId());
        assertEquals("module-a", command.moduleIdentity().moduleName());
        assertEquals("forensic-plugin", command.pluginIdentity().pluginName());
        assertEquals("schema-v1", command.schemaVersion());
        assertEquals("generic", command.payloadType());
        assertArrayEquals(payload, command.payload());
    }
}
