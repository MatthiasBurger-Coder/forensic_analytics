package de.burger.forensics.analytics.application.analysis.view;

import de.burger.forensics.analytics.domain.analysis.AnalysisArtifactCategory;
import de.burger.forensics.analytics.domain.analysis.AnalysisArtifactReference;
import de.burger.forensics.analytics.domain.analysis.AnalysisCompleteness;
import de.burger.forensics.analytics.domain.analysis.AnalysisJob;
import de.burger.forensics.analytics.domain.analysis.AnalysisJobFailure;
import de.burger.forensics.analytics.domain.analysis.AnalysisJobId;
import de.burger.forensics.analytics.domain.analysis.AnalysisJobState;
import de.burger.forensics.analytics.domain.analysis.AnalysisProjection;
import de.burger.forensics.analytics.domain.analysis.AnalysisProjectionKind;
import de.burger.forensics.analytics.domain.analysis.AnalysisProjectionOutputLabel;
import de.burger.forensics.analytics.domain.analysis.AnalysisProjectionStatus;
import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.analysis.AnalysisWorkerKind;
import de.burger.forensics.analytics.domain.artifact.ArtifactReference;
import de.burger.forensics.analytics.domain.repository.RepositoryMetadata;
import de.burger.forensics.analytics.domain.repository.SourceSnapshotId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnalysisStatusViewContractTest {
    @Test
    void serverFacingRequestUsesApplicationTermsWithoutTransportOrRawSourceData() {
        var request = new ServerAnalysisRequest(analysisRunId(), repositoryMetadata(), "baseline");

        assertEquals(analysisRunId(), request.analysisRunId());
        assertEquals(repositoryMetadata(), request.repositoryMetadata());
        assertEquals("baseline", request.analysisProfile());
    }

    @Test
    void statusViewExposesJobStatesDiagnosticsArtifactReferencesAndProjectionAvailability() {
        var job = AnalysisJob.accepted(
            analysisRunId(),
            jobId(),
            AnalysisWorkerKind.JOERN_ANALYSIS,
            sourceSnapshotId(),
            inputArtifacts(),
            AnalysisCompleteness.INCOMPLETE
        )
            .dispatchable()
            .running(1)
            .retryable(new AnalysisJobFailure(
                jobId(),
                AnalysisWorkerKind.JOERN_ANALYSIS,
                1,
                "joern output incomplete",
                List.of("missing call graph artifact"),
                AnalysisCompleteness.INCOMPLETE
            ));
        var projection = AnalysisProjection.unavailable(
            AnalysisProjectionKind.LLM,
            AnalysisProjectionOutputLabel.HYPOTHESIS,
            inputArtifacts(),
            List.of("llm provider has not been selected")
        );
        var artifacts = new ArrayList<>(List.of(inputArtifacts().getFirst().artifact()));
        var status = new AnalysisStatusView(
            analysisRunId(),
            List.of(AnalysisJobStatusView.from(job)),
            artifacts,
            List.of(ProjectionAvailabilityView.from(projection))
        );
        artifacts.add(new ArtifactReference("raw.txt", "sensitive-runtime", "sha256:raw", 1L));

        assertEquals(analysisRunId(), status.analysisRunId());
        assertEquals(AnalysisJobState.RETRYABLE, status.jobs().getFirst().state());
        assertEquals(List.of("missing call graph artifact"), status.jobs().getFirst().diagnostics());
        assertEquals(List.of(inputArtifacts().getFirst().artifact()), status.artifactReferences());
        assertEquals(AnalysisProjectionStatus.UNAVAILABLE, status.projections().getFirst().status());
        assertEquals(Optional.empty(), status.projections().getFirst().artifactReference());
        assertEquals(List.of("llm provider has not been selected"), status.projections().getFirst().diagnostics());
    }

    @Test
    void serverFacingViewsRejectMissingRequiredFields() {
        assertThrows(NullPointerException.class, () -> new ServerAnalysisRequest(null, repositoryMetadata(), "baseline"));
        assertThrows(NullPointerException.class, () -> new ServerAnalysisRequest(analysisRunId(), null, "baseline"));
        assertThrows(IllegalArgumentException.class, () -> new ServerAnalysisRequest(analysisRunId(), repositoryMetadata(), " "));
        assertThrows(
            NullPointerException.class,
            () -> new AnalysisStatusView(analysisRunId(), null, List.of(), List.of())
        );
        assertThrows(
            NullPointerException.class,
            () -> new AnalysisStatusView(analysisRunId(), List.of(), null, List.of())
        );
        assertThrows(
            NullPointerException.class,
            () -> new AnalysisStatusView(analysisRunId(), List.of(), List.of(), null)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new AnalysisJobStatusView(
                jobId(),
                AnalysisWorkerKind.JOERN_ANALYSIS,
                AnalysisJobState.RUNNING,
                -1,
                List.of()
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new AnalysisJobStatusView(
                jobId(),
                AnalysisWorkerKind.JOERN_ANALYSIS,
                AnalysisJobState.RUNNING,
                1,
                List.of(" ")
            )
        );
        assertThrows(NullPointerException.class, () -> AnalysisJobStatusView.from(null));
        assertThrows(NullPointerException.class, () -> ProjectionAvailabilityView.from(null));
        assertThrows(
            IllegalArgumentException.class,
            () -> new ProjectionAvailabilityView(
                AnalysisProjectionKind.REPORT,
                AnalysisProjectionStatus.FAILED,
                AnalysisProjectionOutputLabel.PROJECTION,
                Optional.empty(),
                List.of(" ")
            )
        );
    }

    private static AnalysisRunId analysisRunId() {
        return new AnalysisRunId("analysis-1");
    }

    private static AnalysisJobId jobId() {
        return new AnalysisJobId("job-1");
    }

    private static SourceSnapshotId sourceSnapshotId() {
        return new SourceSnapshotId("snapshot-1");
    }

    private static RepositoryMetadata repositoryMetadata() {
        return new RepositoryMetadata("project-a", "file:///workspace/project", "main", "abcdef");
    }

    private static List<AnalysisArtifactReference> inputArtifacts() {
        return List.of(new AnalysisArtifactReference(
            new ArtifactReference("source.tar", "source-snapshot", "sha256:source", 128L),
            AnalysisArtifactCategory.STATIC
        ));
    }
}
