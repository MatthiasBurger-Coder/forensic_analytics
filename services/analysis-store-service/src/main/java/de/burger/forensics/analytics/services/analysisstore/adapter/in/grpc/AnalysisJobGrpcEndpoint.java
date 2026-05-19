package de.burger.forensics.analytics.services.analysisstore.adapter.in.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobServiceGrpc;
import de.burger.forensics.analytics.analysisjob.v1.CompleteAnalysisJobRequest;
import de.burger.forensics.analytics.analysisjob.v1.FailAnalysisJobRequest;
import de.burger.forensics.analytics.analysisjob.v1.GetAnalysisJobRequest;
import de.burger.forensics.analytics.analysisjob.v1.LeaseAnalysisJobRequest;
import de.burger.forensics.analytics.analysisjob.v1.LeaseAnalysisJobResponse;
import de.burger.forensics.analytics.analysisjob.v1.ListAnalysisJobsRequest;
import de.burger.forensics.analytics.analysisjob.v1.ListAnalysisJobsResponse;
import de.burger.forensics.analytics.analysisjob.v1.OperationStatus;
import de.burger.forensics.analytics.analysisjob.v1.PlanInstrumentationTargetsRequest;
import de.burger.forensics.analytics.analysisjob.v1.PlanInstrumentationTargetsResponse;
import de.burger.forensics.analytics.analysisjob.v1.RegisterAnalysisArtifactsRequest;
import de.burger.forensics.analytics.analysisjob.v1.RegisterAnalysisArtifactsResponse;
import de.burger.forensics.analytics.analysisjob.v1.ReportAnalysisJobProgressRequest;
import de.burger.forensics.analytics.analysisjob.v1.SubmitAnalysisJobRequest;
import de.burger.forensics.analytics.analysisjob.v1.SubmitAnalysisJobResponse;
import de.burger.forensics.analytics.services.analysisstore.application.AnalysisJobApplicationService;
import de.burger.forensics.analytics.services.analysisstore.application.AnalysisJobNotFoundException;
import de.burger.forensics.analytics.services.analysisstore.application.IdempotencyConflictException;
import de.burger.forensics.analytics.services.analysisstore.application.InstrumentationTargetPlanningApplicationService;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJob;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJobId;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJobState;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisRunId;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisWorkerKind;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactByteAccess;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactByteCustody;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactReference;
import de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain;
import de.burger.forensics.analytics.services.analysisstore.domain.SourceSnapshotId;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class AnalysisJobGrpcEndpoint extends AnalysisJobServiceGrpc.AnalysisJobServiceImplBase {
    private static final Map<de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind, AnalysisWorkerKind> WORKER_KINDS = Map.of(
        de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind.ANALYSIS_WORKER_KIND_REPOSITORY_ANALYSIS,
        AnalysisWorkerKind.REPOSITORY_ANALYSIS,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS,
        AnalysisWorkerKind.AST_ANALYSIS,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind.ANALYSIS_WORKER_KIND_JOERN_ANALYSIS,
        AnalysisWorkerKind.JOERN_ANALYSIS,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind.ANALYSIS_WORKER_KIND_BTM_GENERATION,
        AnalysisWorkerKind.BTM_GENERATION,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind.ANALYSIS_WORKER_KIND_GRAPH_ANALYSIS,
        AnalysisWorkerKind.GRAPH_ANALYSIS,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind.ANALYSIS_WORKER_KIND_REPORT,
        AnalysisWorkerKind.REPORT,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind.ANALYSIS_WORKER_KIND_LLM_PROJECTION,
        AnalysisWorkerKind.LLM_PROJECTION
    );
    private static final Map<AnalysisWorkerKind, de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind> WORKER_KIND_PROTOS = Map.of(
        AnalysisWorkerKind.REPOSITORY_ANALYSIS,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind.ANALYSIS_WORKER_KIND_REPOSITORY_ANALYSIS,
        AnalysisWorkerKind.AST_ANALYSIS,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS,
        AnalysisWorkerKind.JOERN_ANALYSIS,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind.ANALYSIS_WORKER_KIND_JOERN_ANALYSIS,
        AnalysisWorkerKind.BTM_GENERATION,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind.ANALYSIS_WORKER_KIND_BTM_GENERATION,
        AnalysisWorkerKind.GRAPH_ANALYSIS,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind.ANALYSIS_WORKER_KIND_GRAPH_ANALYSIS,
        AnalysisWorkerKind.REPORT,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind.ANALYSIS_WORKER_KIND_REPORT,
        AnalysisWorkerKind.LLM_PROJECTION,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind.ANALYSIS_WORKER_KIND_LLM_PROJECTION
    );
    private static final Map<de.burger.forensics.analytics.analysisjob.v1.AnalysisJobState, AnalysisJobState> JOB_STATES = Map.of(
        de.burger.forensics.analytics.analysisjob.v1.AnalysisJobState.ANALYSIS_JOB_STATE_ACCEPTED,
        AnalysisJobState.ACCEPTED,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisJobState.ANALYSIS_JOB_STATE_DISPATCHABLE,
        AnalysisJobState.DISPATCHABLE,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisJobState.ANALYSIS_JOB_STATE_RUNNING,
        AnalysisJobState.RUNNING,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisJobState.ANALYSIS_JOB_STATE_RETRYABLE,
        AnalysisJobState.RETRYABLE,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisJobState.ANALYSIS_JOB_STATE_FAILED,
        AnalysisJobState.FAILED,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisJobState.ANALYSIS_JOB_STATE_DEAD_LETTERED,
        AnalysisJobState.DEAD_LETTERED,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisJobState.ANALYSIS_JOB_STATE_COMPLETED,
        AnalysisJobState.COMPLETED
    );
    private static final Map<AnalysisJobState, de.burger.forensics.analytics.analysisjob.v1.AnalysisJobState> JOB_STATE_PROTOS = Map.of(
        AnalysisJobState.ACCEPTED,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisJobState.ANALYSIS_JOB_STATE_ACCEPTED,
        AnalysisJobState.DISPATCHABLE,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisJobState.ANALYSIS_JOB_STATE_DISPATCHABLE,
        AnalysisJobState.RUNNING,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisJobState.ANALYSIS_JOB_STATE_RUNNING,
        AnalysisJobState.RETRYABLE,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisJobState.ANALYSIS_JOB_STATE_RETRYABLE,
        AnalysisJobState.FAILED,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisJobState.ANALYSIS_JOB_STATE_FAILED,
        AnalysisJobState.DEAD_LETTERED,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisJobState.ANALYSIS_JOB_STATE_DEAD_LETTERED,
        AnalysisJobState.COMPLETED,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisJobState.ANALYSIS_JOB_STATE_COMPLETED
    );
    private static final Map<de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness, AnalysisCompleteness> COMPLETENESS = Map.of(
        de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE,
        AnalysisCompleteness.COMPLETE,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.ANALYSIS_COMPLETENESS_INCOMPLETE,
        AnalysisCompleteness.INCOMPLETE,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNKNOWN,
        AnalysisCompleteness.UNKNOWN
    );
    private static final Map<AnalysisCompleteness, de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness> COMPLETENESS_PROTOS = Map.of(
        AnalysisCompleteness.COMPLETE,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE,
        AnalysisCompleteness.INCOMPLETE,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.ANALYSIS_COMPLETENESS_INCOMPLETE,
        AnalysisCompleteness.UNKNOWN,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNKNOWN
    );
    private static final Map<de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory, de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactCategory> CATEGORIES = Map.of(
        de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC,
        de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactCategory.STATIC,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_RUNTIME,
        de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactCategory.RUNTIME,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_PROJECTION,
        de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactCategory.PROJECTION,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_GENERATED,
        de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactCategory.GENERATED
    );
    private static final Map<de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactCategory, de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory> CATEGORY_PROTOS = Map.of(
        de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactCategory.STATIC,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC,
        de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactCategory.RUNTIME,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_RUNTIME,
        de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactCategory.PROJECTION,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_PROJECTION,
        de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactCategory.GENERATED,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_GENERATED
    );
    private static final Map<de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody, ArtifactByteCustody> BYTE_CUSTODIES = Map.of(
        de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_PRODUCER_RETAINED,
        ArtifactByteCustody.PRODUCER_RETAINED,
        de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_SCOPED_OBJECT_ACCESS,
        ArtifactByteCustody.SCOPED_OBJECT_ACCESS,
        de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_EXPLICIT_HANDOFF,
        ArtifactByteCustody.EXPLICIT_HANDOFF
    );
    private static final Map<ArtifactByteCustody, de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody> BYTE_CUSTODY_PROTOS = Map.of(
        ArtifactByteCustody.PRODUCER_RETAINED,
        de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_PRODUCER_RETAINED,
        ArtifactByteCustody.SCOPED_OBJECT_ACCESS,
        de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_SCOPED_OBJECT_ACCESS,
        ArtifactByteCustody.EXPLICIT_HANDOFF,
        de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_EXPLICIT_HANDOFF
    );
    private static final Map<de.burger.forensics.analytics.analysisjob.v1.InstrumentationProbeKind, InstrumentationTargetPlanningDomain.ProbeKind> PROBE_KINDS = Map.of(
        de.burger.forensics.analytics.analysisjob.v1.InstrumentationProbeKind.INSTRUMENTATION_PROBE_KIND_METHOD_ENTRY,
        InstrumentationTargetPlanningDomain.ProbeKind.METHOD_ENTRY,
        de.burger.forensics.analytics.analysisjob.v1.InstrumentationProbeKind.INSTRUMENTATION_PROBE_KIND_METHOD_EXIT,
        InstrumentationTargetPlanningDomain.ProbeKind.METHOD_EXIT,
        de.burger.forensics.analytics.analysisjob.v1.InstrumentationProbeKind.INSTRUMENTATION_PROBE_KIND_THROW,
        InstrumentationTargetPlanningDomain.ProbeKind.THROW
    );
    private static final Map<InstrumentationTargetPlanningDomain.ProbeKind, de.burger.forensics.analytics.analysisjob.v1.InstrumentationProbeKind> PROBE_KIND_PROTOS = Map.of(
        InstrumentationTargetPlanningDomain.ProbeKind.METHOD_ENTRY,
        de.burger.forensics.analytics.analysisjob.v1.InstrumentationProbeKind.INSTRUMENTATION_PROBE_KIND_METHOD_ENTRY,
        InstrumentationTargetPlanningDomain.ProbeKind.METHOD_EXIT,
        de.burger.forensics.analytics.analysisjob.v1.InstrumentationProbeKind.INSTRUMENTATION_PROBE_KIND_METHOD_EXIT,
        InstrumentationTargetPlanningDomain.ProbeKind.THROW,
        de.burger.forensics.analytics.analysisjob.v1.InstrumentationProbeKind.INSTRUMENTATION_PROBE_KIND_THROW
    );
    private static final Map<InstrumentationTargetPlanningDomain.DiagnosticSeverity, de.burger.forensics.analytics.analysisjob.v1.TargetPlanningDiagnosticSeverity> TARGET_DIAGNOSTIC_SEVERITIES = Map.of(
        InstrumentationTargetPlanningDomain.DiagnosticSeverity.INFO,
        de.burger.forensics.analytics.analysisjob.v1.TargetPlanningDiagnosticSeverity.TARGET_PLANNING_DIAGNOSTIC_SEVERITY_INFO,
        InstrumentationTargetPlanningDomain.DiagnosticSeverity.WARNING,
        de.burger.forensics.analytics.analysisjob.v1.TargetPlanningDiagnosticSeverity.TARGET_PLANNING_DIAGNOSTIC_SEVERITY_WARNING,
        InstrumentationTargetPlanningDomain.DiagnosticSeverity.ERROR,
        de.burger.forensics.analytics.analysisjob.v1.TargetPlanningDiagnosticSeverity.TARGET_PLANNING_DIAGNOSTIC_SEVERITY_ERROR
    );

    private final AnalysisJobApplicationService applicationService;
    private final InstrumentationTargetPlanningApplicationService targetPlanningService;
    private final AnalysisJobRequestValidator validator;

    public AnalysisJobGrpcEndpoint(AnalysisJobApplicationService applicationService) {
        this(
            applicationService,
            new InstrumentationTargetPlanningApplicationService(applicationService),
            new AnalysisJobRequestValidator()
        );
    }

    public AnalysisJobGrpcEndpoint(
        AnalysisJobApplicationService applicationService,
        InstrumentationTargetPlanningApplicationService targetPlanningService
    ) {
        this(applicationService, targetPlanningService, new AnalysisJobRequestValidator());
    }

    AnalysisJobGrpcEndpoint(
        AnalysisJobApplicationService applicationService,
        InstrumentationTargetPlanningApplicationService targetPlanningService,
        AnalysisJobRequestValidator validator
    ) {
        this.applicationService = Objects.requireNonNull(applicationService, "applicationService must not be null");
        this.targetPlanningService = Objects.requireNonNull(targetPlanningService, "targetPlanningService must not be null");
        this.validator = Objects.requireNonNull(validator, "validator must not be null");
    }

    @Override
    public void submitAnalysisJob(
        SubmitAnalysisJobRequest request,
        StreamObserver<SubmitAnalysisJobResponse> responseObserver
    ) {
        try {
            validator.validate(request);
            var result = applicationService.submit(
                request.getIdempotencyKey(),
                request.getCorrelationId(),
                runId(request.getAnalysisRunId()),
                jobId(request.getJobId()),
                request.getSchemaVersion(),
                workerKind(request.getWorkerKind()),
                snapshotId(request.getSourceSnapshotId()),
                artifacts(request.getInputArtifactsList()),
                completeness(request.getInputCompleteness()),
                request.getAttributesMap()
            );
            responseObserver.onNext(SubmitAnalysisJobResponse.newBuilder()
                .setJob(toProto(result.job()))
                .setStatus(status(result.status()))
                .build());
            responseObserver.onCompleted();
        } catch (RuntimeException error) {
            responseObserver.onError(toStatus(error).asRuntimeException());
        }
    }

    @Override
    public void planInstrumentationTargets(
        PlanInstrumentationTargetsRequest request,
        StreamObserver<PlanInstrumentationTargetsResponse> responseObserver
    ) {
        try {
            validator.validate(request);
            var result = targetPlanningService.plan(
                request.getIdempotencyKey(),
                targetPlanningCommand(request)
            );
            responseObserver.onNext(targetPlanningResponse(result));
            responseObserver.onCompleted();
        } catch (RuntimeException error) {
            responseObserver.onError(toStatus(error).asRuntimeException());
        }
    }

    @Override
    public void getAnalysisJob(GetAnalysisJobRequest request, StreamObserver<de.burger.forensics.analytics.analysisjob.v1.AnalysisJob> responseObserver) {
        try {
            validator.validate(request);
            responseObserver.onNext(toProto(applicationService.get(jobId(request.getJobId()))));
            responseObserver.onCompleted();
        } catch (RuntimeException error) {
            responseObserver.onError(toStatus(error).asRuntimeException());
        }
    }

    @Override
    public void listAnalysisJobs(ListAnalysisJobsRequest request, StreamObserver<ListAnalysisJobsResponse> responseObserver) {
        try {
            validator.validate(request);
            var jobs = applicationService.list(
                request.hasAnalysisRunId() ? runId(request.getAnalysisRunId()) : null,
                optionalWorkerKind(request.getWorkerKind()),
                optionalState(request.getState())
            );
            var page = page(jobs, request);
            var response = ListAnalysisJobsResponse.newBuilder();
            page.jobs().forEach(job -> response.addJobs(toProto(job)));
            response.setNextPageToken(page.nextPageToken());
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        } catch (RuntimeException error) {
            responseObserver.onError(toStatus(error).asRuntimeException());
        }
    }

    @Override
    public void leaseAnalysisJob(LeaseAnalysisJobRequest request, StreamObserver<LeaseAnalysisJobResponse> responseObserver) {
        try {
            validator.validate(request);
            var result = applicationService.lease(
                request.getIdempotencyKey(),
                request.getCorrelationId(),
                request.getWorkerId(),
                workerKind(request.getWorkerKind()),
                request.getLeaseSeconds(),
                request.getMaxJobs()
            );
            var response = LeaseAnalysisJobResponse.newBuilder()
                .setStatus(status(result.status()));
            result.jobs().forEach(job -> response.addJobs(toProto(job)));
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        } catch (RuntimeException error) {
            responseObserver.onError(toStatus(error).asRuntimeException());
        }
    }

    @Override
    public void reportAnalysisJobProgress(ReportAnalysisJobProgressRequest request, StreamObserver<de.burger.forensics.analytics.analysisjob.v1.AnalysisJob> responseObserver) {
        try {
            validator.validate(request);
            responseObserver.onNext(toProto(applicationService.progress(
                request.getIdempotencyKey(),
                request.getCorrelationId(),
                jobId(request.getJobId()),
                request.getAttempt(),
                request.getWorkerId(),
                request.getPercentComplete(),
                request.getDiagnosticsList()
            )));
            responseObserver.onCompleted();
        } catch (RuntimeException error) {
            responseObserver.onError(toStatus(error).asRuntimeException());
        }
    }

    @Override
    public void completeAnalysisJob(CompleteAnalysisJobRequest request, StreamObserver<de.burger.forensics.analytics.analysisjob.v1.AnalysisJob> responseObserver) {
        try {
            validator.validate(request);
            responseObserver.onNext(toProto(applicationService.complete(
                request.getIdempotencyKey(),
                request.getCorrelationId(),
                jobId(request.getJobId()),
                request.getAttempt(),
                request.getWorkerId(),
                artifacts(request.getOutputArtifactsList()),
                completeness(request.getOutputCompleteness()),
                request.getDiagnosticsList()
            )));
            responseObserver.onCompleted();
        } catch (RuntimeException error) {
            responseObserver.onError(toStatus(error).asRuntimeException());
        }
    }

    @Override
    public void failAnalysisJob(FailAnalysisJobRequest request, StreamObserver<de.burger.forensics.analytics.analysisjob.v1.AnalysisJob> responseObserver) {
        try {
            validator.validate(request);
            responseObserver.onNext(toProto(applicationService.fail(
                request.getIdempotencyKey(),
                request.getCorrelationId(),
                jobId(request.getJobId()),
                request.getAttempt(),
                request.getWorkerId(),
                request.getReason(),
                request.getDiagnosticsList(),
                completeness(request.getCompleteness()),
                request.getRetryable()
            )));
            responseObserver.onCompleted();
        } catch (RuntimeException error) {
            responseObserver.onError(toStatus(error).asRuntimeException());
        }
    }

    @Override
    public void registerAnalysisArtifacts(
        RegisterAnalysisArtifactsRequest request,
        StreamObserver<RegisterAnalysisArtifactsResponse> responseObserver
    ) {
        try {
            validator.validate(request);
            var result = applicationService.registerArtifacts(
                request.getIdempotencyKey(),
                request.getCorrelationId(),
                runId(request.getAnalysisRunId()),
                jobId(request.getJobId()),
                artifacts(request.getArtifactsList())
            );
            var response = RegisterAnalysisArtifactsResponse.newBuilder()
                .setStatus(status(result.status()));
            result.artifacts().forEach(artifact -> response.addArtifacts(toProto(artifact)));
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        } catch (RuntimeException error) {
            responseObserver.onError(toStatus(error).asRuntimeException());
        }
    }

    private static Status toStatus(RuntimeException error) {
        if (error instanceof ValidationException || error instanceof IllegalArgumentException) {
            return Status.INVALID_ARGUMENT.withDescription(error.getMessage());
        }
        if (error instanceof AnalysisJobNotFoundException) {
            return Status.NOT_FOUND.withDescription(error.getMessage());
        }
        if (error instanceof IdempotencyConflictException) {
            return Status.ALREADY_EXISTS.withDescription(error.getMessage());
        }
        if (error instanceof IllegalStateException) {
            return Status.FAILED_PRECONDITION.withDescription(error.getMessage());
        }
        return Status.INTERNAL.withDescription("Unexpected analysis store failure");
    }

    private static AnalysisRunId runId(de.burger.forensics.analytics.analysisjob.v1.AnalysisRunId id) {
        return new AnalysisRunId(id.getValue());
    }

    private static AnalysisJobId jobId(de.burger.forensics.analytics.analysisjob.v1.AnalysisJobId id) {
        return new AnalysisJobId(id.getValue());
    }

    private static SourceSnapshotId snapshotId(de.burger.forensics.analytics.analysisjob.v1.SourceSnapshotId id) {
        return new SourceSnapshotId(id.getValue());
    }

    private static List<AnalysisArtifactReference> artifacts(List<de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactReference> references) {
        return references.stream()
            .map(AnalysisJobGrpcEndpoint::artifact)
            .toList();
    }

    private static InstrumentationTargetPlanningDomain.PlanInstrumentationTargetsCommand targetPlanningCommand(
        PlanInstrumentationTargetsRequest request
    ) {
        return new InstrumentationTargetPlanningDomain.PlanInstrumentationTargetsCommand(
            new InstrumentationTargetPlanningDomain.TargetPlanningMetadata(
                request.getRequestId(),
                request.getSchemaVersion(),
                request.getCorrelationId(),
                runId(request.getAnalysisRunId()),
                jobId(request.getAnalysisJobId()),
                snapshotId(request.getSourceSnapshotId()),
                request.getAttributesMap()
            ),
            request.getPolicyVersion(),
            targetPolicy(request.getPolicy()),
            request.getStaticFactsList().stream()
                .map(AnalysisJobGrpcEndpoint::staticFact)
                .toList(),
            artifacts(request.getSourceFactArtifactsList()),
            artifacts(request.getSemanticArtifactsList())
        );
    }

    private static InstrumentationTargetPlanningDomain.InstrumentationTargetPolicy targetPolicy(
        de.burger.forensics.analytics.analysisjob.v1.InstrumentationTargetPolicy policy
    ) {
        return new InstrumentationTargetPlanningDomain.InstrumentationTargetPolicy(
            policy.getMaxTargets(),
            policy.getProbeKindsList().stream()
                .map(AnalysisJobGrpcEndpoint::probeKind)
                .toList(),
            policy.getRequireSemanticArtifacts(),
            policy.getSensitivity()
        );
    }

    private static InstrumentationTargetPlanningDomain.AcceptedStaticSourceFact staticFact(
        de.burger.forensics.analytics.analysisjob.v1.AcceptedStaticSourceFact fact
    ) {
        return new InstrumentationTargetPlanningDomain.AcceptedStaticSourceFact(
            fact.getFactId(),
            fact.getFactType(),
            new InstrumentationTargetPlanningDomain.StaticSourceLocation(
                fact.getLocation().getSourcePath(),
                fact.getLocation().getFullyQualifiedClassName(),
                fact.getLocation().getMethodName(),
                fact.getLocation().getLineNumber(),
                fact.getLocation().getColumnNumber()
            ),
            fact.getSignature(),
            fact.getSourceFactArtifactReference(),
            completeness(fact.getCompleteness())
        );
    }

    private static Page page(List<AnalysisJob> jobs, ListAnalysisJobsRequest request) {
        var offset = pageOffset(request.getPageToken());
        if (offset >= jobs.size()) {
            return new Page(List.of(), "");
        }
        if (request.getPageSize() == 0) {
            return new Page(jobs.stream().skip(offset).toList(), "");
        }
        var end = Math.min(offset + request.getPageSize(), jobs.size());
        return new Page(jobs.subList(offset, end), end < jobs.size() ? Integer.toString(end) : "");
    }

    private static int pageOffset(String pageToken) {
        if (pageToken == null || pageToken.isBlank()) {
            return 0;
        }
        try {
            var offset = Integer.parseInt(pageToken.strip());
            if (offset < 0) {
                throw new ValidationException("pageToken must be a non-negative offset");
            }
            return offset;
        } catch (NumberFormatException error) {
            throw new ValidationException("pageToken must be a non-negative offset");
        }
    }

    private static AnalysisArtifactReference artifact(de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactReference reference) {
        return new AnalysisArtifactReference(
            new ArtifactReference(
                reference.getArtifact().getPath(),
                reference.getArtifact().getType(),
                reference.getArtifact().getSha256(),
                reference.getArtifact().getSizeBytes()
            ),
            category(reference.getCategory()),
            reference.getProducerService(),
            reference.getSchemaVersion(),
            completeness(reference.getCompleteness()),
            byteAccess(reference.getByteAccess())
        );
    }

    private static de.burger.forensics.analytics.analysisjob.v1.AnalysisJob toProto(AnalysisJob job) {
        var builder = de.burger.forensics.analytics.analysisjob.v1.AnalysisJob.newBuilder()
            .setAnalysisRunId(de.burger.forensics.analytics.analysisjob.v1.AnalysisRunId.newBuilder()
                .setValue(job.analysisRunId().value()))
            .setJobId(de.burger.forensics.analytics.analysisjob.v1.AnalysisJobId.newBuilder()
                .setValue(job.jobId().value()))
            .setWorkerKind(toProto(job.workerKind()))
            .setSourceSnapshotId(de.burger.forensics.analytics.analysisjob.v1.SourceSnapshotId.newBuilder()
                .setValue(job.sourceSnapshotId().value()))
            .setCompleteness(toProto(job.completeness()))
            .setState(toProto(job.state()))
            .setAttempt(job.attempt())
            .setPercentComplete(job.percentComplete())
            .setLeaseOwner(job.leaseOwner())
            .setLeaseExpiresAt(job.leaseExpiresAt() == null ? "" : job.leaseExpiresAt().toString())
            .setCreatedAt(job.createdAt().toString())
            .setUpdatedAt(job.updatedAt().toString())
            .addAllDiagnostics(job.diagnostics())
            .setSchemaVersion(job.schemaVersion())
            .setCorrelationId(job.correlationId())
            .putAllAttributes(job.attributes());
        job.inputArtifacts().forEach(artifact -> builder.addInputArtifacts(toProto(artifact)));
        job.outputArtifacts().forEach(artifact -> builder.addOutputArtifacts(toProto(artifact)));
        job.failures().forEach(failure -> builder.addFailures(de.burger.forensics.analytics.analysisjob.v1.AnalysisJobFailure.newBuilder()
            .setJobId(de.burger.forensics.analytics.analysisjob.v1.AnalysisJobId.newBuilder()
                .setValue(failure.jobId().value()))
            .setWorkerKind(toProto(failure.workerKind()))
            .setAttempt(failure.attempt())
            .setReason(failure.reason())
            .addAllDiagnostics(failure.diagnostics())
            .setCompleteness(toProto(failure.completeness()))
            .setRetryable(failure.retryable())));
        return builder.build();
    }

    private static PlanInstrumentationTargetsResponse targetPlanningResponse(
        InstrumentationTargetPlanningDomain.PlanInstrumentationTargetsResult result
    ) {
        var response = PlanInstrumentationTargetsResponse.newBuilder()
            .setStatus(targetPlanningStatus(result))
            .setAnalysisRunId(de.burger.forensics.analytics.analysisjob.v1.AnalysisRunId.newBuilder()
                .setValue(result.metadata().analysisRunId().value()))
            .setAnalysisJobId(de.burger.forensics.analytics.analysisjob.v1.AnalysisJobId.newBuilder()
                .setValue(result.metadata().analysisJobId().value()))
            .setSourceSnapshotId(de.burger.forensics.analytics.analysisjob.v1.SourceSnapshotId.newBuilder()
                .setValue(result.metadata().sourceSnapshotId().value()))
            .setCompleteness(toProto(result.completeness()))
            .setTargetSelection(targetSelection(result.selection()))
            .putAllAttributes(result.metadata().attributes());
        result.targets().forEach(target -> response.addTargets(target(target)));
        result.diagnostics().forEach(diagnostic -> response.addDiagnostics(diagnostic(diagnostic)));
        return response.build();
    }

    private static OperationStatus targetPlanningStatus(
        InstrumentationTargetPlanningDomain.PlanInstrumentationTargetsResult result
    ) {
        var status = OperationStatus.newBuilder()
            .setCode(result.completeness() == AnalysisCompleteness.COMPLETE ? "TARGETS_PLANNED" : "TARGETS_PLANNED_INCOMPLETE")
            .setMessage("Instrumentation target planning completed")
            .setRetryable(false)
            .setCorrelationId(result.metadata().correlationId());
        result.diagnostics().stream()
            .map(InstrumentationTargetPlanningDomain.TargetPlanningDiagnostic::code)
            .forEach(status::addDiagnostics);
        return status.build();
    }

    private static de.burger.forensics.analytics.analysisjob.v1.InstrumentationTargetSelection targetSelection(
        InstrumentationTargetPlanningDomain.InstrumentationTargetSelection selection
    ) {
        return de.burger.forensics.analytics.analysisjob.v1.InstrumentationTargetSelection.newBuilder()
            .setSelectionId(selection.selectionId())
            .setOwnerService(selection.ownerService())
            .setPolicyVersion(selection.policyVersion())
            .setSelectionFingerprint(selection.selectionFingerprint())
            .setCompleteness(toProto(selection.completeness()))
            .setDeterministicOrder(selection.deterministicOrder())
            .setCorrelationId(selection.correlationId())
            .setTargetCount(selection.targetCount())
            .build();
    }

    private static de.burger.forensics.analytics.analysisjob.v1.InstrumentationTarget target(
        InstrumentationTargetPlanningDomain.InstrumentationTarget target
    ) {
        return de.burger.forensics.analytics.analysisjob.v1.InstrumentationTarget.newBuilder()
            .setTargetId(target.targetId())
            .setSourceFactId(target.sourceFactId())
            .setSemanticNodeId(target.semanticNodeId())
            .setRelativePath(target.relativePath())
            .setFullyQualifiedClassName(target.fullyQualifiedClassName())
            .setMethodName(target.methodName())
            .setSignature(target.signature())
            .setLineNumber(target.lineNumber())
            .setProbeKind(PROBE_KIND_PROTOS.get(target.probeKind()))
            .setSourceFactArtifactReference(target.sourceFactArtifactReference())
            .setSemanticArtifactReference(target.semanticArtifactReference())
            .setOrderIndex(target.orderIndex())
            .setCompleteness(toProto(target.completeness()))
            .setSensitivity(target.sensitivity())
            .build();
    }

    private static de.burger.forensics.analytics.analysisjob.v1.TargetPlanningDiagnostic diagnostic(
        InstrumentationTargetPlanningDomain.TargetPlanningDiagnostic diagnostic
    ) {
        return de.burger.forensics.analytics.analysisjob.v1.TargetPlanningDiagnostic.newBuilder()
            .setCode(diagnostic.code())
            .setMessage(diagnostic.message())
            .setSeverity(TARGET_DIAGNOSTIC_SEVERITIES.get(diagnostic.severity()))
            .setSourceSnapshotId(diagnostic.sourceSnapshotId().value())
            .setSourceFactId(diagnostic.sourceFactId())
            .setArtifactPath(diagnostic.artifactPath())
            .setRetryable(diagnostic.retryable())
            .setAffectsCompleteness(diagnostic.affectsCompleteness())
            .build();
    }

    private static de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactReference toProto(AnalysisArtifactReference reference) {
        return de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactReference.newBuilder()
            .setArtifact(de.burger.forensics.analytics.analysisjob.v1.ArtifactReference.newBuilder()
                .setPath(reference.artifact().path())
                .setType(reference.artifact().type())
                .setSha256(reference.artifact().sha256())
                .setSizeBytes(reference.artifact().sizeBytes()))
            .setCategory(toProto(reference.category()))
            .setProducerService(reference.producerService())
            .setSchemaVersion(reference.schemaVersion())
            .setCompleteness(toProto(reference.completeness()))
            .setByteAccess(toProto(reference.byteAccess()))
            .build();
    }

    private static OperationStatus status(de.burger.forensics.analytics.services.analysisstore.application.result.OperationOutcome outcome) {
        return OperationStatus.newBuilder()
            .setCode(outcome.code())
            .setMessage(outcome.message())
            .setRetryable(outcome.retryable())
            .setCorrelationId(outcome.correlationId())
            .addAllDiagnostics(outcome.diagnostics())
            .build();
    }

    private static AnalysisWorkerKind workerKind(de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind kind) {
        return required(WORKER_KINDS.get(kind), "workerKind must be specified");
    }

    private static AnalysisWorkerKind optionalWorkerKind(de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind kind) {
        return kind == de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind.ANALYSIS_WORKER_KIND_UNSPECIFIED
            ? null
            : workerKind(kind);
    }

    private static AnalysisJobState optionalState(de.burger.forensics.analytics.analysisjob.v1.AnalysisJobState state) {
        return state == de.burger.forensics.analytics.analysisjob.v1.AnalysisJobState.ANALYSIS_JOB_STATE_UNSPECIFIED
            ? null
            : state(state);
    }

    private static AnalysisJobState state(de.burger.forensics.analytics.analysisjob.v1.AnalysisJobState state) {
        return required(JOB_STATES.get(state), "state must be specified");
    }

    private static AnalysisCompleteness completeness(de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness completeness) {
        return required(COMPLETENESS.get(completeness), "completeness must be specified");
    }

    private static de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactCategory category(
        de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory category
    ) {
        return required(CATEGORIES.get(category), "artifact.category must be specified");
    }

    private static InstrumentationTargetPlanningDomain.ProbeKind probeKind(
        de.burger.forensics.analytics.analysisjob.v1.InstrumentationProbeKind probeKind
    ) {
        return required(PROBE_KINDS.get(probeKind), "policy.probeKinds must contain supported probe kinds");
    }

    private static ArtifactByteAccess byteAccess(de.burger.forensics.analytics.analysisjob.v1.ArtifactByteAccess byteAccess) {
        return new ArtifactByteAccess(
            byteAccess.getOwnerService(),
            byteAccess.getRetrievalContract(),
            byteAccess.getRetrievalReference(),
            required(BYTE_CUSTODIES.get(byteAccess.getByteCustody()), "artifact.byteAccess.byteCustody must be specified")
        );
    }

    private static de.burger.forensics.analytics.analysisjob.v1.ArtifactByteAccess toProto(ArtifactByteAccess byteAccess) {
        return de.burger.forensics.analytics.analysisjob.v1.ArtifactByteAccess.newBuilder()
            .setOwnerService(byteAccess.ownerService())
            .setRetrievalContract(byteAccess.retrievalContract())
            .setRetrievalReference(byteAccess.retrievalReference())
            .setByteCustody(BYTE_CUSTODY_PROTOS.get(byteAccess.byteCustody()))
            .build();
    }

    private static de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind toProto(AnalysisWorkerKind kind) {
        return WORKER_KIND_PROTOS.get(kind);
    }

    private static de.burger.forensics.analytics.analysisjob.v1.AnalysisJobState toProto(AnalysisJobState state) {
        return JOB_STATE_PROTOS.get(state);
    }

    private static de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness toProto(AnalysisCompleteness completeness) {
        return COMPLETENESS_PROTOS.get(completeness);
    }

    private static de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory toProto(
        de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactCategory category
    ) {
        return CATEGORY_PROTOS.get(category);
    }

    private static <T> T required(T value, String message) {
        if (value == null) {
            throw new ValidationException(message);
        }
        return value;
    }

    private record Page(List<AnalysisJob> jobs, String nextPageToken) {
    }
}
