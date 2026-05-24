package de.burger.forensics.analytics.services.repositorysource.application;

import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryWorkspaceIdGenerator;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryWorkspaceRepository;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.CheckoutResult;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.CheckoutStatus;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.Diagnostic;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryIdentity;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspace;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceBranch;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceBranchSelector;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceBranchStatus;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceStatus;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourceSnapshotId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceBranchId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceTitle;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.requireText;
import static de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.safeAttributes;

public final class RepositoryWorkspaceApplicationService {
    private final RepositoryWorkspaceRepository repository;
    private final RepositoryWorkspaceIdGenerator idGenerator;
    private final Clock clock;
    private final Map<String, IdempotentResult<RepositoryWorkspace>> workspaceCreateResults = new HashMap<>();
    private final Map<String, IdempotentResult<RepositoryWorkspaceBranch>> branchCreateResults = new HashMap<>();

    public RepositoryWorkspaceApplicationService(
        RepositoryWorkspaceRepository repository,
        RepositoryWorkspaceIdGenerator idGenerator,
        Clock clock
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.idGenerator = Objects.requireNonNull(idGenerator, "id generator must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public synchronized RepositoryWorkspace createOrReuseRepositoryWorkspace(
        String idempotencyKey,
        RepositoryIdentity repositoryIdentity,
        Map<String, String> attributes
    ) {
        var key = requireText(idempotencyKey, "idempotency key");
        Objects.requireNonNull(repositoryIdentity, "repository identity must not be null");
        var safeAttributes = safeAttributes(attributes);
        var fingerprint = String.join("|", repositoryIdentity.repositoryKey().value(), safeAttributes.toString());
        var replay = workspaceCreateResults.get(key);
        if (replay != null) {
            return replay.sameFingerprintOrThrow(fingerprint);
        }

        var workspace = repository.findByRepositoryKey(repositoryIdentity.repositoryKey())
            .orElseGet(() -> createWorkspace(repositoryIdentity, safeAttributes));
        workspaceCreateResults.put(key, new IdempotentResult<>(fingerprint, workspace));
        return workspace;
    }

    public synchronized RepositoryWorkspaceBranch createOrReuseRepositoryWorkspaceBranch(
        String idempotencyKey,
        WorkspaceId workspaceId,
        RepositoryWorkspaceBranchSelector branchSelector
    ) {
        var key = requireText(idempotencyKey, "idempotency key");
        Objects.requireNonNull(workspaceId, "workspace id must not be null");
        Objects.requireNonNull(branchSelector, "branch selector must not be null");
        var repositoryBranch = branchSelector.requireBranch();
        var fingerprint = String.join("|", workspaceId.value(), repositoryBranch, branchSelector.commit());
        var replay = branchCreateResults.get(key);
        if (replay != null) {
            return replay.sameFingerprintOrThrow(fingerprint);
        }

        var workspace = getRepositoryWorkspace(workspaceId);
        var branch = workspace.branches().stream()
            .filter(existing -> existing.repositoryBranch().equals(repositoryBranch))
            .findFirst()
            .map(existing -> sameRequestedCommitOrThrow(existing, branchSelector))
            .orElseGet(() -> createBranch(workspace, branchSelector));
        branchCreateResults.put(key, new IdempotentResult<>(fingerprint, branch));
        return branch;
    }

    public synchronized RepositoryWorkspace getRepositoryWorkspace(WorkspaceId workspaceId) {
        return repository.findById(workspaceId)
            .orElseThrow(() -> new RepositoryWorkspaceNotFoundException("repository workspace was not found"));
    }

    public synchronized RepositoryWorkspaceBranch markBranchCheckoutStarted(
        WorkspaceId workspaceId,
        WorkspaceBranchId workspaceBranchId
    ) {
        var workspace = getRepositoryWorkspace(workspaceId);
        var branch = repository.findBranch(workspaceId, workspaceBranchId)
            .orElseThrow(() -> new RepositoryWorkspaceNotFoundException("repository workspace branch was not found"));
        return saveBranch(workspace, branch.checkingOut(clock.instant()));
    }

    public synchronized RepositoryWorkspaceBranch markBranchCheckoutCompleted(
        WorkspaceId workspaceId,
        WorkspaceBranchId workspaceBranchId,
        CheckoutResult checkout,
        SourceSnapshotId sourceSnapshotId,
        List<Diagnostic> diagnostics
    ) {
        var workspace = getRepositoryWorkspace(workspaceId);
        var branch = repository.findBranch(workspaceId, workspaceBranchId)
            .orElseThrow(() -> new RepositoryWorkspaceNotFoundException("repository workspace branch was not found"));
        requireMatchingCheckout(branch, checkout);
        var checkedOut = branch.checkedOut(
            Objects.requireNonNull(checkout, "checkout result must not be null").resolvedCommit(),
            sourceSnapshotId,
            checkout.sourceRoots(),
            clock.instant(),
            diagnostics
        );
        return saveBranch(workspace, checkedOut);
    }

    public synchronized RepositoryWorkspaceBranch markBranchCheckoutFailed(
        WorkspaceId workspaceId,
        WorkspaceBranchId workspaceBranchId,
        List<Diagnostic> diagnostics
    ) {
        var workspace = getRepositoryWorkspace(workspaceId);
        var branch = repository.findBranch(workspaceId, workspaceBranchId)
            .orElseThrow(() -> new RepositoryWorkspaceNotFoundException("repository workspace branch was not found"));
        return saveBranch(workspace, branch.failed(clock.instant(), diagnostics));
    }

    private RepositoryWorkspace createWorkspace(
        RepositoryIdentity repositoryIdentity,
        Map<String, String> safeAttributes
    ) {
        var now = clock.instant();
        return repository.save(new RepositoryWorkspace(
            idGenerator.newWorkspaceId(),
            WorkspaceTitle.fromRepositoryName(repositoryIdentity.repositoryName()),
            repositoryIdentity,
            RepositoryWorkspaceStatus.READY,
            now,
            now,
            List.of(),
            List.of(Diagnostic.info("REPOSITORY_WORKSPACE_READY", "Repository workspace is ready")),
            safeAttributes
        ));
    }

    private RepositoryWorkspaceBranch createBranch(
        RepositoryWorkspace workspace,
        RepositoryWorkspaceBranchSelector branchSelector
    ) {
        var now = clock.instant();
        var branch = new RepositoryWorkspaceBranch(
            idGenerator.newWorkspaceBranchId(),
            workspace.workspaceId(),
            branchSelector.requireBranch(),
            branchSelector.commit(),
            "",
            null,
            RepositoryWorkspaceBranchStatus.CHECKING_OUT,
            List.of(),
            null,
            now,
            List.of(Diagnostic.info("REPOSITORY_WORKSPACE_BRANCH_CREATED", "Repository workspace branch was created"))
        );
        repository.save(workspace.withBranch(branch, now));
        return branch;
    }

    private RepositoryWorkspaceBranch saveBranch(RepositoryWorkspace workspace, RepositoryWorkspaceBranch branch) {
        repository.save(workspace.withBranch(branch, clock.instant()));
        return branch;
    }

    private static RepositoryWorkspaceBranch sameRequestedCommitOrThrow(
        RepositoryWorkspaceBranch existing,
        RepositoryWorkspaceBranchSelector branchSelector
    ) {
        if (!existing.requestedCommit().equals(branchSelector.commit())) {
            throw new IllegalArgumentException("repository branch already exists with a different requested commit");
        }
        return existing;
    }

    private static void requireMatchingCheckout(RepositoryWorkspaceBranch branch, CheckoutResult checkout) {
        var checkoutResult = Objects.requireNonNull(checkout, "checkout result must not be null");
        if (checkoutResult.status() != CheckoutStatus.CHECKED_OUT) {
            throw new IllegalArgumentException("checkout result must be checked out before branch state is completed");
        }
        if (!branch.repositoryBranch().equals(checkoutResult.requestedBranch())) {
            throw new IllegalArgumentException("checkout branch must match repository workspace branch");
        }
        if (!branch.requestedCommit().equals(checkoutResult.requestedCommit())) {
            throw new IllegalArgumentException("checkout commit must match repository workspace branch requested commit");
        }
    }

    private record IdempotentResult<T>(String fingerprint, T result) {
        private T sameFingerprintOrThrow(String requestedFingerprint) {
            if (!fingerprint.equals(requestedFingerprint)) {
                throw new IdempotencyConflictException("idempotency key was reused with different input");
            }
            return result;
        }
    }
}
