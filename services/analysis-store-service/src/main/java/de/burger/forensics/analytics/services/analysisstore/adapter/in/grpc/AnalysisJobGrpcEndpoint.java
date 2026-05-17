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
import de.burger.forensics.analytics.analysisjob.v1.RegisterAnalysisArtifactsRequest;
import de.burger.forensics.analytics.analysisjob.v1.RegisterAnalysisArtifactsResponse;
import de.burger.forensics.analytics.analysisjob.v1.ReportAnalysisJobProgressRequest;
import de.burger.forensics.analytics.analysisjob.v1.SubmitAnalysisJobRequest;
import de.burger.forensics.analytics.analysisjob.v1.SubmitAnalysisJobResponse;
import de.burger.forensics.analytics.services.analysisstore.application.AnalysisJobApplicationService;
import de.burger.forensics.analytics.services.analysisstore.application.AnalysisJobNotFoundException;
import de.burger.forensics.analytics.services.analysisstore.application.IdempotencyConflictException;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJob;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJobId;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJobState;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisRunId;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisWorkerKind;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactReference;
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

    private final AnalysisJobApplicationService applicationService;
    private final AnalysisJobRequestValidator validator;

    public AnalysisJobGrpcEndpoint(AnalysisJobApplicationService applicationService) {
        this(applicationService, new AnalysisJobRequestValidator());
    }

    AnalysisJobGrpcEndpoint(AnalysisJobApplicationService applicationService, AnalysisJobRequestValidator validator) {
        this.applicationService = Objects.requireNonNull(applicationService, "applicationService must not be null");
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
            completeness(reference.getCompleteness())
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
