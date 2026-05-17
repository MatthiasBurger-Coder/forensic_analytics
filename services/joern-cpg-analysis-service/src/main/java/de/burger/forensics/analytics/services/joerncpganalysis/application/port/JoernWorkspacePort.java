package de.burger.forensics.analytics.services.joerncpganalysis.application.port;

import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgCommand;

public interface JoernWorkspacePort {
    ResolvedJoernWorkspace resolve(AnalyzeJoernCpgCommand command);
}
