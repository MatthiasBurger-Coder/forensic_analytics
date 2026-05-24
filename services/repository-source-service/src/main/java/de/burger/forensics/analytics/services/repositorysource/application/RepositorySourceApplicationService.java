package de.burger.forensics.analytics.services.repositorysource.application;

import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryCheckoutPort;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryPreparationRepository;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositorySourceIdempotencyRecord;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositorySourceIdempotencyRepository;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryWorkspacePort;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.ArtifactByteAccess;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.ArtifactByteCustody;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.ArtifactReference;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.BuildOutputPackageDescriptor;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.BuildOutputProducer;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.BuildOutputProducerCandidate;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.BuildOutputProducerStatus;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.BuildOutputResolution;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.Diagnostic;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.PackageAvailability;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryPreparation;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryReference;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceStatus;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RevisionSelector;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourcePackageDescriptor;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourceSnapshot;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourceSnapshotCompleteness;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourceSnapshotId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspacePolicy;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.requireText;
import static de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.safeAttributes;

public final class RepositorySourceApplicationService {
    private static final String OPERATION_PREPARE_REPOSITORY = "PREPARE_REPOSITORY";
    private static final String OPERATION_CLEANUP_WORKSPACE = "CLEANUP_WORKSPACE";
    private static final String RESULT_REPOSITORY_PREPARATION = "REPOSITORY_PREPARATION";
    private static final String RESULT_CLEANUP_WORKSPACE = "CLEANUP_WORKSPACE";
    private final RepositoryPreparationRepository repository;
    private final RepositoryWorkspacePort workspacePort;
    private final RepositoryCheckoutPort checkoutPort;
    private final Clock clock;
    private final RepositorySourceIdempotency idempotency;

