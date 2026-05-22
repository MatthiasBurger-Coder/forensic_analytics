package de.burger.forensics.analytics.services.analysisorchestrator.adapter.in.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactReference;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobState;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind;
import de.burger.forensics.analytics.analysisjob.v1.CompleteAnalysisJobRequest;
import de.burger.forensics.analytics.analysisjob.v1.FailAnalysisJobRequest;
import de.burger.forensics.analytics.analysisjob.v1.GetAnalysisJobRequest;
import de.burger.forensics.analytics.analysisjob.v1.GetRepositoryToBtmStatusRequest;
import de.burger.forensics.analytics.analysisjob.v1.LeaseAnalysisJobRequest;
import de.burger.forensics.analytics.analysisjob.v1.ListAnalysisJobsRequest;
import de.burger.forensics.analytics.analysisjob.v1.RegisterAnalysisArtifactsRequest;
import de.burger.forensics.analytics.analysisjob.v1.ReportAnalysisJobProgressRequest;
import de.burger.forensics.analytics.analysisjob.v1.RequestedRepositoryToBtmOutput;
import de.burger.forensics.analytics.analysisjob.v1.StartRepositoryToBtmRequest;
import de.burger.forensics.analytics.analysisjob.v1.SubmitAnalysisJobRequest;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisOrchestratorArtifactOwnership;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.SafeMetadata;

