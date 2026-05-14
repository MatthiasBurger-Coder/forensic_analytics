package de.burger.forensics.analytics.application.ingestion.port;

import de.burger.forensics.analytics.application.ingestion.command.RepositoryCheckoutRequest;
import de.burger.forensics.analytics.domain.repository.CheckoutResult;

public interface RepositoryCheckoutPort {
    CheckoutResult checkout(RepositoryCheckoutRequest request);
}
