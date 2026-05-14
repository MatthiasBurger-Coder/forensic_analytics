package de.burger.forensics.analytics.ingestion.request;

import de.burger.forensics.analytics.domain.ingestion.AnalysisPayloadKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.UncheckedIOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EngineIngestionRequestReaderTest {
    @TempDir
    Path tempDir;

    private final EngineIngestionRequestReader reader = new EngineIngestionRequestReader();

    @Test
    void parsesPluginEngineRequestJson() {
        var requestFile = tempDir.resolve("engine-request.json");
        var payload = tempDir.resolve("rules.btm").toString().replace('\\', '/');

        var request = reader.fromJson(requestFile, """
            {
              "schemaVersion": "1",
              "buildIdentity": {
                "projectId": "project\\\\id",
                "repositoryUrl": "UNKNOWN",
                "branchName": "feature\\\"x",
                "commitHash": "commit\\nhash",
                "buildId": "build\\rid",
                "scanTimestamp": "1970-01-01T00:00:00Z"
              },
              "moduleIdentity": {
                "moduleName": "module\\tname",
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
                    "zeta": "last",
                    "alpha": "first"
                  }
                }
              ]
            }
            """.formatted(payload));

        assertEquals("1", request.schemaVersion());
        assertEquals("project\\id", request.buildIdentity().projectId());
        assertEquals("feature\"x", request.buildIdentity().branchName());
        assertEquals("commit\nhash", request.buildIdentity().commitHash());
        assertEquals("build\rid", request.buildIdentity().buildId());
        assertEquals("module\tname", request.moduleIdentity().moduleName());
        assertEquals("forensics-tracing", request.pluginIdentity().pluginName());
        assertEquals(AnalysisPayloadKind.RULE_ARTIFACTS, request.payloads().getFirst().descriptor().kind());
        assertEquals("first", request.payloads().getFirst().descriptor().attributes().get("alpha"));
        assertEquals(tempDir.resolve("rules.btm").toAbsolutePath().normalize(), request.payloads().getFirst().file());
    }

    @Test
    void resolvesRelativePayloadFilesAgainstRequestDirectory() {
        var request = reader.fromJson(tempDir.resolve("nested/engine-request.json"), validJson("artifacts/rules.btm", "RULE_ARTIFACTS"));

        assertEquals(
            tempDir.resolve("nested/artifacts/rules.btm").toAbsolutePath().normalize(),
            request.payloads().getFirst().file()
        );
    }

    @Test
    void rejectsMissingOrUnsupportedRequestFields() {
        assertThrows(EngineIngestionRequestException.class, () -> reader.fromJson(tempDir.resolve("request.json"), "{}"));
        assertThrows(
            EngineIngestionRequestException.class,
            () -> reader.fromJson(tempDir.resolve("request.json"), validJson("rules.btm", "UNKNOWN_KIND"))
        );
        assertThrows(
            EngineIngestionRequestException.class,
            () -> reader.fromJson(tempDir.resolve("request.json"), validJsonWithPayloads("[]"))
        );
        assertThrows(
            EngineIngestionRequestException.class,
            () -> reader.fromJson(tempDir.resolve("request.json"), "{\"schemaVersion\":\"1\",")
        );
    }

    @Test
    void rejectsWrongJsonFieldShapes() {
        assertThrows(EngineIngestionRequestException.class, () -> reader.fromJson(
            tempDir.resolve("request.json"),
            validJsonWithOverrides("\"schemaVersion\": [],", buildIdentity(), moduleIdentity(), pluginIdentity(), payloads("rules.btm", "RULE_ARTIFACTS"))
        ));
        assertThrows(EngineIngestionRequestException.class, () -> reader.fromJson(
            tempDir.resolve("request.json"),
            validJsonWithOverrides("\"schemaVersion\": \"1\",", "\"buildIdentity\": [],", moduleIdentity(), pluginIdentity(), payloads("rules.btm", "RULE_ARTIFACTS"))
        ));
        assertThrows(EngineIngestionRequestException.class, () -> reader.fromJson(
            tempDir.resolve("request.json"),
            validJsonWithOverrides("\"schemaVersion\": \"1\",", buildIdentity(), moduleIdentity(), pluginIdentity(), "\"payloads\": {}")
        ));
        assertThrows(EngineIngestionRequestException.class, () -> reader.fromJson(
            tempDir.resolve("request.json"),
            validJsonWithOverrides("\"schemaVersion\": \"1\",", buildIdentity(), moduleIdentity(), pluginIdentity(), """
                "payloads": [
                  {
                    "payloadId": "payload-1",
                    "kind": "RULE_ARTIFACTS",
                    "contentType": "text/plain",
                    "file": "rules.btm",
                    "attributes": {
                      "artifact": []
                    }
                  }
                ]
                """)
        ));
    }

    @Test
    void rejectsInvalidJsonSyntax() {
        assertThrows(EngineIngestionRequestException.class, () -> JsonParser.parseObject("\"text\""));
        assertThrows(EngineIngestionRequestException.class, () -> JsonParser.parseObject("\"text\" \"tail\""));
        assertThrows(EngineIngestionRequestException.class, () -> JsonParser.parseObject("1"));
        assertThrows(EngineIngestionRequestException.class, () -> JsonParser.parseObject("{\"key\":\"value\""));
        assertThrows(EngineIngestionRequestException.class, () -> JsonParser.parseObject("{\"key\":\"unterminated"));
        assertThrows(EngineIngestionRequestException.class, () -> JsonParser.parseObject("{\"key\":\"bad\\b\"}"));
        assertThrows(EngineIngestionRequestException.class, () -> JsonParser.parseObject("{\"key\":\"bad\\"));
    }

    @Test
    void wrapsMissingRequestFileReadFailure() {
        assertThrows(UncheckedIOException.class, () -> reader.read(tempDir.resolve("missing-engine-request.json")));
    }

    @Test
    void rejectsInvalidRequestModels() {
        var parsed = reader.fromJson(tempDir.resolve("request.json"), validJson("rules.btm", "RULE_ARTIFACTS"));

        assertThrows(IllegalArgumentException.class, () -> new EngineIngestionImportResult("session-1", de.burger.forensics.analytics.application.ingestion.result.IngestionStatus.COMPLETED, -1));
        assertThrows(IllegalArgumentException.class, () -> new EngineIngestionRequest("", parsed.buildIdentity(), parsed.moduleIdentity(), parsed.pluginIdentity(), parsed.payloads()));
        assertThrows(NullPointerException.class, () -> new EngineIngestionRequest("1", null, parsed.moduleIdentity(), parsed.pluginIdentity(), parsed.payloads()));
        assertThrows(IllegalArgumentException.class, () -> new EngineIngestionRequest("1", parsed.buildIdentity(), parsed.moduleIdentity(), parsed.pluginIdentity(), java.util.List.of()));
    }

    private static String validJson(String payloadFile, String kind) {
        return validJsonWithPayloads("""
            [
              {
                "payloadId": "byteman-rules",
                "kind": "%s",
                "contentType": "text/x-byteman",
                "file": "%s",
                "attributes": {}
              }
            ]
            """.formatted(kind, payloadFile.replace("\\", "\\\\")));
    }

    private static String validJsonWithPayloads(String payloads) {
        return validJsonWithOverrides("\"schemaVersion\": \"1\",", buildIdentity(), moduleIdentity(), pluginIdentity(), "\"payloads\": " + payloads);
    }

    private static String validJsonWithOverrides(
        String schemaVersion,
        String buildIdentity,
        String moduleIdentity,
        String pluginIdentity,
        String payloads
    ) {
        return """
            {
              %s
              %s
              %s
              %s
              %s
            }
            """.formatted(schemaVersion, buildIdentity, moduleIdentity, pluginIdentity, payloads);
    }

    private static String buildIdentity() {
        return """
            "buildIdentity": {
              "projectId": "project-a",
              "repositoryUrl": "UNKNOWN",
              "branchName": "UNKNOWN",
              "commitHash": "UNKNOWN",
              "buildId": "UNKNOWN",
              "scanTimestamp": "1970-01-01T00:00:00Z"
            },
            """;
    }

    private static String moduleIdentity() {
        return """
            "moduleIdentity": {
              "moduleName": "module-a",
              "modulePath": ":module-a"
            },
            """;
    }

    private static String pluginIdentity() {
        return """
            "pluginIdentity": {
              "pluginName": "forensics-tracing",
              "pluginVersion": "1.2.3"
            },
            """;
    }

    private static String payloads(String payloadFile, String kind) {
        return """
            "payloads": [
              {
                "payloadId": "byteman-rules",
                "kind": "%s",
                "contentType": "text/x-byteman",
                "file": "%s",
                "attributes": {}
              }
            ]
            """.formatted(kind, payloadFile.replace("\\", "\\\\"));
    }
}
