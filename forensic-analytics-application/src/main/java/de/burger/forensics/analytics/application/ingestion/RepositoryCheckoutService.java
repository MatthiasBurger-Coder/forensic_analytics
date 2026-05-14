package de.burger.forensics.analytics.application.ingestion;

import de.burger.forensics.analytics.application.ingestion.command.RepositoryCheckoutRequest;
import de.burger.forensics.analytics.application.ingestion.port.RepositoryCheckoutPort;
import de.burger.forensics.analytics.domain.repository.BranchReference;
import de.burger.forensics.analytics.domain.repository.CheckoutResult;
import de.burger.forensics.analytics.domain.repository.CommitReference;
import de.burger.forensics.analytics.domain.workspace.WorkspacePreparationStatus;

import java.util.Objects;

public final class RepositoryCheckoutService {
    private final RepositoryCheckoutPort repositoryCheckoutPort;

    public RepositoryCheckoutService(RepositoryCheckoutPort repositoryCheckoutPort) {
        this.repositoryCheckoutPort = Objects.requireNonNull(
            repositoryCheckoutPort,
            "repositoryCheckoutPort must not be null"
        );
    }

    public CheckoutResult checkout(RepositoryCheckoutRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        requireCheckoutTarget(request.branch(), request.commit());
        requireReadyWorkspace(request);
        return Objects.requireNonNull(
            repositoryCheckoutPort.checkout(request),
            "checkout result must not be null"
        );
    }

    void requireCheckoutTarget(BranchReference branch, CommitReference commit) {
        Objects.requireNonNull(branch, "branch must not be null");
        Objects.requireNonNull(commit, "commit must not be null");
        if (branch.required() && branch.name().isEmpty()) {
            throw new RepositoryAnalysisIngestionException("Required branch reference is missing");
        }
        if (commit.required() && commit.hash().isEmpty()) {
            throw new RepositoryAnalysisIngestionException("Required commit reference is missing");
        }
        if (branch.name().isEmpty() && commit.hash().isEmpty()) {
            throw new RepositoryAnalysisIngestionException("Branch or commit reference is required");
        }
    }

    private static void requireReadyWorkspace(RepositoryCheckoutRequest request) {
        if (!WorkspacePreparationStatus.READY.equals(request.workspace().status())) {
            throw new RepositoryCheckoutException(
                "Repository checkout requires a ready workspace: " + request.workspace().status()
            );
        }
    }
}
