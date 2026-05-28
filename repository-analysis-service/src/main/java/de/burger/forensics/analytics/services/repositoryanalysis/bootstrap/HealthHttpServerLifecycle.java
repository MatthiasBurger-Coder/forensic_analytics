package de.burger.forensics.analytics.services.repositoryanalysis.bootstrap;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.springframework.context.SmartLifecycle;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public final class HealthHttpServerLifecycle implements SmartLifecycle {
    private static final byte[] UP = "{\"status\":\"UP\"}".getBytes(StandardCharsets.UTF_8);
    private static final byte[] DOWN = "{\"status\":\"DOWN\"}".getBytes(StandardCharsets.UTF_8);

    private final RepositoryAnalysisServiceProperties properties;
    private final GrpcServerLifecycle grpcServerLifecycle;
    private HttpServer server;
    private boolean running;

    public HealthHttpServerLifecycle(
        RepositoryAnalysisServiceProperties properties,
        GrpcServerLifecycle grpcServerLifecycle
    ) {
        this.properties = properties;
        this.grpcServerLifecycle = grpcServerLifecycle;
    }

    @Override
    public void start() {
        if (!properties.health().enabled() || running) {
            return;
        }
        try {
            server = HttpServer.create(new InetSocketAddress(properties.health().host(), properties.health().port()), 0);
            server.createContext("/health", this::health);
            server.start();
            running = true;
        } catch (IOException error) {
            throw new IllegalStateException("Failed to start repository analysis health server", error);
        }
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop(0);
        }
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return properties.health().enabled();
    }

    public int port() {
        return server == null ? -1 : server.getAddress().getPort();
    }

    private void health(HttpExchange exchange) throws IOException {
        var healthy = !properties.grpc().enabled() || grpcServerLifecycle.isRunning();
        var body = healthy ? UP : DOWN;
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(healthy ? 200 : 503, body.length);
        try (var output = exchange.getResponseBody()) {
            output.write(body);
        } finally {
            exchange.close();
        }
    }
}
