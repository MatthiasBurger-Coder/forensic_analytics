package de.burger.forensics.analytics.services.analysisstore.application;

import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisWorkerKind;
import de.burger.forensics.analytics.services.analysisstore.domain.RepositoryToBtmOrchestrationDomain;
import de.burger.forensics.analytics.services.analysisstore.domain.RepositoryToBtmOrchestrationDomain.BtmDeliveryReadiness;
import de.burger.forensics.analytics.services.analysisstore.domain.RepositoryToBtmOrchestrationDomain.OrchestrationState;
import de.burger.forensics.analytics.services.analysisstore.domain.RepositoryToBtmOrchestrationDomain.RepositoryToBtmDiagnostic;
import de.burger.forensics.analytics.services.analysisstore.domain.RepositoryToBtmOrchestrationDomain.RepositoryToBtmOrchestrationStatus;
import de.burger.forensics.analytics.services.analysisstore.domain.RepositoryToBtmOrchestrationDomain.StartRepositoryToBtmCommand;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisRunId;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class RepositoryToBtmOrchestrationApplicationService {
    private static final List<RepositoryToBtmDiagnostic> PENDING_REPOSITORY_DIAGNOSTICS = List.of(
        RepositoryToBtmDiagnostic.warning(
            "REPOSITORY_SOURCE_PACKAGE_UNAVAILABLE",
            "Repository source package is not available yet; Repository Analysis must complete before Java AST and Joern workers can run.",
            true
        ),
        RepositoryToBtmDiagnostic.warning(
            "BUILD_OUTPUT_PACKAGE_UNAVAILABLE",
            "Build output package is not available yet; Artifactory, Jenkins or build-artifact-worker must provide it before bytecode-aware analysis can run.",
            true
        ),
        RepositoryToBtmDiagnostic.warning(
            "JOERN_SKIPPED_UNAVAILABLE_PACKAGE",
            "Joern analysis is skipped until source and build-output packages are AVAILABLE and COMPLETE.",
            true
        )
    );

    private final AnalysisJobApplicationService jobs;
    private final Map<String, StoredStart> starts = new ConcurrentHashMap<>();

    public RepositoryToBtmOrchestrationApplicationService(AnalysisJobApplicationService jobs) {
        this.jobs = Objects.requireNonNull(jobs, "jobs must not be null");
    }

    public synchronized RepositoryToBtmOrchestrationStatus start(
        String idempotencyKey,
        StartRepositoryToBtmCommand command
    ) {
        var verifiedCommand = Objects.requireNonNull(command, "command must not be null");
        return idempotent(idempotencyKey, verifiedCommand.fingerprint(), () -> createStart(verifiedCommand));
    }

    public synchronized RepositoryToBtmOrchestrationStatus status(String correlationId, AnalysisRunId analysisRunId) {
        var jobId = RepositoryToBtmOrchestrationDomain.repositoryAnalysisJobId(analysisRunId);
        var job = jobs.get(jobId);
        return status(
            correlationId,
            analysisRunId,
            jobId,
            job.sourceSnapshotId(),
            PENDING_REPOSITORY_DIAGNOSTICS,
            Map.of("repositoryAnalysisJobState", job.state().name())
        );
    }

    private RepositoryToBtmOrchestrationStatus createStart(StartRepositoryToBtmCommand command) {
        var analysisRunId = command.metadata().analysisRunId();
        var repositoryJobId = RepositoryToBtmOrchestrationDomain.repositoryAnalysisJobId(analysisRunId);
        var sourceSnapshotId = RepositoryToBtmOrchestrationDomain.pendingSourceSnapshotId(analysisRunId);
        jobs.submit(
            "repository-to-btm-repository-job:" + command.metadata().requestId(),
            command.metadata().correlationId(),
            analysisRunId,
            repositoryJobId,
            command.metadata().schemaVersion(),
            AnalysisWorkerKind.REPOSITORY_ANALYSIS,
            sourceSnapshotId,
            List.of(),
            AnalysisCompleteness.UNKNOWN,
            Map.of(
                "owner", "analysis-store-service",
                "orchestration", "repository-to-btm",
                "requestedOutput", "BTM_RULES"
            )
        );
        return status(
            command.metadata().correlationId(),
            analysisRunId,
            repositoryJobId,
            sourceSnapshotId,
            PENDING_REPOSITORY_DIAGNOSTICS,
            Map.of("repositoryAnalysisJobState", "DISPATCHABLE")
        );
    }

    private static RepositoryToBtmOrchestrationStatus status(
        String correlationId,
        AnalysisRunId analysisRunId,
        de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJobId repositoryJobId,
        de.burger.forensics.analytics.services.analysisstore.domain.SourceSnapshotId sourceSnapshotId,
        List<RepositoryToBtmDiagnostic> diagnostics,
        Map<String, String> attributes
    ) {
        return new RepositoryToBtmOrchestrationStatus(
            RepositoryToBtmOrchestrationDomain.OperationStatus.accepted(correlationId, diagnostics),
            analysisRunId,
            repositoryJobId,
            sourceSnapshotId,
            AnalysisCompleteness.INCOMPLETE,
            OrchestrationState.WAITING_FOR_REPOSITORY,
            BtmDeliveryReadiness.NOT_READY,
            true,
            diagnostics,
            List.of(),
            attributes
        );
    }

    private RepositoryToBtmOrchestrationStatus idempotent(
        String idempotencyKey,
        String fingerprint,
        Supplier<RepositoryToBtmOrchestrationStatus> supplier
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
        var key = idempotencyKey.strip();
        var existing = starts.get(key);
        if (existing != null) {
            if (!existing.fingerprint().equals(fingerprint)) {
                throw new IdempotencyConflictException(key);
            }
            return existing.status();
        }
        var status = supplier.get();
        starts.put(key, new StoredStart(fingerprint, status));
        return status;
    }

    private record StoredStart(String fingerprint, RepositoryToBtmOrchestrationStatus status) {
    }
}
