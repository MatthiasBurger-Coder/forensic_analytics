package de.burger.forensics.analytics.boot.grpc;

import de.burger.forensics.analytics.boot.config.ForensicAnalyticsProperties;
import de.burger.forensics.analytics.ingestion.grpc.ForensicIngestionGrpcService;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.springframework.context.SmartLifecycle;

import java.io.IOException;
import java.net.InetSocketAddress;

public final class GrpcServerLifecycle implements SmartLifecycle {
    private final ForensicAnalyticsProperties properties;
    private final ForensicIngestionGrpcService service;
    private Server server;
    private boolean running;

    public GrpcServerLifecycle(
        ForensicAnalyticsProperties properties,
        ForensicIngestionGrpcService service
    ) {
        this.properties = properties;
        this.service = service;
    }

    @Override
    public void start() {
        if (!properties.grpc().enabled() || running) {
            return;
        }
        try {
            server = NettyServerBuilder.forAddress(
                    new InetSocketAddress(properties.grpc().host(), properties.grpc().port())
                )
                .addService(service)
                .build()
                .start();
            running = true;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to start gRPC ingestion server", exception);
        }
    }

    @Override
    public void stop() {
        if (server != null) {
            server.shutdown();
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
