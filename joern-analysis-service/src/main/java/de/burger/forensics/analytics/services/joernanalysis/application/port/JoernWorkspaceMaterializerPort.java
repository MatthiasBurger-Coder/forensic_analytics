package de.burger.forensics.analytics.services.joernanalysis.application.port;

import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.MaterializeJoernWorkspaceCommand;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.SourceWorkspace;

public interface JoernWorkspaceMaterializerPort {
    SourceWorkspace materialize(MaterializeJoernWorkspaceCommand command);
}
