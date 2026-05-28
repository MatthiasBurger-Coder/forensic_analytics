package de.burger.forensics.analytics.services.analysisstore.adapter.in.grpc;

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
import de.burger.forensics.analytics.analysisjob.v1.PlanInstrumentationTargetsRequest;
import de.burger.forensics.analytics.analysisjob.v1.RegisterAnalysisArtifactsRequest;
import de.burger.forensics.analytics.analysisjob.v1.ReportAnalysisJobProgressRequest;
import de.burger.forensics.analytics.analysisjob.v1.StartRepositoryToBtmRequest;
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

    void validate(PlanInstrumentationTargetsRequest request) {
        commonMutation(request.getRequestId(), request.getIdempotencyKey(), request.getCorrelationId());
        RequiredFields.nonBlank(request.getSchemaVersion(), "schemaVersion");
        RequiredFields.present(request.hasAnalysisRunId(), "analysisRunId");
        RequiredFields.present(request.hasAnalysisJobId(), "analysisJobId");
        RequiredFields.present(request.hasSourceSnapshotId(), "sourceSnapshotId");
        RequiredFields.nonBlank(request.getAnalysisRunId().getValue(), "analysisRunId.value");
        RequiredFields.nonBlank(request.getAnalysisJobId().getValue(), "analysisJobId.value");
        RequiredFields.nonBlank(request.getSourceSnapshotId().getValue(), "sourceSnapshotId.value");
        RequiredFields.nonBlank(request.getPolicyVersion(), "policyVersion");
        RequiredFields.present(request.hasPolicy(), "policy");
        RequiredFields.positive(request.getPolicy().getMaxTargets(), "policy.maxTargets");
        if (request.getPolicy().getProbeKindsList().isEmpty()) {
            throw new ValidationException("policy.probeKinds must not be empty");
        }
        request.getPolicy().getProbeKindsList().forEach(probeKind -> {
            if (
                probeKind == de.burger.forensics.analytics.analysisjob.v1.InstrumentationProbeKind.INSTRUMENTATION_PROBE_KIND_UNSPECIFIED
                    || probeKind == de.burger.forensics.analytics.analysisjob.v1.InstrumentationProbeKind.UNRECOGNIZED
            ) {
                throw new ValidationException("policy.probeKinds must contain supported probe kinds");
            }
        });
        RequiredFields.nonBlank(request.getPolicy().getSensitivity(), "policy.sensitivity");
        request.getStaticFactsList().forEach(fact -> {
            RequiredFields.nonBlank(fact.getFactId(), "staticFact.factId");
            RequiredFields.nonBlank(fact.getFactType(), "staticFact.factType");
            RequiredFields.present(fact.hasLocation(), "staticFact.location");
            RequiredFields.nonBlank(fact.getLocation().getSourcePath(), "staticFact.location.sourcePath");
            safeByteAccess(fact.getLocation().getSourcePath(), "staticFact.location.sourcePath");
            RequiredFields.nonBlank(fact.getLocation().getFullyQualifiedClassName(), "staticFact.location.fullyQualifiedClassName");
            RequiredFields.nonBlank(fact.getLocation().getMethodName(), "staticFact.location.methodName");
            RequiredFields.positive(fact.getLocation().getLineNumber(), "staticFact.location.lineNumber");
            if (fact.getLocation().getColumnNumber() < 0) {
                throw new ValidationException("staticFact.location.columnNumber must not be negative");
            }
            RequiredFields.nonBlank(fact.getSignature(), "staticFact.signature");
            RequiredFields.nonBlank(fact.getSourceFactArtifactReference(), "staticFact.sourceFactArtifactReference");
            safeByteAccess(fact.getSourceFactArtifactReference(), "staticFact.sourceFactArtifactReference");
            completeness(fact.getCompleteness(), "staticFact.completeness");
        });
        request.getSourceFactArtifactsList().forEach(this::artifact);
        request.getSemanticArtifactsList().forEach(this::artifact);
        request.getAttributesMap().forEach((key, value) -> {
            RequiredFields.nonBlank(key, "attribute.key");
            RequiredFields.nonBlank(value, "attribute.value");
        });
    }

    void validate(StartRepositoryToBtmRequest request) {
        commonMutation(request.getRequestId(), request.getIdempotencyKey(), request.getCorrelationId());
        RequiredFields.nonBlank(request.getSchemaVersion(), "schemaVersion");
        RequiredFields.present(request.hasAnalysisRunId(), "analysisRunId");
        RequiredFields.nonBlank(request.getAnalysisRunId().getValue(), "analysisRunId.value");
        RequiredFields.present(request.hasRepository(), "repository");
        RequiredFields.nonBlank(request.getRepository().getRemoteUrl(), "repository.remoteUrl");
        RequiredFields.present(request.hasRevision(), "revision");
        if (request.getRevision().getBranch().isBlank() && request.getRevision().getCommit().isBlank()) {
            throw new ValidationException("revision.branch or revision.commit is required");
        }
        RequiredFields.present(request.hasWorkspacePolicy(), "workspacePolicy");
        if (request.getWorkspacePolicy().getTimeoutSeconds() < 1) {
            throw new ValidationException("workspacePolicy.timeoutSeconds must be positive");
        }
        if (request.getWorkspacePolicy().getMaxWorkspaceBytes() < 1) {
            throw new ValidationException("workspacePolicy.maxWorkspaceBytes must be positive");
        }
        RequiredFields.present(request.hasBuildContext(), "buildContext");
        RequiredFields.nonBlank(request.getBuildContext().getBuildTool(), "buildContext.buildTool");
        RequiredFields.nonBlank(request.getBuildContext().getBuildId(), "buildContext.buildId");
        if (request.getRequestedOutputsList().isEmpty()) {
            throw new ValidationException("requestedOutputs must not be empty");
        }
        request.getRequestedOutputsList().forEach(output -> {
            if (
                output == de.burger.forensics.analytics.analysisjob.v1.RequestedRepositoryToBtmOutput.REQUESTED_REPOSITORY_TO_BTM_OUTPUT_UNSPECIFIED
                    || output == de.burger.forensics.analytics.analysisjob.v1.RequestedRepositoryToBtmOutput.UNRECOGNIZED
            ) {
                throw new ValidationException("requestedOutputs must contain supported outputs");
            }
        });
        request.getAttributesMap().forEach((key, value) -> {
            RequiredFields.nonBlank(key, "attribute.key");
            RequiredFields.nonBlank(value, "attribute.value");
        });
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
        RequiredFields.nonBlank(reference.getProducerService(), "artifact.producerService");
        RequiredFields.nonBlank(reference.getSchemaVersion(), "artifact.schemaVersion");
        completeness(reference.getCompleteness(), "artifact.completeness");
        RequiredFields.present(reference.hasByteAccess(), "artifact.byteAccess");
        RequiredFields.nonBlank(reference.getByteAccess().getOwnerService(), "artifact.byteAccess.ownerService");
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

    private void safeByteAccess(String value, String fieldName) {
        try {
            de.burger.forensics.analytics.services.analysisstore.domain.ArtifactByteAccess.requirePublicReference(value, fieldName);
        } catch (IllegalArgumentException error) {
            throw new ValidationException(error.getMessage());
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