final class AnalysisJobRequestValidator {
    void validate(SubmitAnalysisJobRequest request) {
        commonMutation(request.getRequestId(), request.getIdempotencyKey(), request.getCorrelationId());
        RequiredFields.nonBlank(request.getSchemaVersion(), "schemaVersion");
        RequiredFields.present(request.hasAnalysisRunId(), "analysisRunId");
        RequiredFields.present(request.hasJobId(), "jobId");
        RequiredFields.present(request.hasSourceSnapshotId(), "sourceSnapshotId");
        RequiredFields.nonBlank(request.getAnalysisRunId().getValue(), "analysisRunId.value");
        RequiredFields.nonBlank(request.getJobId().getValue(), "jobId.value");
        safeOpaqueId(request.getSourceSnapshotId().getValue(), "sourceSnapshotId.value");
        workerKind(request.getWorkerKind());
        completeness(request.getInputCompleteness(), "inputCompleteness");
        request.getInputArtifactsList().forEach(this::artifact);
        safeAttributes(request.getAttributesMap());
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

    void validate(StartRepositoryToBtmRequest request) {
        commonMutation(request.getRequestId(), request.getIdempotencyKey(), request.getCorrelationId());
        RequiredFields.nonBlank(request.getSchemaVersion(), "schemaVersion");
        RequiredFields.present(request.hasAnalysisRunId(), "analysisRunId");
        RequiredFields.nonBlank(request.getAnalysisRunId().getValue(), "analysisRunId.value");
        RequiredFields.present(request.hasRepository(), "repository");
        RequiredFields.nonBlank(request.getRepository().getRemoteUrl(), "repository.remoteUrl");
        httpsRepositoryUrl(request.getRepository().getRemoteUrl());
        safeOpaqueId(request.getRepository().getProvider(), "repository.provider");
        RequiredFields.present(request.hasRevision(), "revision");
        if (request.getRevision().getBranch().isBlank() && request.getRevision().getCommit().isBlank()) {
            throw new ValidationException("revision branch or commit must be provided");
        }
        if (!request.getRevision().getBranch().isBlank()) {
            safeByteAccess(request.getRevision().getBranch(), "revision.branch");
        }
        if (!request.getRevision().getCommit().isBlank()) {
            safeByteAccess(request.getRevision().getCommit(), "revision.commit");
        }
        RequiredFields.present(request.hasWorkspacePolicy(), "workspacePolicy");
        RequiredFields.positive(request.getWorkspacePolicy().getTimeoutSeconds(), "workspacePolicy.timeoutSeconds");
        RequiredFields.positive(request.getWorkspacePolicy().getMaxWorkspaceBytes(), "workspacePolicy.maxWorkspaceBytes");
        RequiredFields.present(request.hasBuildContext(), "buildContext");
        safeByteAccess(request.getBuildContext().getBuildTool(), "buildContext.buildTool");
        if (!request.getBuildContext().getBuildId().isBlank()) {
            safeByteAccess(request.getBuildContext().getBuildId(), "buildContext.buildId");
        }
        if (!request.getBuildContext().getRootProjectName().isBlank()) {
            safeByteAccess(request.getBuildContext().getRootProjectName(), "buildContext.rootProjectName");
        }
        request.getBuildContext().getDeclaredModulesList().forEach(module -> safeByteAccess(module, "buildContext.declaredModules"));
        safeAttributes(request.getBuildContext().getAttributesMap());
        if (request.getRequestedOutputsList().stream().noneMatch(output -> output == RequestedRepositoryToBtmOutput.REQUESTED_REPOSITORY_TO_BTM_OUTPUT_BTM_RULES)) {
            throw new ValidationException("requestedOutputs must include BTM_RULES");
        }
        if (request.getRequestedOutputsList().stream().anyMatch(output ->
            output == RequestedRepositoryToBtmOutput.REQUESTED_REPOSITORY_TO_BTM_OUTPUT_UNSPECIFIED
                || output == RequestedRepositoryToBtmOutput.UNRECOGNIZED
        )) {
            throw new ValidationException("requestedOutputs must be specified");
        }
        safeAttributes(request.getAttributesMap());
    }

    void validate(GetRepositoryToBtmStatusRequest request) {
        RequiredFields.nonBlank(request.getRequestId(), "requestId");
        RequiredFields.nonBlank(request.getCorrelationId(), "correlationId");
        RequiredFields.present(request.hasAnalysisRunId(), "analysisRunId");
        RequiredFields.nonBlank(request.getAnalysisRunId().getValue(), "analysisRunId.value");
    }

    private void commonMutation(String requestId, String idempotencyKey, String correlationId) {
        RequiredFields.nonBlank(requestId, "requestId");
        RequiredFields.nonBlank(idempotencyKey, "idempotencyKey");
        RequiredFields.nonBlank(correlationId, "correlationId");
    }

    private void artifact(AnalysisArtifactReference reference) {
        RequiredFields.present(reference.hasArtifact(), "artifact");
        RequiredFields.nonBlank(reference.getArtifact().getPath(), "artifact.path");
        safeByteAccess(reference.getArtifact().getPath(), "artifact.path");
        RequiredFields.nonBlank(reference.getArtifact().getType(), "artifact.type");
        RequiredFields.nonBlank(reference.getArtifact().getSha256(), "artifact.sha256");
        if (reference.getArtifact().getSizeBytes() < 0) {
            throw new ValidationException("artifact.sizeBytes must not be negative");
        }
        if (reference.getCategory() == de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_UNSPECIFIED) {
            throw new ValidationException("artifact.category must be specified");
        }
        externalProducer(reference.getProducerService());
        RequiredFields.nonBlank(reference.getSchemaVersion(), "artifact.schemaVersion");
        completeness(reference.getCompleteness(), "artifact.completeness");
        RequiredFields.present(reference.hasByteAccess(), "artifact.byteAccess");
        externalByteOwner(reference.getByteAccess().getOwnerService());
        RequiredFields.nonBlank(reference.getByteAccess().getRetrievalContract(), "artifact.byteAccess.retrievalContract");
        RequiredFields.nonBlank(reference.getByteAccess().getRetrievalReference(), "artifact.byteAccess.retrievalReference");
        safeByteAccess(reference.getByteAccess().getRetrievalContract(), "artifact.byteAccess.retrievalContract");
        safeByteAccess(reference.getByteAccess().getRetrievalReference(), "artifact.byteAccess.retrievalReference");
        if (
            reference.getByteAccess().getByteCustody()
                == de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_UNSPECIFIED
                || reference.getByteAccess().getByteCustody()
                == de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody.UNRECOGNIZED
        ) {
            throw new ValidationException("artifact.byteAccess.byteCustody must be specified");
        }
    }

    private void externalProducer(String producerService) {
        try {
            AnalysisOrchestratorArtifactOwnership.requireExternalProducer(producerService);
        } catch (IllegalArgumentException error) {
            throw new ValidationException("artifact." + error.getMessage());
        }
    }

    private void externalByteOwner(String ownerService) {
        try {
            AnalysisOrchestratorArtifactOwnership.requireExternalByteOwner(ownerService);
        } catch (IllegalArgumentException error) {
            throw new ValidationException("artifact.byteAccess." + error.getMessage());
        }
    }

    private void safeByteAccess(String value, String fieldName) {
        try {
            de.burger.forensics.analytics.services.analysisorchestrator.domain.ArtifactByteAccess.requirePublicReference(value, fieldName);
        } catch (IllegalArgumentException error) {
            throw new ValidationException(error.getMessage());
        }
    }

    private void safeOpaqueId(String value, String fieldName) {
        try {
            SafeMetadata.requireOpaqueId(value, fieldName);
        } catch (IllegalArgumentException error) {
            throw new ValidationException(error.getMessage());
        }
    }

    private void safeAttributes(java.util.Map<String, String> attributes) {
        try {
            SafeMetadata.safeAttributes(attributes);
        } catch (IllegalArgumentException error) {
            throw new ValidationException(error.getMessage());
        }
    }

    private void httpsRepositoryUrl(String remoteUrl) {
        var text = remoteUrl.strip();
        if (!remoteUrl.equals(text)
            || !text.startsWith("https://")
            || text.contains("@")
            || text.chars().anyMatch(Character::isWhitespace)) {
            throw new ValidationException("repository.remoteUrl must be a clean HTTPS URL without embedded credentials");
        }
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
