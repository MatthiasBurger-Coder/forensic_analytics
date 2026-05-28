package de.burger.forensics.analytics.services.gateway.bootstrap;

import com.sun.net.httpserver.HttpServer;
import de.burger.forensics.analytics.services.gateway.adapter.in.http.GatewayHttpHandler;
import org.springframework.context.SmartLifecycle;

import java.io.IOException;
import java.net.InetSocketAddress;

public final class GatewayHttpServerLifecycle implements SmartLifecycle {
    private final ForensicGatewayServiceProperties properties;
    private final GatewayHttpHandler gatewayHttpHandler;
    private HttpServer server;
    private boolean running;

    public GatewayHttpServerLifecycle(
        ForensicGatewayServiceProperties properties,
        GatewayHttpHandler gatewayHttpHandler
    ) {
        this.properties = properties;
        this.gatewayHttpHandler = gatewayHttpHandler;
    }

    @Override
    public void start() {
        if (!properties.http().enabled() || running) {
            return;
        }
        try {
            server = HttpServer.create(new InetSocketAddress(properties.http().host(), properties.http().port()), 0);
            server.createContext("/", gatewayHttpHandler);
            server.start();
            running = true;
        } catch (IOException error) {
            throw new IllegalStateException("Failed to start forensic gateway HTTP server", error);
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
        return properties.http().enabled();
    }

    public int port() {
        return server == null ? -1 : server.getAddress().getPort();
    }
}
