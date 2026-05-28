package de.burger.forensics.analytics.services.gateway.application.port;

import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.RepositoryToBtmSubmission;
import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.StatusRequest;
import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.SubmissionRequest;

public interface RepositoryToBtmOrchestrationPort {
    RepositoryToBtmSubmission start(SubmissionRequest request);

    RepositoryToBtmSubmission status(StatusRequest request);
}
