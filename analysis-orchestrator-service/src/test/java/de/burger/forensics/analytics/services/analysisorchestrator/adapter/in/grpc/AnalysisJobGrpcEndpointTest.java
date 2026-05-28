package de.burger.forensics.analytics.services.analysisorchestrator.adapter.in.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactReference;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobServiceGrpc;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobState;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisRunId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind;
import de.burger.forensics.analytics.analysisjob.v1.ArtifactByteAccess;
import de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody;
import de.burger.forensics.analytics.analysisjob.v1.ArtifactReference;
import de.burger.forensics.analytics.analysisjob.v1.BtmDeliveryReadiness;
import de.burger.forensics.analytics.analysisjob.v1.CompleteAnalysisJobRequest;
import de.burger.forensics.analytics.analysisjob.v1.FailAnalysisJobRequest;
import de.burger.forensics.analytics.analysisjob.v1.GetAnalysisJobRequest;
import de.burger.forensics.analytics.analysisjob.v1.GetRepositoryToBtmStatusRequest;
import de.burger.forensics.analytics.analysisjob.v1.LeaseAnalysisJobRequest;
import de.burger.forensics.analytics.analysisjob.v1.ListAnalysisJobsRequest;
import de.burger.forensics.analytics.analysisjob.v1.PlanInstrumentationTargetsRequest;
import de.burger.forensics.analytics.analysisjob.v1.RegisterAnalysisArtifactsRequest;
import de.burger.forensics.analytics.analysisjob.v1.RepositoryToBtmBuildContext;
import de.burger.forensics.analytics.analysisjob.v1.RepositoryToBtmDiagnosticSeverity;
import de.burger.forensics.analytics.analysisjob.v1.RepositoryToBtmOrchestrationState;
import de.burger.forensics.analytics.analysisjob.v1.RepositoryToBtmRepositoryReference;
import de.burger.forensics.analytics.analysisjob.v1.RepositoryToBtmRevision;
import de.burger.forensics.analytics.analysisjob.v1.RepositoryToBtmWorkspacePolicy;
import de.burger.forensics.analytics.analysisjob.v1.ReportAnalysisJobProgressRequest;
import de.burger.forensics.analytics.analysisjob.v1.RequestedRepositoryToBtmOutput;
import de.burger.forensics.analytics.analysisjob.v1.SourceSnapshotId;
import de.burger.forensics.analytics.analysisjob.v1.StartRepositoryToBtmRequest;
import de.burger.forensics.analytics.analysisjob.v1.SubmitAnalysisJobRequest;
import de.burger.forensics.analytics.services.analysisorchestrator.adapter.out.memory.InMemoryAnalysisJobRepository;
import de.burger.forensics.analytics.services.analysisorchestrator.application.AnalysisJobApplicationService;
import de.burger.forensics.analytics.services.analysisorchestrator.application.RepositoryToBtmOrchestrationApplicationService;
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
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisJobGrpcEndpointTest {
    private Server server;
    private ManagedChannel channel;
    private AnalysisJobServiceGrpc.AnalysisJobServiceBlockingStub stub;
    private MutableClock clock;

    @BeforeEach
    void startServer() throws IOException {
        var serverName = InProcessServerBuilder.generateName();
        clock = new MutableClock(Instant.parse("2026-05-16T10:15:30Z"));
        var applicationService = new AnalysisJobApplicationService(
            new InMemoryAnalysisJobRepository(),
            clock
        );
        server = InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(new AnalysisJobGrpcEndpoint(
                applicationService,
                new RepositoryToBtmOrchestrationApplicationService()
            ))
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
    void supportsJobLifecycleAndJobToArtifactReferences() {
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
            .addDiagnostics("worker still running")
            .build());
        var completed = stub.completeAnalysisJob(CompleteAnalysisJobRequest.newBuilder()
            .setRequestId("request-complete")
            .setIdempotencyKey("complete-1")
            .setCorrelationId("correlation-1")
            .setJobId(jobId("job-1"))
            .setAttempt(1)
            .setWorkerId("worker-a")
            .addOutputArtifacts(artifact("facts/output.json", "output-sha", AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC))
            .setOutputCompleteness(AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE)
            .addDiagnostics("done")
            .build());
        var registered = stub.registerAnalysisArtifacts(RegisterAnalysisArtifactsRequest.newBuilder()
            .setRequestId("request-register")
            .setIdempotencyKey("register-1")
            .setCorrelationId("correlation-1")
            .setAnalysisRunId(runId())
            .setJobId(jobId("job-1"))
            .addArtifacts(artifact("reports/run-1.json", "report-sha", AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_GENERATED))
            .build());

        assertEquals("ACCEPTED", submitted.getStatus().getCode());
        assertEquals(AnalysisJobState.ANALYSIS_JOB_STATE_DISPATCHABLE, submitted.getJob().getState());
        assertEquals("schema-v1", submitted.getJob().getSchemaVersion());
        assertEquals("demo", submitted.getJob().getAttributesMap().get("repository"));
        assertEquals(AnalysisJobState.ANALYSIS_JOB_STATE_RUNNING, leased.getJobs(0).getState());
        assertEquals("worker-a", leased.getJobs(0).getLeaseOwner());
        assertEquals("2026-05-16T10:16:30Z", leased.getJobs(0).getLeaseExpiresAt());
        assertEquals("worker still running", progressed.getDiagnostics(0));
        assertEquals(40, progressed.getPercentComplete());
        assertEquals(AnalysisJobState.ANALYSIS_JOB_STATE_COMPLETED, completed.getState());
        assertEquals(AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE, completed.getCompleteness());
        assertEquals(100, completed.getPercentComplete());
        assertEquals("facts/output.json", completed.getOutputArtifacts(0).getByteAccess().getRetrievalReference());
        assertEquals(2, registered.getArtifactsCount());
        assertEquals("reports/run-1.json", registered.getArtifacts(1).getByteAccess().getRetrievalReference());
    }

    @Test
    void acceptsRepositorySourceArtifactReferencesWithoutWorkspacePaths() {
        var submitted = stub.submitAnalysisJob(submitRequest(
            "submit-repository-source",
            "job-repository-source",
            AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS
        ).toBuilder()
            .setSourceSnapshotId(SourceSnapshotId.newBuilder().setValue("source-snapshot-1"))
            .clearInputArtifacts()
            .addInputArtifacts(repositorySourceArtifact())
            .clearAttributes()
            .putAttributes("repository", "demo")
            .build());

        assertEquals("ACCEPTED", submitted.getStatus().getCode());
        assertEquals("source-snapshot-1", submitted.getJob().getSourceSnapshotId().getValue());
        assertEquals("repository-source-service", submitted.getJob().getInputArtifacts(0).getByteAccess().getOwnerService());
        assertEquals("repository-source.v1.SourcePackage", submitted.getJob().getInputArtifacts(0).getByteAccess().getRetrievalContract());
        assertEquals("source-snapshot/source-snapshot-1", submitted.getJob().getInputArtifacts(0).getByteAccess().getRetrievalReference());
    }

    @Test
    void supportsListPaginationAndStatusLookup() {
        stub.submitAnalysisJob(submitRequest("submit-a", "job-a", AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS));
        stub.submitAnalysisJob(submitRequest("submit-b", "job-b", AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS));

        var firstPage = stub.listAnalysisJobs(ListAnalysisJobsRequest.newBuilder()
            .setRequestId("request-list-1")
            .setCorrelationId("correlation-1")
            .setAnalysisRunId(runId())
            .setWorkerKind(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS)
            .setState(AnalysisJobState.ANALYSIS_JOB_STATE_DISPATCHABLE)
            .setPageSize(1)
            .build());
        var secondPage = stub.listAnalysisJobs(ListAnalysisJobsRequest.newBuilder()
            .setRequestId("request-list-2")
            .setCorrelationId("correlation-1")
            .setAnalysisRunId(runId())
            .setWorkerKind(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS)
            .setState(AnalysisJobState.ANALYSIS_JOB_STATE_DISPATCHABLE)
            .setPageSize(1)
            .setPageToken(firstPage.getNextPageToken())
            .build());
        var lookedUp = stub.getAnalysisJob(GetAnalysisJobRequest.newBuilder()
            .setRequestId("request-get")
            .setCorrelationId("correlation-1")
            .setJobId(jobId("job-a"))
            .build());

        assertEquals("job-a", firstPage.getJobs(0).getJobId().getValue());
        assertEquals("1", firstPage.getNextPageToken());
        assertEquals("job-b", secondPage.getJobs(0).getJobId().getValue());
        assertEquals("", secondPage.getNextPageToken());
        assertEquals("job-a", lookedUp.getJobId().getValue());
    }

    @Test
    void supportsUnfilteredListWithDefaultPageSize() {
        stub.submitAnalysisJob(submitRequest("submit-unfiltered-a", "job-unfiltered-a", AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS));
        stub.submitAnalysisJob(submitRequest("submit-unfiltered-b", "job-unfiltered-b", AnalysisWorkerKind.ANALYSIS_WORKER_KIND_JOERN_ANALYSIS));

        var listed = stub.listAnalysisJobs(ListAnalysisJobsRequest.newBuilder()
            .setRequestId("request-list-unfiltered")
            .setCorrelationId("correlation-1")
            .build());

        assertEquals(2, listed.getJobsCount());
        assertEquals("", listed.getNextPageToken());
    }

    @Test
    void treatsUnrecognizedListFiltersAsUnfiltered() {
        stub.submitAnalysisJob(submitRequest("submit-unrecognized-filter", "job-unrecognized-filter", AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS));

        var listed = stub.listAnalysisJobs(ListAnalysisJobsRequest.newBuilder()
            .setRequestId("request-list-unrecognized")
            .setCorrelationId("correlation-1")
            .setWorkerKindValue(-1)
            .setStateValue(-1)
            .build());

        assertEquals(1, listed.getJobsCount());
        assertEquals("job-unrecognized-filter", listed.getJobs(0).getJobId().getValue());
    }

    @Test
    void mapsInvalidListAndLeaseRequestsToInvalidArgument() {
        var invalidList = assertThrows(StatusRuntimeException.class, () -> stub.listAnalysisJobs(ListAnalysisJobsRequest.newBuilder()
            .setRequestId("request-list-invalid")
            .setCorrelationId("correlation-1")
            .setPageSize(-1)
            .build()));
        var invalidLease = assertThrows(StatusRuntimeException.class, () -> stub.leaseAnalysisJob(LeaseAnalysisJobRequest.newBuilder()
            .setRequestId("request-lease-invalid")
            .setIdempotencyKey("lease-invalid")
            .setCorrelationId("correlation-1")
            .setWorkerId("worker-a")
            .setWorkerKind(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS)
            .setLeaseSeconds(60)
            .setMaxJobs(0)
            .build()));

        assertEquals(Status.Code.INVALID_ARGUMENT, invalidList.getStatus().getCode());
        assertEquals(Status.Code.INVALID_ARGUMENT, invalidLease.getStatus().getCode());
    }

    @Test
    void mapsMissingDuplicateAndIdempotencyConflictsToPublicStatuses() {
        stub.submitAnalysisJob(submitRequest("submit-conflict", "job-conflict-a", AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS));

        var missing = assertThrows(StatusRuntimeException.class, () -> stub.getAnalysisJob(GetAnalysisJobRequest.newBuilder()
            .setRequestId("request-missing")
            .setCorrelationId("correlation-1")
            .setJobId(jobId("missing-job"))
            .build()));
        var duplicate = assertThrows(StatusRuntimeException.class, () -> stub.submitAnalysisJob(
            submitRequest("submit-duplicate", "job-conflict-a", AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS)
        ));
        var idempotencyConflict = assertThrows(StatusRuntimeException.class, () -> stub.submitAnalysisJob(
            submitRequest("submit-conflict", "job-conflict-b", AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS)
        ));

        assertEquals(Status.Code.NOT_FOUND, missing.getStatus().getCode());
        assertEquals(Status.Code.INVALID_ARGUMENT, duplicate.getStatus().getCode());
        assertEquals(Status.Code.ALREADY_EXISTS, idempotencyConflict.getStatus().getCode());
    }

    @Test
    void mapsArtifactRegistrationRunMismatchToInvalidArgument() {
        stub.submitAnalysisJob(submitRequest("submit-register-mismatch", "job-register-mismatch", AnalysisWorkerKind.ANALYSIS_WORKER_KIND_REPORT));

        var error = assertThrows(StatusRuntimeException.class, () -> stub.registerAnalysisArtifacts(RegisterAnalysisArtifactsRequest.newBuilder()
            .setRequestId("request-register-mismatch")
            .setIdempotencyKey("register-mismatch")
            .setCorrelationId("correlation-1")
            .setAnalysisRunId(AnalysisRunId.newBuilder().setValue("other-run"))
            .setJobId(jobId("job-register-mismatch"))
            .addArtifacts(artifact("reports/run-1.json", "report-sha", AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_GENERATED))
            .build()));

        assertEquals(Status.Code.INVALID_ARGUMENT, error.getStatus().getCode());
        assertTrue(error.getStatus().getDescription().contains("analysisRunId does not match"));
    }

    @Test
    void defensivePrivateMappingsKeepUnexpectedInputsExplicit() throws Exception {
        var internal = invokePrivateStatus(new RuntimeException("boom"));
        var negativePageToken = assertThrows(ValidationException.class, () -> invokePrivatePageOffset("-1"));
        var invalidPageToken = assertThrows(ValidationException.class, () -> invokePrivatePageOffset("not-a-number"));
        var missingMapping = assertThrows(IllegalArgumentException.class, () -> invokePrivateRequired(null, "unsupported mapping"));

        assertEquals(Status.Code.INTERNAL, internal.getCode());
        assertEquals("pageToken must not be negative", negativePageToken.getMessage());
        assertEquals("pageToken must be an integer offset", invalidPageToken.getMessage());
        assertEquals("unsupported mapping", missingMapping.getMessage());
    }

    @Test
    void defensiveRepositoryToBtmEnumMappingsRemainExplicit() throws Exception {
        assertEquals(
            RepositoryToBtmOrchestrationState.REPOSITORY_TO_BTM_ORCHESTRATION_STATE_ACCEPTED,
            invokePrivateRepositoryToBtmState(de.burger.forensics.analytics.services.analysisorchestrator.domain.RepositoryToBtmOrchestrationState.ACCEPTED)
        );
        assertEquals(
            RepositoryToBtmOrchestrationState.REPOSITORY_TO_BTM_ORCHESTRATION_STATE_READY_FOR_BTM_DELIVERY,
            invokePrivateRepositoryToBtmState(
                de.burger.forensics.analytics.services.analysisorchestrator.domain.RepositoryToBtmOrchestrationState.READY_FOR_BTM_DELIVERY
            )
        );
        assertEquals(
            RepositoryToBtmOrchestrationState.REPOSITORY_TO_BTM_ORCHESTRATION_STATE_INCOMPLETE,
            invokePrivateRepositoryToBtmState(de.burger.forensics.analytics.services.analysisorchestrator.domain.RepositoryToBtmOrchestrationState.INCOMPLETE)
        );
        assertEquals(
            RepositoryToBtmOrchestrationState.REPOSITORY_TO_BTM_ORCHESTRATION_STATE_FAILED,
            invokePrivateRepositoryToBtmState(de.burger.forensics.analytics.services.analysisorchestrator.domain.RepositoryToBtmOrchestrationState.FAILED)
        );
        assertEquals(
            BtmDeliveryReadiness.BTM_DELIVERY_READINESS_READY,
            invokePrivateBtmDeliveryReadiness(de.burger.forensics.analytics.services.analysisorchestrator.domain.BtmDeliveryReadiness.READY)
        );
        assertEquals(
            BtmDeliveryReadiness.BTM_DELIVERY_READINESS_UNAVAILABLE,
            invokePrivateBtmDeliveryReadiness(de.burger.forensics.analytics.services.analysisorchestrator.domain.BtmDeliveryReadiness.UNAVAILABLE)
        );
        assertEquals(
            BtmDeliveryReadiness.BTM_DELIVERY_READINESS_UNKNOWN,
            invokePrivateBtmDeliveryReadiness(de.burger.forensics.analytics.services.analysisorchestrator.domain.BtmDeliveryReadiness.UNKNOWN)
        );
        assertEquals(
            RepositoryToBtmDiagnosticSeverity.REPOSITORY_TO_BTM_DIAGNOSTIC_SEVERITY_WARNING,
            invokePrivateRepositoryToBtmSeverity(
                de.burger.forensics.analytics.services.analysisorchestrator.domain.RepositoryToBtmDiagnosticSeverity.WARNING
            )
        );
        assertEquals(
            RepositoryToBtmDiagnosticSeverity.REPOSITORY_TO_BTM_DIAGNOSTIC_SEVERITY_ERROR,
            invokePrivateRepositoryToBtmSeverity(de.burger.forensics.analytics.services.analysisorchestrator.domain.RepositoryToBtmDiagnosticSeverity.ERROR)
        );
    }

    @Test
    void mapsRetryableFailureAndDeadLetterFailure() {
        stub.submitAnalysisJob(submitRequest("submit-retry", "job-retry", AnalysisWorkerKind.ANALYSIS_WORKER_KIND_JOERN_ANALYSIS));
        stub.leaseAnalysisJob(leaseRequest("lease-retry", "worker-a", AnalysisWorkerKind.ANALYSIS_WORKER_KIND_JOERN_ANALYSIS));

        var retryable = stub.failAnalysisJob(FailAnalysisJobRequest.newBuilder()
            .setRequestId("request-fail-retry")
            .setIdempotencyKey("fail-retry")
            .setCorrelationId("correlation-1")
            .setJobId(jobId("job-retry"))
            .setAttempt(1)
            .setWorkerId("worker-a")
            .setReason("temporary downstream timeout")
            .addDiagnostics("timeout")
            .setCompleteness(AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNKNOWN)
            .setRetryable(true)
            .build());

        stub.submitAnalysisJob(submitRequest("submit-dead", "job-dead", AnalysisWorkerKind.ANALYSIS_WORKER_KIND_REPOSITORY_ANALYSIS));
        stub.leaseAnalysisJob(leaseRequest("lease-dead", "worker-b", AnalysisWorkerKind.ANALYSIS_WORKER_KIND_REPOSITORY_ANALYSIS));
        var deadLettered = stub.failAnalysisJob(FailAnalysisJobRequest.newBuilder()
            .setRequestId("request-fail-dead")
            .setIdempotencyKey("fail-dead")
            .setCorrelationId("correlation-1")
            .setJobId(jobId("job-dead"))
            .setAttempt(1)
            .setWorkerId("worker-b")
            .setReason("invalid immutable request")
            .addDiagnostics("not retryable")
            .setCompleteness(AnalysisCompleteness.ANALYSIS_COMPLETENESS_INCOMPLETE)
            .setRetryable(false)
            .build());

        assertEquals(AnalysisJobState.ANALYSIS_JOB_STATE_RETRYABLE, retryable.getState());
        assertEquals(AnalysisJobState.ANALYSIS_JOB_STATE_DEAD_LETTERED, deadLettered.getState());
    }

    @Test
    void mapsStaleWorkerCallsAfterLeaseTimeoutToFailedPrecondition() {
        stub.submitAnalysisJob(submitRequest("submit-timeout", "job-timeout", AnalysisWorkerKind.ANALYSIS_WORKER_KIND_JOERN_ANALYSIS));
        stub.leaseAnalysisJob(LeaseAnalysisJobRequest.newBuilder()
            .setRequestId("request-lease-timeout")
            .setIdempotencyKey("lease-timeout")
            .setCorrelationId("correlation-1")
            .setWorkerId("worker-a")
            .setWorkerKind(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_JOERN_ANALYSIS)
            .setLeaseSeconds(1)
            .setMaxJobs(1)
            .build());

        clock.set(Instant.parse("2026-05-16T10:15:32Z"));
        var staleProgress = assertThrows(StatusRuntimeException.class, () -> stub.reportAnalysisJobProgress(ReportAnalysisJobProgressRequest.newBuilder()
            .setRequestId("request-stale-progress")
            .setIdempotencyKey("stale-progress")
            .setCorrelationId("correlation-1")
            .setJobId(jobId("job-timeout"))
            .setAttempt(1)
            .setWorkerId("worker-a")
            .setPercentComplete(75)
            .build()));
        var timedOut = stub.getAnalysisJob(GetAnalysisJobRequest.newBuilder()
            .setRequestId("request-get-timeout")
            .setCorrelationId("correlation-1")
            .setJobId(jobId("job-timeout"))
            .build());
        var staleComplete = assertThrows(StatusRuntimeException.class, () -> stub.completeAnalysisJob(CompleteAnalysisJobRequest.newBuilder()
            .setRequestId("request-stale-complete")
            .setIdempotencyKey("stale-complete")
            .setCorrelationId("correlation-1")
            .setJobId(jobId("job-timeout"))
            .setAttempt(1)
            .setWorkerId("worker-a")
            .setOutputCompleteness(AnalysisCompleteness.ANALYSIS_COMPLETENESS_INCOMPLETE)
            .build()));
        var staleFail = assertThrows(StatusRuntimeException.class, () -> stub.failAnalysisJob(FailAnalysisJobRequest.newBuilder()
            .setRequestId("request-stale-fail")
            .setIdempotencyKey("stale-fail")
            .setCorrelationId("correlation-1")
            .setJobId(jobId("job-timeout"))
            .setAttempt(1)
            .setWorkerId("worker-a")
            .setReason("late failure report")
            .setCompleteness(AnalysisCompleteness.ANALYSIS_COMPLETENESS_INCOMPLETE)
            .setRetryable(true)
            .build()));
        var leasedAgain = stub.leaseAnalysisJob(leaseRequest(
            "lease-timeout-again",
            "worker-b",
            AnalysisWorkerKind.ANALYSIS_WORKER_KIND_JOERN_ANALYSIS
        ));

        assertEquals(Status.Code.FAILED_PRECONDITION, staleProgress.getStatus().getCode());
        assertEquals(Status.Code.FAILED_PRECONDITION, staleComplete.getStatus().getCode());
        assertEquals(Status.Code.FAILED_PRECONDITION, staleFail.getStatus().getCode());
        assertEquals(AnalysisJobState.ANALYSIS_JOB_STATE_RETRYABLE, timedOut.getState());
        assertEquals(1, timedOut.getFailuresCount());
        assertTrue(timedOut.getFailures(0).getDiagnosticsList().contains("worker lease expired before completion"));
        assertEquals(AnalysisJobState.ANALYSIS_JOB_STATE_RUNNING, leasedAgain.getJobs(0).getState());
        assertEquals(2, leasedAgain.getJobs(0).getAttempt());
        assertEquals("worker-b", leasedAgain.getJobs(0).getLeaseOwner());
    }

    @Test
    void rejectsInvalidInputAndDoesNotExposeWorkerOrReportExecutionAsOwnedBehavior() {
        var invalid = assertThrows(StatusRuntimeException.class, () -> stub.submitAnalysisJob(
            SubmitAnalysisJobRequest.newBuilder()
                .setRequestId("request-invalid")
                .setIdempotencyKey("invalid")
                .setCorrelationId("correlation-1")
                .setSchemaVersion("schema-v1")
                .setAnalysisRunId(runId())
                .setJobId(jobId("job-invalid"))
                .setWorkerKind(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS)
                .setSourceSnapshotId(SourceSnapshotId.newBuilder().setValue("snapshot-1"))
                .setInputCompleteness(AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNKNOWN)
                .addInputArtifacts(artifact("/private/output.json", "bad-sha", AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC))
                .build()
        ));
        var planning = assertThrows(
            StatusRuntimeException.class,
            () -> stub.planInstrumentationTargets(PlanInstrumentationTargetsRequest.newBuilder().build())
        );

        assertEquals(Status.Code.INVALID_ARGUMENT, invalid.getStatus().getCode());
        assertEquals(Status.Code.UNIMPLEMENTED, planning.getStatus().getCode());
        assertTrue(planning.getStatus().getDescription().contains("not owned"));
    }

    @Test
    void acceptsRepositoryToBtmAsPendingStatusWithoutWorkerExecution() {
        var started = stub.startRepositoryToBtm(startRepositoryToBtmRequest("start-repository-to-btm").build());
        var status = stub.getRepositoryToBtmStatus(GetRepositoryToBtmStatusRequest.newBuilder()
            .setRequestId("request-get-repository-to-btm")
            .setCorrelationId("correlation-repository-to-btm")
            .setAnalysisRunId(runId())
            .build());
        var repositoryJobs = stub.listAnalysisJobs(ListAnalysisJobsRequest.newBuilder()
            .setRequestId("request-list-repository-workers")
            .setCorrelationId("correlation-repository-to-btm")
            .setAnalysisRunId(runId())
            .setWorkerKind(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_REPOSITORY_ANALYSIS)
            .build());
        var allJobs = stub.listAnalysisJobs(ListAnalysisJobsRequest.newBuilder()
            .setRequestId("request-list-all-workers")
            .setCorrelationId("correlation-repository-to-btm")
            .setAnalysisRunId(runId())
            .build());

        assertEquals("REPOSITORY_TO_BTM_WAITING_FOR_REPOSITORY", started.getStatus().getCode());
        assertEquals("correlation-repository-to-btm", started.getStatus().getCorrelationId());
        assertEquals(false, started.getStatus().getRetryable());
        assertTrue(started.getStatus().getMessage().contains("waiting for repository source handoff"));
        assertEquals(RepositoryToBtmOrchestrationState.REPOSITORY_TO_BTM_ORCHESTRATION_STATE_WAITING_FOR_REPOSITORY, started.getState());
        assertEquals(BtmDeliveryReadiness.BTM_DELIVERY_READINESS_NOT_READY, started.getBtmDeliveryReadiness());
        assertEquals(AnalysisCompleteness.ANALYSIS_COMPLETENESS_INCOMPLETE, started.getCompleteness());
        assertEquals(true, started.getJoernSkipped());
        assertEquals("repository-analysis-", started.getRepositoryAnalysisJobId().getValue().substring(0, "repository-analysis-".length()));
        assertEquals(false, started.hasSourceSnapshotId());
        assertEquals("REPOSITORY_TO_BTM_WAITING_FOR_REPOSITORY", started.getDiagnostics(0).getCode());
        assertEquals(RepositoryToBtmDiagnosticSeverity.REPOSITORY_TO_BTM_DIAGNOSTIC_SEVERITY_INFO, started.getDiagnostics(0).getSeverity());
        assertEquals(false, started.getDiagnostics(0).getRetryable());
        assertEquals(true, started.getDiagnostics(0).getAffectsCompleteness());
        assertEquals(started.getStatus(), status.getStatus());
        assertEquals(started.getCompleteness(), status.getCompleteness());
        assertEquals(started.getRepositoryAnalysisJobId(), status.getRepositoryAnalysisJobId());
        assertEquals(started.getState(), status.getState());
        assertEquals(started.getBtmDeliveryReadiness(), status.getBtmDeliveryReadiness());
        assertEquals(started.getJoernSkipped(), status.getJoernSkipped());
        assertEquals(started.getDiagnosticsList(), status.getDiagnosticsList());
        assertEquals(0, repositoryJobs.getJobsCount());
        assertEquals(0, allJobs.getJobsCount());
        assertEquals(0, started.getAcceptedGeneratedArtifactsCount());
        assertNoWorkersCanLeaseJobs();
    }

    @Test
    void rejectsLocalFileAndNonHttpsRepositoryToBtmInputs() {
        for (String remote : List.of(
            "file:///tmp/repo",
            "/tmp/repo",
            "http://example.test/repo.git",
            "ssh://example.test/repo.git"
        )) {
            var failure = assertThrows(StatusRuntimeException.class, () -> stub.startRepositoryToBtm(
                startRepositoryToBtmRequest("reject-repository-to-btm-" + Math.abs(remote.hashCode()))
                    .setRepository(RepositoryToBtmRepositoryReference.newBuilder()
                        .setRemoteUrl(remote)
                        .setProvider("git"))
                    .build()
            ));

            assertEquals(Status.Code.INVALID_ARGUMENT, failure.getStatus().getCode());
            assertTrue(failure.getStatus().getDescription().contains("clean HTTPS URL"));
        }
    }

    @Test
    void rejectsUnknownUnsafeAndConflictingRepositoryToBtmRequests() {
        stub.startRepositoryToBtm(startRepositoryToBtmRequest("start-original-repository-to-btm").build());

        var missing = assertThrows(StatusRuntimeException.class, () -> stub.getRepositoryToBtmStatus(GetRepositoryToBtmStatusRequest.newBuilder()
            .setRequestId("request-get-missing-repository-to-btm")
            .setCorrelationId("correlation-repository-to-btm")
            .setAnalysisRunId(AnalysisRunId.newBuilder().setValue("missing-run"))
            .build()));
        var unsafeRemote = assertThrows(StatusRuntimeException.class, () -> stub.startRepositoryToBtm(
            startRepositoryToBtmRequest("start-unsafe-repository-to-btm")
                .setRepository(RepositoryToBtmRepositoryReference.newBuilder()
                    .setRemoteUrl("https://token@example.test/repo.git")
                    .setProvider("git"))
                .build()
        ));
        var missingOutput = assertThrows(StatusRuntimeException.class, () -> stub.startRepositoryToBtm(
            startRepositoryToBtmRequest("start-missing-output")
                .clearRequestedOutputs()
                .build()
        ));
        var idempotencyConflict = assertThrows(StatusRuntimeException.class, () -> stub.startRepositoryToBtm(
            startRepositoryToBtmRequest("start-original-repository-to-btm")
                .setRevision(RepositoryToBtmRevision.newBuilder().setBranch("feature/parity"))
                .build()
        ));
        var analysisRunConflict = assertThrows(StatusRuntimeException.class, () -> stub.startRepositoryToBtm(
            startRepositoryToBtmRequest("start-conflicting-repository-to-btm")
                .setRevision(RepositoryToBtmRevision.newBuilder().setBranch("feature/parity"))
                .build()
        ));

        assertEquals(Status.Code.NOT_FOUND, missing.getStatus().getCode());
        assertEquals(Status.Code.INVALID_ARGUMENT, unsafeRemote.getStatus().getCode());
        assertEquals(Status.Code.INVALID_ARGUMENT, missingOutput.getStatus().getCode());
        assertEquals(Status.Code.ALREADY_EXISTS, idempotencyConflict.getStatus().getCode());
        assertTrue(idempotencyConflict.getStatus().getDescription().contains("start-original-repository-to-btm"));
        assertEquals(Status.Code.ALREADY_EXISTS, analysisRunConflict.getStatus().getCode());
        assertTrue(analysisRunConflict.getStatus().getDescription().contains("run-1"));
    }

    @Test
    void rejectsPrivateRepositorySourceHandoffValues() {
        var privateSnapshotId = assertThrows(StatusRuntimeException.class, () -> stub.submitAnalysisJob(
            submitRequest("submit-private-snapshot", "job-private-snapshot", AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS)
                .toBuilder()
                .setSourceSnapshotId(SourceSnapshotId.newBuilder().setValue("/tmp/source-snapshot"))
                .build()
        ));
        var privateAttribute = assertThrows(StatusRuntimeException.class, () -> stub.submitAnalysisJob(
            submitRequest("submit-private-attribute", "job-private-attribute", AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS)
                .toBuilder()
                .putAttributes("note", "checkout failed at /tmp/private/workspace")
                .build()
        ));

        assertEquals(Status.Code.INVALID_ARGUMENT, privateSnapshotId.getStatus().getCode());
        assertEquals(Status.Code.INVALID_ARGUMENT, privateAttribute.getStatus().getCode());
    }

    private static SubmitAnalysisJobRequest submitRequest(String idempotencyKey, String jobId, AnalysisWorkerKind workerKind) {
        return SubmitAnalysisJobRequest.newBuilder()
            .setRequestId("request-" + jobId)
            .setIdempotencyKey(idempotencyKey)
            .setSchemaVersion("schema-v1")
            .setCorrelationId("correlation-1")
            .setAnalysisRunId(runId())
            .setJobId(jobId(jobId))
            .setWorkerKind(workerKind)
            .setSourceSnapshotId(SourceSnapshotId.newBuilder().setValue("snapshot-1"))
            .setInputCompleteness(AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNKNOWN)
            .addInputArtifacts(artifact("inputs/source.json", "input-sha", AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC))
            .putAttributes("repository", "demo")
            .build();
    }

    private static LeaseAnalysisJobRequest leaseRequest(String idempotencyKey, String workerId, AnalysisWorkerKind workerKind) {
        return LeaseAnalysisJobRequest.newBuilder()
            .setRequestId("request-" + idempotencyKey)
            .setIdempotencyKey(idempotencyKey)
            .setCorrelationId("correlation-1")
            .setWorkerId(workerId)
            .setWorkerKind(workerKind)
            .setLeaseSeconds(60)
            .setMaxJobs(1)
            .build();
    }

    private void assertNoWorkersCanLeaseJobs() {
        for (var workerKind : List.of(
            AnalysisWorkerKind.ANALYSIS_WORKER_KIND_REPOSITORY_ANALYSIS,
            AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS,
            AnalysisWorkerKind.ANALYSIS_WORKER_KIND_JOERN_ANALYSIS,
            AnalysisWorkerKind.ANALYSIS_WORKER_KIND_BTM_GENERATION,
            AnalysisWorkerKind.ANALYSIS_WORKER_KIND_GRAPH_ANALYSIS,
            AnalysisWorkerKind.ANALYSIS_WORKER_KIND_REPORT,
            AnalysisWorkerKind.ANALYSIS_WORKER_KIND_LLM_PROJECTION
        )) {
            var leased = stub.leaseAnalysisJob(LeaseAnalysisJobRequest.newBuilder()
                .setRequestId("request-lease-empty-" + workerKind.getNumber())
                .setIdempotencyKey("lease-empty-" + workerKind.getNumber())
                .setCorrelationId("correlation-repository-to-btm")
                .setWorkerId("worker-" + workerKind.getNumber())
                .setWorkerKind(workerKind)
                .setLeaseSeconds(60)
                .setMaxJobs(1)
                .build());

            assertEquals(0, leased.getJobsCount());
        }
    }

    private static StartRepositoryToBtmRequest.Builder startRepositoryToBtmRequest(String idempotencyKey) {
        return StartRepositoryToBtmRequest.newBuilder()
            .setRequestId("request-" + idempotencyKey)
            .setIdempotencyKey(idempotencyKey)
            .setSchemaVersion("schema-v1")
            .setCorrelationId("correlation-repository-to-btm")
            .setAnalysisRunId(runId())
            .setRepository(RepositoryToBtmRepositoryReference.newBuilder()
                .setRemoteUrl("https://example.test/repository.git")
                .setProvider("git"))
            .setRevision(RepositoryToBtmRevision.newBuilder()
                .setBranch("main"))
            .setWorkspacePolicy(RepositoryToBtmWorkspacePolicy.newBuilder()
                .setEphemeral(true)
                .setAllowShallowClone(true)
                .setTimeoutSeconds(60)
                .setMaxWorkspaceBytes(1_000_000))
            .setBuildContext(RepositoryToBtmBuildContext.newBuilder()
                .setBuildTool("gradle")
                .setBuildId("build-1")
                .setRootProjectName("demo")
                .addDeclaredModules("app"))
            .addRequestedOutputs(RequestedRepositoryToBtmOutput.REQUESTED_REPOSITORY_TO_BTM_OUTPUT_BTM_RULES)
            .putAttributes("repository", "demo");
    }

    private static AnalysisRunId runId() {
        return AnalysisRunId.newBuilder().setValue("run-1").build();
    }

    private static AnalysisJobId jobId(String value) {
        return AnalysisJobId.newBuilder().setValue(value).build();
    }

    private static AnalysisArtifactReference artifact(
        String path,
        String sha256,
        AnalysisArtifactCategory category
    ) {
        return AnalysisArtifactReference.newBuilder()
            .setArtifact(ArtifactReference.newBuilder()
                .setPath(path)
                .setType("application/json")
                .setSha256(sha256)
                .setSizeBytes(42))
            .setCategory(category)
            .setProducerService("producer-service")
            .setSchemaVersion("schema-v1")
            .setCompleteness(AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE)
            .setByteAccess(ArtifactByteAccess.newBuilder()
                .setOwnerService("query-report-api-service")
                .setRetrievalContract("query-report-api-service.generated-reports.v1")
                .setRetrievalReference(path)
                .setByteCustody(ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_PRODUCER_RETAINED))
            .build();
    }

    private static AnalysisArtifactReference repositorySourceArtifact() {
        return AnalysisArtifactReference.newBuilder()
            .setArtifact(ArtifactReference.newBuilder()
                .setPath("source-snapshot/source-snapshot-1")
                .setType("application/json")
                .setSha256("source-sha")
                .setSizeBytes(42))
            .setCategory(AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC)
            .setProducerService("repository-source-service")
            .setSchemaVersion("repository-source.v1.SourcePackage")
            .setCompleteness(AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE)
            .setByteAccess(ArtifactByteAccess.newBuilder()
                .setOwnerService("repository-source-service")
                .setRetrievalContract("repository-source.v1.SourcePackage")
                .setRetrievalReference("source-snapshot/source-snapshot-1")
                .setByteCustody(ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_PRODUCER_RETAINED))
            .build();
    }

    private static Status invokePrivateStatus(RuntimeException error) throws Exception {
        return invokePrivate("toStatus", new Class<?>[] {RuntimeException.class}, error);
    }

    private static int invokePrivatePageOffset(String pageToken) throws Exception {
        return invokePrivate("pageOffset", new Class<?>[] {String.class}, pageToken);
    }

    private static Object invokePrivateRequired(Object value, String message) throws Exception {
        return invokePrivate("required", new Class<?>[] {Object.class, String.class}, value, message);
    }

    private static RepositoryToBtmOrchestrationState invokePrivateRepositoryToBtmState(
        de.burger.forensics.analytics.services.analysisorchestrator.domain.RepositoryToBtmOrchestrationState state
    ) throws Exception {
        return invokePrivate(
            "toProto",
            new Class<?>[] {de.burger.forensics.analytics.services.analysisorchestrator.domain.RepositoryToBtmOrchestrationState.class},
            state
        );
    }

    private static BtmDeliveryReadiness invokePrivateBtmDeliveryReadiness(
        de.burger.forensics.analytics.services.analysisorchestrator.domain.BtmDeliveryReadiness readiness
    ) throws Exception {
        return invokePrivate(
            "toProto",
            new Class<?>[] {de.burger.forensics.analytics.services.analysisorchestrator.domain.BtmDeliveryReadiness.class},
            readiness
        );
    }

    private static RepositoryToBtmDiagnosticSeverity invokePrivateRepositoryToBtmSeverity(
        de.burger.forensics.analytics.services.analysisorchestrator.domain.RepositoryToBtmDiagnosticSeverity severity
    ) throws Exception {
        return invokePrivate(
            "toProto",
            new Class<?>[] {de.burger.forensics.analytics.services.analysisorchestrator.domain.RepositoryToBtmDiagnosticSeverity.class},
            severity
        );
    }

    @SuppressWarnings("unchecked")
    private static <T> T invokePrivate(String name, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = AnalysisJobGrpcEndpoint.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        try {
            return (T) method.invoke(null, args);
        } catch (ReflectiveOperationException error) {
            var cause = error.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error severeError) {
                throw severeError;
            }
            throw error;
        }
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> instant;

        private MutableClock(Instant instant) {
            this.instant = new AtomicReference<>(instant);
        }

        private void set(Instant nextInstant) {
            instant.set(nextInstant);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant.get();
        }
    }
}
