package de.burger.forensics.analytics.services.ingestion.adapter.in.file;

import de.burger.forensics.analytics.services.ingestion.adapter.out.memory.InMemoryIngestionSessionRepository;
import de.burger.forensics.analytics.services.ingestion.application.IngestionApplicationService;
import de.burger.forensics.analytics.services.ingestion.application.port.AcceptedIngestionHandoffPort;
import de.burger.forensics.analytics.services.ingestion.domain.AnalysisPayloadKind;
import de.burger.forensics.analytics.services.ingestion.domain.IngestionStatus;
import de.burger.forensics.analytics.services.ingestion.domain.RawIngestionPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EngineIngestionRequestImporterTest {
    @TempDir
    Path tempDir;

    @Test
    void importsEngineRequestThroughServiceLocalIngestionApplication() throws Exception {
        var rules = Files.writeString(tempDir.resolve("rules.btm"), "RULE test\n", StandardCharsets.UTF_8);
        var manifest = Files.writeString(tempDir.resolve("manifest.json"), "{\"analysis\":true}", StandardCharsets.UTF_8);
        var checksums = Files.writeString(tempDir.resolve("checksums.sha256"), "abc123  rules.btm\n", StandardCharsets.UTF_8);
        var requestFile = Files.writeString(tempDir.resolve("engine-request.json"), requestJson(rules, manifest, checksums), StandardCharsets.UTF_8);
        var handoff = new RecordingAcceptedIngestionHandoffPort();
        var importer = new EngineIngestionRequestImporter(new IngestionApplicationService(
            new InMemoryIngestionSessionRepository(),
            handoff
        ));

        var result = importer.importRequest(requestFile);

        assertEquals(IngestionStatus.COMPLETED, result.completionStatus());
        assertFalse(result.sessionId().isBlank());
        assertEquals(3, result.uploadedPayloads());
        assertEquals(List.of(result.sessionId(), result.sessionId(), result.sessionId()), handoff.sessionIds);
        assertEquals(List.of("byteman-rules", "analysis-manifest", "analysis-checksums"), handoff.payloads.stream()
            .map(RawIngestionPayload::descriptor)
            .map(descriptor -> descriptor.payloadId())
            .toList());
        assertEquals(AnalysisPayloadKind.RULE_ARTIFACTS, handoff.payloads.getFirst().descriptor().kind());
        assertEquals(AnalysisPayloadKind.DIAGNOSTIC_REPORT, handoff.payloads.get(1).descriptor().kind());
        assertEquals(AnalysisPayloadKind.DIAGNOSTIC_REPORT, handoff.payloads.get(2).descriptor().kind());
        assertEquals("btm-rules", handoff.payloads.getFirst().descriptor().attributes().get("artifact"));
        assertEquals("analysis-manifest", handoff.payloads.get(1).descriptor().attributes().get("artifact"));
        assertEquals("analysis-checksums", handoff.payloads.get(2).descriptor().attributes().get("artifact"));
        assertArrayEquals("RULE test\n".getBytes(StandardCharsets.UTF_8), handoff.payloads.getFirst().payload());
        assertArrayEquals("{\"analysis\":true}".getBytes(StandardCharsets.UTF_8), handoff.payloads.get(1).payload());
        assertArrayEquals("abc123  rules.btm\n".getBytes(StandardCharsets.UTF_8), handoff.payloads.get(2).payload());
    }

    @Test
    void rejectsMissingPayloadFilesBeforeStartingSession() throws Exception {
        var manifest = Files.writeString(tempDir.resolve("manifest.json"), "{}", StandardCharsets.UTF_8);
        var checksums = Files.writeString(tempDir.resolve("checksums.sha256"), "", StandardCharsets.UTF_8);
        var requestFile = Files.writeString(
            tempDir.resolve("engine-request.json"),
            requestJson(tempDir.resolve("missing.btm"), manifest, checksums),
            StandardCharsets.UTF_8
        );
        var handoff = new RecordingAcceptedIngestionHandoffPort();
        var importer = new EngineIngestionRequestImporter(new IngestionApplicationService(
            new InMemoryIngestionSessionRepository(),
            handoff
        ));

        assertThrows(EngineIngestionRequestException.class, () -> importer.importRequest(requestFile));
        assertEquals(0, handoff.payloads.size());
    }

    @Test
    void dependenciesAreRequired() {
        var ingestionService = new IngestionApplicationService(
            new InMemoryIngestionSessionRepository(),
            new RecordingAcceptedIngestionHandoffPort()
        );
        var reader = new EngineIngestionRequestReader();

        assertThrows(NullPointerException.class, () -> new EngineIngestionRequestImporter(null));
        assertThrows(NullPointerException.class, () -> new EngineIngestionRequestImporter(null, reader));
        assertThrows(NullPointerException.class, () -> new EngineIngestionRequestImporter(ingestionService, null));
    }

    private static String requestJson(Path rules, Path manifest, Path checksums) {
        return """
            {
              "schemaVersion": "1",
              "buildIdentity": {
                "projectId": "project-a",
                "repositoryUrl": "UNKNOWN",
                "branchName": "UNKNOWN",
                "commitHash": "UNKNOWN",
                "buildId": "UNKNOWN",
                "scanTimestamp": "1970-01-01T00:00:00Z"
              },
              "moduleIdentity": {
                "moduleName": "module-a",
                "modulePath": ":module-a"
              },
              "pluginIdentity": {
                "pluginName": "forensics-tracing",
                "pluginVersion": "1.2.3"
              },
              "payloads": [
                {
                  "payloadId": "byteman-rules",
                  "kind": "RULE_ARTIFACTS",
                  "contentType": "text/x-byteman",
                  "file": "%s",
                  "attributes": {
                    "artifact": "btm-rules"
                  }
                },
                {
                  "payloadId": "analysis-manifest",
                  "kind": "DIAGNOSTIC_REPORT",
                  "contentType": "application/json",
                  "file": "%s",
                  "attributes": {
                    "artifact": "analysis-manifest"
                  }
                },
                {
                  "payloadId": "analysis-checksums",
                  "kind": "DIAGNOSTIC_REPORT",
                  "contentType": "text/plain",
                  "file": "%s",
                  "attributes": {
                    "artifact": "analysis-checksums"
                  }
                }
              ]
            }
            """.formatted(jsonPath(rules), jsonPath(manifest), jsonPath(checksums));
    }

    private static String jsonPath(Path path) {
        return path.toAbsolutePath().normalize().toString().replace('\\', '/');
    }

    private static final class RecordingAcceptedIngestionHandoffPort implements AcceptedIngestionHandoffPort {
        private final List<String> sessionIds = new ArrayList<>();
        private final List<RawIngestionPayload> payloads = new ArrayList<>();

        @Override
        public void accepted(String sessionId, RawIngestionPayload payload) {
            sessionIds.add(sessionId);
            payloads.add(payload);
        }
    }
}
