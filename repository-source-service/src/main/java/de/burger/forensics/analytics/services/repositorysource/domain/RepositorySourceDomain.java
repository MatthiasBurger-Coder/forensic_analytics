package de.burger.forensics.analytics.services.repositorysource.domain;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

public final class RepositorySourceDomain {
    private static final long MAX_TIMEOUT_SECONDS = 3_600;
    private static final long MAX_WORKSPACE_BYTES = 107_374_182_400L;
    private static final int MAX_OPAQUE_ID_LENGTH = 128;
    private static final int MAX_PUBLIC_LABEL_LENGTH = 255;
    private static final int MAX_REPOSITORY_KEY_LENGTH = 512;
    private static final Pattern SHA_256 = Pattern.compile("[a-fA-F0-9]{64}");
    private static final Pattern COMMIT = Pattern.compile("[a-fA-F0-9]{7,64}");
    private static final Pattern OPAQUE_ID = Pattern.compile("[A-Za-z0-9_-]+");
    private static final Pattern REPOSITORY_KEY_PART = Pattern.compile("[a-z0-9][a-z0-9._-]*");
    private static final Pattern PRIVATE_IPV4 = Pattern.compile(
        "^(0\\.|127\\.|10\\.|192\\.168\\.|172\\.(1[6-9]|2[0-9]|3[0-1])\\.|169\\.254\\.|"
            + "100\\.(6[4-9]|[7-9][0-9]|1[01][0-9]|12[0-7])\\.|192\\.0\\.(0|2)\\.|"
            + "192\\.88\\.99\\.|198\\.(1[89]|51\\.100)\\.|203\\.0\\.113\\.|"
            + "(22[4-9]|23[0-9]|24[0-9]|25[0-5])\\.)"
    );
    private static final Set<String> SENSITIVE_TOKENS = Set.of(
        "authorization",
        "credential",
        "h2",
        "jdbc",
        "password",
        "secret",
        "stderr",
        "stdout",
        "token"
    );

    private RepositorySourceDomain() {
    }

    public record AnalysisRunId(String value) {
        public AnalysisRunId {
            value = requireText(value, "analysis run id");
        }
    }

    public record SourceSnapshotId(String value) {
        public SourceSnapshotId {
            value = requireText(value, "source snapshot id");
        }

        public static SourceSnapshotId deterministic(
            RepositoryReference repository,
            RevisionSelector revision,
            String resolvedCommit,
            String manifestSha256
        ) {
            return new SourceSnapshotId("source-snapshot-" + sha256Hex(String.join(
                "\n",
                repository.remoteUrl(),
                revision.branch(),
                revision.commit(),
                requireText(resolvedCommit, "resolved commit"),
                requireSha256(manifestSha256, "manifest checksum")
            )).substring(0, 32));
        }
    }

    public record WorkspaceId(String value) {
        public WorkspaceId {
            value = requireText(value, "workspace id");
            requireOpaqueId(value, "workspace id", "workspace-");
        }
    }

    public record WorkspaceBranchId(String value) {
        public WorkspaceBranchId {
            value = requireText(value, "workspace branch id");
            requireOpaqueId(value, "workspace branch id", "workspace-branch-");
        }
    }

    public record WorkspaceTitle(String value) {
        public WorkspaceTitle {
            value = requirePublicLabel(value, "workspace title");
        }

        public static WorkspaceTitle fromRepositoryName(String repositoryName) {
            return new WorkspaceTitle(repositoryName);
        }
    }

    public record RepositoryKey(String value) {
        public RepositoryKey {
            value = requireText(value, "repository key").toLowerCase(Locale.ROOT);
            if (value.length() > MAX_REPOSITORY_KEY_LENGTH || value.contains("://")
                || value.contains("@") || value.contains("?") || value.contains("#")
                || value.contains("\\") || value.chars().anyMatch(Character::isISOControl)
                || hasUnsafeSegments(value, false) || value.chars().filter(character -> character == '/').count() != 2
                || Arrays.stream(value.split("/")).anyMatch(part -> !REPOSITORY_KEY_PART.matcher(part).matches())) {
                throw new IllegalArgumentException("repository key must use normalized host/owner/repository form");
            }
        }

        public static RepositoryKey of(String repositoryHost, String repositoryOwner, String repositoryName) {
            return new RepositoryKey(String.join(
                "/",
                normalizedRepositoryPart(repositoryHost, "repository host"),
                normalizedRepositoryPart(repositoryOwner, "repository owner"),
                normalizedRepositoryPart(stripGitSuffix(repositoryName), "repository name")
            ));
        }
    }

