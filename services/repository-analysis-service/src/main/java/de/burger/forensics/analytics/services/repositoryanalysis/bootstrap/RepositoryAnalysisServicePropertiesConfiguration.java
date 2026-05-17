package de.burger.forensics.analytics.services.repositoryanalysis.bootstrap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.nio.file.Path;

@Configuration(proxyBeanMethods = false)
public class RepositoryAnalysisServicePropertiesConfiguration {
    @Bean
    public RepositoryAnalysisServiceProperties repositoryAnalysisServiceProperties(Environment environment) {
        return new RepositoryAnalysisServiceProperties(
            new RepositoryAnalysisServiceProperties.Grpc(
                bool(environment, "forensics.repository-analysis.service.grpc.enabled", true),
                text(environment, "forensics.repository-analysis.service.grpc.host", "127.0.0.1"),
                integer(environment, "forensics.repository-analysis.service.grpc.port", 9092)
            ),
            new RepositoryAnalysisServiceProperties.Health(
                bool(environment, "forensics.repository-analysis.service.health.enabled", true),
                text(environment, "forensics.repository-analysis.service.health.host", "127.0.0.1"),
                integer(environment, "forensics.repository-analysis.service.health.port", 8083)
            ),
            new RepositoryAnalysisServiceProperties.Workspace(
                Path.of(text(
                    environment,
                    "forensics.repository-analysis.service.workspace.root",
                    "build/repository-analysis-workspaces"
                ))
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
