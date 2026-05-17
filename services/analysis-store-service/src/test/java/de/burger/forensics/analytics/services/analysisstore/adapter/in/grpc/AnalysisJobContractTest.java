package de.burger.forensics.analytics.services.analysisstore.adapter.in.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactReference;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisJob;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobServiceGrpc;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobState;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AnalysisJobContractTest {
    @Test
    void generatedContractPreservesPlannedRpcSurfaceAndKeyEnumValues() {
        assertNotNull(AnalysisJobServiceGrpc.getSubmitAnalysisJobMethod());
        assertNotNull(AnalysisJobServiceGrpc.getGetAnalysisJobMethod());
        assertNotNull(AnalysisJobServiceGrpc.getListAnalysisJobsMethod());
        assertNotNull(AnalysisJobServiceGrpc.getLeaseAnalysisJobMethod());
        assertNotNull(AnalysisJobServiceGrpc.getReportAnalysisJobProgressMethod());
        assertNotNull(AnalysisJobServiceGrpc.getCompleteAnalysisJobMethod());
        assertNotNull(AnalysisJobServiceGrpc.getFailAnalysisJobMethod());
        assertNotNull(AnalysisJobServiceGrpc.getRegisterAnalysisArtifactsMethod());

        assertEquals(2, AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS.getNumber());
        assertEquals(3, AnalysisJobState.ANALYSIS_JOB_STATE_RUNNING.getNumber());
        assertEquals(3, AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNKNOWN.getNumber());
        assertEquals(4, AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_GENERATED.getNumber());
        assertEquals(5, AnalysisArtifactReference.getDescriptor().findFieldByName("completeness").getNumber());
        assertEquals(16, AnalysisJob.getDescriptor().findFieldByName("schema_version").getNumber());
        assertEquals(17, AnalysisJob.getDescriptor().findFieldByName("correlation_id").getNumber());
        assertEquals(18, AnalysisJob.getDescriptor().findFieldByName("attributes").getNumber());
        assertEquals(19, AnalysisJob.getDescriptor().findFieldByName("percent_complete").getNumber());
    }
}
