package de.burger.forensics.analytics.services.joerncpganalysis.application;

import de.burger.forensics.analytics.services.joerncpganalysis.application.port.JoernArtifactCollectorPort;
import de.burger.forensics.analytics.services.joerncpganalysis.application.port.JoernRuntimePort;
import de.burger.forensics.analytics.services.joerncpganalysis.application.port.JoernWorkspacePort;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgCommand;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgResult;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.JoernCpgDiagnostic;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.JoernCpgSummary;

import java.util.ArrayList;
import java.util.Objects;

import static de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.PRODUCER_SERVICE;
import static de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.SEMANTIC_ARTIFACT_SCHEMA_VERSION;
import static de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.completeness;

public final class JoernCpgAnalysisApplicationService {
    private final JoernWorkspacePort workspacePort;
    private final JoernRuntimePort runtimePort;
    private final JoernArtifactCollectorPort artifactCollector;

    public JoernCpgAnalysisApplicationService(
        JoernWorkspacePort workspacePort,
        JoernRuntimePort runtimePort,
        JoernArtifactCollectorPort artifactCollector
    ) {
        this.workspacePort = Objects.requireNonNull(workspacePort, "workspace port must not be null");
        this.runtimePort = Objects.requireNonNull(runtimePort, "runtime port must not be null");
        this.artifactCollector = Objects.requireNonNull(artifactCollector, "artifact collector must not be null");
    }

    public AnalyzeJoernCpgResult analyze(AnalyzeJoernCpgCommand command) {
        var verifiedCommand = Objects.requireNonNull(command, "command must not be null");
        var workspace = workspacePort.resolve(verifiedCommand);
        if (workspace.workspaceBytes() > verifiedCommand.policy().maxWorkspaceBytes()) {
            throw new IllegalArgumentException("workspace byte size exceeds scan policy");
        }
        var runtimeResult = runtimePort.analyze(verifiedCommand, workspace);
        var collectionResult = artifactCollector.collect(verifiedCommand, runtimeResult);

        var diagnostics = new ArrayList<JoernCpgDiagnostic>();
        diagnostics.addAll(runtimeResult.diagnostics());
        diagnostics.addAll(collectionResult.diagnostics());
        var resultCompleteness = collectionResult.artifacts().isEmpty()
            ? AnalysisCompleteness.UNKNOWN
            : completeness(diagnostics);
        var summary = new JoernCpgSummary(
            workspace.sourceRootPaths().size(),
            collectionResult.artifacts().size(),
            collectionResult.missingArtifactCount(),
            runtimeResult.joernVersion(),
            runtimeResult.joernImageReference(),
            verifiedCommand.policy().queryBundleVersion(),
            PRODUCER_SERVICE,
            SEMANTIC_ARTIFACT_SCHEMA_VERSION
        );

        return new AnalyzeJoernCpgResult(
            verifiedCommand.metadata(),
            resultCompleteness,
            collectionResult.artifacts(),
            summary,
            diagnostics
        );
    }
}
