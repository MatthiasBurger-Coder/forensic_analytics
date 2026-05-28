package de.burger.forensics.analytics.services.joernanalysis.application.port;

import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgCommand;

public interface JoernWorkspacePort {
    ResolvedJoernWorkspace resolve(AnalyzeJoernCpgCommand command);
}
