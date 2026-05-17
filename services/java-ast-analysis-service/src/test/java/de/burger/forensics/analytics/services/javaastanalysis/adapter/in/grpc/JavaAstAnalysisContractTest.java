package de.burger.forensics.analytics.services.javaastanalysis.adapter.in.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind;
import de.burger.forensics.analytics.javaastanalysis.v1.AnalyzeSourceSnapshotRequest;
import de.burger.forensics.analytics.javaastanalysis.v1.AnalyzeSourceSnapshotResponse;
import de.burger.forensics.analytics.javaastanalysis.v1.EvidenceKind;
import de.burger.forensics.analytics.javaastanalysis.v1.JavaAstAnalysisServiceGrpc;
import de.burger.forensics.analytics.javaastanalysis.v1.JavaAstDiagnostic;
import de.burger.forensics.analytics.javaastanalysis.v1.JavaSourceFact;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JavaAstAnalysisContractTest {
    @Test
    void generatedContractPreservesProvisionalRpcSurfaceAndKeyFields() {
        assertNotNull(JavaAstAnalysisServiceGrpc.getAnalyzeSourceSnapshotMethod());

        assertEquals(5, AnalyzeSourceSnapshotRequest.getDescriptor().findFieldByName("worker_kind").getNumber());
        assertEquals(12, AnalyzeSourceSnapshotRequest.getDescriptor().findFieldByName("source_files").getNumber());
        assertEquals(6, AnalyzeSourceSnapshotResponse.getDescriptor().findFieldByName("source_fact_artifact").getNumber());
        assertEquals(9, AnalyzeSourceSnapshotResponse.getDescriptor().findFieldByName("safe_attributes").getNumber());
        assertEquals(2, AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS.getNumber());
        assertEquals(6, JavaSourceFact.getDescriptor().findFieldByName("evidence_kind").getNumber());
        assertEquals(1, EvidenceKind.EVIDENCE_KIND_STATIC_SOURCE_FACT.getNumber());
        assertEquals(9, JavaAstDiagnostic.getDescriptor().findFieldByName("affects_completeness").getNumber());
    }
}