    public record RepositoryIdentity(
        RepositoryKey repositoryKey,
        String repositoryUrl,
        String repositoryHost,
        String repositoryOwner,
        String repositoryName,
        String defaultBranch
    ) {
        public RepositoryIdentity {
            Objects.requireNonNull(repositoryKey, "repository key must not be null");
            repositoryUrl = requireCleanHttpsRemote(repositoryUrl);
            var uri = URI.create(repositoryUrl);
            repositoryHost = normalizedRepositoryPart(repositoryHost, "repository host");
            var actualHost = normalizedRepositoryPart(requireText(uri.getHost(), "repository remote host"), "repository host");
            if (!repositoryHost.equals(actualHost)) {
                throw new IllegalArgumentException("repository host must match repository url host");
            }
            repositoryOwner = normalizedRepositoryPart(repositoryOwner, "repository owner");
            repositoryName = normalizedRepositoryPart(stripGitSuffix(repositoryName), "repository name");
            var pathIdentity = repositoryPathIdentity(uri);
            if (!repositoryOwner.equals(pathIdentity.owner()) || !repositoryName.equals(pathIdentity.repositoryName())) {
                throw new IllegalArgumentException("repository identity must match repository url path");
            }
            if (!repositoryKey.equals(RepositoryKey.of(repositoryHost, repositoryOwner, repositoryName))) {
                throw new IllegalArgumentException("repository key must match repository identity");
            }
            defaultBranch = optionalRef(defaultBranch, "default branch");
        }

        public static RepositoryIdentity from(RepositoryReference repository, String defaultBranch) {
            var uri = URI.create(repository.remoteUrl());
            var pathIdentity = repositoryPathIdentity(uri);
            var host = normalizedHost(uri.getHost());
            return new RepositoryIdentity(
                RepositoryKey.of(host, pathIdentity.owner(), pathIdentity.repositoryName()),
                repository.remoteUrl(),
                host,
                pathIdentity.owner(),
                pathIdentity.repositoryName(),
                defaultBranch
            );
        }
    }

    public record RepositoryWorkspace(
        WorkspaceId workspaceId,
        WorkspaceTitle workspaceTitle,
        RepositoryIdentity repository,
        RepositoryWorkspaceStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<RepositoryWorkspaceBranch> branches,
        List<Diagnostic> diagnostics,
        Map<String, String> safeAttributes
    ) {
        public RepositoryWorkspace {
            Objects.requireNonNull(workspaceId, "workspace id must not be null");
            Objects.requireNonNull(workspaceTitle, "workspace title must not be null");
            Objects.requireNonNull(repository, "repository must not be null");
            status = Objects.requireNonNullElse(status, RepositoryWorkspaceStatus.READY);
            Objects.requireNonNull(createdAt, "created at must not be null");
            Objects.requireNonNull(updatedAt, "updated at must not be null");
            branches = List.copyOf(Objects.requireNonNullElse(branches, List.of()));
            diagnostics = List.copyOf(Objects.requireNonNullElse(diagnostics, List.of()));
            safeAttributes = RepositorySourceDomain.safeAttributes(safeAttributes);
            requireWorkspaceBranchesMatch(workspaceId, branches);
        }

        public RepositoryWorkspace withBranch(RepositoryWorkspaceBranch branch, Instant timestamp) {
            Objects.requireNonNull(branch, "workspace branch must not be null");
            if (!workspaceId.equals(branch.workspaceId())) {
                throw new IllegalArgumentException("workspace branch must belong to workspace");
            }
            var updatedBranches = new LinkedHashMap<WorkspaceBranchId, RepositoryWorkspaceBranch>();
            branches.forEach(existing -> updatedBranches.put(existing.workspaceBranchId(), existing));
            updatedBranches.put(branch.workspaceBranchId(), branch);
            return new RepositoryWorkspace(
                workspaceId,
                workspaceTitle,
                repository,
                status,
                createdAt,
                Objects.requireNonNull(timestamp, "updated at must not be null"),
                List.copyOf(updatedBranches.values()),
                diagnostics,
                safeAttributes
            );
        }
    }

