package de.burger.forensics.analytics.domain.analysis;

import de.burger.forensics.analytics.domain.repository.SourceSnapshotId;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record AnalysisJob(
    AnalysisRunId analysisRunId,
    AnalysisJobId id,
    AnalysisWorkerKind workerKind,
    SourceSnapshotId sourceSnapshotId,
    List<AnalysisArtifactReference> inputArtifacts,
    AnalysisCompleteness completeness,
    AnalysisJobState state,
    int attempt,
    List<AnalysisJobFailure> failures,
    Optional<DeadLetterProvenance> deadLetterProvenance
) {
    public AnalysisJob {
        Objects.requireNonNull(analysisRunId, "analysisRunId must not be null");
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(workerKind, "workerKind must not be null");
        Objects.requireNonNull(sourceSnapshotId, "sourceSnapshotId must not be null");
        inputArtifacts = copyRequiredArtifacts(inputArtifacts);
        Objects.requireNonNull(completeness, "completeness must not be null");
        Objects.requireNonNull(state, "state must not be null");
        if (attempt < 0) {
            throw new IllegalArgumentException("attempt must not be negative");
        }
        failures = List.copyOf(Objects.requireNonNull(failures, "failures must not be null"));
        deadLetterProvenance = Objects.requireNonNull(
            deadLetterProvenance,
            "deadLetterProvenance must not be null"
        );
        requireStateProvenance(state, failures, deadLetterProvenance);
    }

    public static AnalysisJob accepted(
        AnalysisRunId analysisRunId,
        AnalysisJobId id,
        AnalysisWorkerKind workerKind,
        SourceSnapshotId sourceSnapshotId,
        List<AnalysisArtifactReference> inputArtifacts,
        AnalysisCompleteness completeness
    ) {
        return new AnalysisJob(
            analysisRunId,
            id,
            workerKind,
            sourceSnapshotId,
            inputArtifacts,
            completeness,
            AnalysisJobState.ACCEPTED,
            0,
            List.of(),
            Optional.empty()
        );
    }

    public AnalysisJob dispatchable() {
        return transitionTo(AnalysisJobState.DISPATCHABLE, attempt, failures, Optional.empty());
    }

    public AnalysisJob running(int nextAttempt) {
        if (nextAttempt <= attempt) {
            throw new IllegalArgumentException("next attempt must be greater than current attempt");
        }
        return transitionTo(AnalysisJobState.RUNNING, nextAttempt, failures, Optional.empty());
    }

    public AnalysisJob retryable(AnalysisJobFailure failure) {
        return transitionWithFailure(AnalysisJobState.RETRYABLE, failure);
    }

    public AnalysisJob failed(AnalysisJobFailure failure) {
        return transitionWithFailure(AnalysisJobState.FAILED, failure);
    }

    public AnalysisJob deadLettered(DeadLetterProvenance provenance) {
        Objects.requireNonNull(provenance, "provenance must not be null");
        requireMatchingDeadLetter(provenance);
        var recordedFailures = failures.contains(provenance.finalFailure())
            ? failures
            : appendFailure(failures, provenance.finalFailure());
        return transitionTo(AnalysisJobState.DEAD_LETTERED, attempt, recordedFailures, Optional.of(provenance));
    }

    public AnalysisJob completed() {
        return transitionTo(AnalysisJobState.COMPLETED, attempt, failures, Optional.empty());
    }

    private AnalysisJob transitionWithFailure(AnalysisJobState target, AnalysisJobFailure failure) {
        Objects.requireNonNull(failure, "failure must not be null");
        requireMatchingFailure(failure);
        return transitionTo(target, attempt, appendFailure(failures, failure), Optional.empty());
    }

    private AnalysisJob transitionTo(
        AnalysisJobState target,
        int nextAttempt,
        List<AnalysisJobFailure> nextFailures,
        Optional<DeadLetterProvenance> nextDeadLetterProvenance
    ) {
        state.requireTransitionTo(target);
        return new AnalysisJob(
            analysisRunId,
            id,
            workerKind,
            sourceSnapshotId,
            inputArtifacts,
            completeness,
            target,
            nextAttempt,
            nextFailures,
            nextDeadLetterProvenance
        );
    }

    private void requireMatchingFailure(AnalysisJobFailure failure) {
        if (!id.equals(failure.jobId())) {
            throw new IllegalArgumentException("failure job id must match job id");
        }
        if (!workerKind.equals(failure.workerKind())) {
            throw new IllegalArgumentException("failure worker kind must match job worker kind");
        }
        if (failure.attempt() != attempt) {
            throw new IllegalArgumentException("failure attempt must match current attempt");
        }
    }

    private void requireMatchingDeadLetter(DeadLetterProvenance provenance) {
        if (!id.equals(provenance.jobId())) {
            throw new IllegalArgumentException("dead-letter job id must match job id");
        }
        if (!workerKind.equals(provenance.workerKind())) {
            throw new IllegalArgumentException("dead-letter worker kind must match job worker kind");
        }
        if (!inputArtifacts.equals(provenance.inputArtifacts())) {
            throw new IllegalArgumentException("dead-letter input artifacts must match job input artifacts");
        }
        if (!completeness.equals(provenance.completeness())) {
            throw new IllegalArgumentException("dead-letter completeness must match job completeness");
        }
    }

    private static List<AnalysisArtifactReference> copyRequiredArtifacts(List<AnalysisArtifactReference> artifacts) {
        var copied = List.copyOf(Objects.requireNonNull(artifacts, "inputArtifacts must not be null"));
        if (copied.isEmpty()) {
            throw new IllegalArgumentException("inputArtifacts must not be empty");
        }
        return copied;
    }

    private static List<AnalysisJobFailure> appendFailure(
        List<AnalysisJobFailure> currentFailures,
        AnalysisJobFailure failure
    ) {
        return java.util.stream.Stream.concat(currentFailures.stream(), java.util.stream.Stream.of(failure)).toList();
    }

    private static void requireStateProvenance(
        AnalysisJobState state,
        List<AnalysisJobFailure> failures,
        Optional<DeadLetterProvenance> deadLetterProvenance
    ) {
        if (AnalysisJobState.DEAD_LETTERED.equals(state) && deadLetterProvenance.isEmpty()) {
            throw new IllegalArgumentException("dead-lettered job must include dead-letter provenance");
        }
        if (!AnalysisJobState.DEAD_LETTERED.equals(state) && deadLetterProvenance.isPresent()) {
            throw new IllegalArgumentException("dead-letter provenance is only valid for dead-lettered jobs");
        }
        if ((AnalysisJobState.RETRYABLE.equals(state) || AnalysisJobState.FAILED.equals(state)) && failures.isEmpty()) {
            throw new IllegalArgumentException("failed job state must include failure provenance");
        }
    }
}
