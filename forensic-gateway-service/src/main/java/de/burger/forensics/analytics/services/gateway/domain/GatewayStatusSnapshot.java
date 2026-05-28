package de.burger.forensics.analytics.services.gateway.domain;

import java.util.List;

public record GatewayStatusSnapshot(
    String status,
    List<DownstreamServiceStatus> services
) {
    public GatewayStatusSnapshot {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status must not be blank");
        }
        services = List.copyOf(services);
    }
}