    public record RepositoryWorkspaceBranch(
        WorkspaceBranchId workspaceBranchId,
        WorkspaceId workspaceId,
        String repositoryBranch,
        String requestedCommit,
        String resolvedCommit,
        SourceSnapshotId sourceSnapshotId,
        RepositoryWorkspaceBranchStatus status,
        List<SourceRoot> sourceRoots,
        Instant lastCheckedAt,
        Instant lastUpdatedAt,
        List<Diagnostic> diagnostics
    ) {
        public RepositoryWorkspaceBranch {
            Objects.requireNonNull(workspaceBranchId, "workspace branch id must not be null");
            Objects.requireNonNull(workspaceId, "workspace id must not be null");
            repositoryBranch = requireRepositoryBranch(repositoryBranch);
            requestedCommit = optionalCommit(requestedCommit);
            resolvedCommit = optionalCommit(resolvedCommit);
            status = Objects.requireNonNull(status, "branch status must not be null");
            sourceRoots = List.copyOf(Objects.requireNonNullElse(sourceRoots, List.of()));
            Objects.requireNonNull(lastUpdatedAt, "last updated at must not be null");
            diagnostics = List.copyOf(Objects.requireNonNullElse(diagnostics, List.of()));
            if ((status == RepositoryWorkspaceBranchStatus.CHECKED_OUT
                || status == RepositoryWorkspaceBranchStatus.UP_TO_DATE
                || status == RepositoryWorkspaceBranchStatus.UPDATED)
                && (sourceSnapshotId == null || resolvedCommit.isBlank() || sourceRoots.isEmpty() || lastCheckedAt == null)) {
                throw new IllegalArgumentException("checked-out branch state requires snapshot, commit, source roots and checked timestamp");
            }
        }

        public RepositoryWorkspaceBranch checkingOut(Instant timestamp) {
            return new RepositoryWorkspaceBranch(
                workspaceBranchId,
                workspaceId,
                repositoryBranch,
                requestedCommit,
                resolvedCommit,
                sourceSnapshotId,
                RepositoryWorkspaceBranchStatus.CHECKING_OUT,
                sourceRoots,
                lastCheckedAt,
                Objects.requireNonNull(timestamp, "last updated at must not be null"),
                List.of(Diagnostic.info("REPOSITORY_CHECKOUT_STARTED", "Repository checkout started"))
            );
        }

        public RepositoryWorkspaceBranch checkedOut(
            String newResolvedCommit,
            SourceSnapshotId newSourceSnapshotId,
            List<SourceRoot> newSourceRoots,
            Instant timestamp,
            List<Diagnostic> newDiagnostics
        ) {
            return new RepositoryWorkspaceBranch(
                workspaceBranchId,
                workspaceId,
                repositoryBranch,
                requestedCommit,
                newResolvedCommit,
                Objects.requireNonNull(newSourceSnapshotId, "source snapshot id must not be null"),
                RepositoryWorkspaceBranchStatus.CHECKED_OUT,
                List.copyOf(Objects.requireNonNull(newSourceRoots, "source roots must not be null")),
                Objects.requireNonNull(timestamp, "last checked at must not be null"),
                timestamp,
                List.copyOf(Objects.requireNonNullElse(newDiagnostics, List.of()))
            );
        }

        public RepositoryWorkspaceBranch failed(Instant timestamp, List<Diagnostic> newDiagnostics) {
            return new RepositoryWorkspaceBranch(
                workspaceBranchId,
                workspaceId,
                repositoryBranch,
                requestedCommit,
                resolvedCommit,
                sourceSnapshotId,
                RepositoryWorkspaceBranchStatus.FAILED,
                sourceRoots,
                lastCheckedAt,
                Objects.requireNonNull(timestamp, "last updated at must not be null"),
                List.copyOf(Objects.requireNonNullElse(newDiagnostics, List.of()))
            );
        }
    }

    public record RepositoryWorkspaceBranchSelector(String branch, String commit) {
        public RepositoryWorkspaceBranchSelector {
            branch = optionalRef(branch, "repository branch");
            commit = optionalCommit(commit);
        }

        public String requireBranch() {
            if (branch.isBlank()) {
                throw new IllegalArgumentException("repository branch is required until metadata resolution is available");
            }
            return branch;
        }
    }

    public record RepositoryReference(String remoteUrl, String provider, Map<String, String> safeAttributes) {
        public RepositoryReference {
            remoteUrl = requireCleanHttpsRemote(remoteUrl);
            provider = optionalText(provider, "provider");
            safeAttributes = RepositorySourceDomain.safeAttributes(safeAttributes);
        }
    }

    public record RevisionSelector(String branch, boolean branchRequired, String commit, boolean commitRequired) {
        public RevisionSelector {
            branch = optionalRef(branch, "branch");
            commit = optionalCommit(commit);
            if (branch.isBlank() && commit.isBlank()) {
                throw new IllegalArgumentException("branch or commit is required");
            }
            if (branchRequired && branch.isBlank()) {
                throw new IllegalArgumentException("branch is required by revision policy");
            }
            if (commitRequired && commit.isBlank()) {
                throw new IllegalArgumentException("commit is required by revision policy");
            }
        }

        public boolean hasBranch() {
            return !branch.isBlank();
        }

        public boolean hasCommit() {
            return !commit.isBlank();
        }
    }

