package de.burger.forensics.analytics.services.repositorysource.application;

import de.burger.forensics.analytics.services.repositorysource.application.port.PreparedWorkspace;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryCheckoutPort;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryMetadataPort;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryMetadataPreviewPolicy;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryMetadataResolution;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryWorkspacePort;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryWorkspaceIdGenerator;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryWorkspaceRepository;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.CheckoutResult;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.CheckoutStatus;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.Diagnostic;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryIdentity;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryReference;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspace;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceBranch;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceBranchSelector;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceBranchStatus;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceStatus;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RevisionSelector;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourceSnapshotId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceBranchId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspacePolicy;
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
    private final RepositoryWorkspacePort workspacePort;
    private final RepositoryCheckoutPort checkoutPort;
    private final RepositoryMetadataPort metadataPort;
    private final Clock clock;
    private final Map<String, IdempotentResult<RepositoryWorkspace>> workspaceCreateResults = new HashMap<>();
    private final Map<String, IdempotentResult<RepositoryWorkspaceBranch>> branchCreateResults = new HashMap<>();
    private final Map<String, IdempotentResult<RepositoryWorkspace>> workspaceCheckoutResults = new HashMap<>();
    private final Map<String, IdempotentResult<RefreshRepositoryWorkspaceBranchResult>> branchRefreshResults = new HashMap<>();

    public RepositoryWorkspaceApplicationService(
        RepositoryWorkspaceRepository repository,
        RepositoryWorkspaceIdGenerator idGenerator,
        RepositoryWorkspacePort workspacePort,
        RepositoryCheckoutPort checkoutPort,
        RepositoryMetadataPort metadataPort,
        Clock clock
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.idGenerator = Objects.requireNonNull(idGenerator, "id generator must not be null");
        this.workspacePort = Objects.requireNonNull(workspacePort, "workspace port must not be null");
        this.checkoutPort = Objects.requireNonNull(checkoutPort, "checkout port must not be null");
        this.metadataPort = Objects.requireNonNull(metadataPort, "metadata port must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public synchronized RepositoryWorkspaceMetadataPreview previewRepositoryWorkspaceMetadata(
        String schemaVersion,
        String correlationId,
        RepositoryReference repositoryReference,
        RepositoryMetadataPreviewPolicy metadataPolicy,
        Map<String, String> attributes
    ) {
        requireText(schemaVersion, "schema version");
        requireText(correlationId, "correlation id");
        var safeAttributes = safeAttributes(attributes);
        var resolution = metadataPort.resolveMetadata(
            Objects.requireNonNull(repositoryReference, "repository reference must not be null"),
            Objects.requireNonNull(metadataPolicy, "metadata preview policy must not be null")
        );
        return new RepositoryWorkspaceMetadataPreview(
            resolution.repository(),
            WorkspaceTitle.fromRepositoryName(resolution.repository().repositoryName()),
            resolution.diagnostics(),
            safeAttributes
        );
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

    public synchronized RepositoryWorkspace createOrReuseRepositoryWorkspaceWithCheckout(
        String idempotencyKey,
        String schemaVersion,
        String correlationId,
        RepositoryReference repositoryReference,
        RepositoryWorkspaceBranchSelector branchSelector,
        WorkspacePolicy workspacePolicy,
        Map<String, String> attributes
    ) {
        var key = requireText(idempotencyKey, "idempotency key");
        requireText(schemaVersion, "schema version");
        requireText(correlationId, "correlation id");
        Objects.requireNonNull(repositoryReference, "repository reference must not be null");
        Objects.requireNonNull(branchSelector, "branch selector must not be null");
        Objects.requireNonNull(workspacePolicy, "workspace policy must not be null");
        var safeAttributes = safeAttributes(attributes);
        var metadata = metadataPort.resolveMetadata(
            repositoryReference,
            new RepositoryMetadataPreviewPolicy(workspacePolicy.timeoutSeconds())
        );
        var resolvedSelector = resolvedBranchSelector(branchSelector, metadata);
        var fingerprint = workspaceCheckoutFingerprint(metadata.repository(), resolvedSelector, workspacePolicy, safeAttributes);
        var replay = workspaceCheckoutResults.get(key);
        if (replay != null) {
            return replay.sameFingerprintOrThrow(fingerprint);
        }

        var workspace = repository.findByRepositoryKey(metadata.repository().repositoryKey())
            .orElseGet(() -> createWorkspace(metadata.repository(), safeAttributes));
        var branch = workspace.branches().stream()
            .filter(existing -> existing.repositoryBranch().equals(resolvedSelector.requireBranch()))
            .findFirst()
            .map(existing -> sameRequestedCommitOrThrow(existing, resolvedSelector))
            .orElseGet(() -> createBranch(workspace, resolvedSelector));
        if (hasCompletedCheckout(branch)) {
            var existingWorkspace = getRepositoryWorkspace(workspace.workspaceId());
            workspaceCheckoutResults.put(key, new IdempotentResult<>(fingerprint, existingWorkspace));
            return existingWorkspace;
        }

        checkoutWorkspaceBranch(workspace, branch, repositoryReference, resolvedSelector, workspacePolicy);
        var result = getRepositoryWorkspace(workspace.workspaceId());
        workspaceCheckoutResults.put(key, new IdempotentResult<>(fingerprint, result));
        return result;
    }

    public synchronized RefreshRepositoryWorkspaceBranchResult refreshRepositoryWorkspaceBranch(
        String idempotencyKey,
        String schemaVersion,
        String correlationId,
        WorkspaceId workspaceId,
        WorkspaceBranchId workspaceBranchId,
        WorkspacePolicy workspacePolicy,
        Map<String, String> attributes
    ) {
        var key = requireText(idempotencyKey, "idempotency key");
        requireText(schemaVersion, "schema version");
        requireText(correlationId, "correlation id");
        Objects.requireNonNull(workspacePolicy, "workspace policy must not be null");
        var safeAttributes = safeAttributes(attributes);
        var workspace = getRepositoryWorkspace(workspaceId);
        var branch = repository.findBranch(workspaceId, workspaceBranchId)
            .orElseThrow(() -> new RepositoryWorkspaceNotFoundException("repository workspace branch was not found"));
        var fingerprint = String.join(
            "|",
            workspaceId.value(),
            workspaceBranchId.value(),
            branch.repositoryBranch(),
            workspacePolicyFingerprint(workspacePolicy),
            safeAttributes.toString()
        );
        var replay = branchRefreshResults.get(key);
        if (replay != null) {
            return replay.sameFingerprintOrThrow(fingerprint);
        }
        if (branch.resolvedCommit().isBlank() || branch.sourceSnapshotId() == null) {
            throw new IllegalArgumentException("repository workspace branch must be checked out before refresh");
        }

        saveBranch(workspace, branchWithStatus(
            branch,
            RepositoryWorkspaceBranchStatus.UPDATING,
            branch.resolvedCommit(),
            branch.sourceSnapshotId(),
            branch.sourceRoots(),
            branch.diagnostics()
        ));
        var checkout = checkoutForBranch(
            workspace,
            branch,
            repositoryReference(workspace.repository()),
            refreshSelector(branch),
            workspacePolicy,
            false
        );
        var result = refreshResult(branch, checkout, safeAttributes);
        branchRefreshResults.put(key, new IdempotentResult<>(fingerprint, result));
        return result;
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

    private RepositoryWorkspaceBranch checkoutWorkspaceBranch(
        RepositoryWorkspace workspace,
        RepositoryWorkspaceBranch branch,
        RepositoryReference repositoryReference,
        RepositoryWorkspaceBranchSelector branchSelector,
        WorkspacePolicy workspacePolicy
    ) {
        saveBranch(workspace, branch.checkingOut(clock.instant()));
        var checkout = checkoutForBranch(workspace, branch, repositoryReference, branchSelector, workspacePolicy, true);
        var sourceSnapshotId = RepositorySourceSnapshotFactory.sourceSnapshotId(
            repositoryReference,
            revision(branchSelector),
            checkout
        );
        var checkedOut = branch.checkedOut(
            checkout.resolvedCommit(),
            sourceSnapshotId,
            checkout.sourceRoots(),
            clock.instant(),
            checkout.diagnostics()
        );
        return saveBranch(getRepositoryWorkspace(workspace.workspaceId()), checkedOut);
    }

    private CheckoutResult checkoutForBranch(
        RepositoryWorkspace workspace,
        RepositoryWorkspaceBranch branch,
        RepositoryReference repositoryReference,
        RepositoryWorkspaceBranchSelector branchSelector,
        WorkspacePolicy workspacePolicy,
        boolean cleanupOnFailure
    ) {
        PreparedWorkspace preparedWorkspace = null;
        try {
            preparedWorkspace = workspacePort.prepareBranchCheckout(workspace.workspaceId(), branch.workspaceBranchId(), workspacePolicy);
            var checkout = checkoutPort.checkout(preparedWorkspace, repositoryReference, revision(branchSelector), workspacePolicy);
            requireMatchingCheckout(branch, checkout);
            return checkout;
        } catch (RuntimeException error) {
            if (cleanupOnFailure) {
                workspacePort.cleanupBranchCheckout(workspace.workspaceId(), branch.workspaceBranchId());
            }
            saveBranch(
                getRepositoryWorkspace(workspace.workspaceId()),
                branch.failed(clock.instant(), List.of(Diagnostic.error("REPOSITORY_CHECKOUT_FAILED", "Repository checkout failed")))
            );
            throw new IllegalStateException("Repository workspace branch checkout failed", error);
        }
    }

    private RefreshRepositoryWorkspaceBranchResult refreshResult(
        RepositoryWorkspaceBranch previous,
        CheckoutResult checkout,
        Map<String, String> safeAttributes
    ) {
        var previousCommit = previous.resolvedCommit();
        var previousSnapshot = previous.sourceSnapshotId();
        var changed = !previousCommit.equals(checkout.resolvedCommit());
        var next = changed
            ? branchWithStatus(
                previous,
                RepositoryWorkspaceBranchStatus.UPDATED,
                checkout.resolvedCommit(),
                RepositorySourceSnapshotFactory.sourceSnapshotId(
                    repositoryReference(getRepositoryWorkspace(previous.workspaceId()).repository()),
                    new RevisionSelector(previous.repositoryBranch(), true, previous.requestedCommit(), !previous.requestedCommit().isBlank()),
                    checkout
                ),
                checkout.sourceRoots(),
                checkout.diagnostics()
            )
            : branchWithStatus(
                previous,
                RepositoryWorkspaceBranchStatus.UP_TO_DATE,
                previousCommit,
                previousSnapshot,
                previous.sourceRoots(),
                List.of(Diagnostic.info("REPOSITORY_BRANCH_UP_TO_DATE", "Repository branch is up to date"))
            );
        var saved = saveBranch(getRepositoryWorkspace(previous.workspaceId()), next);
        return new RefreshRepositoryWorkspaceBranchResult(
            saved,
            changed,
            previousCommit,
            previousSnapshot,
            saved.diagnostics(),
            safeAttributes
        );
    }

    private RepositoryWorkspaceBranch branchWithStatus(
        RepositoryWorkspaceBranch branch,
        RepositoryWorkspaceBranchStatus status,
        String resolvedCommit,
        SourceSnapshotId sourceSnapshotId,
        List<de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourceRoot> sourceRoots,
        List<Diagnostic> diagnostics
    ) {
        return new RepositoryWorkspaceBranch(
            branch.workspaceBranchId(),
            branch.workspaceId(),
            branch.repositoryBranch(),
            branch.requestedCommit(),
            resolvedCommit,
            sourceSnapshotId,
            status,
            sourceRoots,
            clock.instant(),
            clock.instant(),
            diagnostics
        );
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

    private static boolean hasCompletedCheckout(RepositoryWorkspaceBranch branch) {
        return branch.status() == RepositoryWorkspaceBranchStatus.CHECKED_OUT
            || branch.status() == RepositoryWorkspaceBranchStatus.UP_TO_DATE
            || branch.status() == RepositoryWorkspaceBranchStatus.UPDATED;
    }

    private static RepositoryWorkspaceBranchSelector resolvedBranchSelector(
        RepositoryWorkspaceBranchSelector branchSelector,
        RepositoryMetadataResolution metadata
    ) {
        if (!branchSelector.branch().isBlank()) {
            return branchSelector;
        }
        if (!metadata.defaultBranchResolved() || metadata.repository().defaultBranch().isBlank()) {
            throw new IllegalArgumentException("repository default branch could not be resolved");
        }
        return new RepositoryWorkspaceBranchSelector(metadata.repository().defaultBranch(), branchSelector.commit());
    }

    private static RevisionSelector revision(RepositoryWorkspaceBranchSelector branchSelector) {
        return new RevisionSelector(
            branchSelector.requireBranch(),
            true,
            branchSelector.commit(),
            !branchSelector.commit().isBlank()
        );
    }

    private static RepositoryWorkspaceBranchSelector refreshSelector(RepositoryWorkspaceBranch branch) {
        return new RepositoryWorkspaceBranchSelector(branch.repositoryBranch(), branch.requestedCommit());
    }

    private static RepositoryReference repositoryReference(RepositoryIdentity repository) {
        return new RepositoryReference(repository.repositoryUrl(), "", Map.of());
    }

    private static String workspaceCheckoutFingerprint(
        RepositoryIdentity repository,
        RepositoryWorkspaceBranchSelector branchSelector,
        WorkspacePolicy policy,
        Map<String, String> safeAttributes
    ) {
        return String.join(
            "|",
            repository.repositoryKey().value(),
            branchSelector.requireBranch(),
            branchSelector.commit(),
            Boolean.toString(policy.ephemeral()),
            Boolean.toString(policy.allowShallowClone()),
            Boolean.toString(policy.allowPartialClone()),
            Boolean.toString(policy.allowSparseCheckout()),
            Long.toString(policy.timeoutSeconds()),
            Long.toString(policy.maxWorkspaceBytes()),
            safeAttributes.toString()
        );
    }

    private static String workspacePolicyFingerprint(WorkspacePolicy policy) {
        return String.join(
            "|",
            Boolean.toString(policy.ephemeral()),
            Boolean.toString(policy.allowShallowClone()),
            Boolean.toString(policy.allowPartialClone()),
            Boolean.toString(policy.allowSparseCheckout()),
            Long.toString(policy.timeoutSeconds()),
            Long.toString(policy.maxWorkspaceBytes())
        );
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
