package de.burger.forensics.analytics.services.cliclient.adapter.out.http;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.burger.forensics.analytics.services.cliclient.domain.CliClientSubmissionCommand;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpRepositoryAnalysisSubmissionClientTest {
    @Test
    void sendsOpenApiAlignedHeadersAndPayloadAndMapsAcceptedResponse() throws Exception {
        var captured = new AtomicReference<CapturedRequest>();
        var server = server(exchange -> {
            captured.set(capture(exchange));
            respond(exchange, 202, """
                {
                  "analysisRunId": "analysis-run-1",
                  "status": "ACCEPTED",
                  "statusUrl": "/repository-analyses/analysis-run-1",
                  "jobsUrl": "/repository-analyses/analysis-run-1/jobs",
                  "btmDeliveryStatus": "BTM_DELIVERY_NOT_READY",
                  "btmDeliveryService": "BtmArtifactDeliveryService",
                  "correlationId": "correlation-1",
                  "diagnostics": [{"code": "WAITING", "message": "pending"}]
                }
                """);
        });
        try {
            var result = client().submit(command(server));

            assertEquals("analysis-run-1", result.analysisRunId());
            assertEquals("ACCEPTED", result.status());
            assertEquals("/repository-analyses/analysis-run-1", result.statusUrl());
            assertEquals("/repository-analyses/analysis-run-1/jobs", result.jobsUrl());
            assertEquals("BTM_DELIVERY_NOT_READY", result.btmDeliveryStatus());
            assertEquals("BtmArtifactDeliveryService", result.btmDeliveryService());
            assertEquals("correlation-1", result.correlationId());
            assertEquals(1, result.diagnosticCount());
            assertEquals("POST", captured.get().method());
            assertEquals("/api/repository-analyses", captured.get().path());
            assertEquals("application/json", captured.get().accept());
            assertEquals("application/json; charset=utf-8", captured.get().contentType());
            assertEquals("correlation-1", captured.get().correlationId());
            assertEquals("idem-1", captured.get().idempotencyKey());
            assertContainsAll(
                captured.get().body(),
                "\"requestId\":\"request-1\"",
                "\"schemaVersion\":\"gateway.v1\"",
                "\"repositoryUrl\":\"https://example.com/acme/demo.git\"",
                "\"requestedOutputs\":[\"BTM_RULES\"]",
                "\"provider\":\"github\"",
                "\"branch\":\"main\"",
                "\"commit\":\"\"",
                "\"buildTool\":\"gradle\"",
                "\"buildId\":\"build-1\"",
                "\"rootProjectName\":\"demo\"",
                "\"declaredModules\":[\":app\",\":lib\"]",
                "\"attributes\":{}",
                "\"ephemeral\":false",
                "\"allowShallowClone\":true",
                "\"allowPartialClone\":false",
                "\"allowSparseCheckout\":false",
                "\"timeoutSeconds\":60",
                "\"maxWorkspaceBytes\":100000"
            );
            assertFalse(captured.get().body().contains("workspace-"));
            assertFalse(captured.get().body().contains("/tmp"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void mapsPublicErrorEnvelopeWithoutLeakingPrivateResponseBody() throws Exception {
        var server = server(exchange -> respond(exchange, 409, """
            {
              "code": "CONFLICT",
              "retryable": false,
              "correlationId": "correlation-1",
              "message": "private path /tmp/workspace and token=abc must not leak"
            }
            """));
        try {
            var failure = assertThrows(PublicApiClientException.class, () -> client().submit(command(server)));

            assertEquals(
                "Public API error status=409 code=CONFLICT retryable=false correlationId=correlation-1",
                failure.getMessage()
            );
            assertFalse(failure.getMessage().contains("/tmp"));
            assertFalse(failure.getMessage().contains("token"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void sendsContractHeadersAndPayloadToPublicApiBaseUrl() {
        var httpClient = new RecordingHttpClient(
            202,
            """
                {
                  "analysisRunId": "analysis-run-1",
                  "status": "ACCEPTED",
                  "statusUrl": "/repository-analyses/analysis-run-1",
                  "jobsUrl": "/repository-analyses/analysis-run-1/jobs",
                  "btmDeliveryStatus": "BTM_DELIVERY_NOT_READY",
                  "btmDeliveryService": "BtmArtifactDeliveryService",
                  "correlationId": "correlation-1",
                  "diagnostics": []
                }
                """
        );

        var result = new HttpRepositoryAnalysisSubmissionClient(httpClient, new Gson()).submit(command());

        assertEquals("analysis-run-1", result.analysisRunId());
        assertEquals("ACCEPTED", result.status());
        assertEquals(URI.create("http://gateway.example/api/repository-analyses"), httpClient.request.uri());
        assertEquals("application/json", httpClient.request.headers().firstValue("Accept").orElseThrow());
        assertEquals("application/json; charset=utf-8", httpClient.request.headers().firstValue("Content-Type").orElseThrow());
        assertEquals("correlation-1", httpClient.request.headers().firstValue("X-Correlation-Id").orElseThrow());
        assertEquals("idem-1", httpClient.request.headers().firstValue("Idempotency-Key").orElseThrow());
        var payload = JsonParser.parseString(httpClient.body).getAsJsonObject();
        assertEquals("request-1", payload.get("requestId").getAsString());
        assertEquals("gateway.v1", payload.get("schemaVersion").getAsString());
        assertEquals("https://example.com/acme/demo.git", payload.get("repositoryUrl").getAsString());
        assertEquals("main", payload.get("branch").getAsString());
        assertEquals("BTM_RULES", payload.getAsJsonArray("requestedOutputs").get(0).getAsString());
        var buildContext = payload.getAsJsonObject("buildContext");
        assertEquals("gradle", buildContext.get("buildTool").getAsString());
        assertEquals("build-1", buildContext.get("buildId").getAsString());
        assertEquals(":app", buildContext.getAsJsonArray("declaredModules").get(0).getAsString());
        assertEquals(0, buildContext.getAsJsonObject("attributes").size());
        var workspacePolicy = payload.getAsJsonObject("workspacePolicy");
        assertFalse(workspacePolicy.get("ephemeral").getAsBoolean());
        assertTrue(workspacePolicy.get("allowShallowClone").getAsBoolean());
        assertFalse(httpClient.body.contains("RunRepositoryAnalysisUseCase"));
        assertFalse(httpClient.body.contains("workspace-"));
    }

    @Test
    void mapsDocumentedErrorCodesToRedactedPublicApiExceptions() {
        for (var code : List.of("VALIDATION_ERROR", "CONFLICT", "BACKEND_UNAVAILABLE", "TIMEOUT", "NOT_FOUND", "UNEXPECTED_ERROR")) {
            var httpClient = new RecordingHttpClient(
                409,
                """
                    {
                      "code": "%s",
                      "message": "unsafe /tmp/private Authorization token",
                      "retryable": true,
                      "correlationId": "correlation-1",
                      "diagnostics": [
                        {"code": "PRIVATE_PATH", "message": "/tmp/private Authorization token"}
                      ]
                    }
                    """.formatted(code)
            );

            var error = assertThrows(
                PublicApiClientException.class,
                () -> new HttpRepositoryAnalysisSubmissionClient(httpClient, new Gson()).submit(command())
            );

            assertTrue(error.getMessage().contains("status=409"), code);
            assertTrue(error.getMessage().contains("code=" + code), code);
            assertTrue(error.getMessage().contains("retryable=true"), code);
            assertTrue(error.getMessage().contains("correlationId=correlation-1"), code);
            assertFalse(error.getMessage().contains("/tmp"), code);
            assertFalse(error.getMessage().contains("Authorization"), code);
        }
    }

    @Test
    void mapsIncompleteOrMalformedPublicApiResponsesToRedactedExceptions() {
        var invalidSuccessJson = new RecordingHttpClient(202, "{not-json");
        var missingPublicField = new RecordingHttpClient(
            202,
            """
                {"analysisRunId":"analysis-run-1","status":"ACCEPTED","diagnostics":"not-an-array"}
                """
        );
        var malformedError = new RecordingHttpClient(500, "{not-json");
        var sparseError = new RecordingHttpClient(503, "{\"code\":null,\"diagnostics\":[]}");

        assertEquals("Public API returned invalid JSON", assertThrows(
            PublicApiClientException.class,
            () -> new HttpRepositoryAnalysisSubmissionClient(invalidSuccessJson, new Gson()).submit(command())
        ).getMessage());
        assertTrue(assertThrows(
            PublicApiClientException.class,
            () -> new HttpRepositoryAnalysisSubmissionClient(missingPublicField, new Gson()).submit(command())
        ).getMessage().contains("statusUrl"));
        assertTrue(assertThrows(
            PublicApiClientException.class,
            () -> new HttpRepositoryAnalysisSubmissionClient(malformedError, new Gson()).submit(command())
        ).getMessage().contains("code=UNKNOWN"));
        assertTrue(assertThrows(
            PublicApiClientException.class,
            () -> new HttpRepositoryAnalysisSubmissionClient(sparseError, new Gson()).submit(command())
        ).getMessage().contains("correlationId=unknown"));
    }

    @Test
    void defaultsOptionalPublicFieldsWhenPublicApiOmitsThem() {
        var httpClient = new RecordingHttpClient(
            202,
            """
                {
                  "analysisRunId": "analysis-run-1",
                  "status": "ACCEPTED",
                  "statusUrl": "/repository-analyses/analysis-run-1",
                  "jobsUrl": "/repository-analyses/analysis-run-1/jobs",
                  "btmDeliveryStatus": "BTM_DELIVERY_NOT_READY",
                  "correlationId": "correlation-1"
                }
                """
        );

        var result = new HttpRepositoryAnalysisSubmissionClient(httpClient, new Gson()).submit(command());

        assertEquals("", result.btmDeliveryService());
        assertEquals(0, result.diagnosticCount());
    }

    @Test
    void ignoresNonPrimitiveOptionalSuccessFieldsAndNonArrayDiagnostics() {
        var httpClient = new RecordingHttpClient(
            202,
            """
                {
                  "analysisRunId": "analysis-run-1",
                  "status": "ACCEPTED",
                  "statusUrl": "/repository-analyses/analysis-run-1",
                  "jobsUrl": "/repository-analyses/analysis-run-1/jobs",
                  "btmDeliveryStatus": "BTM_DELIVERY_NOT_READY",
                  "btmDeliveryService": {},
                  "correlationId": "correlation-1",
                  "diagnostics": {}
                }
                """
        );

        var result = new HttpRepositoryAnalysisSubmissionClient(httpClient, new Gson()).submit(command());

        assertEquals("", result.btmDeliveryService());
        assertEquals(0, result.diagnosticCount());
    }

    @Test
    void mapsBlankErrorFieldsAndNonPrimitiveRetryableToUnknownPublicValues() {
        var httpClient = new RecordingHttpClient(
            503,
            """
                {
                  "code": "",
                  "retryable": {},
                  "correlationId": ""
                }
                """
        );

        var error = assertThrows(
            PublicApiClientException.class,
            () -> new HttpRepositoryAnalysisSubmissionClient(httpClient, new Gson()).submit(command())
        );

        assertEquals("Public API error status=503 code=unknown retryable=false correlationId=unknown", error.getMessage());
    }

    @Test
    void mapsTransportIoFailuresToRedactedPublicApiExceptions() {
        var httpClient = new RecordingHttpClient(new IOException("unsafe /tmp/private Authorization token"));

        var error = assertThrows(
            PublicApiClientException.class,
            () -> new HttpRepositoryAnalysisSubmissionClient(httpClient, new Gson()).submit(command())
        );

        assertEquals("Public API request failed", error.getMessage());
        assertFalse(error.getMessage().contains("/tmp"));
        assertFalse(error.getMessage().contains("Authorization"));
    }

    @Test
    void mapsInterruptedTransportFailuresToRedactedPublicApiExceptionsAndRestoresInterrupt() {
        var httpClient = new RecordingHttpClient(new InterruptedException("unsafe /tmp/private Authorization token"));

        try {
            var error = assertThrows(
                PublicApiClientException.class,
                () -> new HttpRepositoryAnalysisSubmissionClient(httpClient, new Gson()).submit(command())
            );

            assertEquals("Public API request was interrupted", error.getMessage());
            assertFalse(error.getMessage().contains("/tmp"));
            assertFalse(error.getMessage().contains("Authorization"));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void rejectsMalformedAcceptedResponseAsInvalidPublicJson() throws Exception {
        var server = server(exchange -> respond(exchange, 202, "{\"status\":\"ACCEPTED\""));
        try {
            var failure = assertThrows(PublicApiClientException.class, () -> client().submit(command(server)));

            assertEquals("Public API returned invalid JSON", failure.getMessage());
        } finally {
            server.stop(0);
        }
    }

    private static HttpRepositoryAnalysisSubmissionClient client() {
        return new HttpRepositoryAnalysisSubmissionClient(HttpClient.newHttpClient(), new Gson());
    }

    private static HttpServer server(ExchangeHandler handler) throws IOException {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/repository-analyses", exchange -> {
            try {
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
        server.start();
        return server;
    }

    private static CliClientSubmissionCommand command(HttpServer server) {
        return new CliClientSubmissionCommand(
            URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api"),
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
            60,
            100_000,
            true
        );
    }

    private static CliClientSubmissionCommand command() {
        return new CliClientSubmissionCommand(
            URI.create("http://gateway.example/api"),
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
            List.of(":app"),
            "correlation-1",
            "idem-1",
            60,
            100_000,
            true
        );
    }

    private static CapturedRequest capture(HttpExchange exchange) throws IOException {
        return new CapturedRequest(
            exchange.getRequestMethod(),
            exchange.getRequestURI().getPath(),
            exchange.getRequestHeaders().getFirst("Accept"),
            exchange.getRequestHeaders().getFirst("Content-Type"),
            exchange.getRequestHeaders().getFirst("X-Correlation-Id"),
            exchange.getRequestHeaders().getFirst("Idempotency-Key"),
            new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)
        );
    }

    private static void respond(HttpExchange exchange, int statusCode, String response) throws IOException {
        var bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private static void assertContainsAll(String content, String... expectedFragments) {
        for (var fragment : expectedFragments) {
            assertTrue(content.contains(fragment), () -> "Missing expected fragment: " + fragment);
        }
    }

    private static final class RecordingHttpClient extends HttpClient {
        private final int statusCode;
        private final String responseBody;
        private final IOException ioFailure;
        private final InterruptedException interruptedFailure;
        private HttpRequest request;
        private String body;

        private RecordingHttpClient(int statusCode, String responseBody) {
            this.statusCode = statusCode;
            this.responseBody = responseBody;
            this.ioFailure = null;
            this.interruptedFailure = null;
        }

        private RecordingHttpClient(IOException ioFailure) {
            this.statusCode = 0;
            this.responseBody = "";
            this.ioFailure = ioFailure;
            this.interruptedFailure = null;
        }

        private RecordingHttpClient(InterruptedException interruptedFailure) {
            this.statusCode = 0;
            this.responseBody = "";
            this.ioFailure = null;
            this.interruptedFailure = interruptedFailure;
        }

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return Optional.empty();
        }

        @Override
        public Redirect followRedirects() {
            return Redirect.NEVER;
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return Optional.empty();
        }

        @Override
        public SSLContext sslContext() {
            return null;
        }

        @Override
        public SSLParameters sslParameters() {
            return null;
        }

        @Override
        public Optional<Authenticator> authenticator() {
            return Optional.empty();
        }

        @Override
        public Version version() {
            return Version.HTTP_1_1;
        }

        @Override
        public Optional<Executor> executor() {
            return Optional.empty();
        }

        @Override
        public <T> HttpResponse<T> send(
            HttpRequest request,
            HttpResponse.BodyHandler<T> responseBodyHandler
        ) throws IOException, InterruptedException {
            if (ioFailure != null) {
                throw ioFailure;
            }
            if (interruptedFailure != null) {
                throw interruptedFailure;
            }
            this.request = request;
            this.body = body(request);
            @SuppressWarnings("unchecked")
            var response = (HttpResponse<T>) new StubHttpResponse<>(request, statusCode, responseBody);
            return response;
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
            HttpRequest request,
            HttpResponse.BodyHandler<T> responseBodyHandler
        ) {
            throw new UnsupportedOperationException("sendAsync is not used by this test");
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
            HttpRequest request,
            HttpResponse.BodyHandler<T> responseBodyHandler,
            HttpResponse.PushPromiseHandler<T> pushPromiseHandler
        ) {
            throw new UnsupportedOperationException("sendAsync is not used by this test");
        }

        private static String body(HttpRequest request) throws IOException {
            var publisher = request.bodyPublisher().orElseThrow();
            var subscriber = new BodyCaptureSubscriber();
            publisher.subscribe(subscriber);
            return subscriber.body();
        }
    }

    private static final class BodyCaptureSubscriber implements Flow.Subscriber<ByteBuffer> {
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();
        private final CountDownLatch completed = new CountDownLatch(1);

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(ByteBuffer item) {
            var bytes = new byte[item.remaining()];
            item.get(bytes);
            output.writeBytes(bytes);
        }

        @Override
        public void onError(Throwable throwable) {
            completed.countDown();
        }

        @Override
        public void onComplete() {
            completed.countDown();
        }

        private String body() throws IOException {
            try {
                if (!completed.await(5, TimeUnit.SECONDS)) {
                    throw new IOException("request body was not published");
                }
                return output.toString(StandardCharsets.UTF_8);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("request body capture was interrupted", e);
            }
        }
    }

    private record StubHttpResponse<T>(HttpRequest request, int statusCode, T body) implements HttpResponse<T> {
        @Override
        public Optional<HttpResponse<T>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(java.util.Map.of(), (name, value) -> true);
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return request.uri();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }

    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }

    private record CapturedRequest(
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
