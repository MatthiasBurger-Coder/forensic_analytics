package de.burger.forensics.analytics.application.analysis.port;

import de.burger.forensics.analytics.domain.analysis.AnalysisJob;
import de.burger.forensics.analytics.domain.analysis.AnalysisJobId;
import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;

import java.util.List;
import java.util.Optional;

public interface AnalysisStorePort {
    AnalysisJob storeJob(AnalysisJob job);

    Optional<AnalysisJob> findJob(AnalysisJobId jobId);

    List<AnalysisJob> findJobs(AnalysisRunId analysisRunId);
}
