package de.burger.forensics.analytics.services.joernanalysis.application;

import de.burger.forensics.analytics.services.joernanalysis.application.port.AnalysisStoreArtifactRegistryPort;
import de.burger.forensics.analytics.services.joernanalysis.application.port.JoernArtifactCollectorPort;
import de.burger.forensics.analytics.services.joernanalysis.application.port.JoernRuntimePort;
import de.burger.forensics.analytics.services.joernanalysis.application.port.JoernWorkspaceMaterializerPort;
import de.burger.forensics.analytics.services.joernanalysis.application.port.JoernWorkspacePort;
import de.burger.forensics.analytics.services.joernanalysis.application.port.ResolvedJoernWorkspace;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisArtifactCategory;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisJobId;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgCommand;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.ArtifactByteAccess;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.ArtifactByteCustody;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.ArtifactReference;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.JoernArtifactCollectionResult;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.JoernCpgDiagnostic;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.JoernCpgPolicy;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.JoernRuntimeResult;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.RequestMetadata;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.SourceRoot;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.SourceSnapshotId;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.SourceWorkspace;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.PRODUCER_SERVICE;
import static de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.SEMANTIC_ARTIFACT_SCHEMA_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JoernCpgAnalysisApplicationServiceTest {
    @Test
    void analyzesWithServiceOwnedPortsAndPreservesIncompleteDiagnostics() {
        var registered = new AtomicReference<de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgResult>();
        var application = new JoernCpgAnalysisApplicationService(
            materializer(),
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
            ),
            registered::set
        );

        var result = application.analyze(command());

        assertEquals(AnalysisCompleteness.INCOMPLETE, result.completeness());
        assertEquals("joern 1.2.3", result.summary().joernVersion());
        assertEquals(1, result.summary().producedArtifactCount());
        assertEquals(List.of("JOERN_ARTIFACT_MISSING"), result.diagnostics().stream().map(JoernCpgDiagnostic::code).toList());
        assertEquals(result, registered.get());
        assertEquals(List.of("joern-cpg/run-1/cpg.bin.zip"), registered.get().semanticArtifacts().stream()
            .map(reference -> reference.artifact().path())
            .toList());
    }

    @Test
    void rejectsOversizedWorkspaceBeforeInvokingJoern() {
        var runtimeInvoked = new AtomicBoolean();
        var application = new JoernCpgAnalysisApplicationService(
            materializer(),
            workspace(1_001),
            (command, workspace) -> {
                runtimeInvoked.set(true);
                return new JoernRuntimeResult("joern 1.2.3", image(), "joern-cpg/run-1", List.of());
            },
            emptyCollector(),
            registry()
        );

        assertThrows(IllegalArgumentException.class, () -> application.analyze(command()));
        assertFalse(runtimeInvoked.get());
    }

    @Test
    void returnsUnknownWhenJoernIsUnavailableAndPropagatesTimeouts() {
        var registered = new AtomicReference<de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgResult>();
        var unavailable = new JoernCpgAnalysisApplicationService(
            materializer(),
            workspace(100),
            (command, workspace) -> {
                throw new JoernRuntimeUnavailableException("Joern unavailable");
            },
            unavailableCollector(),
            registered::set
        );
        var timeoutRegistered = new AtomicReference<de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgResult>();
        var timeout = new JoernCpgAnalysisApplicationService(
            materializer(),
            workspace(100),
            (command, workspace) -> {
                throw new JoernCpgAnalysisTimeoutException("timeout");
            },
            unavailableCollector(),
            timeoutRegistered::set
        );

        var unavailableResult = unavailable.analyze(command());
        var minimalUnavailableResult = unavailable.analyze(command(false, false, false));

        assertEquals(AnalysisCompleteness.UNKNOWN, unavailableResult.completeness());
        assertEquals(5, unavailableResult.summary().missingArtifactCount());
        assertEquals(List.of("JOERN_RUNTIME_UNAVAILABLE"), unavailableResult.diagnostics().stream().map(JoernCpgDiagnostic::code).toList());
        assertEquals(List.of("joern-cpg/run-1/joern-provenance.json"), unavailableResult.semanticArtifacts().stream()
            .map(reference -> reference.artifact().path())
            .toList());
        assertEquals(1, minimalUnavailableResult.summary().missingArtifactCount());
        assertEquals(minimalUnavailableResult, registered.get());
        var timeoutResult = timeout.analyze(command());
        assertEquals(AnalysisCompleteness.UNKNOWN, timeoutResult.completeness());
        assertEquals(List.of("JOERN_RUNTIME_TIMEOUT"), timeoutResult.diagnostics().stream().map(JoernCpgDiagnostic::code).toList());
        assertEquals(timeoutResult, timeoutRegistered.get());
    }

    @Test
    void reportsUnknownCompletenessWhenJoernProducesNoArtifacts() {
        var application = new JoernCpgAnalysisApplicationService(
            materializer(),
            workspace(100),
            (command, workspace) -> new JoernRuntimeResult("joern 1.2.3", image(), "joern-cpg/run-1", List.of()),
            emptyCollector(),
            registry()
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
            "joern-workspace-" + command.metadata().sourceSnapshotId().value(),
            Path.of("build/test-workspace"),
            List.of(Path.of("build/test-workspace/src/main/java")),
            bytes
        );
    }

    private static JoernWorkspaceMaterializerPort materializer() {
        return command -> new SourceWorkspace(
            "joern-workspace-" + command.metadata().sourceSnapshotId().value(),
            command.sourceRoots(),
            List.of(
                new AnalysisArtifactReference(
                    command.sourcePackage().packageArtifact(),
                    AnalysisArtifactCategory.STATIC,
                    command.sourcePackage().producerService(),
                    command.sourcePackage().schemaVersion(),
                    command.sourcePackage().completeness(),
                    command.sourcePackage().byteAccess()
                ),
                new AnalysisArtifactReference(
                    command.buildOutputPackage().packageArtifact(),
                    AnalysisArtifactCategory.STATIC,
                    command.buildOutputPackage().producerService(),
                    command.buildOutputPackage().schemaVersion(),
                    command.buildOutputPackage().completeness(),
                    command.buildOutputPackage().byteAccess()
                )
            )
        );
    }

    private static JoernArtifactCollectorPort emptyCollector() {
        return (command, runtimeResult) -> new JoernArtifactCollectionResult(List.of(), 1, List.of());
    }

    private static JoernArtifactCollectorPort unavailableCollector() {
        return new JoernArtifactCollectorPort() {
            @Override
            public JoernArtifactCollectionResult collect(AnalyzeJoernCpgCommand command, JoernRuntimeResult runtimeResult) {
                return new JoernArtifactCollectionResult(List.of(), 1, List.of());
            }

            @Override
            public JoernArtifactCollectionResult collectUnavailable(
                AnalyzeJoernCpgCommand command,
                ResolvedJoernWorkspace workspace,
                JoernCpgDiagnostic diagnostic
            ) {
                return new JoernArtifactCollectionResult(
                    List.of(reference("joern-cpg/run-1/joern-provenance.json", AnalysisCompleteness.UNKNOWN)),
                    1,
                    List.of(diagnostic)
                );
            }
        };
    }

    private static AnalysisStoreArtifactRegistryPort registry() {
        return result -> {
        };
    }

    private static AnalysisArtifactReference reference(String path) {
        return reference(path, AnalysisCompleteness.COMPLETE);
    }

    private static AnalysisArtifactReference reference(String path, AnalysisCompleteness completeness) {
        return new AnalysisArtifactReference(
            new ArtifactReference(path, "application/vnd.forensic-analytics.joern-cpg.v1+binary", "a".repeat(64), 3),
            AnalysisArtifactCategory.STATIC,
            PRODUCER_SERVICE,
            SEMANTIC_ARTIFACT_SCHEMA_VERSION,
            completeness,
            new ArtifactByteAccess(
                PRODUCER_SERVICE,
                "analysis-job.v1.ArtifactBytes",
                "artifacts/" + path,
                ArtifactByteCustody.PRODUCER_RETAINED
            )
        );
    }

    private static AnalyzeJoernCpgCommand command() {
        return command(true, true, true);
    }

    private static AnalyzeJoernCpgCommand command(boolean requireCallgraph, boolean requireControlflow, boolean requireDataflow) {
        return new AnalyzeJoernCpgCommand(
            new RequestMetadata(
                "request-1",
                "idempotency-1",
                "joern-cpg-analysis-v1",
                "correlation-1",
                new AnalysisRunId("run-1"),
                new AnalysisJobId("job-1"),
                new SourceSnapshotId("snapshot-1"),
                "joern-analysis-service-test",
                Map.of("tenant", "demo")
            ),
            new JoernCpgPolicy(
                2,
                1_000,
                10_000,
                60,
                image(),
                "queries-v1",
                requireCallgraph,
                requireControlflow,
                requireDataflow
            ),
            new SourceWorkspace("joern-workspace-snapshot-1", List.of(new SourceRoot("src/main/java", "java")), List.of(reference("input.json")))
        );
    }

    private static String image() {
        return "ghcr.io/joernio/joern@sha256:" + "a".repeat(64);
    }
}
