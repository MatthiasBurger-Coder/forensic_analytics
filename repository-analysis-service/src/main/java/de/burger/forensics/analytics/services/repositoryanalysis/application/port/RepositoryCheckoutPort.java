package de.burger.forensics.analytics.services.repositoryanalysis.application.port;

import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.CheckoutResult;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RepositoryReference;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RevisionSelector;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.WorkspacePolicy;

public interface RepositoryCheckoutPort {
    CheckoutResult checkout(
        PreparedWorkspace workspace,
        RepositoryReference repository,
        RevisionSelector revision,
        WorkspacePolicy policy
    );
}
