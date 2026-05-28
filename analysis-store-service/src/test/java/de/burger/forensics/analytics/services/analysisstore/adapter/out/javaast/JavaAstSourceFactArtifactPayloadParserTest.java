package de.burger.forensics.analytics.services.analysisstore.adapter.out.javaast;

import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactCategory;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJobId;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisRunId;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactByteAccess;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactByteCustody;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactReference;
import de.burger.forensics.analytics.services.analysisstore.domain.SourceSnapshotId;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaAstSourceFactArtifactPayloadParserTest {
    private final JavaAstSourceFactArtifactPayloadParser parser = new JavaAstSourceFactArtifactPayloadParser();

    @Test
    void parsesValidPayloadIntoAnalysisStoreOwnedFacts() {
        var result = parser.parse(runId(), jobId(), snapshotId(), artifact(AnalysisCompleteness.COMPLETE), validPayload());

        assertEquals(1, result.facts().size());
        assertEquals(AnalysisCompleteness.COMPLETE, result.completeness());
        assertTrue(result.diagnostics().isEmpty());
        var fact = result.facts().getFirst();
        assertEquals("fact-1", fact.factId());
        assertEquals("java-method", fact.factType());
        assertEquals("src/main/java/a/A.java", fact.location().sourcePath());
        assertEquals("a.A", fact.location().fullyQualifiedClassName());
        assertEquals("run", fact.location().methodName());
        assertEquals("a.A#run()", fact.signature());
        assertEquals("java-ast/snapshot-1-source-facts.json", fact.sourceFactArtifactReference());
        assertEquals(AnalysisCompleteness.COMPLETE, fact.completeness());
    }

    @Test
    void returnsIncompleteDiagnosticsForIdentityMalformedExtraFieldAndUnsafePathFailures() {
        assertFailure(
            validJson().replace("\"analysisRunId\": \"run-1\"", "\"analysisRunId\": \"other-run\""),
            "SOURCE_FACT_ARTIFACT_SCHEMA_INVALID"
        );
        assertFailure("{", "SOURCE_FACT_ARTIFACT_SCHEMA_INVALID");
        assertFailure(
            validJson().replace("\"diagnostics\": []", "\"diagnostics\": [], \"extra\": true"),
            "SOURCE_FACT_ARTIFACT_SCHEMA_INVALID"
        );
        assertFailure(
            validJson().replace("\"sourcePath\": \"src/main/java/a/A.java\"", "\"sourcePath\": \"../A.java\""),
            "SOURCE_FACT_ARTIFACT_FACT_INVALID"
        );
        assertFailure(
            validJson().replace("\"sourcePath\": \"src/main/java/a/A.java\"", "\"sourcePath\": \"FILE:/tmp/A.java\""),
            "SOURCE_FACT_ARTIFACT_FACT_INVALID"
        );
        assertFailure(
            validJson().replace("\"sourcePath\": \"src/main/java/a/A.java\"", "\"sourcePath\": \"src/main/java/a/\\tA.java\""),
            "SOURCE_FACT_ARTIFACT_FACT_INVALID"
        );
        assertFailure(
            validJson().replace("\"sourcePath\": \"src/main/java/a/A.java\"", "\"sourcePath\": \"src/main/java/a/\\u0000A.java\""),
            "SOURCE_FACT_ARTIFACT_FACT_INVALID"
        );
        assertFailure(
            validJson().replace("\"sourcePath\": \"src/main/java/a/A.java\"", "\"sourcePath\": \"src/main/java/a/\\u001BA.java\""),
            "SOURCE_FACT_ARTIFACT_FACT_INVALID"
        );
    }

    @Test
    void failsClosedForUnsupportedFactTypeAndEvidenceKind() {
        assertFailure(
            validJson().replace("\"factType\": \"java-method\"", "\"factType\": \"java-field\""),
            "UNSUPPORTED_STATIC_FACT_TYPE"
        );
        assertFailure(
            validJson().replace("\"evidenceKind\": \"STATIC_SOURCE_FACT\"", "\"evidenceKind\": \"RUNTIME_TRACE\""),
            "UNSUPPORTED_STATIC_EVIDENCE_KIND"
        );
    }

    @Test
    void preservesIncompleteCompletenessWhenJavaAstDiagnosticsAffectCompleteness() {
        var result = parser.parse(
            runId(),
            jobId(),
            snapshotId(),
            artifact(AnalysisCompleteness.INCOMPLETE),
            validJson().replace("\"diagnostics\": []", """
                "diagnostics": [
                    {
                      "code": "SYMBOL_RESOLUTION_NOT_CONFIGURED",
                      "message": "symbol solving is not configured",
                      "severity": "WARNING",
                      "sourceSnapshotId": "snapshot-1",
                      "sourcePath": "",
                      "lineNumber": 0,
                      "columnNumber": 0,
                      "retryable": false,
                      "affectsCompleteness": true
                    }
                  ]""").getBytes(StandardCharsets.UTF_8)
        );

        assertEquals(1, result.facts().size());
        assertEquals(AnalysisCompleteness.INCOMPLETE, result.completeness());
        assertEquals("SYMBOL_RESOLUTION_NOT_CONFIGURED", result.diagnostics().getFirst().code());
    }

    @Test
    void reportsUnsupportedMediaTypeBeforeParsingPayloadBytes() {
        var result = parser.parse(
            runId(),
            jobId(),
            snapshotId(),
            artifact(AnalysisCompleteness.COMPLETE, "application/json", "java-ast-analysis-v1"),
            validPayload()
        );

        assertTrue(result.facts().isEmpty());
        assertEquals(AnalysisCompleteness.INCOMPLETE, result.completeness());
        assertEquals("SOURCE_FACT_ARTIFACT_MEDIA_TYPE_UNSUPPORTED", result.diagnostics().getFirst().code());
    }

    @Test
    void derivesCompletenessFromPayloadDiagnosticsOnly() {
        var result = parser.parse(
            runId(),
            jobId(),
            snapshotId(),
            artifact(AnalysisCompleteness.INCOMPLETE),
            validPayload()
        );

        assertEquals(1, result.facts().size());
        assertEquals(AnalysisCompleteness.COMPLETE, result.completeness());
        assertEquals(AnalysisCompleteness.COMPLETE, result.facts().getFirst().completeness());
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    void failsClosedForSchemaIdentitySummaryDiagnosticAndPrimitiveContractViolations() {
        assertFailure(
            validJson().replace("\"schemaVersion\": \"java-ast-analysis-v1\"", "\"schemaVersion\": \"java-ast-analysis-v2\""),
            "SOURCE_FACT_ARTIFACT_SCHEMA_INVALID"
        );
        assertFailure(
            validJson().replace("\"analysisJobId\": \"job-1\"", "\"analysisJobId\": \"other-job\""),
            "SOURCE_FACT_ARTIFACT_SCHEMA_INVALID"
        );
        assertFailure(
            validJson().replace("\"sourceSnapshotId\": \"snapshot-1\"", "\"sourceSnapshotId\": \"other-snapshot\""),
            "SOURCE_FACT_ARTIFACT_SCHEMA_INVALID"
        );
        assertFailure(
            validJson().replace("\"sourceFactCount\": 1", "\"sourceFactCount\": 2"),
            "SOURCE_FACT_ARTIFACT_SCHEMA_INVALID"
        );
        assertFailure(
            validJson().replace("\"sourceFactCount\": 1", "\"sourceFactCount\": 1.5"),
            "SOURCE_FACT_ARTIFACT_SCHEMA_INVALID"
        );
        assertFailure(
            validJson().replace("\"parseErrorCount\": 0", "\"parseErrorCount\": -1"),
            "SOURCE_FACT_ARTIFACT_SCHEMA_INVALID"
        );
        assertFailure(
            validJson().replace("\"lineNumber\": 4", "\"lineNumber\": 0"),
            "SOURCE_FACT_ARTIFACT_FACT_INVALID"
        );
        assertFailure(
            validJson().replace("\"columnNumber\": 9", "\"columnNumber\": 9.5"),
            "SOURCE_FACT_ARTIFACT_FACT_INVALID"
        );
        assertFailure(
            validJson().replace("\"factId\": \"fact-1\"", "\"factId\": \"\""),
            "SOURCE_FACT_ARTIFACT_FACT_INVALID"
        );
        assertFailure(
            validJson().replace("\"methodName\": \"run\"", "\"methodName\": null"),
            "SOURCE_FACT_ARTIFACT_FACT_INVALID"
        );
        assertFailure(
            withDiagnostic(validJson(), diagnosticJson("WARNING", "other-snapshot", "src/main/java/a/A.java", "4", "9", "false")),
            "SOURCE_FACT_ARTIFACT_SCHEMA_INVALID"
        );
        assertFailure(
            withDiagnostic(validJson(), diagnosticJson("TRACE", "snapshot-1", "src/main/java/a/A.java", "4", "9", "false")),
            "SOURCE_FACT_ARTIFACT_SCHEMA_INVALID"
        );
        assertFailure(
            withDiagnostic(validJson(), diagnosticJson("WARNING", "snapshot-1", "../A.java", "4", "9", "false")),
            "SOURCE_FACT_ARTIFACT_SCHEMA_INVALID"
        );
        assertFailure(
            withDiagnostic(validJson(), diagnosticJson("WARNING", "snapshot-1", "FILE:/tmp/A.java", "4", "9", "false")),
            "SOURCE_FACT_ARTIFACT_SCHEMA_INVALID"
        );
        assertFailure(
            withDiagnostic(validJson(), diagnosticJsonWithRawSourcePath("42")),
            "SOURCE_FACT_ARTIFACT_SCHEMA_INVALID"
        );
        assertFailure(
            withDiagnostic(validJson(), diagnosticJson("WARNING", "snapshot-1", "src/main/java/a/\\tA.java", "4", "9", "false")),
            "SOURCE_FACT_ARTIFACT_SCHEMA_INVALID"
        );
        assertFailure(
            withDiagnostic(validJson(), diagnosticJson("WARNING", "snapshot-1", "src/main/java/a/\\u0000A.java", "4", "9", "false")),
            "SOURCE_FACT_ARTIFACT_SCHEMA_INVALID"
        );
        assertFailure(
            withDiagnostic(validJson(), diagnosticJson("WARNING", "snapshot-1", "src/main/java/a/\\u001BA.java", "4", "9", "false")),
            "SOURCE_FACT_ARTIFACT_SCHEMA_INVALID"
        );
        assertFailure(
            withDiagnostic(validJson(), diagnosticJson("WARNING", "snapshot-1", "src/main/java/a/A.java", "4.5", "9", "false")),
            "SOURCE_FACT_ARTIFACT_SCHEMA_INVALID"
        );
        assertFailure(
            withDiagnostic(validJson(), diagnosticJsonWithMessage("file:/tmp/source.java")),
            "SOURCE_FACT_ARTIFACT_SCHEMA_INVALID"
        );
        assertFailure(
            withDiagnostic(validJson(), diagnosticJsonWithMessage("/tmp/source.java")),
            "SOURCE_FACT_ARTIFACT_SCHEMA_INVALID"
        );
        assertFailure(
            withDiagnostic(validJson(), diagnosticJsonWithMessage("https://example.test/source.java")),
            "SOURCE_FACT_ARTIFACT_SCHEMA_INVALID"
        );
        assertFailure(
            withDiagnostic(validJson(), diagnosticJsonWithMessage("password value leaked")),
            "SOURCE_FACT_ARTIFACT_SCHEMA_INVALID"
        );
        assertFailure(
            withDiagnostic(validJson(), diagnosticJsonWithMessage("line one\\nline two")),
            "SOURCE_FACT_ARTIFACT_SCHEMA_INVALID"
        );
        assertFailure(
            withDiagnostic(validJson(), diagnosticJson("WARNING", "snapshot-1", "src/main/java/a/A.java", "4", "9", "\"false\"")),
            "SOURCE_FACT_ARTIFACT_SCHEMA_INVALID"
        );
        assertFailure(
            withDiagnostic(validJson(), diagnosticJson("WARNING", "snapshot-1", "src/main/java/a/A.java", "4", "9", "\"false\"", "false")),
            "SOURCE_FACT_ARTIFACT_SCHEMA_INVALID"
        );
        assertFailure(
            withDiagnostic(validJson(), diagnosticJson("WARNING", "snapshot-1", "src/main/java/a/A.java", "4", "9", "null", "false")),
            "SOURCE_FACT_ARTIFACT_SCHEMA_INVALID"
        );
    }

    private void assertFailure(String payload, String code) {
        var result = parser.parse(runId(), jobId(), snapshotId(), artifact(AnalysisCompleteness.COMPLETE), payload.getBytes(StandardCharsets.UTF_8));

        assertTrue(result.facts().isEmpty());
        assertEquals(AnalysisCompleteness.INCOMPLETE, result.completeness());
        assertEquals(code, result.diagnostics().getFirst().code());
    }

    private static byte[] validPayload() {
        return validJson().getBytes(StandardCharsets.UTF_8);
    }

    private static String validJson() {
        return """
            {
              "schemaVersion": "java-ast-analysis-v1",
              "analysisRunId": "run-1",
              "analysisJobId": "job-1",
              "sourceSnapshotId": "snapshot-1",
              "summary": {
                "receivedFileCount": 1,
                "parsedFileCount": 1,
                "skippedFileCount": 0,
                "parseErrorCount": 0,
                "sourceFactCount": 1,
                "parser": "JavaParser",
                "parserVersion": "3.27.1"
              },
              "sourceFacts": [
                {
                  "factId": "fact-1",
                  "factType": "java-method",
                  "location": {
                    "sourcePath": "src/main/java/a/A.java",
                    "fullyQualifiedClassName": "a.A",
                    "methodName": "run",
                    "lineNumber": 4,
                    "columnNumber": 9
                  },
                  "signature": "a.A#run()",
                  "summary": "AST method a.A#run()",
                  "evidenceKind": "STATIC_SOURCE_FACT"
                }
              ],
              "diagnostics": []
            }
            """;
    }

    private static AnalysisRunId runId() {
        return new AnalysisRunId("run-1");
    }

    private static AnalysisJobId jobId() {
        return new AnalysisJobId("job-1");
    }

    private static SourceSnapshotId snapshotId() {
        return new SourceSnapshotId("snapshot-1");
    }

    private static AnalysisArtifactReference artifact(AnalysisCompleteness completeness) {
        return artifact(completeness, JavaAstSourceFactArtifactPayloadParser.MEDIA_TYPE, "java-ast-analysis-v1");
    }

    private static AnalysisArtifactReference artifact(AnalysisCompleteness completeness, String mediaType, String schemaVersion) {
        return new AnalysisArtifactReference(
            new ArtifactReference(
                "java-ast/snapshot-1-source-facts.json",
                mediaType,
                "a".repeat(64),
                128
            ),
            AnalysisArtifactCategory.STATIC,
            "java-ast-analysis-service",
            schemaVersion,
            completeness,
            new ArtifactByteAccess(
                "java-ast-analysis-service",
                "java-ast-analysis.v1.JavaAstAnalysisService.GetSourceFactArtifactBytes",
                "java-ast/snapshot-1-source-facts.json",
                ArtifactByteCustody.PRODUCER_RETAINED
            )
        );
    }

    private static String withDiagnostic(String payload, String diagnostic) {
        return payload.replace("\"diagnostics\": []", """
            "diagnostics": [
            %s
              ]""".formatted(diagnostic));
    }

    private static String diagnosticJson(
        String severity,
        String sourceSnapshotId,
        String sourcePath,
        String lineNumber,
        String columnNumber,
        String affectsCompleteness
    ) {
        return diagnosticJson(severity, sourceSnapshotId, sourcePath, lineNumber, columnNumber, "false", affectsCompleteness);
    }

    private static String diagnosticJson(
        String severity,
        String sourceSnapshotId,
        String sourcePath,
        String lineNumber,
        String columnNumber,
        String retryable,
        String affectsCompleteness
    ) {
        return """
                {
                  "code": "SYMBOL_RESOLUTION_NOT_CONFIGURED",
                  "message": "symbol solving is not configured",
                  "severity": "%s",
                  "sourceSnapshotId": "%s",
                  "sourcePath": "%s",
                  "lineNumber": %s,
                  "columnNumber": %s,
                  "retryable": %s,
                  "affectsCompleteness": %s
                }
            """.formatted(severity, sourceSnapshotId, sourcePath, lineNumber, columnNumber, retryable, affectsCompleteness);
    }

    private static String diagnosticJsonWithRawSourcePath(String sourcePath) {
        return """
                {
                  "code": "SYMBOL_RESOLUTION_NOT_CONFIGURED",
                  "message": "symbol solving is not configured",
                  "severity": "WARNING",
                  "sourceSnapshotId": "snapshot-1",
                  "sourcePath": %s,
                  "lineNumber": 4,
                  "columnNumber": 9,
                  "retryable": false,
                  "affectsCompleteness": false
                }
            """.formatted(sourcePath);
    }

    private static String diagnosticJsonWithMessage(String message) {
        return """
                {
                  "code": "SYMBOL_RESOLUTION_NOT_CONFIGURED",
                  "message": "%s",
                  "severity": "WARNING",
                  "sourceSnapshotId": "snapshot-1",
                  "sourcePath": "",
                  "lineNumber": 0,
                  "columnNumber": 0,
                  "retryable": false,
                  "affectsCompleteness": false
                }
            """.formatted(message);
    }
}
