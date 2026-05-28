package de.burger.forensics.analytics.services.queryreportapi.domain;

import java.util.List;

public record QueryReportApiStatusSnapshot(
    String status,
    List<DownstreamServiceStatus> services
) {
    public QueryReportApiStatusSnapshot {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status must not be blank");
        }
        services = List.copyOf(services);
    }
}
