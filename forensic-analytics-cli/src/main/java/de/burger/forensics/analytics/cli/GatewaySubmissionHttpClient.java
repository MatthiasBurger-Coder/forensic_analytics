package de.burger.forensics.analytics.cli;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

final class GatewaySubmissionHttpClient implements GatewaySubmissionClient {
    private static final int HTTP_ACCEPTED = 202;

    private final HttpClient httpClient;
    private final Gson gson;

    GatewaySubmissionHttpClient() {
        this(HttpClient.newHttpClient(), new Gson());
    }

    GatewaySubmissionHttpClient(HttpClient httpClient, Gson gson) {
        this.httpClient = httpClient;
        this.gson = gson;
    }

    @Override
    public GatewaySubmissionResult submit(GatewaySubmitCommand command) {
        var response = send(command);
        if (response.statusCode() == HTTP_ACCEPTED) {
            return success(response.body());
        }
        throw error(response.body(), response.statusCode());
    }

    private HttpResponse<String> send(GatewaySubmitCommand command) {
        var request = HttpRequest.newBuilder(command.repositoryAnalysesUri())
            .timeout(command.timeout())
            .header("Accept", "application/json")
            .header("Content-Type", "application/json; charset=utf-8")
            .header("X-Correlation-Id", command.correlationId())
            .header("Idempotency-Key", command.idempotencyKey())
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload(command)), StandardCharsets.UTF_8))
            .build();
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Gateway request failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Gateway request was interrupted", e);
        }
    }

    private static Map<String, Object> payload(GatewaySubmitCommand command) {
        return Map.of(
            "requestId", command.requestId(),
            "schemaVersion", command.schemaVersion(),
            "requestedOutputs", command.requestedOutputs(),
            "repositoryUrl", command.repositoryUrl(),
            "provider", command.provider(),
            "branch", command.branch(),
            "commit", command.commit(),
            "buildContext", Map.of(
                "buildTool", command.buildTool(),
                "buildId", command.buildId(),
                "rootProjectName", command.rootProjectName(),
                "declaredModules", command.declaredModules(),
                "attributes", Map.of()
            ),
            "workspacePolicy", Map.of(
                "ephemeral", false,
                "allowShallowClone", command.allowShallowClone(),
                "allowPartialClone", false,
                "allowSparseCheckout", false,
                "timeoutSeconds", command.timeoutSeconds(),
                "maxWorkspaceBytes", command.maxWorkspaceBytes()
            )
        );
    }

    private GatewaySubmissionResult success(String body) {
        var json = parseObject(body);
        return new GatewaySubmissionResult(
            requiredString(json, "analysisRunId"),
            requiredString(json, "status"),
            requiredString(json, "statusUrl"),
            requiredString(json, "jobsUrl"),
            requiredString(json, "btmDeliveryStatus"),
            optionalString(json, "btmDeliveryService"),
            requiredString(json, "correlationId"),
            diagnosticCount(json)
        );
    }

    private CliGatewayException error(String body, int statusCode) {
        try {
            var json = parseObject(body);
            var code = optionalString(json, "code");
            var retryable = json.has("retryable") && json.get("retryable").isJsonPrimitive()
                ? json.get("retryable").getAsBoolean()
                : false;
            var correlationId = optionalString(json, "correlationId");
            return new CliGatewayException(
                "Gateway error status=" + statusCode
                    + " code=" + emptyAsUnknown(code)
                    + " retryable=" + retryable
                    + " correlationId=" + emptyAsUnknown(correlationId)
            );
        } catch (RuntimeException ignored) {
            return new CliGatewayException("Gateway error status=" + statusCode + " code=UNKNOWN retryable=false correlationId=unknown");
        }
    }

    private static JsonObject parseObject(String body) {
        try {
            return JsonParser.parseString(body).getAsJsonObject();
        } catch (IllegalStateException | JsonSyntaxException e) {
            throw new CliGatewayException("Gateway returned invalid JSON");
        }
    }

    private static String requiredString(JsonObject json, String name) {
        var value = optionalString(json, name);
        if (value.isBlank()) {
            throw new CliGatewayException("Gateway response is missing public field: " + name);
        }
        return value;
    }

    private static String optionalString(JsonObject json, String name) {
        if (!json.has(name) || json.get(name).isJsonNull()) {
            return "";
        }
        return json.get(name).getAsString();
    }

    private static int diagnosticCount(JsonObject json) {
        if (!json.has("diagnostics") || !json.get("diagnostics").isJsonArray()) {
            return 0;
        }
        return json.getAsJsonArray("diagnostics").size();
    }

    private static String emptyAsUnknown(String value) {
        return value.isBlank() ? "unknown" : value;
    }
}
