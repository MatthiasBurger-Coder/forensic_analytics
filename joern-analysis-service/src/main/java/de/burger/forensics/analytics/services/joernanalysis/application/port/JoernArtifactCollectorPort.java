package de.burger.forensics.analytics.services.joernanalysis.application.port;

import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgCommand;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.JoernArtifactCollectionResult;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.JoernCpgDiagnostic;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.JoernRuntimeResult;

import java.util.List;

public interface JoernArtifactCollectorPort {
    JoernArtifactCollectionResult collect(AnalyzeJoernCpgCommand command, JoernRuntimeResult runtimeResult);

    default JoernArtifactCollectionResult collectUnavailable(
        AnalyzeJoernCpgCommand command,
        ResolvedJoernWorkspace workspace,
        JoernCpgDiagnostic diagnostic
    ) {
        return new JoernArtifactCollectionResult(List.of(), 0, List.of(diagnostic));
    }
}
