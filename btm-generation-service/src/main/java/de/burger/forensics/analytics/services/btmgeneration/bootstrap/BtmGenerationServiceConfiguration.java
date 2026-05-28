package de.burger.forensics.analytics.services.btmgeneration.bootstrap;

import de.burger.forensics.analytics.services.btmgeneration.adapter.in.grpc.BtmArtifactDeliveryGrpcEndpoint;
import de.burger.forensics.analytics.services.btmgeneration.adapter.in.grpc.BtmGenerationGrpcEndpoint;
import de.burger.forensics.analytics.services.btmgeneration.adapter.out.filesystem.FileSystemBtmArtifactReader;
import de.burger.forensics.analytics.services.btmgeneration.adapter.out.filesystem.FileSystemBtmArtifactWriter;
import de.burger.forensics.analytics.services.btmgeneration.application.BtmArtifactDeliveryApplicationService;
import de.burger.forensics.analytics.services.btmgeneration.application.BtmGenerationApplicationService;
import de.burger.forensics.analytics.services.btmgeneration.application.port.BtmArtifactReaderPort;
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
    public BtmArtifactReaderPort btmArtifactReaderPort(BtmGenerationServiceProperties properties) {
        return new FileSystemBtmArtifactReader(properties.artifacts().root());
    }

    @Bean
    public BtmGenerationApplicationService btmGenerationApplicationService(BtmArtifactWriterPort artifactWriter) {
        return new BtmGenerationApplicationService(artifactWriter);
    }

    @Bean
    public BtmArtifactDeliveryApplicationService btmArtifactDeliveryApplicationService(BtmArtifactReaderPort artifactReader) {
        return new BtmArtifactDeliveryApplicationService(artifactReader);
    }

    @Bean
    public BtmGenerationGrpcEndpoint btmGenerationGrpcEndpoint(BtmGenerationApplicationService applicationService) {
        return new BtmGenerationGrpcEndpoint(applicationService);
    }

    @Bean
    public BtmArtifactDeliveryGrpcEndpoint btmArtifactDeliveryGrpcEndpoint(
        BtmArtifactDeliveryApplicationService applicationService
    ) {
        return new BtmArtifactDeliveryGrpcEndpoint(applicationService);
    }

    @Bean
    public GrpcServerLifecycle grpcServerLifecycle(
        BtmGenerationServiceProperties properties,
        BtmGenerationGrpcEndpoint generationEndpoint,
        BtmArtifactDeliveryGrpcEndpoint deliveryEndpoint
    ) {
        return new GrpcServerLifecycle(properties, generationEndpoint, deliveryEndpoint);
    }

    @Bean
    public HealthHttpServerLifecycle healthHttpServerLifecycle(
        BtmGenerationServiceProperties properties,
        GrpcServerLifecycle grpcServerLifecycle
    ) {
        return new HealthHttpServerLifecycle(properties, grpcServerLifecycle);
    }
}
