package de.burger.forensics.analytics.services.javaparseranalysis.adapter.in.grpc;

import com.google.gson.JsonParser;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind;
import de.burger.forensics.analytics.javaastanalysis.v1.AnalyzeSourceSnapshotRequest;
import de.burger.forensics.analytics.javaastanalysis.v1.AnalyzeSourceSnapshotResponse;
import de.burger.forensics.analytics.javaastanalysis.v1.EvidenceKind;
import de.burger.forensics.analytics.javaastanalysis.v1.GetSourceFactArtifactBytesRequest;
import de.burger.forensics.analytics.javaastanalysis.v1.GetSourceFactArtifactBytesResponse;
import de.burger.forensics.analytics.javaastanalysis.v1.JavaAstAnalysisServiceGrpc;
import de.burger.forensics.analytics.javaastanalysis.v1.JavaAstDiagnostic;
import de.burger.forensics.analytics.javaastanalysis.v1.JavaSourceFact;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JavaAstAnalysisContractTest {
    @Test
    void generatedContractPreservesProvisionalRpcSurfaceAndKeyFields() {
        assertNotNull(JavaAstAnalysisServiceGrpc.getAnalyzeSourceSnapshotMethod());
        assertNotNull(JavaAstAnalysisServiceGrpc.getGetSourceFactArtifactBytesMethod());

        assertEquals(5, AnalyzeSourceSnapshotRequest.getDescriptor().findFieldByName("worker_kind").getNumber());
        assertEquals(12, AnalyzeSourceSnapshotRequest.getDescriptor().findFieldByName("source_files").getNumber());
        assertEquals(6, AnalyzeSourceSnapshotResponse.getDescriptor().findFieldByName("source_fact_artifact").getNumber());
        assertEquals(9, AnalyzeSourceSnapshotResponse.getDescriptor().findFieldByName("safe_attributes").getNumber());
        assertEquals(6, GetSourceFactArtifactBytesRequest.getDescriptor().findFieldByName("retrieval_reference").getNumber());
        assertEquals(7, GetSourceFactArtifactBytesRequest.getDescriptor().findFieldByName("expected_sha256").getNumber());
        assertEquals(8, GetSourceFactArtifactBytesRequest.getDescriptor().findFieldByName("expected_size_bytes").getNumber());
        assertEquals(10, GetSourceFactArtifactBytesRequest.getDescriptor().findFieldByName("schema_version").getNumber());
        assertEquals(5, GetSourceFactArtifactBytesResponse.getDescriptor().findFieldByName("source_fact_artifact").getNumber());
        assertEquals(6, GetSourceFactArtifactBytesResponse.getDescriptor().findFieldByName("content").getNumber());
        assertEquals(7, GetSourceFactArtifactBytesResponse.getDescriptor().findFieldByName("sha256").getNumber());
        assertEquals(2, AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS.getNumber());
        assertEquals(6, JavaSourceFact.getDescriptor().findFieldByName("evidence_kind").getNumber());
        assertEquals(1, EvidenceKind.EVIDENCE_KIND_STATIC_SOURCE_FACT.getNumber());
        assertEquals(9, JavaAstDiagnostic.getDescriptor().findFieldByName("affects_completeness").getNumber());
    }

    @Test
    void sourceFactArtifactSchemaRequiresExplicitSourceRootContext() throws Exception {
        var schema = JsonParser.parseString(Files.readString(contractSchema()))
            .getAsJsonObject();
        var sourceFact = schema.getAsJsonObject("$defs").getAsJsonObject("sourceFact");

        assertEquals("sourceRoot", sourceFact.getAsJsonArray("required").get(2).getAsString());
        assertEquals(
            "#/$defs/safeRelativePath",
            sourceFact.getAsJsonObject("properties").getAsJsonObject("sourceRoot").get("$ref").getAsString()
        );
        assertEquals(
            "STATIC_SOURCE_FACT",
            sourceFact.getAsJsonObject("properties").getAsJsonObject("evidenceKind").get("const").getAsString()
        );

        var diagnostic = schema.getAsJsonObject("$defs").getAsJsonObject("diagnostic");
        assertEquals("affectsCompleteness", diagnostic.getAsJsonArray("required").get(8).getAsString());
        assertEquals(
            "boolean",
            diagnostic.getAsJsonObject("properties").getAsJsonObject("affectsCompleteness").get("type").getAsString()
        );
    }

    private static Path contractSchema() {
        var fromRoot = Path.of("contracts/grpc/java-ast-source-facts-v1.schema.json");
        return Files.exists(fromRoot) ? fromRoot : Path.of("..", "..", "contracts", "grpc", "java-ast-source-facts-v1.schema.json");
    }
}
