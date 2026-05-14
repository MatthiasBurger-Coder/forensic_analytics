package de.burger.forensics.analytics.application.analysis.worker;

import de.burger.forensics.analytics.domain.analysis.AnalysisArtifactCategory;
import de.burger.forensics.analytics.domain.analysis.AnalysisArtifactReference;
import de.burger.forensics.analytics.domain.analysis.AnalysisCompleteness;
import de.burger.forensics.analytics.domain.analysis.AnalysisJobId;
import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.analysis.AnalysisWorkerKind;
import de.burger.forensics.analytics.domain.artifact.ArtifactReference;
import de.burger.forensics.analytics.domain.repository.SourceSnapshotId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnalysisWorkerContractTest {
    @Test
    void workerKindsCoverTheProviderNeutralBaseline() {
        assertEquals(
            List.of(
                "REPOSITORY_ANALYSIS",
                "AST_ANALYSIS",
                "JOERN_ANALYSIS",
                "BTM_GENERATION",
                "GRAPH_ANALYSIS",
                "REPORT",
                "LLM_PROJECTION"
            ),
            List.of(AnalysisWorkerKind.values()).stream().map(Enum::name).toList()
        );
    }

    @Test
    void workerInputCarriesRequiredRunJobSnapshotArtifactVersionDiagnosticsAndCompleteness() {
        var artifacts = new ArrayList<>(inputArtifacts());
        var diagnostics = new ArrayList<>(List.of("synthetic fixture diagnostic"));

        var input = new AnalysisWorkerInput(
            analysisRunId(),
            jobId(),
            AnalysisWorkerKind.AST_ANALYSIS,
            sourceSnapshotId(),
            artifacts,
            "worker-1",
            diagnostics,
            AnalysisCompleteness.COMPLETE
        );
        artifacts.add(artifact("runtime/trace.json", "runtime-trace", "sha256:runtime", AnalysisArtifactCategory.RUNTIME));
        diagnostics.add("mutated");

        assertEquals(analysisRunId(), input.analysisRunId());
        assertEquals(jobId(), input.jobId());
        assertEquals(sourceSnapshotId(), input.sourceSnapshotId());
        assertEquals(inputArtifacts(), input.inputArtifacts());
        assertEquals("worker-1", input.workerVersion());
        assertEquals(List.of("synthetic fixture diagnostic"), input.diagnostics());
        assertEquals(AnalysisCompleteness.COMPLETE, input.completeness());
    }

    @Test
    void workerOutputKeepsStaticRuntimeProjectionAndGeneratedArtifactsDistinct() {
        var output = new AnalysisWorkerOutput(
            analysisRunId(),
            jobId(),
            AnalysisWorkerKind.LLM_PROJECTION,
            sourceSnapshotId(),
            inputArtifacts(),
            List.of(
                artifact("projections/graph.json", "graph-projection", "sha256:graph", AnalysisArtifactCategory.PROJECTION),
                artifact("llm/hypothesis.json", "llm-hypothesis", "sha256:llm", AnalysisArtifactCategory.GENERATED)
            ),
            "worker-1",
            List.of("llm output is generated and not evidence"),
            AnalysisCompleteness.INCOMPLETE
        );

        assertEquals(AnalysisArtifactCategory.STATIC, output.inputArtifacts().getFirst().category());
        assertEquals(AnalysisArtifactCategory.PROJECTION, output.outputArtifacts().get(0).category());
        assertEquals(AnalysisArtifactCategory.GENERATED, output.outputArtifacts().get(1).category());
    }

    @Test
    void workerContractsRejectMissingRequiredReferences() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new AnalysisWorkerInput(
                analysisRunId(),
                jobId(),
                AnalysisWorkerKind.REPOSITORY_ANALYSIS,
                sourceSnapshotId(),
                List.of(),
                "worker-1",
                List.of(),
                AnalysisCompleteness.COMPLETE
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new AnalysisWorkerOutput(
                analysisRunId(),
                jobId(),
                AnalysisWorkerKind.REPORT,
                sourceSnapshotId(),
                inputArtifacts(),
                List.of(),
                "worker-1",
                List.of(),
                AnalysisCompleteness.UNKNOWN
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new AnalysisWorkerInput(
                analysisRunId(),
                jobId(),
                AnalysisWorkerKind.REPOSITORY_ANALYSIS,
                sourceSnapshotId(),
                inputArtifacts(),
                " ",
                List.of(),
                AnalysisCompleteness.COMPLETE
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

    private static List<AnalysisArtifactReference> inputArtifacts() {
        return List.of(artifact("source.tar", "source-snapshot", "sha256:source", AnalysisArtifactCategory.STATIC));
    }

    private static AnalysisArtifactReference artifact(
        String path,
        String type,
        String checksum,
        AnalysisArtifactCategory category
    ) {
        return new AnalysisArtifactReference(new ArtifactReference(path, type, checksum, 128L), category);
    }
}
