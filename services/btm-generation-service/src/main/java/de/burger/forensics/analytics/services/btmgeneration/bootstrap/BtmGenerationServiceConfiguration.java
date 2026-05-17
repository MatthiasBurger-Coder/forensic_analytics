package de.burger.forensics.analytics.services.btmgeneration.bootstrap;

import de.burger.forensics.analytics.services.btmgeneration.adapter.in.grpc.BtmGenerationGrpcEndpoint;
import de.burger.forensics.analytics.services.btmgeneration.adapter.out.filesystem.FileSystemBtmArtifactWriter;
import de.burger.forensics.analytics.services.btmgeneration.application.BtmGenerationApplicationService;
import de.burger.forensics.analytics.services.btmgeneration.application.port.BtmArtifactWriterPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class BtmGenerationServiceConfiguration {
    @Bean
    public BtmArtifactWriterPort btmArtifactWriterPort(BtmGenerationServiceProperties properties) {
        return new FileSystemBtmArtifactWriter(properties.artifacts().root());
    }

    @Bean
    public BtmGenerationApplicationService btmGenerationApplicationService(BtmArtifactWriterPort artifactWriter) {
        return new BtmGenerationApplicationService(artifactWriter);
    }

    @Bean
    public BtmGenerationGrpcEndpoint btmGenerationGrpcEndpoint(BtmGenerationApplicationService applicationService) {
        return new BtmGenerationGrpcEndpoint(applicationService);
    }

    @Bean
    public GrpcServerLifecycle grpcServerLifecycle(
        BtmGenerationServiceProperties properties,
        BtmGenerationGrpcEndpoint endpoint
    ) {
        return new GrpcServerLifecycle(properties, endpoint);
    }

    @Bean
    public HealthHttpServerLifecycle healthHttpServerLifecycle(
        BtmGenerationServiceProperties properties,
        GrpcServerLifecycle grpcServerLifecycle
    ) {
        return new HealthHttpServerLifecycle(properties, grpcServerLifecycle);
    }
}
