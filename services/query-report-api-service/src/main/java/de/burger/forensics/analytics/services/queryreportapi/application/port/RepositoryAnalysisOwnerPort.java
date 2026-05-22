package de.burger.forensics.analytics.services.queryreportapi.application.port;

import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.RepositoryToBtmSubmission;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.RepositoryToBtmStatus;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.StatusRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.SubmissionRequest;

public interface RepositoryAnalysisOwnerPort {
    RepositoryToBtmSubmission start(SubmissionRequest request);

    RepositoryToBtmStatus status(StatusRequest request);
}
