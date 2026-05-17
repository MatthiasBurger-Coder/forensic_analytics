package de.burger.forensics.analytics.services.analysisstore.adapter.in.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactReference;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobState;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind;
import de.burger.forensics.analytics.analysisjob.v1.CompleteAnalysisJobRequest;
import de.burger.forensics.analytics.analysisjob.v1.FailAnalysisJobRequest;
import de.burger.forensics.analytics.analysisjob.v1.GetAnalysisJobRequest;
import de.burger.forensics.analytics.analysisjob.v1.LeaseAnalysisJobRequest;
import de.burger.forensics.analytics.analysisjob.v1.ListAnalysisJobsRequest;
import de.burger.forensics.analytics.analysisjob.v1.RegisterAnalysisArtifactsRequest;
import de.burger.forensics.analytics.analysisjob.v1.ReportAnalysisJobProgressRequest;
import de.burger.forensics.analytics.analysisjob.v1.SubmitAnalysisJobRequest;

final class AnalysisJobRequestValidator {
    void validate(SubmitAnalysisJobRequest request) {
        commonMutation(request.getRequestId(), request.getIdempotencyKey(), request.getCorrelationId());
        RequiredFields.nonBlank(request.getSchemaVersion(), "schemaVersion");
        RequiredFields.present(request.hasAnalysisRunId(), "analysisRunId");
        RequiredFields.present(request.hasJobId(), "jobId");
        RequiredFields.present(request.hasSourceSnapshotId(), "sourceSnapshotId");
        RequiredFields.nonBlank(request.getAnalysisRunId().getValue(), "analysisRunId.value");
        RequiredFields.nonBlank(request.getJobId().getValue(), "jobId.value");
        RequiredFields.nonBlank(request.getSourceSnapshotId().getValue(), "sourceSnapshotId.value");
        workerKind(request.getWorkerKind());
        completeness(request.getInputCompleteness(), "inputCompleteness");
        request.getInputArtifactsList().forEach(this::artifact);
        request.getAttributesMap().forEach((key, value) -> {
            RequiredFields.nonBlank(key, "attribute.key");
            RequiredFields.nonBlank(value, "attribute.value");
        });
    }

    void validate(GetAnalysisJobRequest request) {
        RequiredFields.nonBlank(request.getRequestId(), "requestId");
        RequiredFields.nonBlank(request.getCorrelationId(), "correlationId");
        RequiredFields.present(request.hasJobId(), "jobId");
        RequiredFields.nonBlank(request.getJobId().getValue(), "jobId.value");
    }

    void validate(ListAnalysisJobsRequest request) {
        RequiredFields.nonBlank(request.getRequestId(), "requestId");
        RequiredFields.nonBlank(request.getCorrelationId(), "correlationId");
        if (request.hasAnalysisRunId()) {
            RequiredFields.nonBlank(request.getAnalysisRunId().getValue(), "analysisRunId.value");
        }
        if (request.getPageSize() < 0) {
            throw new ValidationException("pageSize must not be negative");
        }
        if (!request.getPageToken().isBlank()) {
            pageToken(request.getPageToken());
        }
    }

    void validate(LeaseAnalysisJobRequest request) {
        commonMutation(request.getRequestId(), request.getIdempotencyKey(), request.getCorrelationId());
        RequiredFields.nonBlank(request.getWorkerId(), "workerId");
        workerKind(request.getWorkerKind());
        RequiredFields.positive(request.getLeaseSeconds(), "leaseSeconds");
        RequiredFields.positive(request.getMaxJobs(), "maxJobs");
    }

    void validate(ReportAnalysisJobProgressRequest request) {
        commonMutation(request.getRequestId(), request.getIdempotencyKey(), request.getCorrelationId());
        RequiredFields.present(request.hasJobId(), "jobId");
        RequiredFields.nonBlank(request.getJobId().getValue(), "jobId.value");
        RequiredFields.positive(request.getAttempt(), "attempt");
        RequiredFields.nonBlank(request.getWorkerId(), "workerId");
        RequiredFields.percent(request.getPercentComplete(), "percentComplete");
    }

