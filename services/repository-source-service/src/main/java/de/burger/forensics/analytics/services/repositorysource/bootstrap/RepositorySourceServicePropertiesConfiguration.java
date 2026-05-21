package de.burger.forensics.analytics.services.repositorysource.bootstrap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.nio.file.Path;

@Configuration(proxyBeanMethods = false)
public class RepositorySourceServicePropertiesConfiguration {
    @Bean
    public RepositorySourceServiceProperties repositorySourceServiceProperties(Environment environment) {
        return new RepositorySourceServiceProperties(
            new RepositorySourceServiceProperties.Grpc(
                bool(environment, "forensics.repository-source.service.grpc.enabled", true),
                text(environment, "forensics.repository-source.service.grpc.host", "127.0.0.1"),
                integer(environment, "forensics.repository-source.service.grpc.port", 9092)
            ),
            new RepositorySourceServiceProperties.Health(
                bool(environment, "forensics.repository-source.service.health.enabled", true),
                text(environment, "forensics.repository-source.service.health.host", "127.0.0.1"),
                integer(environment, "forensics.repository-source.service.health.port", 8083)
            ),
            new RepositorySourceServiceProperties.Workspace(
                Path.of(text(
                    environment,
                    "forensics.repository-source.service.workspace.root",
                    "build/repository-source-workspaces"
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
