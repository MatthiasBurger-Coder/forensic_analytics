package de.burger.forensics.analytics.services.analysisstore.adapter.in.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactReference;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobServiceGrpc;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobState;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisRunId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind;
import de.burger.forensics.analytics.analysisjob.v1.ArtifactReference;
import de.burger.forensics.analytics.analysisjob.v1.CompleteAnalysisJobRequest;
import de.burger.forensics.analytics.analysisjob.v1.FailAnalysisJobRequest;
import de.burger.forensics.analytics.analysisjob.v1.GetAnalysisJobRequest;
import de.burger.forensics.analytics.analysisjob.v1.LeaseAnalysisJobRequest;
import de.burger.forensics.analytics.analysisjob.v1.ListAnalysisJobsRequest;
import de.burger.forensics.analytics.analysisjob.v1.RegisterAnalysisArtifactsRequest;
import de.burger.forensics.analytics.analysisjob.v1.ReportAnalysisJobProgressRequest;
import de.burger.forensics.analytics.analysisjob.v1.SourceSnapshotId;
import de.burger.forensics.analytics.analysisjob.v1.SubmitAnalysisJobRequest;
import de.burger.forensics.analytics.services.analysisstore.adapter.out.memory.InMemoryAnalysisJobRepository;
import de.burger.forensics.analytics.services.analysisstore.application.AnalysisJobApplicationService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnalysisJobGrpcEndpointTest {
    private Server server;
    private ManagedChannel channel;
    private AnalysisJobServiceGrpc.AnalysisJobServiceBlockingStub stub;

    @BeforeEach
    void startServer() throws IOException {
        var serverName = InProcessServerBuilder.generateName();
        var applicationService = new AnalysisJobApplicationService(
            new InMemoryAnalysisJobRepository(),
            Clock.fixed(Instant.parse("2026-05-16T10:15:30Z"), ZoneOffset.UTC)
        );
        server = InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(new AnalysisJobGrpcEndpoint(applicationService))
            .build()
            .start();
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        stub = AnalysisJobServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void stopServer() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void supportsJobLifecycleAndArtifactRegistration() {
        var submitted = stub.submitAnalysisJob(submitRequest("submit-1", "job-1", AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS));
        var leased = stub.leaseAnalysisJob(LeaseAnalysisJobRequest.newBuilder()
            .setRequestId("request-lease")
            .setIdempotencyKey("lease-1")
            .setCorrelationId("correlation-1")
            .setWorkerId("worker-a")
            .setWorkerKind(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS)
            .setLeaseSeconds(60)
            .setMaxJobs(1)
            .build());
        var progressed = stub.reportAnalysisJobProgress(ReportAnalysisJobProgressRequest.newBuilder()
            .setRequestId("request-progress")
            .setIdempotencyKey("progress-1")
            .setCorrelationId("correlation-1")
            .setJobId(jobId("job-1"))
            .setAttempt(1)
            .setWorkerId("worker-a")
            .setPercentComplete(40)
            .addDiagnostics("scanner running")
            .build());
        var completed = stub.completeAnalysisJob(CompleteAnalysisJobRequest.newBuilder()
            .setRequestId("request-complete")
            .setIdempotencyKey("complete-1")
            .setCorrelationId("correlation-1")
            .setJobId(jobId("job-1"))
            .setAttempt(1)
            .setWorkerId("worker-a")
            .addOutputArtifacts(artifact("output.json", "output-sha", AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC))
            .setOutputCompleteness(AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE)
            .addDiagnostics("done")
            .build());
        var registered = stub.registerAnalysisArtifacts(RegisterAnalysisArtifactsRequest.newBuilder()
            .setRequestId("request-register")
            .setIdempotencyKey("register-1")
            .setCorrelationId("correlation-1")
            .setAnalysisRunId(runId())
            .setJobId(jobId("job-1"))
            .addArtifacts(artifact("report.json", "report-sha", AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_GENERATED))
            .build());

        assertEquals("ACCEPTED", submitted.getStatus().getCode());
        assertEquals(AnalysisJobState.ANALYSIS_JOB_STATE_DISPATCHABLE, submitted.getJob().getState());
        assertEquals("schema-v1", submitted.getJob().getSchemaVersion());
        assertEquals("correlation-1", submitted.getJob().getCorrelationId());
        assertEquals("demo", submitted.getJob().getAttributesMap().get("repository"));
        assertEquals(AnalysisJobState.ANALYSIS_JOB_STATE_RUNNING, leased.getJobs(0).getState());
        assertEquals("worker-a", leased.getJobs(0).getLeaseOwner());
        assertEquals("scanner running", progressed.getDiagnostics(0));
        assertEquals(40, progressed.getPercentComplete());
        assertEquals(AnalysisJobState.ANALYSIS_JOB_STATE_COMPLETED, completed.getState());
        assertEquals(AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE, completed.getCompleteness());
        assertEquals(100, completed.getPercentComplete());
        assertEquals(2, registered.getArtifactsCount());
    }

    @Test
    void listsGetsFailsAndMapsErrorStatuses() {
        stub.submitAnalysisJob(submitRequest("submit-1", "job-1", AnalysisWorkerKind.ANALYSIS_WORKER_KIND_JOERN_ANALYSIS));
        stub.leaseAnalysisJob(LeaseAnalysisJobRequest.newBuilder()
            .setRequestId("request-lease")
            .setIdempotencyKey("lease-1")
            .setCorrelationId("correlation-1")
            .setWorkerId("worker-a")
            .setWorkerKind(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_JOERN_ANALYSIS)
            .setLeaseSeconds(60)
            .setMaxJobs(1)
            .build());
        var failed = stub.failAnalysisJob(FailAnalysisJobRequest.newBuilder()
            .setRequestId("request-fail")
            .setIdempotencyKey("fail-1")
            .setCorrelationId("correlation-1")
            .setJobId(jobId("job-1"))
            .setAttempt(1)
            .setWorkerId("worker-a")
            .setReason("joern unavailable")
            .addDiagnostics("container missing")
            .setCompleteness(AnalysisCompleteness.ANALYSIS_COMPLETENESS_INCOMPLETE)
            .setRetryable(true)
            .build());
        var listed = stub.listAnalysisJobs(ListAnalysisJobsRequest.newBuilder()
            .setRequestId("request-list")
            .setCorrelationId("correlation-1")
            .setAnalysisRunId(runId())
            .setState(AnalysisJobState.ANALYSIS_JOB_STATE_RETRYABLE)
            .build());
        var loaded = stub.getAnalysisJob(GetAnalysisJobRequest.newBuilder()
            .setRequestId("request-get")
            .setCorrelationId("correlation-1")
            .setJobId(jobId("job-1"))
            .build());
        var missing = assertThrows(
            StatusRuntimeException.class,
            () -> stub.getAnalysisJob(GetAnalysisJobRequest.newBuilder()
                .setRequestId("request-get")
                .setCorrelationId("correlation-1")
                .setJobId(jobId("missing"))
                .build())
        );
        var invalid = assertThrows(
            StatusRuntimeException.class,
            () -> stub.submitAnalysisJob(SubmitAnalysisJobRequest.getDefaultInstance())
        );
        var failedPrecondition = assertThrows(
            StatusRuntimeException.class,
            () -> stub.reportAnalysisJobProgress(ReportAnalysisJobProgressRequest.newBuilder()
                .setRequestId("request-progress-invalid")
                .setIdempotencyKey("progress-invalid")
                .setCorrelationId("correlation-1")
                .setJobId(jobId("job-1"))
                .setAttempt(1)
                .setWorkerId("worker-a")
                .setPercentComplete(50)
                .build())
        );
        var conflict = assertThrows(
            StatusRuntimeException.class,
            () -> stub.submitAnalysisJob(submitRequest("submit-1", "job-2", AnalysisWorkerKind.ANALYSIS_WORKER_KIND_JOERN_ANALYSIS))
        );

        assertEquals(AnalysisJobState.ANALYSIS_JOB_STATE_RETRYABLE, failed.getState());
        assertEquals(1, failed.getFailuresCount());
        assertEquals(1, listed.getJobsCount());
        assertEquals("job-1", loaded.getJobId().getValue());
        assertEquals(Status.Code.NOT_FOUND, missing.getStatus().getCode());
        assertEquals(Status.Code.INVALID_ARGUMENT, invalid.getStatus().getCode());
        assertEquals(Status.Code.FAILED_PRECONDITION, failedPrecondition.getStatus().getCode());
        assertEquals(Status.Code.ALREADY_EXISTS, conflict.getStatus().getCode());
    }

    @Test
    void paginatesListResultsAndRejectsInvalidPageTokens() {
        stub.submitAnalysisJob(submitRequest("submit-1", "job-1", AnalysisWorkerKind.ANALYSIS_WORKER_KIND_REPOSITORY_ANALYSIS));
        stub.submitAnalysisJob(submitRequest("submit-2", "job-2", AnalysisWorkerKind.ANALYSIS_WORKER_KIND_REPOSITORY_ANALYSIS));

        var firstPage = stub.listAnalysisJobs(ListAnalysisJobsRequest.newBuilder()
            .setRequestId("request-list")
            .setCorrelationId("correlation-1")
            .setPageSize(1)
            .build());
        var secondPage = stub.listAnalysisJobs(ListAnalysisJobsRequest.newBuilder()
            .setRequestId("request-list-2")
            .setCorrelationId("correlation-1")
            .setPageSize(1)
            .setPageToken(firstPage.getNextPageToken())
            .build());
        var invalidToken = assertThrows(
            StatusRuntimeException.class,
            () -> stub.listAnalysisJobs(ListAnalysisJobsRequest.newBuilder()
                .setRequestId("request-list-invalid")
                .setCorrelationId("correlation-1")
                .setPageSize(1)
                .setPageToken("not-a-number")
                .build())
        );
        var empty = stub.listAnalysisJobs(ListAnalysisJobsRequest.newBuilder()
            .setRequestId("request-empty")
            .setCorrelationId("correlation-1")
            .setWorkerKind(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_REPORT)
            .build());

        assertEquals(1, firstPage.getJobsCount());
        assertEquals("1", firstPage.getNextPageToken());
        assertEquals(1, secondPage.getJobsCount());
        assertEquals("", secondPage.getNextPageToken());
        assertEquals("job-2", secondPage.getJobs(0).getJobId().getValue());
        assertEquals(Status.Code.INVALID_ARGUMENT, invalidToken.getStatus().getCode());
        assertFalse(firstPage.getJobs(0).getCreatedAt().isBlank());
        assertEquals(0, empty.getJobsCount());
    }

    static SubmitAnalysisJobRequest submitRequest(String idempotencyKey, String jobId, AnalysisWorkerKind workerKind) {
        return SubmitAnalysisJobRequest.newBuilder()
            .setRequestId("request-" + jobId)
            .setIdempotencyKey(idempotencyKey)
            .setSchemaVersion("schema-v1")
            .setCorrelationId("correlation-1")
            .setAnalysisRunId(runId())
            .setJobId(jobId(jobId))
            .setWorkerKind(workerKind)
            .setSourceSnapshotId(SourceSnapshotId.newBuilder().setValue("snapshot-1"))
            .addInputArtifacts(artifact("input-" + jobId + ".json", "input-sha-" + jobId, AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC))
            .setInputCompleteness(AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNKNOWN)
            .putAttributes("repository", "demo")
            .build();
    }

    static AnalysisRunId runId() {
        return AnalysisRunId.newBuilder().setValue("run-1").build();
    }

    static AnalysisJobId jobId(String value) {
        return AnalysisJobId.newBuilder().setValue(value).build();
    }

    static AnalysisArtifactReference artifact(String path, String sha256, AnalysisArtifactCategory category) {
        return AnalysisArtifactReference.newBuilder()
            .setArtifact(ArtifactReference.newBuilder()
                .setPath(path)
                .setType("application/json")
                .setSha256(sha256)
                .setSizeBytes(42))
            .setCategory(category)
            .setProducerService("analysis-store-test")
            .setSchemaVersion("schema-v1")
            .setCompleteness(AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNKNOWN)
            .build();
    }
}
