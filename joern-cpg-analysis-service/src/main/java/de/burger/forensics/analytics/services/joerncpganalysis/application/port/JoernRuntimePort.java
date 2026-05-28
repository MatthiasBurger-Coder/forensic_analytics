package de.burger.forensics.analytics.services.joerncpganalysis.application.port;

import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgCommand;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.JoernRuntimeResult;

public interface JoernRuntimePort {
    JoernRuntimeResult analyze(AnalyzeJoernCpgCommand command, ResolvedJoernWorkspace workspace);
}
