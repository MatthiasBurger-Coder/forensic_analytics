package de.burger.forensics.analytics.services.analysisstore.adapter.in.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind;
import de.burger.forensics.analytics.analysisjob.v1.LeaseAnalysisJobRequest;
import de.burger.forensics.analytics.analysisjob.v1.ListAnalysisJobsRequest;
import de.burger.forensics.analytics.analysisjob.v1.ReportAnalysisJobProgressRequest;
import de.burger.forensics.analytics.analysisjob.v1.SubmitAnalysisJobRequest;
import org.junit.jupiter.api.Test;

import static de.burger.forensics.analytics.services.analysisstore.adapter.in.grpc.AnalysisJobGrpcEndpointTest.artifact;
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
}
