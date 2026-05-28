package de.burger.forensics.analytics.services.javaastanalysis.bootstrap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.nio.file.Path;

@Configuration(proxyBeanMethods = false)
public class JavaAstAnalysisServicePropertiesConfiguration {
    @Bean
    public JavaAstAnalysisServiceProperties javaAstAnalysisServiceProperties(Environment environment) {
        return new JavaAstAnalysisServiceProperties(
            new JavaAstAnalysisServiceProperties.Grpc(
                bool(environment, "forensics.java-ast-analysis.service.grpc.enabled", true),
                text(environment, "forensics.java-ast-analysis.service.grpc.host", "127.0.0.1"),
                integer(environment, "forensics.java-ast-analysis.service.grpc.port", 9093)
            ),
            new JavaAstAnalysisServiceProperties.Health(
                bool(environment, "forensics.java-ast-analysis.service.health.enabled", true),
                text(environment, "forensics.java-ast-analysis.service.health.host", "127.0.0.1"),
                integer(environment, "forensics.java-ast-analysis.service.health.port", 8084)
            ),
            new JavaAstAnalysisServiceProperties.Artifacts(
                Path.of(text(environment, "forensics.java-ast-analysis.service.artifacts.root", "build/java-ast-analysis-artifacts"))
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
