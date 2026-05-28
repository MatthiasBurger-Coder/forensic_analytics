package de.burger.forensics.analytics.services.ingestion.bootstrap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
public class ForensicIngestionServicePropertiesConfiguration {
    @Bean
    public ForensicIngestionServiceProperties forensicIngestionServiceProperties(Environment environment) {
        return new ForensicIngestionServiceProperties(
            new ForensicIngestionServiceProperties.Grpc(
                bool(environment, "forensics.ingestion.service.grpc.enabled", true),
                text(environment, "forensics.ingestion.service.grpc.host", "127.0.0.1"),
                integer(environment, "forensics.ingestion.service.grpc.port", 9090)
            ),
            new ForensicIngestionServiceProperties.Health(
                bool(environment, "forensics.ingestion.service.health.enabled", true),
                text(environment, "forensics.ingestion.service.health.host", "127.0.0.1"),
                integer(environment, "forensics.ingestion.service.health.port", 8081)
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
