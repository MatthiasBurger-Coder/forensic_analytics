package de.burger.forensics.analytics.services.analysisstore.adapter.in.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind;
import de.burger.forensics.analytics.analysisjob.v1.LeaseAnalysisJobRequest;
import de.burger.forensics.analytics.analysisjob.v1.ListAnalysisJobsRequest;
import de.burger.forensics.analytics.analysisjob.v1.ReportAnalysisJobProgressRequest;
import de.burger.forensics.analytics.analysisjob.v1.RequestedRepositoryToBtmOutput;
import de.burger.forensics.analytics.analysisjob.v1.SubmitAnalysisJobRequest;
import org.junit.jupiter.api.Test;

import static de.burger.forensics.analytics.services.analysisstore.adapter.in.grpc.AnalysisJobGrpcEndpointTest.artifact;
import static de.burger.forensics.analytics.services.analysisstore.adapter.in.grpc.AnalysisJobGrpcEndpointTest.repositoryToBtmRequest;
import static de.burger.forensics.analytics.services.analysisstore.adapter.in.grpc.AnalysisJobGrpcEndpointTest.jobId;
import static de.burger.forensics.analytics.services.analysisstore.adapter.in.grpc.AnalysisJobGrpcEndpointTest.submitRequest;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnalysisJobRequestValidatorTest {
    private final AnalysisJobRequestValidator validator = new AnalysisJobRequestValidator();

    @Test
    void acceptsCompleteSubmitRequestAndRejectsMissingRequiredFields() {
        assertDoesNotThrow(() -> validator.validate(submitRequest(
            "submit-1",
            "job-1",
            AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS
        )));

        assertThrows(ValidationException.class, () -> validator.validate(SubmitAnalysisJobRequest.getDefaultInstance()));
        assertThrows(ValidationException.class, () -> validator.validate(submitRequest(
            "submit-2",
            "job-2",
            AnalysisWorkerKind.ANALYSIS_WORKER_KIND_UNSPECIFIED
        )));
        assertThrows(ValidationException.class, () -> validator.validate(submitRequest(
            "submit-3",
            "job-3",
            AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS
        ).toBuilder()
            .setInputCompleteness(AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNSPECIFIED)
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(submitRequest(
            "submit-4",
            "job-4",
            AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS
        ).toBuilder()
            .putAttributes(" ", "value")
            .build()));
    }

    @Test
    void rejectsInvalidArtifactAndProgressValues() {
        var invalidArtifact = submitRequest(
            "submit-1",
            "job-1",
            AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS
        ).toBuilder()
            .clearInputArtifacts()
            .addInputArtifacts(artifact(
                "artifact.json",
                "sha",
                AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_UNSPECIFIED
            ))
            .build();
        var invalidProgress = ReportAnalysisJobProgressRequest.newBuilder()
            .setRequestId("request-progress")
            .setIdempotencyKey("progress-1")
            .setCorrelationId("correlation-1")
            .setJobId(jobId("job-1"))
            .setAttempt(1)
            .setWorkerId("worker-a")
            .setPercentComplete(-1)
            .build();

        assertThrows(ValidationException.class, () -> validator.validate(invalidArtifact));
        assertThrows(ValidationException.class, () -> validator.validate(submitRequest(
            "submit-private-artifact-path",
            "job-private-artifact-path",
            AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS
        ).toBuilder()
            .clearInputArtifacts()
            .addInputArtifacts(artifact(
                "file:/tmp/private/artifact.json",
                "sha",
                AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC
            ))
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(submitRequest(
            "submit-current-directory-artifact-path",
            "job-current-directory-artifact-path",
            AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS
        ).toBuilder()
            .clearInputArtifacts()
            .addInputArtifacts(artifact(
                "artifact/./path.json",
                "sha",
                AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC
            ))
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(submitRequest(
            "submit-missing-byte-access",
            "job-missing-byte-access",
            AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS
        ).toBuilder()
            .clearInputArtifacts()
            .addInputArtifacts(artifact(
                "artifact.json",
                "sha",
                AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC
            ).toBuilder().clearByteAccess())
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(submitRequest(
            "submit-unspecified-byte-custody",
            "job-unspecified-byte-custody",
            AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS
        ).toBuilder()
            .clearInputArtifacts()
            .addInputArtifacts(artifact(
                "artifact.json",
                "sha",
                AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC
            ).toBuilder()
                .setByteAccess(artifact(
                    "artifact.json",
                    "sha",
                    AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC
                ).getByteAccess().toBuilder()
                    .setByteCustody(de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_UNSPECIFIED)))
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(submitRequest(
            "submit-private-byte-reference",
            "job-private-byte-reference",
            AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS
        ).toBuilder()
            .clearInputArtifacts()
            .addInputArtifacts(artifact(
                "artifact.json",
                "sha",
                AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC
            ).toBuilder()
                .setByteAccess(artifact(
                    "artifact.json",
                    "sha",
                    AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC
                ).getByteAccess().toBuilder()
                    .setRetrievalReference("file:/tmp/private/artifact")))
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(submitRequest(
            "submit-current-directory-byte-reference",
            "job-current-directory-byte-reference",
            AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS
        ).toBuilder()
            .clearInputArtifacts()
            .addInputArtifacts(artifact(
                "artifact.json",
                "sha",
                AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC
            ).toBuilder()
                .setByteAccess(artifact(
                    "artifact.json",
                    "sha",
                    AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC
                ).getByteAccess().toBuilder()
                    .setRetrievalReference("artifacts/./artifact")))
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(invalidProgress));
        assertThrows(ValidationException.class, () -> validator.validate(LeaseAnalysisJobRequest.newBuilder()
            .setRequestId("request-lease")
            .setIdempotencyKey("lease-1")
            .setCorrelationId("correlation-1")
            .setWorkerId("worker-a")
            .setWorkerKind(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS)
            .setLeaseSeconds(60)
            .setMaxJobs(0)
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(
            invalidProgress.toBuilder()
                .setJobId(AnalysisJobId.newBuilder().setValue(" "))
                .setPercentComplete(50)
                .build()
        ));
        assertThrows(ValidationException.class, () -> validator.validate(ListAnalysisJobsRequest.newBuilder()
            .setRequestId("request-list")
            .setCorrelationId("correlation-1")
            .setPageToken("-1")
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(ListAnalysisJobsRequest.newBuilder()
            .setRequestId("request-list")
            .setCorrelationId("correlation-1")
            .setPageToken("not-a-number")
            .build()));
    }

    @Test
    void validatesRepositoryToBtmOwnerRequests() {
        assertDoesNotThrow(() -> validator.validate(repositoryToBtmRequest().build()));

        assertThrows(ValidationException.class, () -> validator.validate(repositoryToBtmRequest()
            .clearRepository()
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(repositoryToBtmRequest()
            .clearRequestedOutputs()
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(repositoryToBtmRequest()
            .clearRequestedOutputs()
            .addRequestedOutputs(RequestedRepositoryToBtmOutput.REQUESTED_REPOSITORY_TO_BTM_OUTPUT_UNSPECIFIED)
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(repositoryToBtmRequest()
            .clearRevision()
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(repositoryToBtmRequest()
            .setWorkspacePolicy(repositoryToBtmRequest().getWorkspacePolicyBuilder()
                .setTimeoutSeconds(0))
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(repositoryToBtmRequest()
            .setWorkspacePolicy(repositoryToBtmRequest().getWorkspacePolicyBuilder()
                .setMaxWorkspaceBytes(0))
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(repositoryToBtmRequest()
            .setBuildContext(repositoryToBtmRequest().getBuildContextBuilder()
                .setBuildTool(" "))
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(repositoryToBtmRequest()
            .setBuildContext(repositoryToBtmRequest().getBuildContextBuilder()
                .setBuildId(" "))
            .build()));
        assertThrows(ValidationException.class, () -> validator.validate(repositoryToBtmRequest()
            .putAttributes("tenant", " ")
            .build()));
    }
}
