package de.burger.forensics.analytics.services.gateway.adapter.in.http;

import com.sun.net.httpserver.HttpServer;
import de.burger.forensics.analytics.services.gateway.application.GatewayRepositoryAnalysisException;
import de.burger.forensics.analytics.services.gateway.application.GatewayRepositoryAnalysisSubmissionService;
import de.burger.forensics.analytics.services.gateway.application.GatewayStatusService;
import de.burger.forensics.analytics.services.gateway.application.port.RepositoryAnalysisPreparationPort;
import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.Diagnostic;
import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.RepositoryPreparationCommand;
import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.RepositoryPreparationResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForensicGatewayHttpAdapterTest {
    @Test
    void exposesGatewayShellRoutesOnly() throws Exception {
        var server = server();
        try {
            var port = server.getAddress().getPort();

            assertEquals(new Response(200, "{\"status\":\"UP\"}", null), response(port, "/health", "GET"));
            assertEquals(new Response(200, "{\"status\":\"UP\"}", null), response(port, "/api/health", "GET"));

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

    @Test
    void acceptsRepositoryAnalysisSubmissionThroughGatewayFacade() throws Exception {
        var server = server();
        try {
            var port = server.getAddress().getPort();

            var accepted = response(port, "/api/repository-analyses", "POST", validRequest(), "correlation-1", "idem-1");

            assertEquals(202, accepted.code());
            assertEquals("correlation-1", accepted.correlationId());
            assertTrue(accepted.body().contains("\"status\":\"ACCEPTED\""));
            assertTrue(accepted.body().contains("\"btmDeliveryStatus\":\"BTM_DELIVERY_NOT_READY\""));
            assertTrue(accepted.body().contains("\"statusUrl\":\"/repository-analyses/analysis-run-"));
            assertTrue(accepted.body().contains("\"jobsUrl\":\"/repository-analyses/analysis-run-"));
            assertFalse(accepted.body().contains("workspace-"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsMissingHeadersUnsafeRemotesAndConflictingIdempotency() throws Exception {
        var server = server();
        try {
            var port = server.getAddress().getPort();

            assertEquals(400, response(port, "/api/repository-analyses", "POST", validRequest(), null, "idem-1").code());
            assertEquals(400, response(port, "/api/repository-analyses", "POST", validRequest(), "correlation-1", null).code());
            assertEquals(400, response(
                port,
                "/api/repository-analyses",
                "POST",
                validRequest().replace("https://example.com/acme/demo.git", "https://127.0.0.1/acme/demo.git"),
                "correlation-1",
                "idem-2"
            ).code());
            assertEquals(202, response(port, "/api/repository-analyses", "POST", validRequest(), "correlation-1", "idem-3").code());
            var conflict = response(
                port,
                "/api/repository-analyses",
                "POST",
                validRequest().replace("https://example.com/acme/demo.git", "https://example.com/acme/other.git"),
                "correlation-1",
                "idem-3"
            );

            assertEquals(409, conflict.code());
            assertTrue(conflict.body().contains("\"code\":\"CONFLICT\""));
            assertFalse(conflict.body().contains("/tmp"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void mapsUnsupportedMethodsMalformedPayloadsAndBackendFailures() throws Exception {
        var server = server();
        try {
            var port = server.getAddress().getPort();

            assertEquals(405, response(port, "/api/status", "PUT").code());
            assertEquals(404, response(port, "/api/unknown", "POST", "{}", "correlation-1", "idem-404").code());
            assertEquals(400, response(
                port,
                "/api/repository-analyses",
                "POST",
                validRequest().replace("\"ephemeral\": false,", ""),
                "correlation-1",
                "idem-missing-boolean"
            ).code());
            assertEquals(400, response(
                port,
                "/api/repository-analyses",
                "POST",
                validRequest().replace("\"maxWorkspaceBytes\": 100000", "\"maxWorkspaceBytes\": null"),
                "correlation-1",
                "idem-missing-long"
            ).code());
            assertEquals(400, response(
                port,
                "/api/repository-analyses",
                "POST",
                "x".repeat(64 * 1024 + 1),
                "correlation-1",
                "idem-too-large"
            ).code());
            var unsafeCorrelation = response(
                port,
                "/api/repository-analyses",
                "POST",
                validRequest(),
                "bad\"correlation",
                "idem-unsafe-correlation"
            );
            var unsafeIdempotency = response(
                port,
                "/api/repository-analyses",
                "POST",
                validRequest(),
                "correlation-1",
                "idem unsafe"
            );

            assertEquals(400, unsafeCorrelation.code());
            assertTrue(unsafeCorrelation.body().contains("\"correlationId\":\"unknown\""));
            assertFalse(unsafeCorrelation.body().contains("bad\"correlation"));
            assertEquals(400, unsafeIdempotency.code());
            assertEquals("correlation-1", unsafeIdempotency.correlationId());
        } finally {
            server.stop(0);
        }

        var failing = server(command -> {
            throw new GatewayRepositoryAnalysisException(
                503,
                "BACKEND_UNAVAILABLE",
                true,
                "Repository analysis service is unavailable"
            );
        });
        try {
            var response = response(
                failing.getAddress().getPort(),
                "/api/repository-analyses",
                "POST",
                validRequest(),
                "correlation-1",
                "idem-backend"
            );

            assertEquals(503, response.code());
            assertEquals("correlation-1", response.correlationId());
            assertTrue(response.body().contains("\"retryable\":true"));
            assertFalse(response.body().contains("/tmp"));
        } finally {
            failing.stop(0);
        }
    }

    private static HttpServer server() throws IOException {
        return server(new FakePreparationPort());
    }

    private static HttpServer server(RepositoryAnalysisPreparationPort port) throws IOException {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", new GatewayHttpHandler(
            new GatewayStatusService(),
            new GatewayRepositoryAnalysisSubmissionService(port)
        ));
        server.start();
        return server;
    }

    private static Response response(int port, String path, String method) throws IOException {
        return response(port, path, method, "", null, null);
    }

    private static Response response(
        int port,
        String path,
        String method,
        String body,
        String correlationId,
        String idempotencyKey
    ) throws IOException {
        var connection = (HttpURLConnection) URI.create("http://127.0.0.1:" + port + path).toURL().openConnection();
        connection.setRequestMethod(method);
        if (correlationId != null) {
            connection.setRequestProperty("X-Correlation-Id", correlationId);
        }
        if (idempotencyKey != null) {
            connection.setRequestProperty("Idempotency-Key", idempotencyKey);
        }
        if (!body.isEmpty()) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
        }
        var stream = connection.getResponseCode() >= 400 ? connection.getErrorStream() : connection.getInputStream();
        try (stream) {
            return new Response(
                connection.getResponseCode(),
                new String(stream.readAllBytes(), StandardCharsets.UTF_8),
                connection.getHeaderField("X-Correlation-Id")
            );
        }
    }

    private static String validRequest() {
        return """
            {
              "requestId": "request-1",
              "schemaVersion": "gateway.v1",
              "requestedOutputs": ["BTM_RULES"],
              "repositoryUrl": "https://example.com/acme/demo.git",
              "provider": "github",
              "branch": "main",
              "buildContext": {
                "buildTool": "gradle",
                "buildId": "build-1",
                "rootProjectName": "demo",
                "declaredModules": [":app"],
                "attributes": {"tenant": "demo"}
              },
              "workspacePolicy": {
                "ephemeral": false,
                "allowShallowClone": true,
                "allowPartialClone": false,
                "allowSparseCheckout": false,
                "timeoutSeconds": 60,
                "maxWorkspaceBytes": 100000
              }
            }
            """;
    }

    private record Response(int code, String body, String correlationId) {
    }

    private static final class FakePreparationPort implements RepositoryAnalysisPreparationPort {
        @Override
        public RepositoryPreparationResult prepare(RepositoryPreparationCommand command) {
            return new RepositoryPreparationResult(
                command.analysisRunId(),
                "source-snapshot-1",
                "CHECKED_OUT",
                List.of(Diagnostic.info("DOWNSTREAM_OK", "Repository Analysis accepted the snapshot"))
            );
        }
    }
}
