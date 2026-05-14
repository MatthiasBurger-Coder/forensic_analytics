package de.burger.forensics.analytics.domain.analysis;

import de.burger.forensics.analytics.domain.artifact.ArtifactReference;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnalysisProjectionContractTest {
    @Test
    void availableProjectionRequiresConcreteArtifactAndCanonicalInputs() {
        var projection = AnalysisProjection.available(
            AnalysisProjectionKind.GRAPH,
            AnalysisProjectionOutputLabel.PROJECTION,
            canonicalInputs(),
            projectionArtifact(),
            List.of("graph projection built from canonical semantic facts")
        );

        assertEquals(AnalysisProjectionStatus.AVAILABLE, projection.status());
        assertEquals(Optional.of(projectionArtifact()), projection.artifact());
        assertEquals(canonicalInputs(), projection.canonicalInputs());
    }

    @Test
    void unavailableAndFailedProjectionsCarryDiagnosticsWithoutArtifacts() {
        var unavailable = AnalysisProjection.unavailable(
            AnalysisProjectionKind.VECTOR,
            AnalysisProjectionOutputLabel.PROJECTION,
            canonicalInputs(),
            List.of("vector projection provider has not been selected")
        );
        var failed = AnalysisProjection.failed(
            AnalysisProjectionKind.REPORT,
            AnalysisProjectionOutputLabel.PROJECTION,
            canonicalInputs(),
            List.of("report projection input artifact is incomplete")
        );

        assertEquals(AnalysisProjectionStatus.UNAVAILABLE, unavailable.status());
        assertEquals(Optional.empty(), unavailable.artifact());
        assertEquals(List.of("vector projection provider has not been selected"), unavailable.diagnostics());
        assertEquals(AnalysisProjectionStatus.FAILED, failed.status());
        assertEquals(List.of("report projection input artifact is incomplete"), failed.diagnostics());
    }

    @Test
    void llmProjectionOutputMustBeGeneratedOrHypothesis() {
        assertThrows(
            IllegalArgumentException.class,
            () -> AnalysisProjection.available(
                AnalysisProjectionKind.LLM,
                AnalysisProjectionOutputLabel.PROJECTION,
                canonicalInputs(),
                projectionArtifact(),
                List.of("llm output cannot be labeled as evidence")
            )
        );

        var generated = AnalysisProjection.available(
            AnalysisProjectionKind.LLM,
            AnalysisProjectionOutputLabel.GENERATED,
            canonicalInputs(),
            projectionArtifact(),
            List.of("llm output is generated analysis")
        );
        var hypothesis = AnalysisProjection.available(
            AnalysisProjectionKind.LLM,
            AnalysisProjectionOutputLabel.HYPOTHESIS,
            canonicalInputs(),
            projectionArtifact(),
            List.of("llm output is an unverified hypothesis")
        );

        assertEquals(AnalysisProjectionOutputLabel.GENERATED, generated.outputLabel());
        assertEquals(AnalysisProjectionOutputLabel.HYPOTHESIS, hypothesis.outputLabel());
    }

    @Test
    void projectionsRejectMissingInputsArtifactsAndDiagnostics() {
        assertThrows(
            IllegalArgumentException.class,
            () -> AnalysisProjection.available(
                AnalysisProjectionKind.GRAPH,
                AnalysisProjectionOutputLabel.PROJECTION,
                List.of(),
                projectionArtifact(),
                List.of("missing canonical inputs")
            )
        );
        assertThrows(
            NullPointerException.class,
            () -> AnalysisProjection.available(
                AnalysisProjectionKind.GRAPH,
                AnalysisProjectionOutputLabel.PROJECTION,
                canonicalInputs(),
                null,
                List.of("missing artifact")
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> AnalysisProjection.failed(
                AnalysisProjectionKind.REPORT,
                AnalysisProjectionOutputLabel.PROJECTION,
                canonicalInputs(),
                List.of()
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new AnalysisProjection(
                AnalysisProjectionKind.REPORT,
                AnalysisProjectionStatus.FAILED,
                AnalysisProjectionOutputLabel.PROJECTION,
                canonicalInputs(),
                Optional.of(projectionArtifact()),
                List.of("failed projection must not expose an artifact")
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new AnalysisProjection(
                AnalysisProjectionKind.GRAPH,
                AnalysisProjectionStatus.AVAILABLE,
                AnalysisProjectionOutputLabel.PROJECTION,
                canonicalInputs(),
                Optional.empty(),
                List.of()
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new AnalysisProjection(
                AnalysisProjectionKind.GRAPH,
                AnalysisProjectionStatus.UNAVAILABLE,
                AnalysisProjectionOutputLabel.PROJECTION,
                canonicalInputs(),
                Optional.empty(),
                List.of(" ")
            )
        );
        assertThrows(
            NullPointerException.class,
            () -> new AnalysisProjection(
                null,
                AnalysisProjectionStatus.UNAVAILABLE,
                AnalysisProjectionOutputLabel.PROJECTION,
                canonicalInputs(),
                Optional.empty(),
                List.of("missing kind")
            )
        );
    }

    private static List<AnalysisArtifactReference> canonicalInputs() {
        return List.of(new AnalysisArtifactReference(
            new ArtifactReference("analysis/semantic.json", "semantic-report", "sha256:semantic", 256L),
            AnalysisArtifactCategory.STATIC
        ));
    }

    private static AnalysisArtifactReference projectionArtifact() {
        return new AnalysisArtifactReference(
            new ArtifactReference("projections/graph.json", "graph-projection", "sha256:graph", 512L),
            AnalysisArtifactCategory.PROJECTION
        );
    }
}
