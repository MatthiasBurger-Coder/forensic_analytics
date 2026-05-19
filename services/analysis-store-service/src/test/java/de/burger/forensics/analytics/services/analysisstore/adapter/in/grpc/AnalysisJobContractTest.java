package de.burger.forensics.analytics.services.analysisstore.adapter.in.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactReference;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness;
import de.burger.forensics.analytics.analysisjob.v1.ArtifactByteAccess;
import de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisJob;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobServiceGrpc;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobState;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind;
import de.burger.forensics.analytics.analysisjob.v1.InstrumentationProbeKind;
import de.burger.forensics.analytics.analysisjob.v1.InstrumentationTarget;
import de.burger.forensics.analytics.analysisjob.v1.InstrumentationTargetSelection;
import de.burger.forensics.analytics.analysisjob.v1.PlanInstrumentationTargetsRequest;
import de.burger.forensics.analytics.analysisjob.v1.PlanInstrumentationTargetsResponse;
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
        assertNotNull(AnalysisJobServiceGrpc.getPlanInstrumentationTargetsMethod());

        assertEquals(2, AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS.getNumber());
        assertEquals(3, AnalysisJobState.ANALYSIS_JOB_STATE_RUNNING.getNumber());
        assertEquals(3, AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNKNOWN.getNumber());
        assertEquals(4, AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_GENERATED.getNumber());
        assertEquals(5, AnalysisArtifactReference.getDescriptor().findFieldByName("completeness").getNumber());
        assertEquals(6, AnalysisArtifactReference.getDescriptor().findFieldByName("byte_access").getNumber());
        assertEquals(1, ArtifactByteAccess.getDescriptor().findFieldByName("owner_service").getNumber());
        assertEquals(2, ArtifactByteAccess.getDescriptor().findFieldByName("retrieval_contract").getNumber());
        assertEquals(3, ArtifactByteAccess.getDescriptor().findFieldByName("retrieval_reference").getNumber());
        assertEquals(4, ArtifactByteAccess.getDescriptor().findFieldByName("byte_custody").getNumber());
        assertEquals(1, ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_PRODUCER_RETAINED.getNumber());
        assertEquals(2, ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_SCOPED_OBJECT_ACCESS.getNumber());
        assertEquals(3, ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_EXPLICIT_HANDOFF.getNumber());
        assertEquals(16, AnalysisJob.getDescriptor().findFieldByName("schema_version").getNumber());
        assertEquals(17, AnalysisJob.getDescriptor().findFieldByName("correlation_id").getNumber());
        assertEquals(18, AnalysisJob.getDescriptor().findFieldByName("attributes").getNumber());
        assertEquals(19, AnalysisJob.getDescriptor().findFieldByName("percent_complete").getNumber());
        assertEquals(9, PlanInstrumentationTargetsRequest.getDescriptor().findFieldByName("policy").getNumber());
        assertEquals(10, PlanInstrumentationTargetsRequest.getDescriptor().findFieldByName("static_facts").getNumber());
        assertEquals(12, PlanInstrumentationTargetsRequest.getDescriptor().findFieldByName("semantic_artifacts").getNumber());
        assertEquals(6, PlanInstrumentationTargetsResponse.getDescriptor().findFieldByName("target_selection").getNumber());
        assertEquals(7, PlanInstrumentationTargetsResponse.getDescriptor().findFieldByName("targets").getNumber());
        assertEquals(1, InstrumentationTargetSelection.getDescriptor().findFieldByName("selection_id").getNumber());
        assertEquals(4, InstrumentationTargetSelection.getDescriptor().findFieldByName("selection_fingerprint").getNumber());
        assertEquals(9, InstrumentationTarget.getDescriptor().findFieldByName("probe_kind").getNumber());
        assertEquals(10, InstrumentationTarget.getDescriptor().findFieldByName("source_fact_artifact_reference").getNumber());
        assertEquals(11, InstrumentationTarget.getDescriptor().findFieldByName("semantic_artifact_reference").getNumber());
        assertEquals(12, InstrumentationTarget.getDescriptor().findFieldByName("order_index").getNumber());
        assertEquals(13, InstrumentationTarget.getDescriptor().findFieldByName("completeness").getNumber());
        assertEquals(14, InstrumentationTarget.getDescriptor().findFieldByName("sensitivity").getNumber());
        assertEquals(1, InstrumentationProbeKind.INSTRUMENTATION_PROBE_KIND_METHOD_ENTRY.getNumber());
        assertEquals(2, InstrumentationProbeKind.INSTRUMENTATION_PROBE_KIND_METHOD_EXIT.getNumber());
        assertEquals(3, InstrumentationProbeKind.INSTRUMENTATION_PROBE_KIND_THROW.getNumber());
    }
}
