package de.burger.forensics.analytics.services.analysisorchestrator.bootstrap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
public class AnalysisOrchestratorServicePropertiesConfiguration {
    @Bean
    public AnalysisOrchestratorServiceProperties analysisOrchestratorServiceProperties(Environment environment) {
        return new AnalysisOrchestratorServiceProperties(
            new AnalysisOrchestratorServiceProperties.Grpc(
                bool(environment, "forensics.analysis-orchestrator.service.grpc.enabled", true),
                text(environment, "forensics.analysis-orchestrator.service.grpc.host", "127.0.0.1"),
                integer(environment, "forensics.analysis-orchestrator.service.grpc.port", 9098)
            ),
            new AnalysisOrchestratorServiceProperties.Health(
                bool(environment, "forensics.analysis-orchestrator.service.health.enabled", true),
                text(environment, "forensics.analysis-orchestrator.service.health.host", "127.0.0.1"),
                integer(environment, "forensics.analysis-orchestrator.service.health.port", 8089)
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
