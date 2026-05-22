package de.burger.forensics.analytics.services.cliclient.application;

import de.burger.forensics.analytics.services.cliclient.application.port.out.RepositoryAnalysisSubmissionPort;
import de.burger.forensics.analytics.services.cliclient.domain.CliClientSubmissionCommand;
import de.burger.forensics.analytics.services.cliclient.domain.CliClientSubmissionResult;

import java.util.Objects;

public final class CliClientSubmissionService {
    private final RepositoryAnalysisSubmissionPort submissionPort;

    public CliClientSubmissionService(RepositoryAnalysisSubmissionPort submissionPort) {
        this.submissionPort = Objects.requireNonNull(submissionPort, "submissionPort must not be null");
    }

    public CliClientSubmissionResult submit(CliClientSubmissionCommand command) {
        return submissionPort.submit(Objects.requireNonNull(command, "command must not be null"));
    }
}
