package de.burger.forensics.analytics.services.joernanalysis.application.port;

import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgCommand;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.JoernRuntimeResult;

public interface JoernRuntimePort {
    JoernRuntimeResult analyze(AnalyzeJoernCpgCommand command, ResolvedJoernWorkspace workspace);
}
