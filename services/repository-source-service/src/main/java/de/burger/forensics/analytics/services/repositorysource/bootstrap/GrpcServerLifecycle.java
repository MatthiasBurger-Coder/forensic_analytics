package de.burger.forensics.analytics.services.repositorysource.bootstrap;

import de.burger.forensics.analytics.services.repositorysource.adapter.in.grpc.RepositorySourceGrpcEndpoint;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.springframework.context.SmartLifecycle;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public final class GrpcServerLifecycle implements SmartLifecycle {
    private final RepositorySourceServiceProperties properties;
    private final RepositorySourceGrpcEndpoint endpoint;
    private Server server;
    private boolean running;

    public GrpcServerLifecycle(RepositorySourceServiceProperties properties, RepositorySourceGrpcEndpoint endpoint) {
        this.properties = properties;
        this.endpoint = endpoint;
    }

    @Override
    public void start() {
        if (!properties.grpc().enabled() || running) {
            return;
        }
        try {
            server = NettyServerBuilder.forAddress(new InetSocketAddress(properties.grpc().host(), properties.grpc().port()))
                .addService(endpoint)
                .build()
                .start();
            running = true;
        } catch (IOException error) {
            throw new IllegalStateException("Failed to start repository source gRPC server", error);
        }
    }

    @Override
    public void stop() {
        if (server != null) {
            server.shutdown();
            try {
                if (!server.awaitTermination(10, TimeUnit.SECONDS)) {
                    server.shutdownNow();
                }
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                server.shutdownNow();
            }
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
