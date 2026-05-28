package de.burger.forensics.analytics.services.analysisorchestrator.adapter.in.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobServiceGrpc;
import de.burger.forensics.analytics.analysisjob.v1.CompleteAnalysisJobRequest;
import de.burger.forensics.analytics.analysisjob.v1.FailAnalysisJobRequest;
import de.burger.forensics.analytics.analysisjob.v1.GetAnalysisJobRequest;
import de.burger.forensics.analytics.analysisjob.v1.GetRepositoryToBtmStatusRequest;
import de.burger.forensics.analytics.analysisjob.v1.LeaseAnalysisJobRequest;
import de.burger.forensics.analytics.analysisjob.v1.LeaseAnalysisJobResponse;
import de.burger.forensics.analytics.analysisjob.v1.ListAnalysisJobsRequest;
import de.burger.forensics.analytics.analysisjob.v1.ListAnalysisJobsResponse;
import de.burger.forensics.analytics.analysisjob.v1.OperationStatus;
import de.burger.forensics.analytics.analysisjob.v1.PlanInstrumentationTargetsRequest;
import de.burger.forensics.analytics.analysisjob.v1.PlanInstrumentationTargetsResponse;
import de.burger.forensics.analytics.analysisjob.v1.RegisterAnalysisArtifactsRequest;
import de.burger.forensics.analytics.analysisjob.v1.RegisterAnalysisArtifactsResponse;
import de.burger.forensics.analytics.analysisjob.v1.RepositoryToBtmOrchestrationStatus;
import de.burger.forensics.analytics.analysisjob.v1.ReportAnalysisJobProgressRequest;
import de.burger.forensics.analytics.analysisjob.v1.StartRepositoryToBtmRequest;
import de.burger.forensics.analytics.analysisjob.v1.SubmitAnalysisJobRequest;
import de.burger.forensics.analytics.analysisjob.v1.SubmitAnalysisJobResponse;
import de.burger.forensics.analytics.services.analysisorchestrator.application.AnalysisJobApplicationService;
import de.burger.forensics.analytics.services.analysisorchestrator.application.AnalysisJobNotFoundException;
import de.burger.forensics.analytics.services.analysisorchestrator.application.IdempotencyConflictException;
import de.burger.forensics.analytics.services.analysisorchestrator.application.RepositoryToBtmOrchestrationApplicationService;
import de.burger.forensics.analytics.services.analysisorchestrator.application.RepositoryToBtmOrchestrationApplicationService.RepositoryToBtmStartCommand;
import de.burger.forensics.analytics.services.analysisorchestrator.application.RepositoryToBtmOrchestrationConflictException;
import de.burger.forensics.analytics.services.analysisorchestrator.application.RepositoryToBtmOrchestrationNotFoundException;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisJob;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisJobId;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisJobState;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisRunId;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisWorkerKind;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.ArtifactByteAccess;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.ArtifactByteCustody;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.ArtifactReference;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.SourceSnapshotId;
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
    private static final Map<de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory, de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisArtifactCategory> CATEGORIES = Map.of(
        de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC,
        de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisArtifactCategory.STATIC,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_RUNTIME,
        de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisArtifactCategory.RUNTIME,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_PROJECTION,
        de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisArtifactCategory.PROJECTION,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_GENERATED,
        de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisArtifactCategory.GENERATED
    );
    private static final Map<de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisArtifactCategory, de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory> CATEGORY_PROTOS = Map.of(
        de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisArtifactCategory.STATIC,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC,
        de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisArtifactCategory.RUNTIME,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_RUNTIME,
        de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisArtifactCategory.PROJECTION,
        de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_PROJECTION,
        de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisArtifactCategory.GENERATED,
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

    private final AnalysisJobApplicationService applicationService;
    private final RepositoryToBtmOrchestrationApplicationService repositoryToBtmService;
    private final AnalysisJobRequestValidator validator;

    public AnalysisJobGrpcEndpoint(
        AnalysisJobApplicationService applicationService,
        RepositoryToBtmOrchestrationApplicationService repositoryToBtmService
    ) {
        this(applicationService, repositoryToBtmService, new AnalysisJobRequestValidator());
    }

    AnalysisJobGrpcEndpoint(
        AnalysisJobApplicationService applicationService,
        RepositoryToBtmOrchestrationApplicationService repositoryToBtmService,
        AnalysisJobRequestValidator validator
    ) {
        this.applicationService = Objects.requireNonNull(applicationService, "applicationService must not be null");
        this.repositoryToBtmService = Objects.requireNonNull(repositoryToBtmService, "repositoryToBtmService must not be null");
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
            responseObserver.onNext(ListAnalysisJobsResponse.newBuilder()
                .addAllJobs(page.jobs().stream().map(AnalysisJobGrpcEndpoint::toProto).toList())
                .setNextPageToken(page.nextPageToken())
                .build());
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
            responseObserver.onNext(LeaseAnalysisJobResponse.newBuilder()
                .addAllJobs(result.jobs().stream().map(AnalysisJobGrpcEndpoint::toProto).toList())
                .setStatus(status(result.status()))
                .build());
            responseObserver.onCompleted();
        } catch (RuntimeException error) {
            responseObserver.onError(toStatus(error).asRuntimeException());
        }
    }

    @Override
    public void reportAnalysisJobProgress(
        ReportAnalysisJobProgressRequest request,
        StreamObserver<de.burger.forensics.analytics.analysisjob.v1.AnalysisJob> responseObserver
    ) {
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
    public void completeAnalysisJob(
        CompleteAnalysisJobRequest request,
        StreamObserver<de.burger.forensics.analytics.analysisjob.v1.AnalysisJob> responseObserver
    ) {
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
    public void failAnalysisJob(
        FailAnalysisJobRequest request,
        StreamObserver<de.burger.forensics.analytics.analysisjob.v1.AnalysisJob> responseObserver
    ) {
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
            responseObserver.onNext(RegisterAnalysisArtifactsResponse.newBuilder()
                .addAllArtifacts(result.artifacts().stream().map(AnalysisJobGrpcEndpoint::toProto).toList())
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
        responseObserver.onError(unsupported("instrumentation target planning is not owned by analysis-orchestrator-service").asRuntimeException());
    }

    @Override
    public void startRepositoryToBtm(
        StartRepositoryToBtmRequest request,
        StreamObserver<RepositoryToBtmOrchestrationStatus> responseObserver
    ) {
        try {
            validator.validate(request);
            responseObserver.onNext(toProto(repositoryToBtmService.start(startCommand(request))));
            responseObserver.onCompleted();
        } catch (RuntimeException error) {
            responseObserver.onError(toStatus(error).asRuntimeException());
        }
    }

    @Override
    public void getRepositoryToBtmStatus(
        GetRepositoryToBtmStatusRequest request,
        StreamObserver<RepositoryToBtmOrchestrationStatus> responseObserver
    ) {
        try {
            validator.validate(request);
            var status = repositoryToBtmService.get(runId(request.getAnalysisRunId()));
            if (status == null) {
                throw new RepositoryToBtmOrchestrationNotFoundException(request.getAnalysisRunId().getValue());
            }
            responseObserver.onNext(toProto(status));
            responseObserver.onCompleted();
        } catch (RuntimeException error) {
            responseObserver.onError(toStatus(error).asRuntimeException());
        }
    }

    private static Status unsupported(String description) {
        return Status.UNIMPLEMENTED.withDescription(description);
    }

    private static Status toStatus(RuntimeException error) {
        return switch (error) {
            case ValidationException validation -> Status.INVALID_ARGUMENT.withDescription(validation.getMessage());
            case IllegalArgumentException illegalArgument -> Status.INVALID_ARGUMENT.withDescription(illegalArgument.getMessage());
            case IllegalStateException illegalState -> Status.FAILED_PRECONDITION.withDescription(illegalState.getMessage());
            case AnalysisJobNotFoundException notFound -> Status.NOT_FOUND.withDescription(notFound.getMessage());
            case RepositoryToBtmOrchestrationNotFoundException notFound -> Status.NOT_FOUND.withDescription(notFound.getMessage());
            case IdempotencyConflictException conflict -> Status.ALREADY_EXISTS.withDescription(conflict.getMessage());
            case RepositoryToBtmOrchestrationConflictException conflict -> Status.ALREADY_EXISTS.withDescription(conflict.getMessage());
            default -> Status.INTERNAL.withDescription("analysis orchestration request failed");
        };
    }

    private static RepositoryToBtmStartCommand startCommand(StartRepositoryToBtmRequest request) {
        return new RepositoryToBtmStartCommand(
            request.getIdempotencyKey(),
            request.getCorrelationId(),
            request.getSchemaVersion(),
            runId(request.getAnalysisRunId()),
            request.getRepository().getRemoteUrl(),
            request.getRepository().getProvider(),
            request.getRevision().getBranch(),
            request.getRevision().getCommit(),
            request.getWorkspacePolicy().getTimeoutSeconds(),
            request.getWorkspacePolicy().getMaxWorkspaceBytes(),
            request.getBuildContext().getBuildTool(),
            request.getBuildContext().getBuildId(),
            request.getBuildContext().getRootProjectName(),
            request.getBuildContext().getDeclaredModulesList(),
            request.getRequestedOutputsList().stream().map(Enum::name).toList(),
            request.getAttributesMap()
        );
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
        return references.stream().map(AnalysisJobGrpcEndpoint::artifact).toList();
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

    private static Page page(List<AnalysisJob> jobs, ListAnalysisJobsRequest request) {
        var offset = pageOffset(request.getPageToken());
        var pageSize = request.getPageSize() <= 0 ? jobs.size() : request.getPageSize();
        var pageItems = jobs.stream()
            .skip(offset)
            .limit(pageSize)
            .toList();
        var nextOffset = offset + pageItems.size();
        var nextToken = nextOffset < jobs.size() ? Integer.toString(nextOffset) : "";
        return new Page(pageItems, nextToken);
    }

    private static int pageOffset(String pageToken) {
        if (pageToken == null || pageToken.isBlank()) {
            return 0;
        }
        try {
            var offset = Integer.parseInt(pageToken);
            if (offset < 0) {
                throw new ValidationException("pageToken must not be negative");
            }
            return offset;
        } catch (NumberFormatException error) {
            throw new ValidationException("pageToken must be an integer offset");
        }
    }

    private static de.burger.forensics.analytics.analysisjob.v1.AnalysisJob toProto(AnalysisJob job) {
        var builder = de.burger.forensics.analytics.analysisjob.v1.AnalysisJob.newBuilder()
            .setAnalysisRunId(de.burger.forensics.analytics.analysisjob.v1.AnalysisRunId.newBuilder()
                .setValue(job.analysisRunId().value()))
            .setJobId(de.burger.forensics.analytics.analysisjob.v1.AnalysisJobId.newBuilder()
                .setValue(job.jobId().value()))
            .setSchemaVersion(job.schemaVersion())
            .setCorrelationId(job.correlationId())
            .setWorkerKind(toProto(job.workerKind()))
            .setSourceSnapshotId(de.burger.forensics.analytics.analysisjob.v1.SourceSnapshotId.newBuilder()
                .setValue(job.sourceSnapshotId().value()))
            .addAllInputArtifacts(job.inputArtifacts().stream().map(AnalysisJobGrpcEndpoint::toProto).toList())
            .addAllOutputArtifacts(job.outputArtifacts().stream().map(AnalysisJobGrpcEndpoint::toProto).toList())
            .setCompleteness(toProto(job.completeness()))
            .setState(toProto(job.state()))
            .setAttempt(job.attempt())
            .setPercentComplete(job.percentComplete())
            .addAllDiagnostics(job.diagnostics())
            .putAllAttributes(job.attributes())
            .setCreatedAt(job.createdAt().toString())
            .setUpdatedAt(job.updatedAt().toString());
        if (!job.leaseOwner().isBlank()) {
            builder.setLeaseOwner(job.leaseOwner());
        }
        if (job.leaseExpiresAt() != null) {
            builder.setLeaseExpiresAt(job.leaseExpiresAt().toString());
        }
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

    private static RepositoryToBtmOrchestrationStatus toProto(
        de.burger.forensics.analytics.services.analysisorchestrator.domain.RepositoryToBtmOrchestrationStatus status
    ) {
        var builder = RepositoryToBtmOrchestrationStatus.newBuilder()
            .setStatus(OperationStatus.newBuilder()
                .setCode("REPOSITORY_TO_BTM_WAITING_FOR_REPOSITORY")
                .setMessage("Repository-to-BTM orchestration accepted; waiting for repository source handoff")
                .setRetryable(false)
                .setCorrelationId(status.correlationId())
                .addAllDiagnostics(status.diagnostics().stream()
                    .map(de.burger.forensics.analytics.services.analysisorchestrator.domain.RepositoryToBtmDiagnostic::code)
                    .toList()))
            .setAnalysisRunId(de.burger.forensics.analytics.analysisjob.v1.AnalysisRunId.newBuilder()
                .setValue(status.analysisRunId().value()))
            .setRepositoryAnalysisJobId(de.burger.forensics.analytics.analysisjob.v1.AnalysisJobId.newBuilder()
                .setValue(status.repositoryAnalysisJobId().value()))
            .setCompleteness(toProto(status.completeness()))
            .setState(toProto(status.state()))
            .setBtmDeliveryReadiness(toProto(status.btmDeliveryReadiness()))
            .setJoernSkipped(status.joernSkipped())
            .addAllDiagnostics(status.diagnostics().stream().map(AnalysisJobGrpcEndpoint::toProto).toList())
            .putAllAttributes(status.attributes());
        if (!status.sourceSnapshotId().isBlank()) {
            builder.setSourceSnapshotId(de.burger.forensics.analytics.analysisjob.v1.SourceSnapshotId.newBuilder()
                .setValue(status.sourceSnapshotId()));
        }
        return builder.build();
    }

    private static de.burger.forensics.analytics.analysisjob.v1.RepositoryToBtmDiagnostic toProto(
        de.burger.forensics.analytics.services.analysisorchestrator.domain.RepositoryToBtmDiagnostic diagnostic
    ) {
        return de.burger.forensics.analytics.analysisjob.v1.RepositoryToBtmDiagnostic.newBuilder()
            .setCode(diagnostic.code())
            .setMessage(diagnostic.message())
            .setSeverity(toProto(diagnostic.severity()))
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

    private static OperationStatus status(de.burger.forensics.analytics.services.analysisorchestrator.application.result.OperationOutcome outcome) {
        return OperationStatus.newBuilder()
            .setCode(outcome.code())
            .setMessage(outcome.message())
            .setRetryable(outcome.retryable())
            .setCorrelationId(outcome.correlationId())
            .addAllDiagnostics(outcome.diagnostics())
            .build();
    }

    private static AnalysisWorkerKind workerKind(de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind kind) {
        return required(WORKER_KINDS.get(kind), "unsupported worker kind");
    }

    private static AnalysisWorkerKind optionalWorkerKind(de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind kind) {
        if (kind == de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind.ANALYSIS_WORKER_KIND_UNSPECIFIED
            || kind == de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind.UNRECOGNIZED) {
            return null;
        }
        return workerKind(kind);
    }

    private static AnalysisJobState optionalState(de.burger.forensics.analytics.analysisjob.v1.AnalysisJobState state) {
        if (state == de.burger.forensics.analytics.analysisjob.v1.AnalysisJobState.ANALYSIS_JOB_STATE_UNSPECIFIED
            || state == de.burger.forensics.analytics.analysisjob.v1.AnalysisJobState.UNRECOGNIZED) {
            return null;
        }
        return required(JOB_STATES.get(state), "unsupported analysis job state");
    }

    private static AnalysisCompleteness completeness(de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness completeness) {
        return required(COMPLETENESS.get(completeness), "unsupported completeness");
    }

    private static de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisArtifactCategory category(
        de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory category
    ) {
        return required(CATEGORIES.get(category), "unsupported artifact category");
    }

    private static ArtifactByteAccess byteAccess(de.burger.forensics.analytics.analysisjob.v1.ArtifactByteAccess byteAccess) {
        return new ArtifactByteAccess(
            byteAccess.getOwnerService(),
            byteAccess.getRetrievalContract(),
            byteAccess.getRetrievalReference(),
            required(BYTE_CUSTODIES.get(byteAccess.getByteCustody()), "unsupported byte custody")
        );
    }

    private static de.burger.forensics.analytics.analysisjob.v1.ArtifactByteAccess toProto(ArtifactByteAccess byteAccess) {
        return de.burger.forensics.analytics.analysisjob.v1.ArtifactByteAccess.newBuilder()
            .setOwnerService(byteAccess.ownerService())
            .setRetrievalContract(byteAccess.retrievalContract())
            .setRetrievalReference(byteAccess.retrievalReference())
            .setByteCustody(required(BYTE_CUSTODY_PROTOS.get(byteAccess.byteCustody()), "unsupported byte custody"))
            .build();
    }

    private static de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind toProto(AnalysisWorkerKind kind) {
        return required(WORKER_KIND_PROTOS.get(kind), "unsupported worker kind");
    }

    private static de.burger.forensics.analytics.analysisjob.v1.AnalysisJobState toProto(AnalysisJobState state) {
        return required(JOB_STATE_PROTOS.get(state), "unsupported analysis job state");
    }

    private static de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness toProto(AnalysisCompleteness completeness) {
        return required(COMPLETENESS_PROTOS.get(completeness), "unsupported completeness");
    }

    private static de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory toProto(
        de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisArtifactCategory category
    ) {
        return required(CATEGORY_PROTOS.get(category), "unsupported artifact category");
    }

    private static de.burger.forensics.analytics.analysisjob.v1.RepositoryToBtmOrchestrationState toProto(
        de.burger.forensics.analytics.services.analysisorchestrator.domain.RepositoryToBtmOrchestrationState state
    ) {
        return switch (state) {
            case ACCEPTED ->
                de.burger.forensics.analytics.analysisjob.v1.RepositoryToBtmOrchestrationState.REPOSITORY_TO_BTM_ORCHESTRATION_STATE_ACCEPTED;
            case WAITING_FOR_REPOSITORY ->
                de.burger.forensics.analytics.analysisjob.v1.RepositoryToBtmOrchestrationState.REPOSITORY_TO_BTM_ORCHESTRATION_STATE_WAITING_FOR_REPOSITORY;
            case READY_FOR_BTM_DELIVERY ->
                de.burger.forensics.analytics.analysisjob.v1.RepositoryToBtmOrchestrationState.REPOSITORY_TO_BTM_ORCHESTRATION_STATE_READY_FOR_BTM_DELIVERY;
            case INCOMPLETE ->
                de.burger.forensics.analytics.analysisjob.v1.RepositoryToBtmOrchestrationState.REPOSITORY_TO_BTM_ORCHESTRATION_STATE_INCOMPLETE;
            case FAILED ->
                de.burger.forensics.analytics.analysisjob.v1.RepositoryToBtmOrchestrationState.REPOSITORY_TO_BTM_ORCHESTRATION_STATE_FAILED;
        };
    }

    private static de.burger.forensics.analytics.analysisjob.v1.BtmDeliveryReadiness toProto(
        de.burger.forensics.analytics.services.analysisorchestrator.domain.BtmDeliveryReadiness readiness
    ) {
        return switch (readiness) {
            case NOT_READY -> de.burger.forensics.analytics.analysisjob.v1.BtmDeliveryReadiness.BTM_DELIVERY_READINESS_NOT_READY;
            case READY -> de.burger.forensics.analytics.analysisjob.v1.BtmDeliveryReadiness.BTM_DELIVERY_READINESS_READY;
            case UNAVAILABLE -> de.burger.forensics.analytics.analysisjob.v1.BtmDeliveryReadiness.BTM_DELIVERY_READINESS_UNAVAILABLE;
            case UNKNOWN -> de.burger.forensics.analytics.analysisjob.v1.BtmDeliveryReadiness.BTM_DELIVERY_READINESS_UNKNOWN;
        };
    }

    private static de.burger.forensics.analytics.analysisjob.v1.RepositoryToBtmDiagnosticSeverity toProto(
        de.burger.forensics.analytics.services.analysisorchestrator.domain.RepositoryToBtmDiagnosticSeverity severity
    ) {
        return switch (severity) {
            case INFO ->
                de.burger.forensics.analytics.analysisjob.v1.RepositoryToBtmDiagnosticSeverity.REPOSITORY_TO_BTM_DIAGNOSTIC_SEVERITY_INFO;
            case WARNING ->
                de.burger.forensics.analytics.analysisjob.v1.RepositoryToBtmDiagnosticSeverity.REPOSITORY_TO_BTM_DIAGNOSTIC_SEVERITY_WARNING;
            case ERROR ->
                de.burger.forensics.analytics.analysisjob.v1.RepositoryToBtmDiagnosticSeverity.REPOSITORY_TO_BTM_DIAGNOSTIC_SEVERITY_ERROR;
        };
    }

    private static <T> T required(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private record Page(List<AnalysisJob> jobs, String nextPageToken) {
    }
}
