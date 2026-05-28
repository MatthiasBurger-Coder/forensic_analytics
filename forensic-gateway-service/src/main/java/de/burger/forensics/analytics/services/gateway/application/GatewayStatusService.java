package de.burger.forensics.analytics.services.gateway.application;

import de.burger.forensics.analytics.services.gateway.domain.GatewayStatusSnapshot;

import java.util.List;

public final class GatewayStatusService {
    public GatewayStatusSnapshot currentStatus() {
        return new GatewayStatusSnapshot("UP", List.of());
    }
}
