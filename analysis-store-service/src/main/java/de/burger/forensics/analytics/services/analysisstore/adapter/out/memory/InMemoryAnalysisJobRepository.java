package de.burger.forensics.analytics.services.analysisstore.adapter.out.memory;

import de.burger.forensics.analytics.services.analysisstore.application.port.AnalysisJobRepository;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJob;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJobId;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJobState;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisRunId;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisWorkerKind;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryAnalysisJobRepository implements AnalysisJobRepository {
    private final ConcurrentHashMap<AnalysisJobId, AnalysisJob> jobs = new ConcurrentHashMap<>();

    @Override
    public void save(AnalysisJob job) {
        jobs.put(job.jobId(), job);
    }

    @Override
    public Optional<AnalysisJob> findById(AnalysisJobId jobId) {
        return Optional.ofNullable(jobs.get(jobId));
    }

    @Override
    public List<AnalysisJob> list(AnalysisRunId runId, AnalysisWorkerKind workerKind, AnalysisJobState state) {
        return jobs.values().stream()
            .filter(job -> job.matches(runId, workerKind, state))
            .sorted(Comparator.comparing((AnalysisJob job) -> job.analysisRunId().value())
                .thenComparing(job -> job.jobId().value()))
            .toList();
    }
}
