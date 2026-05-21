package de.burger.forensics.analytics.services.repositoryanalysis.application;

import de.burger.forensics.analytics.services.repositoryanalysis.application.port.RepositoryCheckoutPort;
import de.burger.forensics.analytics.services.repositoryanalysis.application.port.RepositoryPreparationRepository;
import de.burger.forensics.analytics.services.repositoryanalysis.application.port.RepositoryWorkspacePort;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.ArtifactByteAccess;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.ArtifactByteCustody;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.ArtifactReference;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.BuildOutputPackageDescriptor;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.BuildOutputProducer;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.BuildOutputProducerCandidate;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.BuildOutputProducerStatus;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.BuildOutputResolution;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.Diagnostic;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.PackageAvailability;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RepositoryPreparation;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RepositoryReference;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RepositoryWorkspaceStatus;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RevisionSelector;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourcePackageDescriptor;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceSnapshot;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceSnapshotCompleteness;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceSnapshotId;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.WorkspaceId;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.WorkspacePolicy;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.requireText;
import static de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.safeAttributes;
import static de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.sha256Hex;

public final class RepositoryAnalysisApplicationService {
    private final RepositoryPreparationRepository repository;
    private final RepositoryWorkspacePort workspacePort;
    private final RepositoryCheckoutPort checkoutPort;
    private final Clock clock;
    private final Map<String, IdempotentResult<RepositoryPreparation>> prepareResults = new HashMap<>();
    private final Map<String, IdempotentResult<CleanupRepositoryWorkspaceResult>> cleanupResults = new HashMap<>();

    public RepositoryAnalysisApplicationService(
        RepositoryPreparationRepository repository,
        RepositoryWorkspacePort workspacePort,
        RepositoryCheckoutPort checkoutPort,
        Clock clock
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.workspacePort = Objects.requireNonNull(workspacePort, "workspace port must not be null");
        this.checkoutPort = Objects.requireNonNull(checkoutPort, "checkout port must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
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
        var replay = prepareResults.get(key);
        if (replay != null) {
            return replay.sameFingerprintOrThrow(fingerprint);
        }

        requireText(schemaVersion, "schema version");
        requireText(correlationId, "correlation id");
        var safeAttributes = safeAttributes(attributes);
        var workspace = workspacePort.prepare(analysisRunId, workspacePolicy);
        var checkout = checkoutWithCleanupOnFailure(repositoryReference, revision, workspacePolicy, workspace);
        var manifestSha = manifestSha256(repositoryReference, revision, checkout.sourceRoots(), checkout.resolvedCommit());
        var sourceSnapshotId = SourceSnapshotId.deterministic(repositoryReference, revision, checkout.resolvedCommit(), manifestSha);
        var manifest = new ArtifactReference(
            "snapshots/" + sourceSnapshotId.value() + "/manifest.json",
            "application/json",
            manifestSha,
            manifestPayload(repositoryReference, revision, checkout.resolvedCommit(), checkout.sourceRoots()).getBytes(StandardCharsets.UTF_8).length
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
        prepareResults.put(key, new IdempotentResult<>(fingerprint, preparation));
        return preparation;
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
        var replay = cleanupResults.get(key);
        if (replay != null) {
            return replay.sameFingerprintOrThrow(fingerprint);
        }

        var preparation = repository.findByRunAndWorkspace(analysisRunId, workspaceId)
            .orElseThrow(() -> new RepositoryPreparationNotFoundException("repository preparation was not found"));
        workspacePort.cleanup(workspaceId);
        var cleaned = repository.save(preparation.withWorkspaceStatus(RepositoryWorkspaceStatus.CLEANED, clock.instant()));
        var result = new CleanupRepositoryWorkspaceResult(
            cleaned.workspaceId(),
            cleaned.workspaceStatus(),
            List.of(Diagnostic.info("WORKSPACE_CLEANED", "Repository workspace was cleaned"))
        );
        cleanupResults.put(key, new IdempotentResult<>(fingerprint, result));
        return result;
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

    private de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.CheckoutResult checkoutWithCleanupOnFailure(
        RepositoryReference repositoryReference,
        RevisionSelector revision,
        WorkspacePolicy workspacePolicy,
        de.burger.forensics.analytics.services.repositoryanalysis.application.port.PreparedWorkspace workspace
    ) {
        try {
            return checkoutPort.checkout(workspace, repositoryReference, revision, workspacePolicy);
        } catch (RuntimeException error) {
            workspacePort.cleanup(workspace.workspaceId());
            throw error;
        }
    }

    private static String manifestSha256(
        RepositoryReference repository,
        RevisionSelector revision,
        List<de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceRoot> sourceRoots,
        String resolvedCommit
    ) {
        return sha256Hex(manifestPayload(repository, revision, resolvedCommit, sourceRoots));
    }

    private static String manifestPayload(
        RepositoryReference repository,
        RevisionSelector revision,
        String resolvedCommit,
        List<de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceRoot> sourceRoots
    ) {
        return String.join(
            "\n",
            repository.remoteUrl(),
            revision.branch(),
            revision.commit(),
            resolvedCommit,
            sourceRoots.toString()
        );
    }

    private static SourcePackageDescriptor sourcePackage(SourceSnapshotId sourceSnapshotId, ArtifactReference manifest) {
        return new SourcePackageDescriptor(
            PackageAvailability.PENDING,
            manifest,
            null,
            "source-package-descriptor-v1",
            "repository-analysis-service",
            new ArtifactByteAccess(
                "repository-analysis-service",
                "repository-analysis.v1.SourcePackage",
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

    private record IdempotentResult<T>(String fingerprint, T result) {
        private T sameFingerprintOrThrow(String requestedFingerprint) {
            if (!fingerprint.equals(requestedFingerprint)) {
                throw new IdempotencyConflictException("idempotency key was reused with different input");
            }
            return result;
        }
    }
}
