package de.burger.forensics.analytics.services.queryreportapi.application;

import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiStatusSnapshot;
import de.burger.forensics.analytics.services.queryreportapi.domain.DownstreamServiceStatus;

import java.util.List;

public final class QueryReportApiStatusService {
    public QueryReportApiStatusSnapshot currentStatus() {
        return new QueryReportApiStatusSnapshot(
            "UP",
            List.of(new DownstreamServiceStatus("analysis-orchestrator-service", "UNKNOWN"))
        );
    }
}
