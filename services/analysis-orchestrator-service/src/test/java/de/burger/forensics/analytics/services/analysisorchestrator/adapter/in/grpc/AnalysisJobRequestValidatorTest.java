package de.burger.forensics.analytics.services.analysisorchestrator.adapter.in.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactReference;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisRunId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind;
import de.burger.forensics.analytics.analysisjob.v1.ArtifactByteAccess;
import de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody;
import de.burger.forensics.analytics.analysisjob.v1.ArtifactReference;
import de.burger.forensics.analytics.analysisjob.v1.GetRepositoryToBtmStatusRequest;
import de.burger.forensics.analytics.analysisjob.v1.LeaseAnalysisJobRequest;
import de.burger.forensics.analytics.analysisjob.v1.ListAnalysisJobsRequest;
import de.burger.forensics.analytics.analysisjob.v1.RegisterAnalysisArtifactsRequest;
import de.burger.forensics.analytics.analysisjob.v1.RepositoryToBtmBuildContext;
import de.burger.forensics.analytics.analysisjob.v1.RepositoryToBtmRepositoryReference;
import de.burger.forensics.analytics.analysisjob.v1.RepositoryToBtmRevision;
import de.burger.forensics.analytics.analysisjob.v1.RepositoryToBtmWorkspacePolicy;
import de.burger.forensics.analytics.analysisjob.v1.RequestedRepositoryToBtmOutput;
import de.burger.forensics.analytics.analysisjob.v1.SourceSnapshotId;
import de.burger.forensics.analytics.analysisjob.v1.StartRepositoryToBtmRequest;
import de.burger.forensics.analytics.analysisjob.v1.SubmitAnalysisJobRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnalysisJobRequestValidatorTest {
    private final AnalysisJobRequestValidator validator = new AnalysisJobRequestValidator();

    @Test
    void validatesJobSubmissionAndRejectsPrivateArtifactReferences() {
        assertDoesNotThrow(() -> validator.validate(submitRequest("jobs/input.json").build()));
        assertDoesNotThrow(() -> validator.validate(submitRequest(repositorySourceArtifact()).build()));

        assertThrows(ValidationException.class, () -> validator.validate(submitRequest("/private/input.json").build()));
        assertThrows(ValidationException.class, () -> validator.validate(submitRequest("file:/tmp/input.json").build()));
        assertThrows(ValidationException.class, () -> validator.validate(submitRequest("../input.json").build()));
        assertThrows(ValidationException.class, () -> validator.validate(submitRequest("jobs/input.json")
            .setSourceSnapshotId(SourceSnapshotId.newBuilder().setValue("/tmp/source-snapshot"))
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(submitRequest("jobs/input.json")
            .setSourceSnapshotId(SourceSnapshotId.newBuilder().setValue("file:/tmp/source-snapshot"))
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(submitRequest("jobs/input.json")
            .putAttributes("note", "checkout failed at /tmp/private/workspace")
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(submitRequest("jobs/input.json")
            .putAttributes("token", "demo")
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(submitRequest(artifact(
            "jobs/input.json",
            "analysis-orchestrator-service",
            "producer-service",
            ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_PRODUCER_RETAINED
        )).build()));
        assertThrows(ValidationException.class, () -> validator.validate(submitRequest(artifact(
            "jobs/input.json",
            "producer-service",
            "analysis-orchestrator-service",
            ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_EXPLICIT_HANDOFF
        )).build()));
        assertThrows(ValidationException.class, () -> validator.validate(submitRequest("jobs/input.json")
            .setWorkerKind(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_UNSPECIFIED)
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(submitRequest("jobs/input.json")
            .setInputCompleteness(AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNSPECIFIED)
            .build()));
    }

    @Test
    void validatesLeasePaginationAndArtifactRegistration() {
        assertDoesNotThrow(() -> validator.validate(LeaseAnalysisJobRequest.newBuilder()
            .setRequestId("request-lease")
            .setIdempotencyKey("lease-key")
            .setCorrelationId("correlation-1")
            .setWorkerId("worker-a")
            .setWorkerKind(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS)
            .setLeaseSeconds(60)
            .setMaxJobs(1)
            .build()));
        assertDoesNotThrow(() -> validator.validate(ListAnalysisJobsRequest.newBuilder()
            .setRequestId("request-list")
            .setCorrelationId("correlation-1")
            .setPageToken("0")
            .build()));
        assertDoesNotThrow(() -> validator.validate(RegisterAnalysisArtifactsRequest.newBuilder()
            .setRequestId("request-register")
            .setIdempotencyKey("register-key")
            .setCorrelationId("correlation-1")
            .setAnalysisRunId(runId())
            .setJobId(jobId("job-1"))
            .addArtifacts(artifact("reports/run-1.json"))
            .build()));
        assertDoesNotThrow(() -> validator.validate(startRepositoryToBtmRequest().build()));
        assertDoesNotThrow(() -> validator.validate(GetRepositoryToBtmStatusRequest.newBuilder()
            .setRequestId("request-get-repository-to-btm")
            .setCorrelationId("correlation-1")
            .setAnalysisRunId(runId())
            .build()));

        assertThrows(ValidationException.class, () -> validator.validate(LeaseAnalysisJobRequest.newBuilder()
            .setRequestId("request-lease")
            .setIdempotencyKey("lease-key")
            .setCorrelationId("correlation-1")
            .setWorkerId("worker-a")
            .setWorkerKind(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS)
            .setLeaseSeconds(0)
            .setMaxJobs(1)
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(ListAnalysisJobsRequest.newBuilder()
            .setRequestId("request-list")
            .setCorrelationId("correlation-1")
            .setPageToken("not-a-number")
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(startRepositoryToBtmRequest()
            .setRepository(RepositoryToBtmRepositoryReference.newBuilder()
                .setRemoteUrl("https://token@example.test/repo.git")
                .setProvider("git"))
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(startRepositoryToBtmRequest()
            .setRepository(RepositoryToBtmRepositoryReference.newBuilder()
                .setRemoteUrl("ssh://example.test/repo.git")
                .setProvider("git"))
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(startRepositoryToBtmRequest()
            .setRepository(RepositoryToBtmRepositoryReference.newBuilder()
                .setRemoteUrl("https://example.test/repo.git\n")
                .setProvider("git"))
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(startRepositoryToBtmRequest()
            .setRepository(RepositoryToBtmRepositoryReference.newBuilder()
                .setRemoteUrl("https://example.test/repo.git")
                .setProvider("git/token"))
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(startRepositoryToBtmRequest()
            .clearRequestedOutputs()
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(startRepositoryToBtmRequest()
            .setRequestedOutputs(0, RequestedRepositoryToBtmOutput.REQUESTED_REPOSITORY_TO_BTM_OUTPUT_UNSPECIFIED)
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(startRepositoryToBtmRequest()
            .setRevision(RepositoryToBtmRevision.newBuilder())
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(startRepositoryToBtmRequest()
            .setRevision(RepositoryToBtmRevision.newBuilder().setBranch("../main"))
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(startRepositoryToBtmRequest()
            .setRevision(RepositoryToBtmRevision.newBuilder().setCommit("file:/tmp/commit"))
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(startRepositoryToBtmRequest()
            .setWorkspacePolicy(RepositoryToBtmWorkspacePolicy.newBuilder()
                .setEphemeral(true)
                .setTimeoutSeconds(0)
                .setMaxWorkspaceBytes(1_000_000))
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(startRepositoryToBtmRequest()
            .setWorkspacePolicy(RepositoryToBtmWorkspacePolicy.newBuilder()
                .setEphemeral(true)
                .setTimeoutSeconds(60)
                .setMaxWorkspaceBytes(0))
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(startRepositoryToBtmRequest()
            .clearBuildContext()
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(startRepositoryToBtmRequest()
            .setBuildContext(RepositoryToBtmBuildContext.newBuilder()
                .setBuildTool("gradle://init")
                .setBuildId("build-1")
                .setRootProjectName("demo")
                .addDeclaredModules("app"))
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(startRepositoryToBtmRequest()
            .setBuildContext(RepositoryToBtmBuildContext.newBuilder()
                .setBuildTool("gradle")
                .setBuildId("build-1")
                .setRootProjectName("../demo")
                .addDeclaredModules("app"))
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(startRepositoryToBtmRequest()
            .setBuildContext(RepositoryToBtmBuildContext.newBuilder()
                .setBuildTool("gradle")
                .setBuildId("build-1")
                .setRootProjectName("demo")
                .addDeclaredModules("../app"))
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(startRepositoryToBtmRequest()
            .setBuildContext(RepositoryToBtmBuildContext.newBuilder()
                .setBuildTool("gradle")
                .setBuildId("build-1")
                .setRootProjectName("demo")
                .addDeclaredModules("app")
                .putAttributes("token", "secret"))
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(startRepositoryToBtmRequest()
            .setBuildContext(RepositoryToBtmBuildContext.newBuilder()
                .setBuildTool("gradle")
                .setBuildId("build-1")
                .setRootProjectName("demo")
                .addDeclaredModules("app")
                .putAttributes("note", "created under /tmp/workspace"))
            .build()));
    }

    private static SubmitAnalysisJobRequest.Builder submitRequest(String artifactPath) {
        return submitRequest(artifact(artifactPath));
    }

    private static SubmitAnalysisJobRequest.Builder submitRequest(AnalysisArtifactReference artifact) {
        return SubmitAnalysisJobRequest.newBuilder()
            .setRequestId("request-submit")
            .setIdempotencyKey("submit-key")
            .setSchemaVersion("schema-v1")
            .setCorrelationId("correlation-1")
            .setAnalysisRunId(runId())
            .setJobId(jobId("job-1"))
            .setWorkerKind(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS)
            .setSourceSnapshotId(SourceSnapshotId.newBuilder().setValue("snapshot-1"))
            .setInputCompleteness(AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNKNOWN)
            .addInputArtifacts(artifact);
    }

    private static AnalysisRunId runId() {
        return AnalysisRunId.newBuilder().setValue("run-1").build();
    }

    private static StartRepositoryToBtmRequest.Builder startRepositoryToBtmRequest() {
        return StartRepositoryToBtmRequest.newBuilder()
            .setRequestId("request-start-repository-to-btm")
            .setIdempotencyKey("start-repository-to-btm")
            .setSchemaVersion("schema-v1")
            .setCorrelationId("correlation-1")
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

    private static AnalysisJobId jobId(String value) {
        return AnalysisJobId.newBuilder().setValue(value).build();
    }

    private static AnalysisArtifactReference artifact(String path) {
        return artifact(
            path,
            "producer-service",
            "producer-service",
            ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_PRODUCER_RETAINED
        );
    }

    private static AnalysisArtifactReference artifact(
        String path,
        String producerService,
        String ownerService,
        ArtifactByteCustody byteCustody
    ) {
        return AnalysisArtifactReference.newBuilder()
            .setArtifact(ArtifactReference.newBuilder()
                .setPath(path)
                .setType("application/json")
                .setSha256("sha")
                .setSizeBytes(42))
            .setCategory(AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC)
            .setProducerService(producerService)
            .setSchemaVersion("schema-v1")
            .setCompleteness(AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNKNOWN)
            .setByteAccess(ArtifactByteAccess.newBuilder()
                .setOwnerService(ownerService)
                .setRetrievalContract("producer-service.artifacts.v1")
                .setRetrievalReference(path)
                .setByteCustody(byteCustody))
            .build();
    }

    private static AnalysisArtifactReference repositorySourceArtifact() {
        return artifact(
            "source-snapshot/source-snapshot-1",
            "repository-source-service",
            "repository-source-service",
            ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_PRODUCER_RETAINED
        ).toBuilder()
            .setSchemaVersion("repository-source.v1.SourcePackage")
            .setByteAccess(ArtifactByteAccess.newBuilder()
                .setOwnerService("repository-source-service")
                .setRetrievalContract("repository-source.v1.SourcePackage")
                .setRetrievalReference("source-snapshot/source-snapshot-1")
                .setByteCustody(ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_PRODUCER_RETAINED))
            .build();
    }
}
