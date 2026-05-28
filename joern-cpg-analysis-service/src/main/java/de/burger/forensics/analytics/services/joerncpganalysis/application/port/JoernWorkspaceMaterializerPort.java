package de.burger.forensics.analytics.services.joerncpganalysis.application.port;

import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.MaterializeJoernWorkspaceCommand;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.SourceWorkspace;

public interface JoernWorkspaceMaterializerPort {
    SourceWorkspace materialize(MaterializeJoernWorkspaceCommand command);
}
