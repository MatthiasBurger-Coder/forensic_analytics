package de.burger.forensics.analytics.services.gateway.adapter.in.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.burger.forensics.analytics.services.gateway.application.GatewayStatusService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class GatewayHttpHandler implements HttpHandler {
    private static final String HEALTH = "{\"status\":\"UP\"}";
    private static final String NOT_FOUND = "{\"code\":\"NOT_FOUND\",\"message\":\"Gateway endpoint is not available in this slice\"}";
    private static final String METHOD_NOT_ALLOWED = "{\"code\":\"METHOD_NOT_ALLOWED\",\"message\":\"Gateway shell supports GET only in this slice\"}";

    private final GatewayStatusService statusService;
    private final Gson gson;

    public GatewayHttpHandler(GatewayStatusService statusService) {
        this.statusService = statusService;
        this.gson = new Gson();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!"GET".equals(exchange.getRequestMethod())) {
                write(exchange, 405, METHOD_NOT_ALLOWED);
                return;
            }
            switch (exchange.getRequestURI().getPath()) {
                case "/health", "/api/health" -> write(exchange, 200, HEALTH);
                case "/api/status" -> write(exchange, 200, gson.toJson(statusService.currentStatus()));
                default -> write(exchange, 404, NOT_FOUND);
            }
        } finally {
            exchange.close();
        }
    }

    private static void write(HttpExchange exchange, int statusCode, String body) throws IOException {
        var bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
