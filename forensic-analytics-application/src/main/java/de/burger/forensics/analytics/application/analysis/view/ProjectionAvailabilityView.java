package de.burger.forensics.analytics.application.analysis.view;

import de.burger.forensics.analytics.domain.analysis.AnalysisProjection;
import de.burger.forensics.analytics.domain.analysis.AnalysisProjectionKind;
import de.burger.forensics.analytics.domain.analysis.AnalysisProjectionOutputLabel;
import de.burger.forensics.analytics.domain.analysis.AnalysisProjectionStatus;
import de.burger.forensics.analytics.domain.artifact.ArtifactReference;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ProjectionAvailabilityView(
    AnalysisProjectionKind kind,
    AnalysisProjectionStatus status,
    AnalysisProjectionOutputLabel outputLabel,
    Optional<ArtifactReference> artifactReference,
    List<String> diagnostics
) {
    public ProjectionAvailabilityView {
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(outputLabel, "outputLabel must not be null");
        artifactReference = Objects.requireNonNull(artifactReference, "artifactReference must not be null");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics must not be null")).stream()
            .peek(diagnostic -> ServerAnalysisRequest.requireText(diagnostic, "diagnostic"))
            .toList();
    }

    public static ProjectionAvailabilityView from(AnalysisProjection projection) {
        Objects.requireNonNull(projection, "projection must not be null");
        return new ProjectionAvailabilityView(
            projection.kind(),
            projection.status(),
            projection.outputLabel(),
            projection.artifact().map(artifact -> artifact.artifact()),
            projection.diagnostics()
        );
    }
}
