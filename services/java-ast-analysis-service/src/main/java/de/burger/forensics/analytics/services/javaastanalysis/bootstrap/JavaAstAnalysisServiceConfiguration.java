package de.burger.forensics.analytics.services.javaastanalysis.bootstrap;

import de.burger.forensics.analytics.services.javaastanalysis.adapter.in.grpc.JavaAstAnalysisGrpcEndpoint;
import de.burger.forensics.analytics.services.javaastanalysis.adapter.out.filesystem.FileSystemAstResultArtifactWriter;
import de.burger.forensics.analytics.services.javaastanalysis.adapter.out.javaparser.JavaParserSourceScannerAdapter;
import de.burger.forensics.analytics.services.javaastanalysis.application.JavaAstAnalysisApplicationService;
import de.burger.forensics.analytics.services.javaastanalysis.application.port.AstResultArtifactWriterPort;
import de.burger.forensics.analytics.services.javaastanalysis.application.port.JavaSourceScannerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class JavaAstAnalysisServiceConfiguration {
    @Bean
    public JavaSourceScannerPort javaSourceScannerPort() {
        return new JavaParserSourceScannerAdapter();
    }

    @Bean
    public AstResultArtifactWriterPort astResultArtifactWriterPort(JavaAstAnalysisServiceProperties properties) {
        return new FileSystemAstResultArtifactWriter(properties.artifacts().root());
    }

    @Bean
    public JavaAstAnalysisApplicationService javaAstAnalysisApplicationService(
        JavaSourceScannerPort scanner,
        AstResultArtifactWriterPort artifactWriter
    ) {
        return new JavaAstAnalysisApplicationService(scanner, artifactWriter);
    }

    @Bean
    public JavaAstAnalysisGrpcEndpoint javaAstAnalysisGrpcEndpoint(JavaAstAnalysisApplicationService applicationService) {
        return new JavaAstAnalysisGrpcEndpoint(applicationService);
    }

    @Bean
    public GrpcServerLifecycle grpcServerLifecycle(
        JavaAstAnalysisServiceProperties properties,
        JavaAstAnalysisGrpcEndpoint endpoint
    ) {
        return new GrpcServerLifecycle(properties, endpoint);
    }

    @Bean
    public HealthHttpServerLifecycle healthHttpServerLifecycle(
        JavaAstAnalysisServiceProperties properties,
        GrpcServerLifecycle grpcServerLifecycle
    ) {
        return new HealthHttpServerLifecycle(properties, grpcServerLifecycle);
    }
}
