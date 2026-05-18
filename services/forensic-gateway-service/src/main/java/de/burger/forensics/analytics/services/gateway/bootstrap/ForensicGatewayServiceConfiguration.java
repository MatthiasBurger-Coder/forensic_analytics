package de.burger.forensics.analytics.services.gateway.bootstrap;

import de.burger.forensics.analytics.services.gateway.adapter.in.http.GatewayHttpHandler;
import de.burger.forensics.analytics.services.gateway.application.GatewayStatusService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ForensicGatewayServiceConfiguration {
    @Bean
    public GatewayStatusService gatewayStatusService() {
        return new GatewayStatusService();
    }

    @Bean
    public GatewayHttpHandler gatewayHttpHandler(GatewayStatusService gatewayStatusService) {
        return new GatewayHttpHandler(gatewayStatusService);
    }

    @Bean
    public GatewayHttpServerLifecycle gatewayHttpServerLifecycle(
        ForensicGatewayServiceProperties properties,
        GatewayHttpHandler gatewayHttpHandler
    ) {
        return new GatewayHttpServerLifecycle(properties, gatewayHttpHandler);
    }
}
