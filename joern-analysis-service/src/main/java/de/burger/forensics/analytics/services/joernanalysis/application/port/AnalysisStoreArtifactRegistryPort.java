package de.burger.forensics.analytics.services.joernanalysis.application.port;

import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgResult;

public interface AnalysisStoreArtifactRegistryPort {
    void registerSemanticArtifacts(AnalyzeJoernCpgResult result);
}