    public record WorkspacePolicy(
        boolean ephemeral,
        boolean allowShallowClone,
        boolean allowPartialClone,
        boolean allowSparseCheckout,
        long timeoutSeconds,
        long maxWorkspaceBytes
    ) {
        public WorkspacePolicy {
            if (allowPartialClone) {
                throw new IllegalArgumentException("partial clone is not supported in Slice 05");
            }
            if (allowSparseCheckout) {
                throw new IllegalArgumentException("sparse checkout is not supported in Slice 05");
            }
            if (timeoutSeconds < 1 || timeoutSeconds > MAX_TIMEOUT_SECONDS) {
                throw new IllegalArgumentException("timeout seconds must be between 1 and " + MAX_TIMEOUT_SECONDS);
            }
            if (maxWorkspaceBytes < 1 || maxWorkspaceBytes > MAX_WORKSPACE_BYTES) {
                throw new IllegalArgumentException("max workspace bytes must be between 1 and " + MAX_WORKSPACE_BYTES);
            }
        }
    }

    public record ArtifactReference(String reference, String type, String sha256, long sizeBytes) {
        public ArtifactReference {
            reference = requireServiceOwnedReference(reference, "artifact reference");
            type = requireText(type, "artifact type");
            sha256 = requireSha256(sha256, "artifact sha256");
            if (sizeBytes < 0) {
                throw new IllegalArgumentException("artifact size must not be negative");
            }
        }
    }

    public record ArtifactByteAccess(
        String ownerService,
        String retrievalContract,
        String retrievalReference,
        ArtifactByteCustody byteCustody
    ) {
        public ArtifactByteAccess {
            ownerService = requireText(ownerService, "artifact byte owner service");
            retrievalContract = requireText(retrievalContract, "artifact byte retrieval contract");
            retrievalReference = requirePublicReference(retrievalReference, "artifact byte retrieval reference");
            byteCustody = Objects.requireNonNull(byteCustody, "artifact byte custody must not be null");
        }
    }

    public record SourcePackageDescriptor(
        PackageAvailability availability,
        ArtifactReference manifestArtifact,
        ArtifactReference packageArtifact,
        String schemaVersion,
        String producerService,
        ArtifactByteAccess byteAccess,
        SourceSnapshotCompleteness completeness
    ) {
        public SourcePackageDescriptor {
            availability = Objects.requireNonNull(availability, "source package availability must not be null");
            Objects.requireNonNull(manifestArtifact, "source package manifest artifact must not be null");
            schemaVersion = requireText(schemaVersion, "source package schema version");
            producerService = requireText(producerService, "source package producer service");
            Objects.requireNonNull(byteAccess, "source package byte access must not be null");
            completeness = Objects.requireNonNullElse(completeness, SourceSnapshotCompleteness.UNKNOWN);
            requirePackageArtifactWhenAvailable(availability, packageArtifact, "source package");
        }
    }

    public record BuildOutputPackageDescriptor(
        PackageAvailability availability,
        ArtifactReference manifestArtifact,
        ArtifactReference packageArtifact,
        String schemaVersion,
        String producerService,
        ArtifactByteAccess byteAccess,
        SourceSnapshotCompleteness completeness,
        BuildOutputResolution resolution,
        String buildSystem
    ) {
        public BuildOutputPackageDescriptor {
            availability = Objects.requireNonNull(availability, "build-output package availability must not be null");
            schemaVersion = requireText(schemaVersion, "build-output package schema version");
            producerService = requireText(producerService, "build-output package producer service");
            Objects.requireNonNull(byteAccess, "build-output package byte access must not be null");
            completeness = Objects.requireNonNullElse(completeness, SourceSnapshotCompleteness.UNKNOWN);
            resolution = Objects.requireNonNull(resolution, "build-output resolution must not be null");
            buildSystem = requireText(buildSystem, "build system");
            requirePackageArtifactWhenAvailable(availability, packageArtifact, "build-output package");
            if (availability == PackageAvailability.AVAILABLE && manifestArtifact == null) {
                throw new IllegalArgumentException("build-output package manifest artifact is required when available");
            }
        }
    }

