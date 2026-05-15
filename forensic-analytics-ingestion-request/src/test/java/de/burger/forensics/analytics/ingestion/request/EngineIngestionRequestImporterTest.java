package de.burger.forensics.analytics.ingestion.request;

import de.burger.forensics.analytics.application.ingestion.ForensicIngestionUseCase;
import de.burger.forensics.analytics.application.ingestion.command.AbortAnalysisSessionCommand;
import de.burger.forensics.analytics.application.ingestion.command.CompleteAnalysisSessionCommand;
import de.burger.forensics.analytics.application.ingestion.command.StartAnalysisSessionCommand;
import de.burger.forensics.analytics.application.ingestion.command.UploadAnalysisDataCommand;
import de.burger.forensics.analytics.application.ingestion.result.AbortAnalysisSessionResult;
import de.burger.forensics.analytics.application.ingestion.result.CompleteAnalysisSessionResult;
import de.burger.forensics.analytics.application.ingestion.result.IngestionStatus;
import de.burger.forensics.analytics.application.ingestion.result.StartAnalysisSessionResult;
import de.burger.forensics.analytics.application.ingestion.result.UploadAnalysisDataResult;
import de.burger.forensics.analytics.domain.ingestion.AnalysisPayloadKind;
import de.burger.forensics.analytics.observability.OperationLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EngineIngestionRequestImporterTest {
    @TempDir
    Path tempDir;

    @Test
    void importsEngineRequestThroughExistingIngestionUseCase() throws Exception {
        var rules = Files.writeString(tempDir.resolve("rules.btm"), "RULE test\n", StandardCharsets.UTF_8);
        var manifest = Files.writeString(tempDir.resolve("manifest.json"), "{\"analysis\":true}", StandardCharsets.UTF_8);
        var checksums = Files.writeString(tempDir.resolve("checksums.sha256"), "abc123  rules.btm\n", StandardCharsets.UTF_8);
        var requestFile = Files.writeString(tempDir.resolve("engine-request.json"), requestJson(rules, manifest, checksums), StandardCharsets.UTF_8);
        var useCase = new RecordingIngestionUseCase();
        var importer = new EngineIngestionRequestImporter(useCase);

        var result = importer.importRequest(requestFile);

        assertEquals("session-1", result.sessionId());
        assertEquals(IngestionStatus.COMPLETED, result.completionStatus());
        assertEquals(3, result.uploadedPayloads());
        assertEquals("project-a", useCase.startCommand.buildIdentity().projectId());
        assertEquals("forensics-tracing", useCase.startCommand.pluginIdentity().pluginName());
        assertEquals("session-1", useCase.completedSessionId);
        assertEquals(List.of("byteman-rules", "analysis-manifest", "analysis-checksums"), useCase.uploadCommands.stream()
            .map(command -> command.payloadDescriptor().payloadId())
            .toList());
        assertEquals(AnalysisPayloadKind.RULE_ARTIFACTS, useCase.uploadCommands.getFirst().payloadDescriptor().kind());
        assertEquals(AnalysisPayloadKind.DIAGNOSTIC_REPORT, useCase.uploadCommands.get(1).payloadDescriptor().kind());
        assertEquals(AnalysisPayloadKind.DIAGNOSTIC_REPORT, useCase.uploadCommands.get(2).payloadDescriptor().kind());
        assertEquals("btm-rules", useCase.uploadCommands.getFirst().payloadDescriptor().attributes().get("artifact"));
        assertEquals("analysis-manifest", useCase.uploadCommands.get(1).payloadDescriptor().attributes().get("artifact"));
        assertEquals("analysis-checksums", useCase.uploadCommands.get(2).payloadDescriptor().attributes().get("artifact"));
        assertArrayEquals("RULE test\n".getBytes(StandardCharsets.UTF_8), useCase.uploadCommands.getFirst().payload());
        assertArrayEquals("{\"analysis\":true}".getBytes(StandardCharsets.UTF_8), useCase.uploadCommands.get(1).payload());
        assertArrayEquals("abc123  rules.btm\n".getBytes(StandardCharsets.UTF_8), useCase.uploadCommands.get(2).payload());
    }

    @Test
    void logsEngineRequestImportLifecycle() throws Exception {
        var rules = Files.writeString(tempDir.resolve("rules.btm"), "RULE test\n", StandardCharsets.UTF_8);
        var manifest = Files.writeString(tempDir.resolve("manifest.json"), "{\"analysis\":true}", StandardCharsets.UTF_8);
        var checksums = Files.writeString(tempDir.resolve("checksums.sha256"), "abc123  rules.btm\n", StandardCharsets.UTF_8);
        var requestFile = Files.writeString(tempDir.resolve("engine-request.json"), requestJson(rules, manifest, checksums), StandardCharsets.UTF_8);
        var logger = new RecordingOperationLogger();
        var importer = new EngineIngestionRequestImporter(
            new RecordingIngestionUseCase(),
            new EngineIngestionRequestReader(OperationLogger.noop()),
            logger
        );

        importer.importRequest(requestFile);

        assertEquals(
            List.of("started:ingestion-request.import", "succeeded:ingestion-request.import"),
            logger.events()
        );
    }

    @Test
    void rejectsMissingPayloadFilesBeforeCompletingSession() throws Exception {
        var manifest = Files.writeString(tempDir.resolve("manifest.json"), "{}", StandardCharsets.UTF_8);
        var checksums = Files.writeString(tempDir.resolve("checksums.sha256"), "", StandardCharsets.UTF_8);
        var requestFile = Files.writeString(
            tempDir.resolve("engine-request.json"),
            requestJson(tempDir.resolve("missing.btm"), manifest, checksums),
            StandardCharsets.UTF_8
        );
        var useCase = new RecordingIngestionUseCase();

        assertThrows(EngineIngestionRequestException.class, () -> new EngineIngestionRequestImporter(useCase).importRequest(requestFile));
        assertEquals(null, useCase.startCommand);
        assertEquals(0, useCase.uploadCommands.size());
        assertEquals(null, useCase.completedSessionId);
    }

    @Test
    void dependenciesAreRequired() {
        var useCase = new RecordingIngestionUseCase();
        var reader = new EngineIngestionRequestReader();

        assertThrows(NullPointerException.class, () -> new EngineIngestionRequestImporter(null));
        assertThrows(NullPointerException.class, () -> new EngineIngestionRequestImporter(null, reader));
        assertThrows(NullPointerException.class, () -> new EngineIngestionRequestImporter(useCase, null));
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

    private static final class RecordingIngestionUseCase implements ForensicIngestionUseCase {
        private final List<UploadAnalysisDataCommand> uploadCommands = new ArrayList<>();
        private StartAnalysisSessionCommand startCommand;
        private String completedSessionId;

        @Override
        public StartAnalysisSessionResult start(StartAnalysisSessionCommand command) {
            startCommand = command;
            return new StartAnalysisSessionResult("session-1", IngestionStatus.ACCEPTED, "accepted");
        }

        @Override
        public UploadAnalysisDataResult upload(UploadAnalysisDataCommand command) {
            uploadCommands.add(command);
            return new UploadAnalysisDataResult(command.sessionId(), IngestionStatus.ACCEPTED, uploadCommands.size(), "accepted");
        }

        @Override
        public CompleteAnalysisSessionResult complete(CompleteAnalysisSessionCommand command) {
            completedSessionId = command.sessionId();
            return new CompleteAnalysisSessionResult(command.sessionId(), IngestionStatus.COMPLETED, "completed");
        }

        @Override
        public AbortAnalysisSessionResult abort(AbortAnalysisSessionCommand command) {
            return new AbortAnalysisSessionResult(command.sessionId(), IngestionStatus.ABORTED, command.reason());
        }

    }

    private static final class RecordingOperationLogger implements OperationLogger {
        private final List<String> events = new ArrayList<>();

        @Override
        public void started(String operation) {
            events.add("started:" + operation);
        }

        @Override
        public void succeeded(String operation, long durationMillis) {
            events.add("succeeded:" + operation);
        }

        @Override
        public void failed(String operation, long durationMillis, Throwable error) {
            events.add("failed:" + operation);
        }

        private List<String> events() {
            return List.copyOf(events);
        }
    }
}
