package de.burger.forensics.analytics.services.cliclient.adapter.out.http;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.burger.forensics.analytics.services.cliclient.domain.CliClientSubmissionCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpRepositoryAnalysisSubmissionClientTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendsRepositoryAnalysisSubmissionThroughPublicHttpContract() throws Exception {
        var observed = new AtomicReference<ObservedRequest>();
        startServer(exchange -> {
            observed.set(observe(exchange));
            respond(exchange, 202, """
                {
                  "analysisRunId": "analysis-run-1",
                  "status": "ACCEPTED",
                  "statusUrl": "/repository-analyses/analysis-run-1",
                  "jobsUrl": "/repository-analyses/analysis-run-1/jobs",
                  "btmDeliveryStatus": "BTM_DELIVERY_NOT_READY",
                  "btmDeliveryService": "BtmArtifactDeliveryService",
                  "correlationId": "correlation-1",
                  "diagnostics": [
                    {"code": "QUEUED", "message": "accepted"}
                  ]
                }
                """);
        });
        var client = new HttpRepositoryAnalysisSubmissionClient(HttpClient.newHttpClient(), new Gson());

        var result = client.submit(command(baseUrl()));

        assertEquals("analysis-run-1", result.analysisRunId());
        assertEquals("ACCEPTED", result.status());
        assertEquals("BTM_DELIVERY_NOT_READY", result.btmDeliveryStatus());
        assertEquals(1, result.diagnosticCount());
        assertEquals("POST", observed.get().method());
        assertEquals("/api/repository-analyses", observed.get().path());
        assertEquals("application/json", observed.get().accept());
        assertEquals("application/json; charset=utf-8", observed.get().contentType());
        assertEquals("correlation-1", observed.get().correlationId());
        assertEquals("idem-1", observed.get().idempotencyKey());
        var json = JsonParser.parseString(observed.get().body()).getAsJsonObject();
        assertEquals("request-1", json.get("requestId").getAsString());
        assertEquals("gateway.v1", json.get("schemaVersion").getAsString());
        assertEquals("https://example.com/acme/demo.git", json.get("repositoryUrl").getAsString());
        assertEquals("github", json.get("provider").getAsString());
        assertEquals("main", json.get("branch").getAsString());
        assertEquals("", json.get("commit").getAsString());
        assertEquals("BTM_RULES", json.getAsJsonArray("requestedOutputs").get(0).getAsString());
        var buildContext = json.getAsJsonObject("buildContext");
        assertEquals("gradle", buildContext.get("buildTool").getAsString());
        assertEquals("build-1", buildContext.get("buildId").getAsString());
        assertEquals("demo", buildContext.get("rootProjectName").getAsString());
        assertEquals(":app", buildContext.getAsJsonArray("declaredModules").get(0).getAsString());
        assertEquals(0, buildContext.getAsJsonObject("attributes").size());
        var workspacePolicy = json.getAsJsonObject("workspacePolicy");
        assertFalse(workspacePolicy.get("ephemeral").getAsBoolean());
        assertTrue(workspacePolicy.get("allowShallowClone").getAsBoolean());
        assertFalse(workspacePolicy.get("allowPartialClone").getAsBoolean());
        assertFalse(workspacePolicy.get("allowSparseCheckout").getAsBoolean());
        assertEquals(60L, workspacePolicy.get("timeoutSeconds").getAsLong());
        assertEquals(100_000L, workspacePolicy.get("maxWorkspaceBytes").getAsLong());
    }

    @Test
    void mapsErrorEnvelopeWithoutLeakingPrivateDetails() throws Exception {
        startServer(exchange -> respond(exchange, 409, """
            {
              "code": "CONFLICT",
              "message": "private /tmp/workspace token SECRET should not be printed",
              "retryable": false,
              "correlationId": "correlation-1"
            }
            """));
        var client = new HttpRepositoryAnalysisSubmissionClient(HttpClient.newHttpClient(), new Gson());

        var error = assertThrows(PublicApiClientException.class, () -> client.submit(command(baseUrl())));

        assertTrue(error.getMessage().contains("status=409"));
        assertTrue(error.getMessage().contains("code=CONFLICT"));
        assertTrue(error.getMessage().contains("retryable=false"));
        assertTrue(error.getMessage().contains("correlationId=correlation-1"));
        assertFalse(error.getMessage().contains("/tmp"));
        assertFalse(error.getMessage().contains("SECRET"));
    }

    @Test
    void rejectsInvalidSuccessJson() throws Exception {
        startServer(exchange -> respond(exchange, 202, "{not-json"));
        var client = new HttpRepositoryAnalysisSubmissionClient(HttpClient.newHttpClient(), new Gson());

        var error = assertThrows(PublicApiClientException.class, () -> client.submit(command(baseUrl())));

        assertEquals("Public API returned invalid JSON", error.getMessage());
    }

    @Test
    void rejectsSuccessResponseMissingPublicField() throws Exception {
        startServer(exchange -> respond(exchange, 202, """
            {
              "analysisRunId": "analysis-run-1",
              "status": "ACCEPTED"
            }
            """));
        var client = new HttpRepositoryAnalysisSubmissionClient(HttpClient.newHttpClient(), new Gson());

        var error = assertThrows(PublicApiClientException.class, () -> client.submit(command(baseUrl())));

        assertEquals("Public API response is missing public field: statusUrl", error.getMessage());
    }

    private URI baseUrl() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api");
    }

    private void startServer(ExchangeHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/repository-analyses", exchange -> {
            try {
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    private static ObservedRequest observe(HttpExchange exchange) throws IOException {
        return new ObservedRequest(
            exchange.getRequestMethod(),
            exchange.getRequestURI().getPath(),
            exchange.getRequestHeaders().getFirst("Accept"),
            exchange.getRequestHeaders().getFirst("Content-Type"),
            exchange.getRequestHeaders().getFirst("X-Correlation-Id"),
            exchange.getRequestHeaders().getFirst("Idempotency-Key"),
            new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)
        );
    }

    private static void respond(HttpExchange exchange, int statusCode, String body) throws IOException {
        var payload = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, payload.length);
        exchange.getResponseBody().write(payload);
    }

    private static CliClientSubmissionCommand command(URI baseUrl) {
        return new CliClientSubmissionCommand(
            baseUrl,
            "https://example.com/acme/demo.git",
            "main",
            "",
            "request-1",
            "gateway.v1",
            List.of("BTM_RULES"),
            "github",
            "gradle",
            "build-1",
            "demo",
            List.of(":app", ":lib"),
            "correlation-1",
            "idem-1",
            60L,
            100_000L,
            true
        );
    }

    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }

    private record ObservedRequest(
        String method,
        String path,
        String accept,
        String contentType,
        String correlationId,
        String idempotencyKey,
        String body
    ) {
    }
}
