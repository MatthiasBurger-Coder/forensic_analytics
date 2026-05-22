package de.burger.forensics.analytics.services.javaparseranalysis.bootstrap;

import de.burger.forensics.analytics.services.javaparseranalysis.adapter.in.grpc.JavaAstAnalysisGrpcEndpoint;
import de.burger.forensics.analytics.services.javaparseranalysis.adapter.out.filesystem.FileSystemAstResultArtifactWriter;
import de.burger.forensics.analytics.services.javaparseranalysis.adapter.out.javaparser.JavaParserSourceScannerAdapter;
import de.burger.forensics.analytics.services.javaparseranalysis.application.JavaAstAnalysisApplicationService;
import de.burger.forensics.analytics.services.javaparseranalysis.application.port.AstResultArtifactReaderPort;
import de.burger.forensics.analytics.services.javaparseranalysis.application.port.AstResultArtifactWriterPort;
import de.burger.forensics.analytics.services.javaparseranalysis.application.port.JavaSourceScannerPort;
import org.springframework.beans.factory.annotation.Qualifier;
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
    public AstResultArtifactReaderPort astResultArtifactReaderPort(JavaAstAnalysisServiceProperties properties) {
        return new FileSystemAstResultArtifactWriter(properties.artifacts().root());
    }

    @Bean
    public JavaAstAnalysisApplicationService javaAstAnalysisApplicationService(
        JavaSourceScannerPort scanner,
        @Qualifier("astResultArtifactWriterPort")
        AstResultArtifactWriterPort artifactWriter
    ) {
        return new JavaAstAnalysisApplicationService(scanner, artifactWriter);
    }

    @Bean
    public JavaAstAnalysisGrpcEndpoint javaAstAnalysisGrpcEndpoint(
        JavaAstAnalysisApplicationService applicationService,
        @Qualifier("astResultArtifactReaderPort")
        AstResultArtifactReaderPort artifactReader
    ) {
        return new JavaAstAnalysisGrpcEndpoint(applicationService, artifactReader);
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