    public record BuildOutputResolution(
        List<BuildOutputProducerCandidate> candidates,
        BuildOutputProducer selectedProducer,
        boolean terminalIntegrityFailure,
        List<Diagnostic> diagnostics
    ) {
        public BuildOutputResolution {
            candidates = List.copyOf(Objects.requireNonNull(candidates, "build-output candidates must not be null"));
            selectedProducer = Objects.requireNonNullElse(selectedProducer, BuildOutputProducer.UNSPECIFIED);
            diagnostics = List.copyOf(Objects.requireNonNullElse(diagnostics, List.of()));
            if (candidates.isEmpty()) {
                throw new IllegalArgumentException("build-output resolution candidates must not be empty");
            }
            var order = candidates.stream().map(BuildOutputProducerCandidate::producer).toList();
            var expected = List.of(
                BuildOutputProducer.ARTIFACT_STORE,
                BuildOutputProducer.ARTIFACTORY,
                BuildOutputProducer.JENKINS,
                BuildOutputProducer.BUILD_ARTIFACT_WORKER
            );
            if (!order.equals(expected)) {
                throw new IllegalArgumentException(
                    "build-output resolution order must be Artifact Store, Artifactory, Jenkins, build-artifact-worker"
                );
            }
            var hasIntegrityFailure = candidates.stream()
                .anyMatch(candidate -> candidate.status() == BuildOutputProducerStatus.TERMINAL_INTEGRITY_FAILURE);
            if (hasIntegrityFailure != terminalIntegrityFailure) {
                throw new IllegalArgumentException("terminal integrity failure must match producer candidate status");
            }
            if (terminalIntegrityFailure && selectedProducer != BuildOutputProducer.UNSPECIFIED) {
                throw new IllegalArgumentException("terminal integrity failure must not select a fallback producer");
            }
            if (selectedProducer != BuildOutputProducer.UNSPECIFIED) {
                var producerToSelect = selectedProducer;
                var selectedCandidate = candidates.stream()
                    .filter(candidate -> candidate.producer() == producerToSelect)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("selected build-output producer must be part of candidates"));
                if (selectedCandidate.status() != BuildOutputProducerStatus.AVAILABLE) {
                    throw new IllegalArgumentException("selected build-output producer must be available");
                }
            }
        }
    }

    public record BuildOutputProducerCandidate(
        BuildOutputProducer producer,
        BuildOutputProducerStatus status,
        String reference,
        List<Diagnostic> diagnostics
    ) {
        public BuildOutputProducerCandidate {
            producer = Objects.requireNonNull(producer, "build-output producer must not be null");
            status = Objects.requireNonNull(status, "build-output producer status must not be null");
            reference = reference == null || reference.isBlank() ? "" : requirePublicReference(reference, "build-output producer reference");
            diagnostics = List.copyOf(Objects.requireNonNullElse(diagnostics, List.of()));
        }
    }

    public record SourceRoot(String relativePath, String language) {
        public SourceRoot {
            relativePath = requireSourceRootReference(relativePath, "source root");
            language = requireText(language, "source root language");
        }
    }

    public record Diagnostic(String code, String message, DiagnosticSeverity severity) {
        public Diagnostic {
            code = requireText(code, "diagnostic code");
            message = requireText(message, "diagnostic message");
            if (containsPrivatePublicText(code) || containsPrivatePublicText(message)) {
                throw new IllegalArgumentException("diagnostics must not expose sensitive or private values");
            }
            severity = Objects.requireNonNullElse(severity, DiagnosticSeverity.INFO);
        }

        public static Diagnostic info(String code, String message) {
            return new Diagnostic(code, message, DiagnosticSeverity.INFO);
        }

        public static Diagnostic error(String code, String message) {
            return new Diagnostic(code, message, DiagnosticSeverity.ERROR);
        }
    }

    public record CheckoutResult(
        CheckoutStatus status,
        String resolvedRemoteUrl,
        String resolvedCommit,
        String requestedBranch,
        String requestedCommit,
        boolean shallowClone,
        long elapsedMillis,
        List<Diagnostic> diagnostics,
        boolean partialClone,
        boolean sparseCheckout,
        List<SourceRoot> sourceRoots
    ) {
        public CheckoutResult {
            status = Objects.requireNonNull(status, "checkout status must not be null");
            resolvedRemoteUrl = requireCleanHttpsRemote(resolvedRemoteUrl);
            resolvedCommit = requireText(resolvedCommit, "resolved commit");
            requestedBranch = optionalText(requestedBranch, "requested branch");
            requestedCommit = optionalText(requestedCommit, "requested commit");
            if (elapsedMillis < 0) {
                throw new IllegalArgumentException("elapsed millis must not be negative");
            }
            diagnostics = List.copyOf(Objects.requireNonNullElse(diagnostics, List.of()));
            sourceRoots = List.copyOf(Objects.requireNonNullElse(sourceRoots, List.of()));
            if (sourceRoots.isEmpty()) {
                throw new IllegalArgumentException("at least one source root is required");
            }
        }
    }

    public record SourceSnapshot(
        SourceSnapshotId sourceSnapshotId,
        SourceSnapshotCompleteness completeness,
        List<SourceRoot> sourceRoots,
        ArtifactReference manifestArtifact,
        List<String> limitations,
        SourcePackageDescriptor sourcePackage,
        BuildOutputPackageDescriptor buildOutputPackage
    ) {
        public SourceSnapshot {
            Objects.requireNonNull(sourceSnapshotId, "source snapshot id must not be null");
            completeness = Objects.requireNonNullElse(completeness, SourceSnapshotCompleteness.UNKNOWN);
            sourceRoots = List.copyOf(Objects.requireNonNullElse(sourceRoots, List.of()));
            if (sourceRoots.isEmpty()) {
                throw new IllegalArgumentException("at least one source root is required");
            }
            Objects.requireNonNull(manifestArtifact, "manifest artifact must not be null");
            limitations = List.copyOf(Objects.requireNonNullElse(limitations, List.of()));
            Objects.requireNonNull(sourcePackage, "source package must not be null");
            Objects.requireNonNull(buildOutputPackage, "build-output package must not be null");
        }
    }

    public record RepositoryPreparation(
        AnalysisRunId analysisRunId,
        SourceSnapshotId sourceSnapshotId,
        WorkspaceId workspaceId,
        RepositoryReference repository,
        RevisionSelector requestedRevision,
        CheckoutResult checkout,
        SourceSnapshot sourceSnapshot,
        RepositoryWorkspaceStatus workspaceStatus,
        Instant createdAt,
        Instant updatedAt,
        List<Diagnostic> diagnostics,
        Map<String, String> safeAttributes
    ) {
        public RepositoryPreparation {
            Objects.requireNonNull(analysisRunId, "analysis run id must not be null");
            Objects.requireNonNull(sourceSnapshotId, "source snapshot id must not be null");
            Objects.requireNonNull(workspaceId, "workspace id must not be null");
            Objects.requireNonNull(repository, "repository must not be null");
            Objects.requireNonNull(requestedRevision, "requested revision must not be null");
            Objects.requireNonNull(checkout, "checkout must not be null");
            Objects.requireNonNull(sourceSnapshot, "source snapshot must not be null");
            if (!sourceSnapshotId.equals(sourceSnapshot.sourceSnapshotId())) {
                throw new IllegalArgumentException("preparation source snapshot id must match nested snapshot");
            }
            workspaceStatus = Objects.requireNonNullElse(workspaceStatus, RepositoryWorkspaceStatus.CHECKED_OUT);
            Objects.requireNonNull(createdAt, "created at must not be null");
            Objects.requireNonNull(updatedAt, "updated at must not be null");
            diagnostics = List.copyOf(Objects.requireNonNullElse(diagnostics, List.of()));
            safeAttributes = RepositorySourceDomain.safeAttributes(safeAttributes);
        }

        public RepositoryPreparation withWorkspaceStatus(RepositoryWorkspaceStatus status, Instant timestamp) {
            return new RepositoryPreparation(
                analysisRunId,
                sourceSnapshotId,
                workspaceId,
                repository,
                requestedRevision,
                checkout,
                sourceSnapshot,
                status,
                createdAt,
                timestamp,
                diagnostics,
                safeAttributes
            );
        }
    }

    public enum CheckoutStatus {
        ACCEPTED,
        WORKSPACE_PREPARED,
        CHECKED_OUT,
        FAILED
    }

    public enum RepositoryWorkspaceStatus {
        READY,
        CHECKED_OUT,
        CLEANED,
        FAILED
    }

    public enum RepositoryWorkspaceBranchStatus {
        CHECKING_OUT,
        CHECKED_OUT,
        UP_TO_DATE,
        UPDATING,
        UPDATED,
        FAILED
    }

    public enum SourceSnapshotCompleteness {
        COMPLETE,
        INCOMPLETE,
        UNKNOWN
    }

    public enum PackageAvailability {
        AVAILABLE,
        PENDING,
        UNAVAILABLE,
        FAILED_INTEGRITY
    }

    public enum ArtifactByteCustody {
        PRODUCER_RETAINED,
        SCOPED_OBJECT_ACCESS,
        EXPLICIT_HANDOFF
    }

    public enum BuildOutputProducer {
        UNSPECIFIED,
        ARTIFACT_STORE,
        ARTIFACTORY,
        JENKINS,
        BUILD_ARTIFACT_WORKER
    }

    public enum BuildOutputProducerStatus {
        AVAILABLE,
        NOT_CONFIGURED,
        MISSING,
        FALLBACK_PLANNED,
        TERMINAL_INTEGRITY_FAILURE
    }

    public enum DiagnosticSeverity {
        INFO,
        WARNING,
        ERROR
    }

    public static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }

    private static void requireOpaqueId(String value, String name, String prefix) {
        if (value.length() > MAX_OPAQUE_ID_LENGTH || value.length() == prefix.length() || !value.startsWith(prefix)
            || !OPAQUE_ID.matcher(value).matches() || looksLikePath(value)) {
            throw new IllegalArgumentException(name + " must be opaque");
        }
    }

    private static String requirePublicLabel(String value, String name) {
        var label = requireText(value, name);
        if (label.length() > MAX_PUBLIC_LABEL_LENGTH || looksLikePath(label) || containsSensitiveToken(label)
            || label.contains("://") || label.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(name + " must be a safe public label");
        }
        return label;
    }

    public static Map<String, String> safeAttributes(Map<String, String> attributes) {
        var sorted = new TreeMap<String, String>();
        Objects.requireNonNullElse(attributes, Map.<String, String>of()).forEach((key, value) -> {
            var safeKey = requireText(key, "safe attribute key");
            var safeValue = requireText(value, "safe attribute value");
            if (containsPrivatePublicText(safeKey) || containsPrivatePublicText(safeValue)) {
                throw new IllegalArgumentException("safe attributes must not contain sensitive or private values");
            }
            sorted.put(safeKey, safeValue);
        });
        return Collections.unmodifiableMap(new LinkedHashMap<>(sorted));
    }

    public static String sha256Hex(String value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            var builder = new StringBuilder(digest.length * 2);
            for (byte current : digest) {
                builder.append("%02x".formatted(current));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }

    private static String requireCleanHttpsRemote(String remoteUrl) {
        var candidate = requireText(remoteUrl, "repository remote url");
        var uri = URI.create(candidate);
        if (!"https".equals(uri.getScheme())) {
            throw new IllegalArgumentException("repository remote url must use https");
        }
        if (uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new IllegalArgumentException("repository remote url must not contain userinfo, query or fragment");
        }
        var host = normalizedHost(requireText(uri.getHost(), "repository remote host"));
        if ("localhost".equals(host) || host.endsWith(".localhost") || host.endsWith(".local")
            || host.endsWith(".test") || host.endsWith(".invalid") || host.endsWith(".example")
            || PRIVATE_IPV4.matcher(host).find()
            || "169.254.169.254".equals(host) || isPrivateIpv6(host)) {
            throw new IllegalArgumentException("repository remote url must not target local or private hosts");
        }
        return candidate;
    }

    private static String normalizedHost(String host) {
        var normalized = host.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        var zoneIndex = normalized.indexOf('%');
        return zoneIndex < 0 ? normalized : normalized.substring(0, zoneIndex);
    }

    private static boolean isPrivateIpv6(String host) {
        return host.contains(":")
            && ("::".equals(host)
            || "::1".equals(host)
            || "0:0:0:0:0:0:0:0".equals(host)
            || "0:0:0:0:0:0:0:1".equals(host)
            || host.startsWith("::ffff:")
            || host.startsWith("0:0:0:0:0:ffff:")
            || host.startsWith("fc")
            || host.startsWith("fd")
            || host.startsWith("fe80:")
            || host.startsWith("ff")
            || host.startsWith("2001:db8:"));
    }

    private static String requireRepositoryBranch(String branch) {
        var repositoryBranch = optionalRef(branch, "repository branch");
        if (repositoryBranch.isBlank()) {
            throw new IllegalArgumentException("repository branch must not be blank");
        }
        return repositoryBranch;
    }

    private static String normalizedRepositoryPart(String value, String name) {
        var part = requireText(value, name).toLowerCase(Locale.ROOT);
        if (!REPOSITORY_KEY_PART.matcher(part).matches()
            || part.contains("/") || part.contains("\\") || part.contains(":") || part.contains("@")
            || part.contains("?") || part.contains("#") || part.chars().anyMatch(Character::isISOControl)
            || hasUnsafeSegments(part, false)) {
            throw new IllegalArgumentException(name + " must be normalized");
        }
        return part;
    }

    private static String stripGitSuffix(String value) {
        var name = requireText(value, "repository name");
        return name.endsWith(".git") ? name.substring(0, name.length() - 4) : name;
    }

    private static RepositoryPathIdentity repositoryPathIdentity(URI uri) {
        var segments = Arrays.stream(Objects.requireNonNullElse(uri.getPath(), "").split("/"))
            .filter(segment -> !segment.isBlank())
            .toList();
        if (segments.size() < 2) {
            throw new IllegalArgumentException("repository url must contain owner and repository name");
        }
        return new RepositoryPathIdentity(
            normalizedRepositoryPart(segments.get(segments.size() - 2), "repository owner"),
            normalizedRepositoryPart(stripGitSuffix(segments.get(segments.size() - 1)), "repository name")
        );
    }

    private record RepositoryPathIdentity(String owner, String repositoryName) {
    }

    private static void requireWorkspaceBranchesMatch(
        WorkspaceId workspaceId,
        List<RepositoryWorkspaceBranch> branches
    ) {
        var branchIds = new java.util.HashSet<WorkspaceBranchId>();
        var branchNames = new java.util.HashSet<String>();
        branches.forEach(branch -> {
            if (!workspaceId.equals(branch.workspaceId())) {
                throw new IllegalArgumentException("workspace branch must belong to workspace");
            }
            if (!branchIds.add(branch.workspaceBranchId())) {
                throw new IllegalArgumentException("workspace branch id must be unique");
            }
            if (!branchNames.add(branch.repositoryBranch())) {
                throw new IllegalArgumentException("repository branch must be unique inside workspace");
            }
        });
    }

    private static String optionalText(String value, String name) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return requireText(value, name);
    }

    private static String optionalRef(String value, String name) {
        var ref = optionalText(value, name);
        if (ref.isBlank()) {
            return "";
        }
        if (ref.startsWith("-") || ref.startsWith("/") || ref.endsWith("/") || ref.contains("..")
            || ref.contains("//") || ref.contains("@{") || ref.contains("\\")
            || !ref.matches("[A-Za-z0-9._/-]+") || hasUnsafeSegments(ref, false)) {
            throw new IllegalArgumentException(name + " is not a safe ref");
        }
        return ref;
    }

    private static String optionalCommit(String value) {
        var commit = optionalText(value, "commit");
        if (!commit.isBlank() && !COMMIT.matcher(commit).matches()) {
            throw new IllegalArgumentException("commit must be a hex commit id");
        }
        return commit.toLowerCase(Locale.ROOT);
    }

    private static String requireSourceRootReference(String value, String name) {
        return requireRelativeReference(value, name, true);
    }

    private static String requireServiceOwnedReference(String value, String name) {
        return requireRelativeReference(value, name, false);
    }

    private static String requireRelativeReference(String value, String name, boolean allowSingleDot) {
        var reference = requireText(value, name);
        var lower = reference.toLowerCase(Locale.ROOT);
        if (reference.startsWith("/") || reference.startsWith("\\") || reference.contains("\\")
            || lower.startsWith("file:") || reference.matches("^[A-Za-z]:.*")
            || reference.chars().anyMatch(Character::isISOControl)
            || hasUnsafeSegments(reference, allowSingleDot)) {
            throw new IllegalArgumentException(name + " must be relative and service-owned");
        }
        return reference;
    }

    private static String requirePublicReference(String value, String name) {
        var reference = requireText(value, name);
        var lower = reference.toLowerCase(Locale.ROOT);
        if (reference.startsWith("/") || reference.startsWith("\\") || reference.contains("\\")
            || lower.startsWith("file:") || reference.matches("^[A-Za-z]:.*")
            || reference.contains("://")
            || reference.chars().anyMatch(Character::isISOControl)
            || hasUnsafeSegments(reference, false)) {
            throw new IllegalArgumentException(name + " must be an opaque public reference");
        }
        return reference;
    }

    private static boolean hasUnsafeSegments(String reference, boolean allowSingleDot) {
        var parts = Arrays.asList(reference.split("/", -1));
        return parts.stream().anyMatch(part ->
            part.isBlank()
                || "..".equals(part)
                || part.endsWith(".lock")
                || (".".equals(part) && !(allowSingleDot && ".".equals(reference)))
        );
    }

    private static void requirePackageArtifactWhenAvailable(
        PackageAvailability availability,
        ArtifactReference packageArtifact,
        String name
    ) {
        if (availability == PackageAvailability.AVAILABLE && packageArtifact == null) {
            throw new IllegalArgumentException(name + " artifact is required when available");
        }
        if (availability == PackageAvailability.FAILED_INTEGRITY && packageArtifact != null) {
            throw new IllegalArgumentException(name + " artifact must be omitted after integrity failure");
        }
    }

    private static String requireSha256(String value, String name) {
        var checksum = requireText(value, name).toLowerCase(Locale.ROOT);
        if (!SHA_256.matcher(checksum).matches()) {
            throw new IllegalArgumentException(name + " must be a SHA-256 hex value");
        }
        return checksum;
    }

    private static boolean containsSensitiveToken(String value) {
        var lower = value.toLowerCase(Locale.ROOT);
        return SENSITIVE_TOKENS.stream().anyMatch(lower::contains);
    }

    private static boolean containsPrivatePublicText(String value) {
        var lower = value.toLowerCase(Locale.ROOT);
        return containsSensitiveToken(value)
            || looksLikePath(value)
            || lower.contains("://")
            || lower.contains("git clone");
    }

    private static boolean looksLikePath(String value) {
        var lower = value.toLowerCase(Locale.ROOT).trim();
        return lower.startsWith("file:")
            || lower.startsWith("/")
            || lower.startsWith("\\")
            || lower.matches(".*[a-z]:[\\\\/].*")
            || lower.contains("/home/")
            || lower.contains("/tmp/")
            || lower.contains("/var/")
            || lower.contains("\\users\\")
            || lower.contains("/users/")
            || lower.contains("/mnt/")
            || lower.contains("\\tmp\\")
            || lower.contains("\\workspace\\")
            || lower.contains("/workspace/")
            || lower.contains("/workspaces/")
            || lower.chars().anyMatch(Character::isISOControl);
    }
}
