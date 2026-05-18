package de.burger.forensics.analytics.services.gateway.adapter.in.http;

import com.sun.net.httpserver.HttpServer;
import de.burger.forensics.analytics.services.gateway.application.GatewayStatusService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForensicGatewayHttpAdapterTest {
    @Test
    void exposesGatewayShellRoutesOnly() throws Exception {
        var server = server();
        try {
            var port = server.getAddress().getPort();

            assertEquals(new Response(200, "{\"status\":\"UP\"}"), response(port, "/health", "GET"));
            assertEquals(new Response(200, "{\"status\":\"UP\"}"), response(port, "/api/health", "GET"));

            var status = response(port, "/api/status", "GET");
            assertEquals(200, status.code());
            assertTrue(status.body().contains("\"status\":\"UP\""));
            assertTrue(status.body().contains("\"services\":[]"));

            var notFound = response(port, "/api/repository-analyses", "GET");
            assertEquals(404, notFound.code());
            assertTrue(notFound.body().contains("NOT_FOUND"));

            var methodNotAllowed = response(port, "/api/status", "POST");
            assertEquals(405, methodNotAllowed.code());
            assertTrue(methodNotAllowed.body().contains("METHOD_NOT_ALLOWED"));
        } finally {
            server.stop(0);
        }
    }

    private static HttpServer server() throws IOException {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", new GatewayHttpHandler(new GatewayStatusService()));
        server.start();
        return server;
    }

    private static Response response(int port, String path, String method) throws IOException {
        var connection = (HttpURLConnection) URI.create("http://127.0.0.1:" + port + path).toURL().openConnection();
        connection.setRequestMethod(method);
        var stream = connection.getResponseCode() >= 400 ? connection.getErrorStream() : connection.getInputStream();
        try (stream) {
            return new Response(connection.getResponseCode(), new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private record Response(int code, String body) {
    }
}
