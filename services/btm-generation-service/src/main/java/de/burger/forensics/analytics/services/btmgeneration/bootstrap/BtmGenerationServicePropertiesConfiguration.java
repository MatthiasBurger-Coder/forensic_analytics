package de.burger.forensics.analytics.services.btmgeneration.bootstrap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.nio.file.Path;

@Configuration(proxyBeanMethods = false)
public class BtmGenerationServicePropertiesConfiguration {
    @Bean
    public BtmGenerationServiceProperties btmGenerationServiceProperties(Environment environment) {
        return new BtmGenerationServiceProperties(
            new BtmGenerationServiceProperties.Grpc(
                bool(environment, "forensics.btm-generation.service.grpc.enabled", true),
                text(environment, "forensics.btm-generation.service.grpc.host", "127.0.0.1"),
                integer(environment, "forensics.btm-generation.service.grpc.port", 9095)
            ),
            new BtmGenerationServiceProperties.Health(
                bool(environment, "forensics.btm-generation.service.health.enabled", true),
                text(environment, "forensics.btm-generation.service.health.host", "127.0.0.1"),
                integer(environment, "forensics.btm-generation.service.health.port", 8086)
            ),
            new BtmGenerationServiceProperties.Artifacts(
                Path.of(text(environment, "forensics.btm-generation.service.artifacts.root", "build/btm-generation-artifacts"))
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
