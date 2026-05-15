package de.burger.forensics.analytics.boot.config;

import de.burger.forensics.analytics.adapter.joern.docker.JoernDockerImage;
import de.burger.forensics.analytics.adapter.joern.docker.JoernDockerSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class JoernDockerConfiguration {
    private static final String DOCKER_EXECUTABLE = "docker";

    @Bean
    public JoernDockerSettings joernDockerSettings(ForensicAnalyticsProperties properties) {
        var joern = properties.joern();
        return new JoernDockerSettings(
            DOCKER_EXECUTABLE,
            new JoernDockerImage(joern.containerImage()),
            joern.outputDirectory(),
            joern.timeout(),
            joern.failOnError()
        );
    }
}
