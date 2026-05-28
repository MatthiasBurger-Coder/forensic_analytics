package de.burger.forensics.analytics.services.analysisorchestrator.adapter.in.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactReference;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisJob;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobServiceGrpc;
import de.burger.forensics.analytics.analysisjob.v1.ArtifactByteAccess;
import de.burger.forensics.analytics.analysisjob.v1.FailAnalysisJobRequest;
import de.burger.forensics.analytics.analysisjob.v1.LeaseAnalysisJobRequest;
import de.burger.forensics.analytics.analysisjob.v1.RegisterAnalysisArtifactsRequest;
import de.burger.forensics.analytics.analysisjob.v1.RepositoryToBtmDiagnostic;
import de.burger.forensics.analytics.analysisjob.v1.RepositoryToBtmOrchestrationStatus;
import de.burger.forensics.analytics.analysisjob.v1.StartRepositoryToBtmRequest;
import de.burger.forensics.analytics.analysisjob.v1.SubmitAnalysisJobRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AnalysisJobContractTest {
    @Test
    void generatesAnalysisJobServiceContractLocally() {
        assertNotNull(AnalysisJobServiceGrpc.getSubmitAnalysisJobMethod());
        assertNotNull(AnalysisJobServiceGrpc.getGetAnalysisJobMethod());
        assertNotNull(AnalysisJobServiceGrpc.getListAnalysisJobsMethod());
        assertNotNull(AnalysisJobServiceGrpc.getLeaseAnalysisJobMethod());
        assertNotNull(AnalysisJobServiceGrpc.getReportAnalysisJobProgressMethod());
        assertNotNull(AnalysisJobServiceGrpc.getCompleteAnalysisJobMethod());
        assertNotNull(AnalysisJobServiceGrpc.getFailAnalysisJobMethod());
        assertNotNull(AnalysisJobServiceGrpc.getRegisterAnalysisArtifactsMethod());
        assertNotNull(AnalysisJobServiceGrpc.getPlanInstrumentationTargetsMethod());
        assertNotNull(AnalysisJobServiceGrpc.getStartRepositoryToBtmMethod());
        assertNotNull(AnalysisJobServiceGrpc.getGetRepositoryToBtmStatusMethod());
    }

    @Test
    void preservesJobLifecycleAndReferenceFieldNumbers() {
        assertEquals(6, SubmitAnalysisJobRequest.getDescriptor().findFieldByName("job_id").getNumber());
        assertEquals(7, SubmitAnalysisJobRequest.getDescriptor().findFieldByName("worker_kind").getNumber());
        assertEquals(9, SubmitAnalysisJobRequest.getDescriptor().findFieldByName("input_artifacts").getNumber());
        assertEquals(6, LeaseAnalysisJobRequest.getDescriptor().findFieldByName("lease_seconds").getNumber());
        assertEquals(10, FailAnalysisJobRequest.getDescriptor().findFieldByName("retryable").getNumber());
        assertEquals(6, RegisterAnalysisArtifactsRequest.getDescriptor().findFieldByName("artifacts").getNumber());
        assertEquals(8, AnalysisJob.getDescriptor().findFieldByName("state").getNumber());
        assertEquals(12, AnalysisJob.getDescriptor().findFieldByName("lease_expires_at").getNumber());
        assertEquals(5, AnalysisArtifactReference.getDescriptor().findFieldByName("completeness").getNumber());
        assertEquals(4, ArtifactByteAccess.getDescriptor().findFieldByName("byte_custody").getNumber());
        assertEquals(5, StartRepositoryToBtmRequest.getDescriptor().findFieldByName("analysis_run_id").getNumber());
        assertEquals(6, StartRepositoryToBtmRequest.getDescriptor().findFieldByName("repository").getNumber());
        assertEquals(7, StartRepositoryToBtmRequest.getDescriptor().findFieldByName("revision").getNumber());
        assertEquals(10, StartRepositoryToBtmRequest.getDescriptor().findFieldByName("requested_outputs").getNumber());
        assertEquals(6, RepositoryToBtmOrchestrationStatus.getDescriptor().findFieldByName("state").getNumber());
        assertEquals(7, RepositoryToBtmOrchestrationStatus.getDescriptor().findFieldByName("btm_delivery_readiness").getNumber());
        assertEquals(8, RepositoryToBtmOrchestrationStatus.getDescriptor().findFieldByName("joern_skipped").getNumber());
        assertEquals(10, RepositoryToBtmOrchestrationStatus.getDescriptor().findFieldByName("accepted_generated_artifacts").getNumber());
        assertEquals(5, RepositoryToBtmDiagnostic.getDescriptor().findFieldByName("affects_completeness").getNumber());
    }
}
