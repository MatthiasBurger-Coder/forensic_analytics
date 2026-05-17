package de.burger.forensics.analytics.services.analysisstore.application;

import de.burger.forensics.analytics.services.analysisstore.application.port.AnalysisJobRepository;
import de.burger.forensics.analytics.services.analysisstore.application.result.LeaseAnalysisJobResult;
import de.burger.forensics.analytics.services.analysisstore.application.result.OperationOutcome;
import de.burger.forensics.analytics.services.analysisstore.application.result.RegisterAnalysisArtifactsResult;
import de.burger.forensics.analytics.services.analysisstore.application.result.SubmitAnalysisJobResult;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJob;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJobId;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJobState;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisRunId;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisWorkerKind;
import de.burger.forensics.analytics.services.analysisstore.domain.SourceSnapshotId;

import java.time.Clock;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class AnalysisJobApplicationService {
    private final AnalysisJobRepository jobs;
    private final Clock clock;
    private final Map<String, StoredOperation> idempotentResults = new java.util.concurrent.ConcurrentHashMap<>();

    public AnalysisJobApplicationService(AnalysisJobRepository jobs, Clock clock) {
        this.jobs = Objects.requireNonNull(jobs, "jobs must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public synchronized SubmitAnalysisJobResult submit(
        String idempotencyKey,
        String correlationId,
        AnalysisRunId analysisRunId,
        AnalysisJobId jobId,
        String schemaVersion,
        AnalysisWorkerKind workerKind,
        SourceSnapshotId sourceSnapshotId,
        List<AnalysisArtifactReference> inputArtifacts,
        AnalysisCompleteness inputCompleteness,
        Map<String, String> attributes
    ) {
        var fingerprint = List.of(
            "submit",
            correlationId,
            analysisRunId,
            jobId,
            schemaVersion,
            workerKind,
            sourceSnapshotId,
            inputArtifacts,
            inputCompleteness,
            attributes
        ).toString();
        return idempotent("submit", idempotencyKey, fingerprint, SubmitAnalysisJobResult.class, () -> {
            jobs.findById(jobId).ifPresent(existing -> {
                throw new IllegalArgumentException("analysis job already exists: " + jobId.value());
            });
            var job = AnalysisJob.submitted(
                analysisRunId,
                jobId,
                schemaVersion,
                correlationId,
                workerKind,
                sourceSnapshotId,
                inputArtifacts,
                inputCompleteness,
                clock.instant(),
                attributes
            );
            jobs.save(job);
            return new SubmitAnalysisJobResult(job, OperationOutcome.accepted(correlationId, "Analysis job accepted"));
        });
    }

    public synchronized AnalysisJob get(AnalysisJobId jobId) {
        return job(jobId);
    }

    public synchronized List<AnalysisJob> list(AnalysisRunId runId, AnalysisWorkerKind workerKind, AnalysisJobState state) {
        return jobs.list(runId, workerKind, state);
    }

    public synchronized LeaseAnalysisJobResult lease(
        String idempotencyKey,
        String correlationId,
        String workerId,
        AnalysisWorkerKind workerKind,
        int leaseSeconds,
        int maxJobs
    ) {
        if (maxJobs < 1) {
            throw new IllegalArgumentException("maxJobs must be positive");
        }
        var fingerprint = List.of("lease", correlationId, workerId, workerKind, leaseSeconds, maxJobs).toString();
        return idempotent("lease", idempotencyKey, fingerprint, LeaseAnalysisJobResult.class, () -> {
        var leaseable = jobs.list(null, workerKind, null).stream()
            .filter(job -> job.state() == AnalysisJobState.DISPATCHABLE || job.state() == AnalysisJobState.RETRYABLE)
            .limit(maxJobs)
            .map(job -> job.leased(workerId, leaseSeconds, clock.instant()))
            .toList();
            leaseable.forEach(jobs::save);
            return new LeaseAnalysisJobResult(
                leaseable,
                OperationOutcome.accepted(correlationId, "Analysis jobs leased")
            );
        });
    }

    public synchronized AnalysisJob progress(
        String idempotencyKey,
        String correlationId,
        AnalysisJobId jobId,
        int attempt,
        String workerId,
        int percentComplete,
        List<String> diagnostics
    ) {
        var fingerprint = List.of("progress", correlationId, jobId, attempt, workerId, percentComplete, diagnostics).toString();
        return idempotent("progress", idempotencyKey, fingerprint, AnalysisJob.class, () -> {
            var progressed = job(jobId).progressed(workerId, attempt, percentComplete, diagnostics, clock.instant());
            jobs.save(progressed);
            return progressed;
        });
    }

    public synchronized AnalysisJob complete(
        String idempotencyKey,
        String correlationId,
        AnalysisJobId jobId,
        int attempt,
        String workerId,
        List<AnalysisArtifactReference> outputArtifacts,
        AnalysisCompleteness outputCompleteness,
        List<String> diagnostics
    ) {
        var fingerprint = List.of("complete", correlationId, jobId, attempt, workerId, outputArtifacts, outputCompleteness, diagnostics).toString();
        return idempotent("complete", idempotencyKey, fingerprint, AnalysisJob.class, () -> {
            var completed = job(jobId).completed(
                workerId,
                attempt,
                outputArtifacts,
                outputCompleteness,
                diagnostics,
                clock.instant()
            );
            jobs.save(completed);
            return completed;
        });
    }

    public synchronized AnalysisJob fail(
        String idempotencyKey,
        String correlationId,
        AnalysisJobId jobId,
        int attempt,
        String workerId,
        String reason,
        List<String> diagnostics,
        AnalysisCompleteness completeness,
        boolean retryable
    ) {
        var fingerprint = List.of("fail", correlationId, jobId, attempt, workerId, reason, diagnostics, completeness, retryable).toString();
        return idempotent("fail", idempotencyKey, fingerprint, AnalysisJob.class, () -> {
            var failed = job(jobId).failed(workerId, attempt, reason, diagnostics, completeness, retryable, clock.instant());
            jobs.save(failed);
            return failed;
        });
    }

    public synchronized RegisterAnalysisArtifactsResult registerArtifacts(
        String idempotencyKey,
        String correlationId,
        AnalysisRunId analysisRunId,
        AnalysisJobId jobId,
        List<AnalysisArtifactReference> artifacts
    ) {
        var fingerprint = List.of("registerArtifacts", correlationId, analysisRunId, jobId, artifacts).toString();
        return idempotent("registerArtifacts", idempotencyKey, fingerprint, RegisterAnalysisArtifactsResult.class, () -> {
            var existing = job(jobId);
            if (!existing.analysisRunId().equals(analysisRunId)) {
                throw new IllegalArgumentException("analysisRunId does not match existing job");
            }
            var registered = new AnalysisJob(
                existing.analysisRunId(),
                existing.jobId(),
                existing.schemaVersion(),
                existing.correlationId(),
                existing.workerKind(),
                existing.sourceSnapshotId(),
                existing.inputArtifacts(),
                mergeArtifacts(existing.outputArtifacts(), artifacts),
                existing.completeness(),
                existing.state(),
                existing.attempt(),
                existing.percentComplete(),
                existing.failures(),
                existing.leaseOwner(),
                existing.leaseExpiresAt(),
                existing.createdAt(),
                clock.instant(),
                existing.diagnostics(),
                existing.attributes()
            );
            jobs.save(registered);
            return new RegisterAnalysisArtifactsResult(
                registered.outputArtifacts(),
                OperationOutcome.accepted(correlationId, "Analysis artifacts registered")
            );
        });
    }

    private AnalysisJob job(AnalysisJobId jobId) {
        return jobs.findById(jobId)
            .orElseThrow(() -> new AnalysisJobNotFoundException(jobId.value()));
    }

    private <T> T idempotent(
        String operation,
        String key,
        String fingerprint,
        Class<T> resultType,
        Supplier<T> supplier
    ) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
        var operationKey = operation + ":" + key.strip();
        var existing = idempotentResults.get(operationKey);
        if (existing != null) {
            if (!existing.fingerprint().equals(fingerprint)) {
                throw new IdempotencyConflictException(key);
            }
            return resultType.cast(existing.result());
        }
        var result = supplier.get();
        idempotentResults.put(operationKey, new StoredOperation(fingerprint, result));
        return result;
    }

    private static List<AnalysisArtifactReference> mergeArtifacts(
        List<AnalysisArtifactReference> existing,
        List<AnalysisArtifactReference> additions
    ) {
        var byPath = new LinkedHashMap<String, AnalysisArtifactReference>();
        existing.stream()
            .sorted(Comparator.comparing(AnalysisArtifactReference::path))
            .forEach(artifact -> byPath.put(artifact.path(), artifact));
        additions.forEach(artifact -> {
            var previous = byPath.putIfAbsent(artifact.path(), artifact);
            if (previous != null && !previous.equals(artifact)) {
                throw new IllegalArgumentException("artifact path " + artifact.path() + " conflicts with existing metadata");
            }
        });
        return List.copyOf(byPath.values());
    }

    private record StoredOperation(String fingerprint, Object result) {
    }
}
