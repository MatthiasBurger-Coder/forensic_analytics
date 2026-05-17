package de.burger.forensics.analytics.services.analysisstore.bootstrap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
public class AnalysisStoreServicePropertiesConfiguration {
    @Bean
    public AnalysisStoreServiceProperties analysisStoreServiceProperties(Environment environment) {
        return new AnalysisStoreServiceProperties(
            new AnalysisStoreServiceProperties.Grpc(
                bool(environment, "forensics.analysis-store.service.grpc.enabled", true),
                text(environment, "forensics.analysis-store.service.grpc.host", "127.0.0.1"),
                integer(environment, "forensics.analysis-store.service.grpc.port", 9091)
            ),
            new AnalysisStoreServiceProperties.Health(
                bool(environment, "forensics.analysis-store.service.health.enabled", true),
                text(environment, "forensics.analysis-store.service.health.host", "127.0.0.1"),
                integer(environment, "forensics.analysis-store.service.health.port", 8082)
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
