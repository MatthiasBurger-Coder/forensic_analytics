package de.burger.forensics.analytics.boot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.nio.file.Path;
import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class ForensicAnalyticsPropertiesConfiguration {
    private static final String DEFAULT_JOERN_IMAGE =
        "ghcr.io/joernio/joern@sha256:7918dc450f185433fe6cfaf43e86f5daf5643fba2139406a41a1e6e1d6134295";

    @Bean
    public ForensicAnalyticsProperties forensicAnalyticsProperties(Environment environment) {
        var allowRelativeWorkspacePaths = bool(environment, "workspace.allow-relative-paths", false);
        return new ForensicAnalyticsProperties(
            new ForensicAnalyticsProperties.Workspace(
                path(environment, "workspace.root-path", "/var/lib/forensic-analytics"),
                path(environment, "workspace.base-path", "/var/lib/forensic-analytics/workspaces"),
                allowRelativeWorkspacePaths
            ),
            new ForensicAnalyticsProperties.Grpc(
                bool(environment, "ingestion.grpc.enabled", true),
                text(environment, "ingestion.grpc.host", "127.0.0.1"),
                integer(environment, "ingestion.grpc.port", 9090)
            ),
            new ForensicAnalyticsProperties.Rest(
                bool(environment, "rest.enabled", true),
                text(environment, "rest.host", "127.0.0.1"),
                integer(environment, "rest.port", 8080)
            ),
            new ForensicAnalyticsProperties.Joern(
                bool(environment, "joern.enabled", false),
                text(environment, "joern.container-image", DEFAULT_JOERN_IMAGE),
                path(environment, "joern.output-directory", "/var/lib/forensic-analytics/workspaces/joern"),
                duration(environment, "joern.timeout", Duration.ofMinutes(5)),
                bool(environment, "joern.fail-on-error", true)
            ),
            new ForensicAnalyticsProperties.Observability(
                bool(environment, "observability.logging.enabled", true)
            )
        );
    }

    private static String text(Environment environment, String suffix, String defaultValue) {
        return environment.getProperty(key(suffix), defaultValue);
    }

    private static boolean bool(Environment environment, String suffix, boolean defaultValue) {
        return Boolean.parseBoolean(text(environment, suffix, Boolean.toString(defaultValue)));
    }

    private static int integer(Environment environment, String suffix, int defaultValue) {
        return Integer.parseInt(text(environment, suffix, Integer.toString(defaultValue)));
    }

    private static Path path(Environment environment, String suffix, String defaultValue) {
        var value = text(environment, suffix, defaultValue);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(key(suffix) + " must not be blank");
        }
        return Path.of(value);
    }

    private static Duration duration(Environment environment, String suffix, Duration defaultValue) {
        return Duration.parse(text(environment, suffix, defaultValue.toString()));
    }

    private static String key(String suffix) {
        return "forensics.analytics." + suffix;
    }
}