    public RepositorySourceApplicationService(
        RepositoryPreparationRepository repository,
        RepositorySourceIdempotencyRepository idempotencyRepository,
        RepositoryWorkspacePort workspacePort,
        RepositoryCheckoutPort checkoutPort,
        Clock clock
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.workspacePort = Objects.requireNonNull(workspacePort, "workspace port must not be null");
        this.checkoutPort = Objects.requireNonNull(checkoutPort, "checkout port must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.idempotency = new RepositorySourceIdempotency(idempotencyRepository, clock);
    }

    public synchronized RepositoryPreparation prepare(
        String idempotencyKey,
        String schemaVersion,
        String correlationId,
        AnalysisRunId analysisRunId,
        RepositoryReference repositoryReference,
        RevisionSelector revision,
        WorkspacePolicy workspacePolicy,
        Map<String, String> attributes
    ) {
        var key = requireText(idempotencyKey, "idempotency key");
        var fingerprint = prepareFingerprint(
            schemaVersion,
            correlationId,
            analysisRunId,
            repositoryReference,
            revision,
            workspacePolicy,
            attributes
        );
        return idempotency.replayOrExecute(
            OPERATION_PREPARE_REPOSITORY,
            key,
            fingerprint,
            this::replayPreparation,
            () -> {
                requireText(schemaVersion, "schema version");
                requireText(correlationId, "correlation id");
                var safeAttributes = safeAttributes(attributes);
                var workspace = workspacePort.prepare(analysisRunId, workspacePolicy);
                var checkout = checkoutWithCleanupOnFailure(repositoryReference, revision, workspacePolicy, workspace);
                var manifestSha = RepositorySourceSnapshotFactory.manifestSha256(
                    repositoryReference,
                    revision,
                    checkout.sourceRoots(),
                    checkout.resolvedCommit()
                );
                var sourceSnapshotId = SourceSnapshotId.deterministic(repositoryReference, revision, checkout.resolvedCommit(), manifestSha);
                var manifest = new ArtifactReference(
                    "snapshots/" + sourceSnapshotId.value() + "/manifest.json",
                    "application/json",
                    manifestSha,
                    RepositorySourceSnapshotFactory
                        .manifestPayload(repositoryReference, revision, checkout.resolvedCommit(), checkout.sourceRoots())
                        .getBytes(StandardCharsets.UTF_8)
                        .length
                );
                var sourceSnapshot = new SourceSnapshot(
                    sourceSnapshotId,
                    SourceSnapshotCompleteness.COMPLETE,
                    checkout.sourceRoots(),
                    manifest,
                    List.of(),
                    sourcePackage(sourceSnapshotId, manifest),
                    buildOutputPackage(sourceSnapshotId)
                );
                var now = clock.instant();
                var preparation = repository.save(new RepositoryPreparation(
                    analysisRunId,
                    sourceSnapshotId,
                    workspace.workspaceId(),
                    repositoryReference,
                    revision,
                    checkout,
                    sourceSnapshot,
                    RepositoryWorkspaceStatus.CHECKED_OUT,
                    now,
                    now,
                    List.of(Diagnostic.info("REPOSITORY_CHECKED_OUT", "Repository checkout completed")),
                    safeAttributes
                ));
                return new RepositorySourceIdempotency.CompletedResult<>(
                    RESULT_REPOSITORY_PREPARATION,
                    preparationReference(preparation),
                    preparationStatusPayload(preparation),
                    preparation
                );
            }
        );
    }

    public synchronized RepositoryPreparation get(AnalysisRunId analysisRunId, SourceSnapshotId sourceSnapshotId) {
        return repository.findByRunAndSnapshot(analysisRunId, sourceSnapshotId)
            .orElseThrow(() -> new RepositoryPreparationNotFoundException("repository preparation was not found"));
    }

    public synchronized CleanupRepositoryWorkspaceResult cleanup(
        String idempotencyKey,
        String correlationId,
        AnalysisRunId analysisRunId,
        WorkspaceId workspaceId
    ) {
        var key = requireText(idempotencyKey, "idempotency key");
        var fingerprint = String.join("|", requireText(correlationId, "correlation id"), analysisRunId.value(), workspaceId.value());
        return idempotency.replayOrExecute(
            OPERATION_CLEANUP_WORKSPACE,
            key,
            fingerprint,
            this::replayCleanup,
            () -> {
                var preparation = repository.findByRunAndWorkspace(analysisRunId, workspaceId)
                    .orElseThrow(() -> new RepositoryPreparationNotFoundException("repository preparation was not found"));
                workspacePort.cleanup(workspaceId);
                var cleaned = repository.save(preparation.withWorkspaceStatus(RepositoryWorkspaceStatus.CLEANED, clock.instant()));
                var result = cleanupResult(cleaned);
                return new RepositorySourceIdempotency.CompletedResult<>(
                    RESULT_CLEANUP_WORKSPACE,
                    cleanupReference(cleaned),
                    cleanupPayload(result),
                    result
                );
            }
        );
    }

    private static String prepareFingerprint(
        String schemaVersion,
        String correlationId,
        AnalysisRunId analysisRunId,
        RepositoryReference repositoryReference,
        RevisionSelector revision,
        WorkspacePolicy workspacePolicy,
        Map<String, String> attributes
    ) {
        return String.join(
            "|",
            requireText(schemaVersion, "schema version"),
            requireText(correlationId, "correlation id"),
            analysisRunId.value(),
            repositoryReference.remoteUrl(),
            revision.branch(),
            revision.commit(),
            Boolean.toString(revision.branchRequired()),
            Boolean.toString(revision.commitRequired()),
            Boolean.toString(workspacePolicy.ephemeral()),
            Boolean.toString(workspacePolicy.allowShallowClone()),
            Long.toString(workspacePolicy.timeoutSeconds()),
            Long.toString(workspacePolicy.maxWorkspaceBytes()),
            safeAttributes(attributes).toString()
        );
    }

    private de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.CheckoutResult checkoutWithCleanupOnFailure(
        RepositoryReference repositoryReference,
        RevisionSelector revision,
        WorkspacePolicy workspacePolicy,
        de.burger.forensics.analytics.services.repositorysource.application.port.PreparedWorkspace workspace
    ) {
        try {
            return checkoutPort.checkout(workspace, repositoryReference, revision, workspacePolicy);
        } catch (RuntimeException error) {
            workspacePort.cleanup(workspace.workspaceId());
            throw error;
        }
    }

    private static SourcePackageDescriptor sourcePackage(SourceSnapshotId sourceSnapshotId, ArtifactReference manifest) {
        return new SourcePackageDescriptor(
            PackageAvailability.PENDING,
            manifest,
            null,
            "source-package-descriptor-v1",
            "repository-source-service",
            new ArtifactByteAccess(
                "repository-source-service",
                "repository-source.v1.SourcePackage",
                "source-snapshot/" + sourceSnapshotId.value(),
                ArtifactByteCustody.PRODUCER_RETAINED
            ),
            SourceSnapshotCompleteness.COMPLETE
        );
    }

    private static BuildOutputPackageDescriptor buildOutputPackage(SourceSnapshotId sourceSnapshotId) {
        return new BuildOutputPackageDescriptor(
            PackageAvailability.PENDING,
            null,
            null,
            "build-output-package-descriptor-v1",
            "build-artifact-worker-service",
            new ArtifactByteAccess(
                "build-artifact-worker-service",
                "build-artifact-worker.v1.BuildOutputPackage",
                "source-snapshot/" + sourceSnapshotId.value(),
                ArtifactByteCustody.PRODUCER_RETAINED
            ),
            SourceSnapshotCompleteness.UNKNOWN,
            new BuildOutputResolution(
                List.of(
                    new BuildOutputProducerCandidate(BuildOutputProducer.ARTIFACT_STORE, BuildOutputProducerStatus.NOT_CONFIGURED, "", List.of()),
                    new BuildOutputProducerCandidate(BuildOutputProducer.ARTIFACTORY, BuildOutputProducerStatus.NOT_CONFIGURED, "", List.of()),
                    new BuildOutputProducerCandidate(BuildOutputProducer.JENKINS, BuildOutputProducerStatus.NOT_CONFIGURED, "", List.of()),
                    new BuildOutputProducerCandidate(
                        BuildOutputProducer.BUILD_ARTIFACT_WORKER,
                        BuildOutputProducerStatus.FALLBACK_PLANNED,
                        "source-snapshot/" + sourceSnapshotId.value(),
                        List.of(Diagnostic.info("BUILD_ARTIFACT_WORKER_FALLBACK_PLANNED", "Build artifact worker fallback is planned"))
                    )
                ),
                BuildOutputProducer.UNSPECIFIED,
                false,
                List.of()
            ),
            "auto-detect"
        );
    }

    private RepositoryPreparation replayPreparation(RepositorySourceIdempotencyRecord record) {
        if (!RESULT_REPOSITORY_PREPARATION.equals(record.resultType())) {
            throw new RepositoryPreparationNotFoundException("repository preparation idempotency result was not found");
        }
        var parts = splitReference(record.resultReference(), 2);
        var preparation = repository.findByRunAndSnapshot(new AnalysisRunId(parts[0]), new SourceSnapshotId(parts[1]))
            .orElseThrow(() -> new RepositoryPreparationNotFoundException("repository preparation was not found"));
        var status = preparationStatus(record.resultPayload());
        return preparation.withWorkspaceStatus(status.status(), status.updatedAt());
    }

    private CleanupRepositoryWorkspaceResult replayCleanup(RepositorySourceIdempotencyRecord record) {
        if (!RESULT_CLEANUP_WORKSPACE.equals(record.resultType())) {
            throw new RepositoryPreparationNotFoundException("cleanup idempotency result was not found");
        }
        if (!record.resultPayload().isBlank()) {
            return cleanupFromPayload(record.resultPayload());
        }
        var parts = splitReference(record.resultReference(), 2);
        var preparation = repository.findByRunAndWorkspace(new AnalysisRunId(parts[0]), new WorkspaceId(parts[1]))
            .orElseThrow(() -> new RepositoryPreparationNotFoundException("repository preparation was not found"));
        return cleanupResult(preparation);
    }

    private static CleanupRepositoryWorkspaceResult cleanupResult(RepositoryPreparation preparation) {
        return new CleanupRepositoryWorkspaceResult(
            preparation.workspaceId(),
            preparation.workspaceStatus(),
            List.of(Diagnostic.info("WORKSPACE_CLEANED", "Repository workspace was cleaned"))
        );
    }

    private static String preparationReference(RepositoryPreparation preparation) {
        return preparation.analysisRunId().value() + "|" + preparation.sourceSnapshotId().value();
    }

    private static String preparationStatusPayload(RepositoryPreparation preparation) {
        return preparation.workspaceStatus().name() + "|" + preparation.updatedAt();
    }

    private static PreparationStatus preparationStatus(String payload) {
        var parts = splitReference(payload, 2);
        return new PreparationStatus(RepositoryWorkspaceStatus.valueOf(parts[0]), java.time.Instant.parse(parts[1]));
    }

    private static String cleanupReference(RepositoryPreparation preparation) {
        return preparation.analysisRunId().value() + "|" + preparation.workspaceId().value();
    }

    private static String cleanupPayload(CleanupRepositoryWorkspaceResult result) {
        return RepositorySourceIdempotencyPayloads.cleanup(result);
    }

    private static CleanupRepositoryWorkspaceResult cleanupFromPayload(String payload) {
        return RepositorySourceIdempotencyPayloads.cleanup(payload);
    }

    private static String[] splitReference(String reference, int expectedParts) {
        var parts = reference.split("\\|", -1);
        if (parts.length != expectedParts) {
            throw new IllegalStateException("Idempotency result reference is invalid");
        }
        return parts;
    }

    private record PreparationStatus(RepositoryWorkspaceStatus status, java.time.Instant updatedAt) {
    }
}
