package de.burger.forensics.analytics.services.cliclient.application.port.out;

import de.burger.forensics.analytics.services.cliclient.domain.CliClientSubmissionCommand;
import de.burger.forensics.analytics.services.cliclient.domain.CliClientSubmissionResult;

public interface RepositoryAnalysisSubmissionPort {
    CliClientSubmissionResult submit(CliClientSubmissionCommand command);
}
