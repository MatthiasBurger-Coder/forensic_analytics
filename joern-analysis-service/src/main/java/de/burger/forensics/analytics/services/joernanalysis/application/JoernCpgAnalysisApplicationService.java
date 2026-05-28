package de.burger.forensics.analytics.services.joernanalysis.application;

import de.burger.forensics.analytics.services.joernanalysis.application.port.AnalysisStoreArtifactRegistryPort;
import de.burger.forensics.analytics.services.joernanalysis.application.port.JoernArtifactCollectorPort;
import de.burger.forensics.analytics.services.joernanalysis.application.port.JoernRuntimePort;
import de.burger.forensics.analytics.services.joernanalysis.application.port.JoernWorkspaceMaterializerPort;
import de.burger.forensics.analytics.services.joernanalysis.application.port.JoernWorkspacePort;
import de.burger.forensics.analytics.services.joernanalysis.application.port.ResolvedJoernWorkspace;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgCommand;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgResult;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.JoernCpgPolicy;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.MaterializeJoernWorkspaceCommand;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.MaterializeJoernWorkspaceResult;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.JoernCpgDiagnostic;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.JoernRuntimeResult;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.JoernCpgSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.PRODUCER_SERVICE;
import static de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.SEMANTIC_ARTIFACT_SCHEMA_VERSION;
import static de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.completeness;

public final class JoernCpgAnalysisApplicationService {
    private final JoernWorkspaceMaterializerPort materializerPort;
    private final JoernWorkspacePort workspacePort;
    private final JoernRuntimePort runtimePort;
    private final JoernArtifactCollectorPort artifactCollector;
    private final AnalysisStoreArtifactRegistryPort artifactRegistryPort;

    public JoernCpgAnalysisApplicationService(
        JoernWorkspaceMaterializerPort materializerPort,
        JoernWorkspacePort workspacePort,
        JoernRuntimePort runtimePort,
        JoernArtifactCollectorPort artifactCollector,
        AnalysisStoreArtifactRegistryPort artifactRegistryPort
    ) {
        this.materializerPort = Objects.requireNonNull(materializerPort, "materializer port must not be null");
        this.workspacePort = Objects.requireNonNull(workspacePort, "workspace port must not be null");
        this.runtimePort = Objects.requireNonNull(runtimePort, "runtime port must not be null");
        this.artifactCollector = Objects.requireNonNull(artifactCollector, "artifact collector must not be null");
        this.artifactRegistryPort = Objects.requireNonNull(artifactRegistryPort, "artifact registry port must not be null");
    }

    public MaterializeJoernWorkspaceResult materialize(MaterializeJoernWorkspaceCommand command) {
        var verifiedCommand = Objects.requireNonNull(command, "command must not be null");
        var workspace = materializerPort.materialize(verifiedCommand);
        return new MaterializeJoernWorkspaceResult(
            verifiedCommand.metadata(),
            workspace,
            List.of(JoernCpgDiagnostic.info(
                verifiedCommand.metadata().sourceSnapshotId(),
                "JOERN_WORKSPACE_MATERIALIZED",
                "Joern-owned workspace materialization accepted"
            ))
        );
    }

    public AnalyzeJoernCpgResult analyze(AnalyzeJoernCpgCommand command) {
        var verifiedCommand = Objects.requireNonNull(command, "command must not be null");
        var workspace = workspacePort.resolve(verifiedCommand);
        if (workspace.workspaceBytes() > verifiedCommand.policy().maxWorkspaceBytes()) {
            throw new IllegalArgumentException("workspace byte size exceeds scan policy");
        }
        var runtimeResult = runtimeResult(verifiedCommand, workspace);
        if (runtimeResult.failureDiagnostic() != null) {
            var unavailable = unavailableResult(verifiedCommand, workspace, runtimeResult.failureDiagnostic());
            artifactRegistryPort.registerSemanticArtifacts(unavailable);
            return unavailable;
        }
        var collectionResult = artifactCollector.collect(verifiedCommand, runtimeResult.runtimeResult());

        var diagnostics = new ArrayList<JoernCpgDiagnostic>();
        diagnostics.addAll(runtimeResult.runtimeResult().diagnostics());
        diagnostics.addAll(collectionResult.diagnostics());
        var resultCompleteness = collectionResult.artifacts().isEmpty()
            ? AnalysisCompleteness.UNKNOWN
            : completeness(diagnostics);
        var summary = new JoernCpgSummary(
            workspace.sourceRootPaths().size(),
            collectionResult.artifacts().size(),
            collectionResult.missingArtifactCount(),
            runtimeResult.runtimeResult().joernVersion(),
            runtimeResult.runtimeResult().joernImageReference(),
            verifiedCommand.policy().queryBundleVersion(),
            PRODUCER_SERVICE,
            SEMANTIC_ARTIFACT_SCHEMA_VERSION
        );

        var result = new AnalyzeJoernCpgResult(
            verifiedCommand.metadata(),
            resultCompleteness,
            collectionResult.artifacts(),
            summary,
            diagnostics
        );
        artifactRegistryPort.registerSemanticArtifacts(result);
        return result;
    }

    private RuntimeOutcome runtimeResult(AnalyzeJoernCpgCommand command, ResolvedJoernWorkspace workspace) {
        try {
            return new RuntimeOutcome(runtimePort.analyze(command, workspace), null);
        } catch (JoernRuntimeUnavailableException error) {
            return new RuntimeOutcome(null, JoernCpgDiagnostic.error(
                command.metadata().sourceSnapshotId(),
                "JOERN_RUNTIME_UNAVAILABLE",
                "Joern runtime unavailable.",
                true
            ));
        } catch (JoernCpgAnalysisTimeoutException error) {
            return new RuntimeOutcome(null, JoernCpgDiagnostic.error(
                command.metadata().sourceSnapshotId(),
                "JOERN_RUNTIME_TIMEOUT",
                "Joern CPG analysis timed out.",
                true
            ));
        }
    }

    private AnalyzeJoernCpgResult unavailableResult(
        AnalyzeJoernCpgCommand command,
        ResolvedJoernWorkspace workspace,
        JoernCpgDiagnostic diagnostic
    ) {
        var collectionResult = artifactCollector.collectUnavailable(command, workspace, diagnostic);
        return new AnalyzeJoernCpgResult(
            command.metadata(),
            AnalysisCompleteness.UNKNOWN,
            collectionResult.artifacts(),
            new JoernCpgSummary(
                workspace.sourceRootPaths().size(),
                collectionResult.artifacts().size(),
                requiredArtifactCount(command.policy()),
                "unavailable",
                command.policy().joernImageReference(),
                command.policy().queryBundleVersion(),
                PRODUCER_SERVICE,
                SEMANTIC_ARTIFACT_SCHEMA_VERSION
            ),
            collectionResult.diagnostics()
        );
    }

    private static int requiredArtifactCount(JoernCpgPolicy policy) {
        var count = 1;
        if (policy.requireCallgraph()) {
            count++;
        }
        if (policy.requireControlflow()) {
            count++;
        }
        if (policy.requireDataflow()) {
            count += 2;
        }
        return count;
    }

    private record RuntimeOutcome(JoernRuntimeResult runtimeResult, JoernCpgDiagnostic failureDiagnostic) {
    }
}
