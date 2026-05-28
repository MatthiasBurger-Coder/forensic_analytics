package de.burger.forensics.analytics.services.analysisorchestrator.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record AnalysisJob(
    AnalysisRunId analysisRunId,
    AnalysisJobId jobId,
    String schemaVersion,
    String correlationId,
    AnalysisWorkerKind workerKind,
    SourceSnapshotId sourceSnapshotId,
    List<AnalysisArtifactReference> inputArtifacts,
    List<AnalysisArtifactReference> outputArtifacts,
    AnalysisCompleteness completeness,
    AnalysisJobState state,
    int attempt,
    int percentComplete,
    List<AnalysisJobFailure> failures,
    String leaseOwner,
    Instant leaseExpiresAt,
    Instant createdAt,
    Instant updatedAt,
    List<String> diagnostics,
    Map<String, String> attributes
) {
    public AnalysisJob {
        Objects.requireNonNull(analysisRunId, "analysisRunId must not be null");
        Objects.requireNonNull(jobId, "jobId must not be null");
        schemaVersion = RequiredText.require(schemaVersion, "schemaVersion");
        correlationId = RequiredText.require(correlationId, "correlationId");
        Objects.requireNonNull(workerKind, "workerKind must not be null");
        Objects.requireNonNull(sourceSnapshotId, "sourceSnapshotId must not be null");
        inputArtifacts = List.copyOf(Objects.requireNonNull(inputArtifacts, "inputArtifacts must not be null"));
        outputArtifacts = List.copyOf(Objects.requireNonNull(outputArtifacts, "outputArtifacts must not be null"));
        Objects.requireNonNull(completeness, "completeness must not be null");
        Objects.requireNonNull(state, "state must not be null");
        if (attempt < 0) {
            throw new IllegalArgumentException("attempt must not be negative");
        }
        if (percentComplete < 0 || percentComplete > 100) {
            throw new IllegalArgumentException("percentComplete must be between 0 and 100");
        }
        failures = List.copyOf(Objects.requireNonNull(failures, "failures must not be null"));
        leaseOwner = leaseOwner == null ? "" : leaseOwner.strip();
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics must not be null"));
        attributes = copyAttributes(attributes);
    }

    public static AnalysisJob submitted(
        AnalysisRunId analysisRunId,
        AnalysisJobId jobId,
        String schemaVersion,
        String correlationId,
        AnalysisWorkerKind workerKind,
        SourceSnapshotId sourceSnapshotId,
        List<AnalysisArtifactReference> inputArtifacts,
        AnalysisCompleteness inputCompleteness,
        Instant now,
        Map<String, String> attributes
    ) {
        return new AnalysisJob(
            analysisRunId,
            jobId,
            schemaVersion,
            correlationId,
            workerKind,
            sourceSnapshotId,
            inputArtifacts,
            List.of(),
            inputCompleteness,
            AnalysisJobState.DISPATCHABLE,
            0,
            0,
            List.of(),
            "",
            null,
            now,
            now,
            List.of(),
            attributes
        );
    }

    public boolean matches(
        AnalysisRunId filterRunId,
        AnalysisWorkerKind filterWorkerKind,
        AnalysisJobState filterState
    ) {
        return (filterRunId == null || analysisRunId.equals(filterRunId))
            && (filterWorkerKind == null || workerKind == filterWorkerKind)
            && (filterState == null || state == filterState);
    }

    public AnalysisJob leased(String workerId, int leaseSeconds, Instant now) {
        if (state != AnalysisJobState.DISPATCHABLE && state != AnalysisJobState.RETRYABLE) {
            throw new IllegalStateException("job " + jobId.value() + " is not leaseable while " + state);
        }
        if (leaseSeconds < 1) {
            throw new IllegalArgumentException("leaseSeconds must be positive");
        }
        return new AnalysisJob(
            analysisRunId,
            jobId,
            schemaVersion,
            correlationId,
            workerKind,
            sourceSnapshotId,
            inputArtifacts,
            outputArtifacts,
            completeness,
            AnalysisJobState.RUNNING,
            attempt + 1,
            percentComplete,
            failures,
            RequiredText.require(workerId, "workerId"),
            now.plusSeconds(leaseSeconds),
            createdAt,
            now,
            diagnostics,
            attributes
        );
    }

    public boolean hasExpiredLease(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return state == AnalysisJobState.RUNNING
            && leaseExpiresAt != null
            && !leaseExpiresAt.isAfter(now);
    }

    public AnalysisJob leaseTimedOut(Instant now) {
        if (!hasExpiredLease(now)) {
            throw new IllegalStateException("job " + jobId.value() + " lease has not expired");
        }
        return failed(
            leaseOwner,
            attempt,
            "worker lease expired at " + leaseExpiresAt,
            List.of("worker lease expired before completion"),
            completeness,
            true,
            now
        );
    }

    public AnalysisJob progressed(String workerId, int expectedAttempt, int percentComplete, List<String> nextDiagnostics, Instant now) {
        requireRunningWorker(workerId, expectedAttempt);
        if (percentComplete < 0 || percentComplete > 100) {
            throw new IllegalArgumentException("percentComplete must be between 0 and 100");
        }
        return withDiagnostics(percentComplete, nextDiagnostics, now);
    }

    public AnalysisJob completed(
        String workerId,
        int expectedAttempt,
        List<AnalysisArtifactReference> nextOutputArtifacts,
        AnalysisCompleteness outputCompleteness,
        List<String> nextDiagnostics,
        Instant now
    ) {
        requireRunningWorker(workerId, expectedAttempt);
        return new AnalysisJob(
            analysisRunId,
            jobId,
            schemaVersion,
            correlationId,
            workerKind,
            sourceSnapshotId,
            inputArtifacts,
            mergeArtifacts(outputArtifacts, nextOutputArtifacts),
            outputCompleteness,
            AnalysisJobState.COMPLETED,
            attempt,
            100,
            failures,
            "",
            null,
            createdAt,
            now,
            nextDiagnostics,
            attributes
        );
    }

    public AnalysisJob failed(
        String workerId,
        int expectedAttempt,
        String reason,
        List<String> nextDiagnostics,
        AnalysisCompleteness failureCompleteness,
        boolean retryable,
        Instant now
    ) {
        requireRunningWorker(workerId, expectedAttempt);
        var failure = new AnalysisJobFailure(jobId, workerKind, attempt, reason, nextDiagnostics, failureCompleteness, retryable);
        var nextFailures = new java.util.ArrayList<>(failures);
        nextFailures.add(failure);
        return new AnalysisJob(
            analysisRunId,
            jobId,
            schemaVersion,
            correlationId,
            workerKind,
            sourceSnapshotId,
            inputArtifacts,
            outputArtifacts,
            failureCompleteness,
            retryable ? AnalysisJobState.RETRYABLE : AnalysisJobState.DEAD_LETTERED,
            attempt,
            percentComplete,
            nextFailures,
            "",
            null,
            createdAt,
            now,
            nextDiagnostics,
            attributes
        );
    }

    private AnalysisJob withDiagnostics(int nextPercentComplete, List<String> nextDiagnostics, Instant now) {
        return new AnalysisJob(
            analysisRunId,
            jobId,
            schemaVersion,
            correlationId,
            workerKind,
            sourceSnapshotId,
            inputArtifacts,
            outputArtifacts,
            completeness,
            state,
            attempt,
            nextPercentComplete,
            failures,
            leaseOwner,
            leaseExpiresAt,
            createdAt,
            now,
            nextDiagnostics,
            attributes
        );
    }

    private void requireRunningWorker(String workerId, int expectedAttempt) {
        if (state != AnalysisJobState.RUNNING) {
            throw new IllegalStateException("job " + jobId.value() + " is not running");
        }
        if (attempt != expectedAttempt) {
            throw new IllegalStateException("job " + jobId.value() + " attempt mismatch");
        }
        if (!leaseOwner.equals(RequiredText.require(workerId, "workerId"))) {
            throw new IllegalStateException("job " + jobId.value() + " lease owner mismatch");
        }
    }

    private static List<AnalysisArtifactReference> mergeArtifacts(
        List<AnalysisArtifactReference> existing,
        List<AnalysisArtifactReference> additions
    ) {
        var byPath = new java.util.LinkedHashMap<String, AnalysisArtifactReference>();
        existing.forEach(artifact -> byPath.put(artifact.path(), artifact));
        additions.forEach(artifact -> {
            var previous = byPath.putIfAbsent(artifact.path(), artifact);
            if (previous != null && !previous.equals(artifact)) {
                throw new IllegalArgumentException("artifact path " + artifact.path() + " conflicts with existing metadata");
            }
        });
        return List.copyOf(byPath.values());
    }

    private static Map<String, String> copyAttributes(Map<String, String> source) {
        return SafeMetadata.safeAttributes(Objects.requireNonNull(source, "attributes must not be null"));
    }
}
