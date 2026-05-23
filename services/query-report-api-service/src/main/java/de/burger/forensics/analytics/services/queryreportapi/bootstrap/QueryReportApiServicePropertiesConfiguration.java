package de.burger.forensics.analytics.services.queryreportapi.bootstrap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
public class QueryReportApiServicePropertiesConfiguration {
    @Bean
    public QueryReportApiServiceProperties queryReportApiServiceProperties(Environment environment) {
        return new QueryReportApiServiceProperties(
            new QueryReportApiServiceProperties.Http(
                bool(environment, "forensics.query-report-api.service.http.enabled", true),
                text(environment, "forensics.query-report-api.service.http.host", "127.0.0.1"),
                integer(environment, "forensics.query-report-api.service.http.port", 8080)
            ),
            new QueryReportApiServiceProperties.AnalysisOrchestrator(
                new QueryReportApiServiceProperties.Grpc(
                    text(environment, "forensics.query-report-api.service.analysis-orchestrator.grpc.host", "127.0.0.1"),
                    integer(environment, "forensics.query-report-api.service.analysis-orchestrator.grpc.port", 9098),
                    integer(environment, "forensics.query-report-api.service.analysis-orchestrator.grpc.deadline-seconds", 5)
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
