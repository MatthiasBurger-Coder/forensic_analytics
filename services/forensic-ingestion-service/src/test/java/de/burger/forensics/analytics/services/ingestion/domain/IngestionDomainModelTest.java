package de.burger.forensics.analytics.services.ingestion.domain;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IngestionDomainModelTest {
    @Test
    void trimsOptionalIdentityFieldsAndRejectsMissingRequiredText() {
        var identity = new BuildIdentity(
            " project-a ",
            " https://example.invalid/repo.git ",
            null,
            " abcdef ",
            " build-1 ",
            null
        );

        assertEquals("project-a", identity.projectId());
        assertEquals("https://example.invalid/repo.git", identity.repositoryUrl());
        assertEquals("", identity.branchName());
        assertEquals("abcdef", identity.commitHash());
        assertEquals("build-1", identity.buildId());
        assertEquals("", identity.scanTimestamp());
        assertThrows(IllegalArgumentException.class, () -> new BuildIdentity(
            " ",
            "https://example.invalid/repo.git",
            "main",
            "abcdef",
            "build-1",
            "2026-05-16T00:00:00Z"
        ));
    }

    @Test
    void protectsPayloadBytesAndRejectsMissingPayloadEvidence() {
        var sourceBytes = "{}".getBytes(StandardCharsets.UTF_8);
        var payload = payload(sourceBytes);
        sourceBytes[0] = '[';
        var returnedBytes = payload.payload();
        returnedBytes[0] = '[';

        assertArrayEquals("{}".getBytes(StandardCharsets.UTF_8), payload.payload());
        assertThrows(NullPointerException.class, () -> new RawIngestionPayload(
            buildIdentity("project-a"),
            moduleIdentity("module-a"),
            pluginIdentity("forensic-plugin"),
            "schema-v1",
            descriptor("payload-a"),
            null
        ));
        assertThrows(IllegalArgumentException.class, () -> new RawIngestionPayload(
            buildIdentity("project-a"),
            moduleIdentity("module-a"),
            pluginIdentity("forensic-plugin"),
            "schema-v1",
            descriptor("payload-a"),
            new byte[0]
        ));
    }

    @Test
    void rawPayloadEqualityIncludesAllProvenanceAndPayloadBytes() {
        var payload = payload("project-a", "module-a", "forensic-plugin", "schema-v1", "payload-a", "{}");

        assertEquals(payload, payload("project-a", "module-a", "forensic-plugin", "schema-v1", "payload-a", "{}"));
        assertEquals(payload.hashCode(), payload("project-a", "module-a", "forensic-plugin", "schema-v1", "payload-a", "{}").hashCode());
        assertNotEquals(payload, "not-a-payload");
        assertNotEquals(payload, payload("project-b", "module-a", "forensic-plugin", "schema-v1", "payload-a", "{}"));
        assertNotEquals(payload, payload("project-a", "module-b", "forensic-plugin", "schema-v1", "payload-a", "{}"));
        assertNotEquals(payload, payload("project-a", "module-a", "other-plugin", "schema-v1", "payload-a", "{}"));
        assertNotEquals(payload, payload("project-a", "module-a", "forensic-plugin", "schema-v2", "payload-a", "{}"));
        assertNotEquals(payload, payload("project-a", "module-a", "forensic-plugin", "schema-v1", "payload-b", "{}"));
        assertNotEquals(payload, payload("project-a", "module-a", "forensic-plugin", "schema-v1", "payload-a", "[]"));
    }

    @Test
    void acceptedSessionRequiresAcceptedStateForStateTransitions() {
        var session = IngestionSession.accepted(
            "session-1",
            buildIdentity("project-a"),
            pluginIdentity("forensic-plugin"),
            "schema-v1"
        );
        var completed = session.completed();

        assertTrue(session.accept(payload("{}")).acceptedNewPayload());
        assertThrows(IllegalStateException.class, () -> completed.completed());
        assertThrows(IllegalStateException.class, () -> completed.aborted("not allowed"));
    }

    @Test
    void acceptedSessionRejectsMismatchedProvenanceAndConflictingDuplicatePayloads() {
        var session = IngestionSession.accepted(
            "session-1",
            buildIdentity("project-a"),
            pluginIdentity("forensic-plugin"),
            "schema-v1"
        );
        var accepted = session.accept(payload("project-a", "module-a", "forensic-plugin", "schema-v1", "payload-a", "{}"));

        assertThrows(IllegalArgumentException.class, () -> session.accept(payload(
            "other-project",
            "module-a",
            "forensic-plugin",
            "schema-v1",
            "payload-a",
            "{}"
        )));
        assertThrows(IllegalArgumentException.class, () -> session.accept(payload(
            "project-a",
            "module-a",
            "other-plugin",
            "schema-v1",
            "payload-a",
            "{}"
        )));
        assertThrows(IllegalArgumentException.class, () -> session.accept(payload(
            "project-a",
            "module-a",
            "forensic-plugin",
            "schema-v2",
            "payload-a",
            "{}"
        )));
        assertThrows(IllegalArgumentException.class, () -> accepted.session().accept(payload(
            "project-a",
            "module-a",
            "forensic-plugin",
            "schema-v1",
            "payload-a",
            "[]"
        )));
    }

    private static RawIngestionPayload payload(byte[] bytes) {
        return new RawIngestionPayload(
            buildIdentity("project-a"),
            moduleIdentity("module-a"),
            pluginIdentity("forensic-plugin"),
            "schema-v1",
            descriptor("payload-a"),
            bytes
        );
    }

    private static RawIngestionPayload payload(String bytes) {
        return payload(bytes.getBytes(StandardCharsets.UTF_8));
    }

    private static RawIngestionPayload payload(
        String projectId,
        String moduleName,
        String pluginName,
        String schemaVersion,
        String payloadId,
        String bytes
    ) {
        return new RawIngestionPayload(
            buildIdentity(projectId),
            moduleIdentity(moduleName),
            pluginIdentity(pluginName),
            schemaVersion,
            descriptor(payloadId),
            bytes.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static BuildIdentity buildIdentity(String projectId) {
        return new BuildIdentity(
            projectId,
            "https://example.invalid/repo.git",
            "main",
            "abcdef",
            "build-1",
            "2026-05-16T00:00:00Z"
        );
    }

    private static ModuleIdentity moduleIdentity(String moduleName) {
        return new ModuleIdentity(moduleName, ":" + moduleName);
    }

    private static PluginIdentity pluginIdentity(String pluginName) {
        return new PluginIdentity(pluginName, "0.1.0");
    }

    private static PayloadDescriptor descriptor(String payloadId) {
        return new PayloadDescriptor(
            payloadId,
            AnalysisPayloadKind.SOURCE_FACTS,
            "application/json",
            Map.of("source", "test")
        );
    }
}
