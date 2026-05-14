package de.burger.forensics.analytics.application.analysis.view;

import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.artifact.ArtifactReference;

import java.util.List;
import java.util.Objects;

public record AnalysisStatusView(
    AnalysisRunId analysisRunId,
    List<AnalysisJobStatusView> jobs,
    List<ArtifactReference> artifactReferences,
    List<ProjectionAvailabilityView> projections
) {
    public AnalysisStatusView {
        Objects.requireNonNull(analysisRunId, "analysisRunId must not be null");
        jobs = List.copyOf(Objects.requireNonNull(jobs, "jobs must not be null"));
        artifactReferences = List.copyOf(Objects.requireNonNull(
            artifactReferences,
            "artifactReferences must not be null"
        ));
        projections = List.copyOf(Objects.requireNonNull(projections, "projections must not be null"));
    }
}
