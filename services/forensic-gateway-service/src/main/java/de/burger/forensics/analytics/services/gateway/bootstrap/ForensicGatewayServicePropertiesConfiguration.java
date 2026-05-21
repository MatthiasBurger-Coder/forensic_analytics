package de.burger.forensics.analytics.services.gateway.bootstrap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
public class ForensicGatewayServicePropertiesConfiguration {
    @Bean
    public ForensicGatewayServiceProperties forensicGatewayServiceProperties(Environment environment) {
        return new ForensicGatewayServiceProperties(
            new ForensicGatewayServiceProperties.Http(
                bool(environment, "forensics.gateway.service.http.enabled", true),
                text(environment, "forensics.gateway.service.http.host", "127.0.0.1"),
                integer(environment, "forensics.gateway.service.http.port", 8080)
            ),
            new ForensicGatewayServiceProperties.AnalysisStore(
                new ForensicGatewayServiceProperties.Grpc(
                    text(environment, "forensics.gateway.service.analysis-store.grpc.host", "127.0.0.1"),
                    integer(environment, "forensics.gateway.service.analysis-store.grpc.port", 9091),
                    integer(environment, "forensics.gateway.service.analysis-store.grpc.deadline-seconds", 5)
                )
            )
        );
    }

    private static boolean bool(Environment environment, String key, boolean defaultValue) {
        return environment.getProperty(key, Boolean.class, defaultValue);
    }

    private static int integer(Environment environment, String key, int defaultValue) {
        return environment.getProperty(key, Integer.class, defaultValue);
    }

    private static String text(Environment environment, String key, String defaultValue) {
        return environment.getProperty(key, defaultValue);
    }
}
