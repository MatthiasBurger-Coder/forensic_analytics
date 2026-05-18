package de.burger.forensics.analytics.services.gateway.application.port;

import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.RepositoryPreparationCommand;
import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.RepositoryPreparationResult;

public interface RepositoryAnalysisPreparationPort {
    RepositoryPreparationResult prepare(RepositoryPreparationCommand command);
}
