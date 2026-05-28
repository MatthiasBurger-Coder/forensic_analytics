package de.burger.forensics.analytics.services.queryreportapi.bootstrap;

import com.sun.net.httpserver.HttpServer;
import de.burger.forensics.analytics.services.queryreportapi.adapter.in.http.QueryReportApiHttpHandler;
import org.springframework.context.SmartLifecycle;

import java.io.IOException;
import java.net.InetSocketAddress;

public final class QueryReportApiHttpServerLifecycle implements SmartLifecycle {
    private final QueryReportApiServiceProperties properties;
    private final QueryReportApiHttpHandler queryReportApiHttpHandler;
    private HttpServer server;
    private boolean running;

    public QueryReportApiHttpServerLifecycle(
        QueryReportApiServiceProperties properties,
        QueryReportApiHttpHandler queryReportApiHttpHandler
    ) {
        this.properties = properties;
        this.queryReportApiHttpHandler = queryReportApiHttpHandler;
    }

    @Override
    public void start() {
        if (!properties.http().enabled() || running) {
            return;
        }
        try {
            server = HttpServer.create(new InetSocketAddress(properties.http().host(), properties.http().port()), 0);
            server.createContext("/", queryReportApiHttpHandler);
            server.start();
            running = true;
        } catch (IOException error) {
            throw new IllegalStateException("Failed to start query report API HTTP server", error);
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
