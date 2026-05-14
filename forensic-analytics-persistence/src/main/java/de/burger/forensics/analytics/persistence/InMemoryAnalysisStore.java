package de.burger.forensics.analytics.persistence;

import de.burger.forensics.analytics.application.analysis.port.AnalysisStorePort;
import de.burger.forensics.analytics.domain.analysis.AnalysisJob;
import de.burger.forensics.analytics.domain.analysis.AnalysisJobId;
import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryAnalysisStore implements AnalysisStorePort {
    private final Map<AnalysisJobId, AnalysisJob> jobs = new ConcurrentHashMap<>();

    @Override
    public AnalysisJob storeJob(AnalysisJob job) {
        Objects.requireNonNull(job, "job must not be null");
        jobs.put(job.id(), job);
        return job;
    }

    @Override
    public Optional<AnalysisJob> findJob(AnalysisJobId jobId) {
        Objects.requireNonNull(jobId, "jobId must not be null");
        return Optional.ofNullable(jobs.get(jobId));
    }

    @Override
    public List<AnalysisJob> findJobs(AnalysisRunId analysisRunId) {
        Objects.requireNonNull(analysisRunId, "analysisRunId must not be null");
        return jobs.values().stream()
            .filter(job -> job.analysisRunId().equals(analysisRunId))
            .sorted(Comparator.comparing(job -> job.id().value()))
            .toList();
    }
}
