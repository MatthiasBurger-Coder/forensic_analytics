package de.burger.forensics.analytics.services.analysisstore.bootstrap;

import de.burger.forensics.analytics.services.analysisstore.adapter.in.grpc.AnalysisJobGrpcEndpoint;
import de.burger.forensics.analytics.services.analysisstore.adapter.out.grpc.BtmGenerationGrpcClient;
import de.burger.forensics.analytics.services.analysisstore.adapter.out.grpc.JavaAstSourceFactArtifactClient;
import de.burger.forensics.analytics.services.analysisstore.adapter.out.grpc.JoernCpgAnalysisGrpcClient;
import de.burger.forensics.analytics.services.analysisstore.adapter.out.grpc.RepositoryAnalysisGrpcClient;
import de.burger.forensics.analytics.services.analysisstore.adapter.out.memory.InMemoryAnalysisJobRepository;
import de.burger.forensics.analytics.services.analysisstore.application.AnalysisJobApplicationService;
import de.burger.forensics.analytics.services.analysisstore.application.InstrumentationTargetPlanningApplicationService;
import de.burger.forensics.analytics.services.analysisstore.application.RepositoryToBtmOrchestrationApplicationService;
import de.burger.forensics.analytics.services.analysisstore.application.port.AnalysisJobRepository;
import de.burger.forensics.analytics.services.analysisstore.application.port.BtmGenerationWorkerPort;
import de.burger.forensics.analytics.services.analysisstore.application.port.JoernSemanticAnalysisPort;
import de.burger.forensics.analytics.services.analysisstore.application.port.RepositoryAnalysisWorkerPort;
import de.burger.forensics.analytics.services.analysisstore.application.port.SourceFactArtifactByteVerifierPort;
import de.burger.forensics.analytics.services.analysisstore.application.port.SourceFactArtifactReaderPort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class AnalysisStoreServiceConfiguration {
    @Bean
    public Clock analysisStoreClock() {
        return Clock.systemUTC();
    }

    @Bean
    public AnalysisJobRepository analysisJobRepository() {
        return new InMemoryAnalysisJobRepository();
    }

    @Bean
    public AnalysisJobApplicationService analysisJobApplicationService(
        AnalysisJobRepository analysisJobRepository,
        Clock analysisStoreClock,
        @Qualifier("sourceFactArtifactByteVerifierPort")
        SourceFactArtifactByteVerifierPort sourceFactArtifactByteVerifier
    ) {
        return new AnalysisJobApplicationService(analysisJobRepository, analysisStoreClock, sourceFactArtifactByteVerifier);
    }

    @Bean
    public JavaAstSourceFactArtifactClient javaAstSourceFactArtifactClient(AnalysisStoreServiceProperties properties) {
        var grpc = properties.javaAstAnalysis().grpc();
        return new JavaAstSourceFactArtifactClient(grpc.host(), grpc.port(), grpc.deadlineSeconds(), grpc.maxBytes());
    }

    @Bean
    public SourceFactArtifactByteVerifierPort sourceFactArtifactByteVerifierPort(
        JavaAstSourceFactArtifactClient javaAstSourceFactArtifactClient
    ) {
        return javaAstSourceFactArtifactClient;
    }

    @Bean
    public SourceFactArtifactReaderPort sourceFactArtifactReaderPort(
        JavaAstSourceFactArtifactClient javaAstSourceFactArtifactClient
    ) {
        return javaAstSourceFactArtifactClient;
    }

    @Bean
    public RepositoryAnalysisWorkerPort repositoryAnalysisWorkerPort(AnalysisStoreServiceProperties properties) {
        var grpc = properties.repositoryAnalysis().grpc();
        return new RepositoryAnalysisGrpcClient(grpc.host(), grpc.port(), grpc.deadlineSeconds());
    }

    @Bean
    public JoernSemanticAnalysisPort joernSemanticAnalysisPort(AnalysisStoreServiceProperties properties) {
        var grpc = properties.joernCpgAnalysis().grpc();
        return new JoernCpgAnalysisGrpcClient(
            grpc.host(),
            grpc.port(),
            grpc.deadlineSeconds(),
            grpc.joernImageReference(),
            grpc.queryBundleVersion()
        );
    }

    @Bean
    public BtmGenerationWorkerPort btmGenerationWorkerPort(AnalysisStoreServiceProperties properties) {
        var grpc = properties.btmGeneration().grpc();
        return new BtmGenerationGrpcClient(grpc.host(), grpc.port(), grpc.deadlineSeconds(), grpc.maxArtifactBytes());
    }

    @Bean
    public InstrumentationTargetPlanningApplicationService instrumentationTargetPlanningApplicationService(
        AnalysisJobApplicationService analysisJobApplicationService
    ) {
        return new InstrumentationTargetPlanningApplicationService(analysisJobApplicationService);
    }

    @Bean
    public RepositoryToBtmOrchestrationApplicationService repositoryToBtmOrchestrationApplicationService(
        AnalysisJobApplicationService analysisJobApplicationService,
        InstrumentationTargetPlanningApplicationService instrumentationTargetPlanningApplicationService,
        RepositoryAnalysisWorkerPort repositoryAnalysisWorkerPort,
        @Qualifier("sourceFactArtifactReaderPort")
        SourceFactArtifactReaderPort sourceFactArtifactReaderPort,
        JoernSemanticAnalysisPort joernSemanticAnalysisPort,
        BtmGenerationWorkerPort btmGenerationWorkerPort
    ) {
        return new RepositoryToBtmOrchestrationApplicationService(
            analysisJobApplicationService,
            instrumentationTargetPlanningApplicationService,
            repositoryAnalysisWorkerPort,
            sourceFactArtifactReaderPort,
            joernSemanticAnalysisPort,
            btmGenerationWorkerPort
        );
    }

    @Bean
    public AnalysisJobGrpcEndpoint analysisJobGrpcEndpoint(
        AnalysisJobApplicationService applicationService,
        InstrumentationTargetPlanningApplicationService targetPlanningService,
        RepositoryToBtmOrchestrationApplicationService orchestrationService
    ) {
        return new AnalysisJobGrpcEndpoint(applicationService, targetPlanningService, orchestrationService);
    }

    @Bean
    public GrpcServerLifecycle grpcServerLifecycle(
        AnalysisStoreServiceProperties properties,
        AnalysisJobGrpcEndpoint endpoint
    ) {
        return new GrpcServerLifecycle(properties, endpoint);
    }

    @Bean
    public HealthHttpServerLifecycle healthHttpServerLifecycle(
        AnalysisStoreServiceProperties properties,
        GrpcServerLifecycle grpcServerLifecycle
    ) {
        return new HealthHttpServerLifecycle(properties, grpcServerLifecycle);
    }
}
