package de.burger.forensics.analytics.services.joerncpganalysis.application;

import de.burger.forensics.analytics.services.joerncpganalysis.application.port.JoernArtifactCollectorPort;
import de.burger.forensics.analytics.services.joerncpganalysis.application.port.JoernRuntimePort;
import de.burger.forensics.analytics.services.joerncpganalysis.application.port.JoernWorkspacePort;
import de.burger.forensics.analytics.services.joerncpganalysis.application.port.ResolvedJoernWorkspace;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisArtifactCategory;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisJobId;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgCommand;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.ArtifactReference;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.JoernArtifactCollectionResult;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.JoernCpgDiagnostic;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.JoernCpgPolicy;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.JoernRuntimeResult;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.RequestMetadata;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.SourceRoot;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.SourceSnapshotId;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.SourceWorkspace;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.PRODUCER_SERVICE;
import static de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.SEMANTIC_ARTIFACT_SCHEMA_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JoernCpgAnalysisApplicationServiceTest {
    @Test
    void analyzesWithServiceOwnedPortsAndPreservesIncompleteDiagnostics() {
        var application = new JoernCpgAnalysisApplicationService(
            workspace(100),
            (command, workspace) -> new JoernRuntimeResult("joern 1.2.3", image(), "joern-cpg/run-1", List.of()),
            (command, runtimeResult) -> new JoernArtifactCollectionResult(
                List.of(reference("joern-cpg/run-1/cpg.bin.zip")),
                1,
                List.of(JoernCpgDiagnostic.warning(
                    command.metadata().sourceSnapshotId(),
                    "JOERN_ARTIFACT_MISSING",
                    "callgraph missing",
                    "callgraph.json",
                    true
                ))
            )
        );

        var result = application.analyze(command());

        assertEquals(AnalysisCompleteness.INCOMPLETE, result.completeness());
        assertEquals("joern 1.2.3", result.summary().joernVersion());
        assertEquals(1, result.summary().producedArtifactCount());
        assertEquals(List.of("JOERN_ARTIFACT_MISSING"), result.diagnostics().stream().map(JoernCpgDiagnostic::code).toList());
    }

    @Test
    void rejectsOversizedWorkspaceBeforeInvokingJoern() {
        var runtimeInvoked = new AtomicBoolean();
        var application = new JoernCpgAnalysisApplicationService(
            workspace(1_001),
            (command, workspace) -> {
                runtimeInvoked.set(true);
                return new JoernRuntimeResult("joern 1.2.3", image(), "joern-cpg/run-1", List.of());
            },
            emptyCollector()
        );

        assertThrows(IllegalArgumentException.class, () -> application.analyze(command()));
        assertFalse(runtimeInvoked.get());
    }

    @Test
    void propagatesUnavailableJoernAndTimeouts() {
        var unavailable = new JoernCpgAnalysisApplicationService(
            workspace(100),
            (command, workspace) -> {
                throw new JoernRuntimeUnavailableException("Joern unavailable");
            },
            emptyCollector()
        );
        var timeout = new JoernCpgAnalysisApplicationService(
            workspace(100),
            (command, workspace) -> {
                throw new JoernCpgAnalysisTimeoutException("timeout");
            },
            emptyCollector()
        );

        assertThrows(JoernRuntimeUnavailableException.class, () -> unavailable.analyze(command()));
        assertThrows(JoernCpgAnalysisTimeoutException.class, () -> timeout.analyze(command()));
    }

    @Test
    void reportsUnknownCompletenessWhenJoernProducesNoArtifacts() {
        var application = new JoernCpgAnalysisApplicationService(
            workspace(100),
            (command, workspace) -> new JoernRuntimeResult("joern 1.2.3", image(), "joern-cpg/run-1", List.of()),
            emptyCollector()
        );

        var result = application.analyze(command());

        assertEquals(AnalysisCompleteness.UNKNOWN, result.completeness());
        assertEquals(0, result.summary().producedArtifactCount());
    }

    @Test
    void coversApplicationExceptionsWithCauses() {
        var cause = new IllegalStateException("cause");

        assertEquals(cause, new JoernCpgArtifactException("artifact", cause).getCause());
        assertEquals(cause, new JoernRuntimeUnavailableException("runtime", cause).getCause());
    }

    private static JoernWorkspacePort workspace(long bytes) {
        return command -> new ResolvedJoernWorkspace(
            command.metadata().sourceSnapshotId(),
            "workspace-1",
            Path.of("build/test-workspace"),
            List.of(Path.of("build/test-workspace/src/main/java")),
            bytes
        );
    }

    private static JoernArtifactCollectorPort emptyCollector() {
        return (command, runtimeResult) -> new JoernArtifactCollectionResult(List.of(), 1, List.of());
    }

    private static AnalysisArtifactReference reference(String path) {
        return new AnalysisArtifactReference(
            new ArtifactReference(path, "application/vnd.forensic-analytics.joern-cpg.v1+binary", "a".repeat(64), 3),
            AnalysisArtifactCategory.STATIC,
            PRODUCER_SERVICE,
            SEMANTIC_ARTIFACT_SCHEMA_VERSION,
            AnalysisCompleteness.COMPLETE
        );
    }

    private static AnalyzeJoernCpgCommand command() {
        return new AnalyzeJoernCpgCommand(
            new RequestMetadata(
                "request-1",
                "idempotency-1",
                "joern-cpg-analysis-v1",
                "correlation-1",
                new AnalysisRunId("run-1"),
                new AnalysisJobId("job-1"),
                new SourceSnapshotId("snapshot-1"),
                "joern-cpg-analysis-service-test",
                Map.of("tenant", "demo")
            ),
            new JoernCpgPolicy(
                2,
                1_000,
                10_000,
                60,
                image(),
                "queries-v1",
                true,
                true,
                true
            ),
            new SourceWorkspace("workspace-1", List.of(new SourceRoot("src/main/java", "java")), List.of())
        );
    }

    private static String image() {
        return "ghcr.io/joernio/joern@sha256:" + "a".repeat(64);
    }
}
