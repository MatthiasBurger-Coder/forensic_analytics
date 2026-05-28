package de.burger.forensics.analytics.services.joerncpganalysis.bootstrap;

import de.burger.forensics.analytics.services.joerncpganalysis.adapter.in.grpc.JoernCpgAnalysisGrpcEndpoint;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.springframework.context.SmartLifecycle;

import java.io.IOException;
import java.net.InetSocketAddress;

public final class GrpcServerLifecycle implements SmartLifecycle {
    private final JoernCpgAnalysisServiceProperties properties;
    private final JoernCpgAnalysisGrpcEndpoint endpoint;
    private Server server;
    private boolean running;

    public GrpcServerLifecycle(
        JoernCpgAnalysisServiceProperties properties,
        JoernCpgAnalysisGrpcEndpoint endpoint
    ) {
        this.properties = properties;
        this.endpoint = endpoint;
    }

    @Override
    public void start() {
        if (!properties.grpc().enabled() || running) {
            return;
        }
        try {
            server = NettyServerBuilder
                .forAddress(new InetSocketAddress(properties.grpc().host(), properties.grpc().port()))
                .addService(endpoint)
                .build()
                .start();
            running = true;
        } catch (IOException error) {
            throw new IllegalStateException("Failed to start Joern CPG analysis gRPC server", error);
        }
    }

    @Override
    public void stop() {
        if (server != null) {
            server.shutdownNow();
        }
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return properties.grpc().enabled();
    }

    public int port() {
        return server == null ? -1 : server.getPort();
    }
}
