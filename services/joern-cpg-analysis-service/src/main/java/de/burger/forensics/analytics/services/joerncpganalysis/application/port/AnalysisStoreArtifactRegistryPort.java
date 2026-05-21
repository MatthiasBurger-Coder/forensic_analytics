package de.burger.forensics.analytics.services.joerncpganalysis.application.port;

import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgResult;

public interface AnalysisStoreArtifactRegistryPort {
    void registerSemanticArtifacts(AnalyzeJoernCpgResult result);
}
