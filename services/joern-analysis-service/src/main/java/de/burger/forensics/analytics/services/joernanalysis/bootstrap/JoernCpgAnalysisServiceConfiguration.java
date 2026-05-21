package de.burger.forensics.analytics.services.joernanalysis.bootstrap;

import de.burger.forensics.analytics.services.joernanalysis.adapter.in.grpc.JoernCpgAnalysisGrpcEndpoint;
import de.burger.forensics.analytics.services.joernanalysis.adapter.out.filesystem.FileSystemJoernArtifactCollector;
import de.burger.forensics.analytics.services.joernanalysis.adapter.out.filesystem.FileSystemJoernWorkspaceAdapter;
import de.burger.forensics.analytics.services.joernanalysis.adapter.out.filesystem.FileSystemJoernWorkspaceMaterializer;
import de.burger.forensics.analytics.services.joernanalysis.adapter.out.grpc.AnalysisStoreArtifactRegistryGrpcClient;
import de.burger.forensics.analytics.services.joernanalysis.adapter.out.joern.ProcessJoernRuntimeAdapter;
import de.burger.forensics.analytics.services.joernanalysis.application.JoernCpgAnalysisApplicationService;
import de.burger.forensics.analytics.services.joernanalysis.application.port.AnalysisStoreArtifactRegistryPort;
import de.burger.forensics.analytics.services.joernanalysis.application.port.JoernArtifactCollectorPort;
import de.burger.forensics.analytics.services.joernanalysis.application.port.JoernRuntimePort;
import de.burger.forensics.analytics.services.joernanalysis.application.port.JoernWorkspaceMaterializerPort;
import de.burger.forensics.analytics.services.joernanalysis.application.port.JoernWorkspacePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class JoernCpgAnalysisServiceConfiguration {
    @Bean
    public JoernWorkspacePort joernWorkspacePort(JoernCpgAnalysisServiceProperties properties) {
        return new FileSystemJoernWorkspaceAdapter(properties.workspace().root());
    }

    @Bean
    public JoernWorkspaceMaterializerPort joernWorkspaceMaterializerPort(JoernCpgAnalysisServiceProperties properties) {
        return new FileSystemJoernWorkspaceMaterializer(
            properties.workspace().root(),
            properties.workspace().root().resolve("package-cache")
        );
    }

    @Bean
    public JoernRuntimePort joernRuntimePort(JoernCpgAnalysisServiceProperties properties) {
        return new ProcessJoernRuntimeAdapter(
            properties.artifacts().root(),
            properties.joern().queryScriptsRoot(),
            properties.joern().executable(),
            properties.joern().parseExecutable(),
            properties.joern().heap(),
            properties.joern().runtimeImageReference()
        );
    }

    @Bean
    public JoernArtifactCollectorPort joernArtifactCollectorPort(JoernCpgAnalysisServiceProperties properties) {
        return new FileSystemJoernArtifactCollector(properties.artifacts().root());
    }

    @Bean
    public AnalysisStoreArtifactRegistryPort analysisStoreArtifactRegistryPort(JoernCpgAnalysisServiceProperties properties) {
        return new AnalysisStoreArtifactRegistryGrpcClient(
            properties.analysisStore().host(),
            properties.analysisStore().port(),
            properties.analysisStore().deadlineSeconds()
        );
    }

    @Bean
    public JoernCpgAnalysisApplicationService joernCpgAnalysisApplicationService(
        JoernWorkspaceMaterializerPort materializerPort,
        JoernWorkspacePort workspacePort,
        JoernRuntimePort runtimePort,
        JoernArtifactCollectorPort artifactCollector,
        AnalysisStoreArtifactRegistryPort artifactRegistryPort
    ) {
        return new JoernCpgAnalysisApplicationService(
            materializerPort,
            workspacePort,
            runtimePort,
            artifactCollector,
            artifactRegistryPort
        );
    }

    @Bean
    public JoernCpgAnalysisGrpcEndpoint joernCpgAnalysisGrpcEndpoint(JoernCpgAnalysisApplicationService applicationService) {
        return new JoernCpgAnalysisGrpcEndpoint(applicationService);
    }

    @Bean
    public GrpcServerLifecycle grpcServerLifecycle(
        JoernCpgAnalysisServiceProperties properties,
        JoernCpgAnalysisGrpcEndpoint endpoint
    ) {
        return new GrpcServerLifecycle(properties, endpoint);
    }

    @Bean
    public HealthHttpServerLifecycle healthHttpServerLifecycle(
        JoernCpgAnalysisServiceProperties properties,
        GrpcServerLifecycle grpcServerLifecycle
    ) {
        return new HealthHttpServerLifecycle(properties, grpcServerLifecycle);
    }
}
