package de.burger.forensics.analytics.services.joerncpganalysis.bootstrap;

import de.burger.forensics.analytics.services.joerncpganalysis.adapter.in.grpc.JoernCpgAnalysisGrpcEndpoint;
import de.burger.forensics.analytics.services.joerncpganalysis.adapter.out.filesystem.FileSystemJoernArtifactCollector;
import de.burger.forensics.analytics.services.joerncpganalysis.adapter.out.filesystem.FileSystemJoernWorkspaceAdapter;
import de.burger.forensics.analytics.services.joerncpganalysis.adapter.out.joern.ProcessJoernRuntimeAdapter;
import de.burger.forensics.analytics.services.joerncpganalysis.application.JoernCpgAnalysisApplicationService;
import de.burger.forensics.analytics.services.joerncpganalysis.application.port.JoernArtifactCollectorPort;
import de.burger.forensics.analytics.services.joerncpganalysis.application.port.JoernRuntimePort;
import de.burger.forensics.analytics.services.joerncpganalysis.application.port.JoernWorkspacePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class JoernCpgAnalysisServiceConfiguration {
    @Bean
    public JoernWorkspacePort joernWorkspacePort(JoernCpgAnalysisServiceProperties properties) {
        return new FileSystemJoernWorkspaceAdapter(properties.workspace().root());
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
    public JoernCpgAnalysisApplicationService joernCpgAnalysisApplicationService(
        JoernWorkspacePort workspacePort,
        JoernRuntimePort runtimePort,
        JoernArtifactCollectorPort artifactCollector
    ) {
        return new JoernCpgAnalysisApplicationService(workspacePort, runtimePort, artifactCollector);
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
