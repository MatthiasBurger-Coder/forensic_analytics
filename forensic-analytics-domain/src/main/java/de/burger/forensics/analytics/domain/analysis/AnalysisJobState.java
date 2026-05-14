package de.burger.forensics.analytics.domain.analysis;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public enum AnalysisJobState {
    ACCEPTED,
    DISPATCHABLE,
    RUNNING,
    RETRYABLE,
    FAILED,
    DEAD_LETTERED,
    COMPLETED;

    private static final Map<AnalysisJobState, Set<AnalysisJobState>> TRANSITIONS = Map.of(
        ACCEPTED, Set.of(DISPATCHABLE),
        DISPATCHABLE, Set.of(RUNNING),
        RUNNING, Set.of(RETRYABLE, FAILED, COMPLETED),
        RETRYABLE, Set.of(DISPATCHABLE, DEAD_LETTERED),
        FAILED, Set.of(DEAD_LETTERED),
        DEAD_LETTERED, Set.of(),
        COMPLETED, Set.of()
    );

    public boolean canTransitionTo(AnalysisJobState target) {
        Objects.requireNonNull(target, "target must not be null");
        return TRANSITIONS.get(this).contains(target);
    }

    void requireTransitionTo(AnalysisJobState target) {
        if (!canTransitionTo(target)) {
            throw new IllegalStateException("analysis job state " + this + " cannot transition to " + target);
        }
    }
}
