package de.burger.forensics.analytics.domain.analysis;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record AnalysisProjection(
    AnalysisProjectionKind kind,
    AnalysisProjectionStatus status,
    AnalysisProjectionOutputLabel outputLabel,
    List<AnalysisArtifactReference> canonicalInputs,
    Optional<AnalysisArtifactReference> artifact,
    List<String> diagnostics
) {
    public AnalysisProjection {
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(outputLabel, "outputLabel must not be null");
        canonicalInputs = copyRequiredInputs(canonicalInputs);
        artifact = Objects.requireNonNull(artifact, "artifact must not be null");
        diagnostics = copyDiagnostics(diagnostics);
        requireAvailabilityArtifact(status, artifact);
        requireUnavailableProjectionWithoutArtifact(status, artifact);
        requireUnavailableDiagnostics(status, diagnostics);
        requireLlmOutputLabel(kind, outputLabel);
    }

    public static AnalysisProjection available(
        AnalysisProjectionKind kind,
        AnalysisProjectionOutputLabel outputLabel,
        List<AnalysisArtifactReference> canonicalInputs,
        AnalysisArtifactReference artifact,
        List<String> diagnostics
    ) {
        return new AnalysisProjection(
            kind,
            AnalysisProjectionStatus.AVAILABLE,
            outputLabel,
            canonicalInputs,
            Optional.of(Objects.requireNonNull(artifact, "artifact must not be null")),
            diagnostics
        );
    }

    public static AnalysisProjection unavailable(
        AnalysisProjectionKind kind,
        AnalysisProjectionOutputLabel outputLabel,
        List<AnalysisArtifactReference> canonicalInputs,
        List<String> diagnostics
    ) {
        return new AnalysisProjection(
            kind,
            AnalysisProjectionStatus.UNAVAILABLE,
            outputLabel,
            canonicalInputs,
            Optional.empty(),
            diagnostics
        );
    }

    public static AnalysisProjection failed(
        AnalysisProjectionKind kind,
        AnalysisProjectionOutputLabel outputLabel,
        List<AnalysisArtifactReference> canonicalInputs,
        List<String> diagnostics
    ) {
        return new AnalysisProjection(
            kind,
            AnalysisProjectionStatus.FAILED,
            outputLabel,
            canonicalInputs,
            Optional.empty(),
            diagnostics
        );
    }

    private static List<AnalysisArtifactReference> copyRequiredInputs(List<AnalysisArtifactReference> canonicalInputs) {
        var copied = List.copyOf(Objects.requireNonNull(canonicalInputs, "canonicalInputs must not be null"));
        if (copied.isEmpty()) {
            throw new IllegalArgumentException("canonicalInputs must not be empty");
        }
        return copied;
    }

    private static List<String> copyDiagnostics(List<String> diagnostics) {
        return List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics must not be null")).stream()
            .peek(diagnostic -> RequiredAnalysisText.requireText(diagnostic, "diagnostic"))
            .toList();
    }

    private static void requireAvailabilityArtifact(
        AnalysisProjectionStatus status,
        Optional<AnalysisArtifactReference> artifact
    ) {
        if (AnalysisProjectionStatus.AVAILABLE.equals(status) && artifact.isEmpty()) {
            throw new IllegalArgumentException("available projection must include an artifact reference");
        }
    }

    private static void requireUnavailableProjectionWithoutArtifact(
        AnalysisProjectionStatus status,
        Optional<AnalysisArtifactReference> artifact
    ) {
        if (!AnalysisProjectionStatus.AVAILABLE.equals(status) && artifact.isPresent()) {
            throw new IllegalArgumentException("unavailable or failed projection must not include an artifact reference");
        }
    }

    private static void requireUnavailableDiagnostics(AnalysisProjectionStatus status, List<String> diagnostics) {
        if (!AnalysisProjectionStatus.AVAILABLE.equals(status) && diagnostics.isEmpty()) {
            throw new IllegalArgumentException("unavailable or failed projection must include diagnostics");
        }
    }

    private static void requireLlmOutputLabel(
        AnalysisProjectionKind kind,
        AnalysisProjectionOutputLabel outputLabel
    ) {
        if (AnalysisProjectionKind.LLM.equals(kind) && AnalysisProjectionOutputLabel.PROJECTION.equals(outputLabel)) {
            throw new IllegalArgumentException("LLM projection output must be labeled as generated or hypothesis");
        }
    }
}