    void validate(CompleteAnalysisJobRequest request) {
        commonMutation(request.getRequestId(), request.getIdempotencyKey(), request.getCorrelationId());
        RequiredFields.present(request.hasJobId(), "jobId");
        RequiredFields.nonBlank(request.getJobId().getValue(), "jobId.value");
        RequiredFields.positive(request.getAttempt(), "attempt");
        RequiredFields.nonBlank(request.getWorkerId(), "workerId");
        completeness(request.getOutputCompleteness(), "outputCompleteness");
        request.getOutputArtifactsList().forEach(this::artifact);
    }

    void validate(FailAnalysisJobRequest request) {
        commonMutation(request.getRequestId(), request.getIdempotencyKey(), request.getCorrelationId());
        RequiredFields.present(request.hasJobId(), "jobId");
        RequiredFields.nonBlank(request.getJobId().getValue(), "jobId.value");
        RequiredFields.positive(request.getAttempt(), "attempt");
        RequiredFields.nonBlank(request.getWorkerId(), "workerId");
        RequiredFields.nonBlank(request.getReason(), "reason");
        completeness(request.getCompleteness(), "completeness");
    }

    void validate(RegisterAnalysisArtifactsRequest request) {
        commonMutation(request.getRequestId(), request.getIdempotencyKey(), request.getCorrelationId());
        RequiredFields.present(request.hasAnalysisRunId(), "analysisRunId");
        RequiredFields.present(request.hasJobId(), "jobId");
        RequiredFields.nonBlank(request.getAnalysisRunId().getValue(), "analysisRunId.value");
        RequiredFields.nonBlank(request.getJobId().getValue(), "jobId.value");
        request.getArtifactsList().forEach(this::artifact);
    }

    private void commonMutation(String requestId, String idempotencyKey, String correlationId) {
        RequiredFields.nonBlank(requestId, "requestId");
        RequiredFields.nonBlank(idempotencyKey, "idempotencyKey");
        RequiredFields.nonBlank(correlationId, "correlationId");
    }

    private void artifact(AnalysisArtifactReference reference) {
        RequiredFields.present(reference.hasArtifact(), "artifact");
        RequiredFields.nonBlank(reference.getArtifact().getPath(), "artifact.path");
        RequiredFields.nonBlank(reference.getArtifact().getType(), "artifact.type");
        RequiredFields.nonBlank(reference.getArtifact().getSha256(), "artifact.sha256");
        if (reference.getArtifact().getSizeBytes() < 0) {
            throw new ValidationException("artifact.sizeBytes must not be negative");
        }
        if (reference.getCategory() == de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_UNSPECIFIED) {
            throw new ValidationException("artifact.category must be specified");
        }
        RequiredFields.nonBlank(reference.getProducerService(), "artifact.producerService");
        RequiredFields.nonBlank(reference.getSchemaVersion(), "artifact.schemaVersion");
        completeness(reference.getCompleteness(), "artifact.completeness");
    }

    private void workerKind(AnalysisWorkerKind workerKind) {
        if (
            workerKind == AnalysisWorkerKind.ANALYSIS_WORKER_KIND_UNSPECIFIED
                || workerKind == AnalysisWorkerKind.UNRECOGNIZED
        ) {
            throw new ValidationException("workerKind must be specified");
        }
    }

    private void completeness(AnalysisCompleteness completeness, String fieldName) {
        if (
            completeness == AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNSPECIFIED
                || completeness == AnalysisCompleteness.UNRECOGNIZED
        ) {
            throw new ValidationException(fieldName + " must be specified");
        }
    }

    private void pageToken(String token) {
        try {
            var offset = Integer.parseInt(token.strip());
            if (offset < 0) {
                throw new ValidationException("pageToken must be a non-negative offset");
            }
        } catch (NumberFormatException error) {
            throw new ValidationException("pageToken must be a non-negative offset");
        }
    }
}
