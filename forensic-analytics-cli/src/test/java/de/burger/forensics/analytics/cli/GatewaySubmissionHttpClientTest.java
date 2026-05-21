package de.burger.forensics.analytics.cli;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
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
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewaySubmissionHttpClientTest {
    @Test
    void sendsContractHeadersAndPayloadToGatewayApiBaseUrl() {
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

        var result = new GatewaySubmissionHttpClient(httpClient, new Gson()).submit(command());

        assertEquals("analysis-run-1", result.analysisRunId());
        assertEquals("ACCEPTED", result.status());
        assertEquals(URI.create("http://gateway.example/api/repository-analyses"), httpClient.request.uri());
        assertEquals("application/json", httpClient.request.headers().firstValue("Accept").orElseThrow());
        assertEquals("correlation-1", httpClient.request.headers().firstValue("X-Correlation-Id").orElseThrow());
        assertEquals("idem-1", httpClient.request.headers().firstValue("Idempotency-Key").orElseThrow());
        assertTrue(httpClient.body.contains("\"repositoryUrl\":\"https://example.com/acme/demo.git\""));
        assertTrue(httpClient.body.contains("\"branch\":\"main\""));
        assertTrue(httpClient.body.contains("\"requestedOutputs\":[\"BTM_RULES\"]"));
        assertTrue(httpClient.body.contains("\"buildTool\":\"gradle\""));
        assertTrue(httpClient.body.contains("\"declaredModules\":[\":app\"]"));
        assertTrue(httpClient.body.contains("\"ephemeral\":false"));
        assertTrue(httpClient.body.contains("\"allowShallowClone\":true"));
        assertFalse(httpClient.body.contains("RunRepositoryAnalysisUseCase"));
        assertFalse(httpClient.body.contains("workspace-"));
    }

    @Test
    void mapsGatewayErrorEnvelopeToRedactedCliException() {
        var httpClient = new RecordingHttpClient(
            409,
            """
                {
                  "code": "CONFLICT",
                  "message": "Idempotency key conflicts with a previous request",
                  "retryable": false,
                  "correlationId": "correlation-1",
                  "diagnostics": []
                }
                """
        );

        var error = assertThrows(CliGatewayException.class, () -> new GatewaySubmissionHttpClient(httpClient, new Gson()).submit(command()));

        assertTrue(error.getMessage().contains("status=409"));
        assertTrue(error.getMessage().contains("code=CONFLICT"));
        assertTrue(error.getMessage().contains("retryable=false"));
        assertTrue(error.getMessage().contains("correlationId=correlation-1"));
        assertFalse(error.getMessage().contains("/tmp"));
    }

    @Test
    void mapsIncompleteOrMalformedGatewayResponsesToRedactedCliExceptions() {
        var missingPublicField = new RecordingHttpClient(
            202,
            """
                {"analysisRunId":"analysis-run-1","status":"ACCEPTED","diagnostics":"not-an-array"}
                """
        );
        var malformedError = new RecordingHttpClient(500, "{not-json");
        var sparseError = new RecordingHttpClient(503, "{\"code\":null,\"diagnostics\":[]}");

        assertTrue(assertThrows(
            CliGatewayException.class,
            () -> new GatewaySubmissionHttpClient(missingPublicField, new Gson()).submit(command())
        ).getMessage().contains("statusUrl"));
        assertTrue(assertThrows(
            CliGatewayException.class,
            () -> new GatewaySubmissionHttpClient(malformedError, new Gson()).submit(command())
        ).getMessage().contains("code=UNKNOWN"));
        assertTrue(assertThrows(
            CliGatewayException.class,
            () -> new GatewaySubmissionHttpClient(sparseError, new Gson()).submit(command())
        ).getMessage().contains("correlationId=unknown"));
    }

    @Test
    void validatesGatewaySubmitCommandBoundaries() {
        assertEquals(
            URI.create("http://gateway.example/api/repository-analyses"),
            commandWithGateway("http://gateway.example/api/").repositoryAnalysesUri()
        );
        assertEquals("commit-1", commandWithBranchAndCommit("", "commit-1").commit());
        assertEquals("", commandWithBranchAndCommit("main", null).commit());

        assertThrows(CliUsageException.class, () -> commandWithOutputs(List.of()));
        assertThrows(CliUsageException.class, () -> commandWithOutputs(List.of("REPORT")));
        assertThrows(CliUsageException.class, () -> commandWithDeclaredModules(List.of()));
        assertThrows(CliUsageException.class, () -> commandWithDeclaredModules(List.of(" ")));
        assertThrows(CliUsageException.class, () -> commandWithTimeout(0));
        assertThrows(CliUsageException.class, () -> commandWithTimeout(3_601));
        assertThrows(CliUsageException.class, () -> commandWithMaxWorkspaceBytes(0));
        assertThrows(CliUsageException.class, () -> commandWithMaxWorkspaceBytes(107_374_182_401L));
        assertThrows(CliUsageException.class, () -> commandWithGateway("ftp://gateway.example/api"));
        assertThrows(CliUsageException.class, () -> commandWithGateway("http:/api"));
        assertThrows(CliUsageException.class, () -> commandWithGateway("http://user@gateway.example/api"));
        assertThrows(CliUsageException.class, () -> commandWithGateway("http://gateway.example/api?debug=true"));
        assertThrows(CliUsageException.class, () -> commandWithGateway("http://gateway.example/api#debug"));
        assertThrows(CliUsageException.class, () -> commandWithRepositoryUrl("http://example.com/acme/demo.git"));
        assertThrows(CliUsageException.class, () -> commandWithRepositoryUrl("https://user@example.com/acme/demo.git"));
        assertThrows(CliUsageException.class, () -> commandWithCorrelationId("bad correlation"));
        assertThrows(CliUsageException.class, () -> commandWithRequestId(" "));
    }

    private static GatewaySubmitCommand command() {
        return new GatewaySubmitCommand(
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

    private static GatewaySubmitCommand commandWithGateway(String gateway) {
        return new GatewaySubmitCommand(
            URI.create(gateway),
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

    private static GatewaySubmitCommand commandWithBranchAndCommit(String branch, String commit) {
        return new GatewaySubmitCommand(
            URI.create("http://gateway.example/api"),
            "https://example.com/acme/demo.git",
            branch,
            commit,
            "request-1",
            "gateway.v1",
            List.of("BTM_RULES"),
            "",
            "gradle",
            "build-1",
            "demo",
            List.of(":app"),
            "correlation-1",
            "idem-1",
            60,
            100_000,
            false
        );
    }

    private static GatewaySubmitCommand commandWithOutputs(List<String> outputs) {
        return new GatewaySubmitCommand(
            URI.create("http://gateway.example/api"),
            "https://example.com/acme/demo.git",
            "main",
            "",
            "request-1",
            "gateway.v1",
            outputs,
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

    private static GatewaySubmitCommand commandWithDeclaredModules(List<String> declaredModules) {
        return new GatewaySubmitCommand(
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
            declaredModules,
            "correlation-1",
            "idem-1",
            60,
            100_000,
            true
        );
    }

    private static GatewaySubmitCommand commandWithTimeout(long timeoutSeconds) {
        return new GatewaySubmitCommand(
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
            timeoutSeconds,
            100_000,
            true
        );
    }

    private static GatewaySubmitCommand commandWithMaxWorkspaceBytes(long maxWorkspaceBytes) {
        return new GatewaySubmitCommand(
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
            maxWorkspaceBytes,
            true
        );
    }

    private static GatewaySubmitCommand commandWithRepositoryUrl(String repositoryUrl) {
        return new GatewaySubmitCommand(
            URI.create("http://gateway.example/api"),
            repositoryUrl,
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

    private static GatewaySubmitCommand commandWithCorrelationId(String correlationId) {
        return new GatewaySubmitCommand(
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
            correlationId,
            "idem-1",
            60,
            100_000,
            true
        );
    }

    private static GatewaySubmitCommand commandWithRequestId(String requestId) {
        return new GatewaySubmitCommand(
            URI.create("http://gateway.example/api"),
            "https://example.com/acme/demo.git",
            "main",
            "",
            requestId,
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

    private static final class RecordingHttpClient extends HttpClient {
        private final int statusCode;
        private final String responseBody;
        private HttpRequest request;
        private String body;

        private RecordingHttpClient(int statusCode, String responseBody) {
            this.statusCode = statusCode;
            this.responseBody = responseBody;
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
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) throws IOException {
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
}
