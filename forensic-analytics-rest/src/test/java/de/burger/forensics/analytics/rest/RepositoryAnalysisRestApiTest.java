package de.burger.forensics.analytics.rest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.burger.forensics.analytics.application.ingestion.RepositoryAnalysisIngestionUseCase;
import de.burger.forensics.analytics.application.ingestion.RepositoryAnalysisQueryUseCase;
import de.burger.forensics.analytics.application.ingestion.RepositoryCheckoutException;
import de.burger.forensics.analytics.application.ingestion.command.AnalyzeRepositoryCommand;
import de.burger.forensics.analytics.application.ingestion.query.RepositoryAnalysisView;
import de.burger.forensics.analytics.application.ingestion.query.RepositoryAnalysisWorkflow;
import de.burger.forensics.analytics.application.ingestion.query.RepositoryAnalysisWorkspaceView;
import de.burger.forensics.analytics.application.ingestion.result.AnalyzeRepositoryResult;
import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.analysis.AnalysisSessionState;
import de.burger.forensics.analytics.domain.repository.CheckoutResult;
import de.burger.forensics.analytics.domain.repository.SourceRoot;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryAnalysisRestApiTest {
    private static final String WILDFLY_REPOSITORY_URL = "https://github.com/wildfly/wildfly.git";

    private final RecordingIngestionUseCase ingestionUseCase = new RecordingIngestionUseCase();
    private final RecordingQueryUseCase queryUseCase = new RecordingQueryUseCase();
    private final HttpClient client = HttpClient.newHttpClient();
    private RestApiServer server;

    @BeforeEach
    void startServer() throws Exception {
        server = new RestApiServerFactory().create(
            new InetSocketAddress("127.0.0.1", 0),
            ingestionUseCase,
            queryUseCase
        );
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop();
    }

    @Test
    void postRepositoryAnalysisRequiresVerifiedCommandFieldsAndReturnsRegisteredWorkflow() throws Exception {
        var response = send("POST", "/api/repository-analyses", validRequest());

        assertEquals(201, response.statusCode());
        var body = json(response);
        assertEquals("analysis-1", body.get("analysisRunId").getAsString());
        assertEquals("https://example.invalid/project.git", body.get("repositoryUrl").getAsString());
        assertEquals("https://mirror.example.invalid/project.git", body.get("resolvedRemoteUrl").getAsString());
        assertEquals("REGISTERED", body.get("status").getAsString());
        assertEquals("REPOSITORY_SESSION_REGISTRATION", body.get("workflow").getAsString());
        assertTrue(body.get("createdAt").isJsonNull());
        assertFalse(body.has("job"));
        assertEquals("request-1", ingestionUseCase.command.requestId());
        assertEquals("schema-v1", ingestionUseCase.command.schemaVersion());
        assertEquals(Optional.of("github"), ingestionUseCase.command.repository().provider());
        assertEquals(Optional.of("main"), ingestionUseCase.command.branch().name());
        assertTrue(ingestionUseCase.command.branch().required());
        assertTrue(ingestionUseCase.command.commit().hash().isEmpty());
        assertEquals("gradle", ingestionUseCase.command.buildContext().buildTool());
        assertEquals(List.of(":app"), ingestionUseCase.command.buildContext().declaredModules());
        assertEquals(Duration.ofSeconds(60), ingestionUseCase.command.workspacePolicy().timeout());
    }

    @Test
    void rejectsNonBlankWorkspaceNameBecauseNoStorageTargetExists() throws Exception {
        var response = send("POST", "/api/repository-analyses", validRequest("""
            "workspaceName": "Investigation",
            """));

        assertEquals(400, response.statusCode());
        var body = json(response);
        assertEquals("VALIDATION_ERROR", body.get("code").getAsString());
        assertFalse(body.get("retryable").getAsBoolean());
        assertTrue(body.get("diagnostics").getAsJsonArray().get(0).getAsString().contains("workspaceName"));
    }

    @Test
    void returnsNotFoundForMissingRepositoryAnalysis() throws Exception {
        var response = send("GET", "/api/repository-analyses/missing", "");

        assertEquals(404, response.statusCode());
        var body = json(response);
        assertEquals("NOT_FOUND", body.get("code").getAsString());
        assertFalse(body.get("retryable").getAsBoolean());
    }

    @Test
    void sanitizesDependencyErrorsWithoutRetryingPostRequests() throws Exception {
        ingestionUseCase.error = new RepositoryCheckoutException(
            "clone failed at /mnt/d/Projects/private token=secret123 at example.Secret(File.java:1)"
        );

        var response = send("POST", "/api/repository-analyses", validRequest());

        assertEquals(503, response.statusCode());
        var body = json(response);
        assertEquals("BACKEND_UNAVAILABLE", body.get("code").getAsString());
        assertFalse(body.get("retryable").getAsBoolean());
        var diagnostic = body.get("diagnostics").getAsJsonArray().get(0).getAsString();
        assertFalse(diagnostic.contains("/mnt/d/Projects/private"));
        assertFalse(diagnostic.contains("secret123"));
        assertFalse(diagnostic.contains("example.Secret"));
    }

    @Test
    void listsRepositoryAnalysisWorkspaceViewsDerivedFromSessionsWithoutInventedWorkspaceState() throws Exception {
        queryUseCase.workspaces = List.of(new RepositoryAnalysisWorkspaceView(
            new WorkspaceId("workspace-1"),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            List.of(view("analysis-1", "workspace-1"))
        ));

        var response = send("GET", "/api/workspaces", "");

        assertEquals(200, response.statusCode());
        var workspace = json(response).getAsJsonArray("items").get(0).getAsJsonObject();
        assertEquals("workspace-1", workspace.get("workspaceId").getAsString());
        assertTrue(workspace.get("name").isJsonNull());
        assertTrue(workspace.get("status").isJsonNull());
        assertTrue(workspace.get("createdAt").isJsonNull());
        assertTrue(workspace.get("updatedAt").isJsonNull());
        assertFalse(workspace.has("analysisRunIds"));
        var analysis = workspace.getAsJsonArray("repositoryAnalyses").get(0).getAsJsonObject();
        assertEquals("REGISTERED", analysis.get("status").getAsString());
        assertEquals("REPOSITORY_SESSION_REGISTRATION", analysis.get("workflow").getAsString());
    }

    @Test
    void returnsRepositoryAnalysisDetailAsPollTargetWithoutInventedJobEvidence() throws Exception {
        queryUseCase.analyses = List.of(view("analysis-1", "workspace-1"));

        var response = send("GET", "/api/repository-analyses/analysis-1", "");

        assertEquals(200, response.statusCode());
        var body = json(response);
        assertEquals("analysis-1", body.get("analysisRunId").getAsString());
        assertEquals("REGISTERED", body.get("status").getAsString());
        assertTrue(body.get("createdAt").isJsonNull());
        assertFalse(body.has("job"));
    }

    @Test
    void listsRepositoryAnalysesAndGeneratesCorrelationIdWhenHeaderIsAbsent() throws Exception {
        queryUseCase.analyses = List.of(view("analysis-1", "workspace-1"), view("analysis-2", "workspace-2"));

        var response = sendWithoutCorrelationId("GET", "/api/repository-analyses", "");

        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("X-Correlation-Id").orElseThrow().length() > 10);
        var items = json(response).getAsJsonArray("items");
        assertEquals(2, items.size());
        assertEquals("analysis-1", items.get(0).getAsJsonObject().get("analysisRunId").getAsString());
    }

    @Test
    void returnsWorkspaceDetailForRepositoryAnalysisWorkspaceView() throws Exception {
        queryUseCase.workspaces = List.of(new RepositoryAnalysisWorkspaceView(
            new WorkspaceId("workspace-1"),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            List.of(view("analysis-1", "workspace-1"))
        ));

        var response = send("GET", "/api/workspaces/workspace-1", "");

        assertEquals(200, response.statusCode());
        assertEquals("workspace-1", json(response).get("workspaceId").getAsString());
    }

    @Test
    void rejectsUnsupportedMethodsOnKnownCollections() throws Exception {
        var workspaceResponse = send("POST", "/api/workspaces", "{}");
        var analysesResponse = send("DELETE", "/api/repository-analyses", "");

        assertEquals(400, workspaceResponse.statusCode());
        assertEquals("VALIDATION_ERROR", json(workspaceResponse).get("code").getAsString());
        assertEquals(400, analysesResponse.statusCode());
        assertEquals("VALIDATION_ERROR", json(analysesResponse).get("code").getAsString());
    }

    @Test
    void returnsNotFoundForUnknownEndpoint() throws Exception {
        var response = send("GET", "/api/unknown", "");

        assertEquals(404, response.statusCode());
        assertEquals("NOT_FOUND", json(response).get("code").getAsString());
    }

    @Test
    void validatesPostRequestShapeWithoutUsingDefaults() throws Exception {
        var invalidBodies = List.of(
            "",
            "{",
            "null",
            validRequestWithoutRevision(),
            validRequestWithRepositoryUrl("http://example.invalid/project.git"),
            validRequestWithRepositoryUrl("file:///tmp/project.git"),
            validRequestWithRepositoryUrl("https://user@example.invalid/project.git"),
            validRequestWithout("buildContext"),
            validRequestWithout("workspacePolicy"),
            validRequestWithWorkspacePolicy("""
                "ephemeral": null,
                "allowShallowClone": false,
                "allowPartialClone": false,
                "allowSparseCheckout": false,
                "timeoutSeconds": 60,
                "maxWorkspaceBytes": 0
                """),
            validRequestWithWorkspacePolicy("""
                "ephemeral": true,
                "allowShallowClone": false,
                "allowPartialClone": false,
                "allowSparseCheckout": false,
                "timeoutSeconds": 60,
                "maxWorkspaceBytes": 0
                """),
            validRequestWithWorkspacePolicy("""
                "ephemeral": false,
                "allowShallowClone": false,
                "allowPartialClone": true,
                "allowSparseCheckout": false,
                "timeoutSeconds": 60,
                "maxWorkspaceBytes": 0
                """),
            validRequestWithWorkspacePolicy("""
                "ephemeral": false,
                "allowShallowClone": false,
                "allowPartialClone": false,
                "allowSparseCheckout": true,
                "timeoutSeconds": 60,
                "maxWorkspaceBytes": 0
                """),
            validRequestWithWorkspacePolicy("""
                "ephemeral": false,
                "allowShallowClone": false,
                "allowPartialClone": false,
                "allowSparseCheckout": false,
                "timeoutSeconds": 60,
                "maxWorkspaceBytes": 1024
                """),
            validRequestWithNegativeTimeout(),
            validRequestWithBuildContext("\"declaredModules\": [\" \"]"),
            validRequestWithInvalidAttributes()
        );

        for (var body : invalidBodies) {
            var response = send("POST", "/api/repository-analyses", body);

            assertEquals(400, response.statusCode(), body);
            assertEquals("VALIDATION_ERROR", json(response).get("code").getAsString());
        }
    }

    @Test
    void unexpectedGetFailuresAreRetryableAndSanitized() throws Exception {
        queryUseCase.error = new IllegalStateException("read failed at C:\\secret\\repo password=hunter2");

        var response = send("GET", "/api/repository-analyses", "");

        assertEquals(500, response.statusCode());
        var body = json(response);
        assertEquals("UNEXPECTED_ERROR", body.get("code").getAsString());
        assertTrue(body.get("retryable").getAsBoolean());
        var diagnostic = body.get("diagnostics").getAsJsonArray().get(0).getAsString();
        assertFalse(diagnostic.contains("C:\\secret\\repo"));
        assertFalse(diagnostic.contains("hunter2"));
    }

    @Test
    void serverStartCanBeCalledMoreThanOnceWithoutMovingPort() {
        var port = server.port();

        server.start();

        assertEquals(port, server.port());
    }

    @Test
    void postRepositoryAnalysisAcceptsCommitOnlyRequestsWithSupportedWorkspacePolicy() throws Exception {
        var response = send("POST", "/api/repository-analyses", commitOnlyRequest());

        assertEquals(201, response.statusCode());
        assertTrue(ingestionUseCase.command.branch().name().isEmpty());
        assertEquals(Optional.of("abcdef"), ingestionUseCase.command.commit().hash());
        assertTrue(ingestionUseCase.command.commit().required());
        assertFalse(ingestionUseCase.command.workspacePolicy().ephemeral());
    }

    @Test
    void postRepositoryAnalysisAcceptsWildFlyRepositoryWithoutExecutingExternalCheckout() throws Exception {
        var response = send("POST", "/api/repository-analyses", wildFlyRequest());

        assertEquals(201, response.statusCode());
        var body = json(response);
        assertEquals(WILDFLY_REPOSITORY_URL, body.get("repositoryUrl").getAsString());
        assertEquals(WILDFLY_REPOSITORY_URL, ingestionUseCase.command.repository().remoteUrl());
        assertEquals(Optional.of("github"), ingestionUseCase.command.repository().provider());
        assertEquals(Optional.of("main"), ingestionUseCase.command.branch().name());
        assertTrue(ingestionUseCase.command.branch().required());
        assertTrue(ingestionUseCase.command.commit().hash().isEmpty());
        assertEquals("maven", ingestionUseCase.command.buildContext().buildTool());
        assertEquals(Optional.of("wildfly"), ingestionUseCase.command.buildContext().rootProjectName());
        assertEquals(Duration.ofMinutes(20), ingestionUseCase.command.workspacePolicy().timeout());
        assertTrue(ingestionUseCase.command.workspacePolicy().allowShallowClone());
        assertFalse(ingestionUseCase.command.workspacePolicy().ephemeral());
    }

    @Test
    void rejectsPathSegmentsThatDecodeToMultipleSegments() throws Exception {
        var response = send("GET", "/api/repository-analyses/analysis%2F1", "");

        assertEquals(400, response.statusCode());
        assertEquals("VALIDATION_ERROR", json(response).get("code").getAsString());
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        return send(method, path, body, true);
    }

    private HttpResponse<String> sendWithoutCorrelationId(String method, String path, String body) throws Exception {
        return send(method, path, body, false);
    }

    private HttpResponse<String> send(String method, String path, String body, boolean includeCorrelationId)
        throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.port() + path))
            .timeout(Duration.ofSeconds(5))
            .header("Content-Type", "application/json");
        if (includeCorrelationId) {
            builder.header("X-Correlation-Id", "test-correlation");
        }
        var publisher = body.isEmpty()
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(body);
        return client.send(builder.method(method, publisher).build(), HttpResponse.BodyHandlers.ofString());
    }

    private static JsonObject json(HttpResponse<String> response) {
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private static String validRequest() {
        return validRequest("");
    }

    private static String validRequest(String extraFields) {
        return """
            {
              "repositoryUrl": "https://example.invalid/project.git",
              "provider": "github",
              "branch": "main",
              %s
              "requestId": "request-1",
              "schemaVersion": "schema-v1",
              "buildContext": {
                "buildTool": "gradle",
                "buildId": "build-1",
                "rootProjectName": "",
                "declaredModules": [":app"],
                "attributes": {"environment": "test"}
              },
              "workspacePolicy": {
                "ephemeral": false,
                "allowShallowClone": true,
                "allowPartialClone": false,
                "allowSparseCheckout": false,
                "timeoutSeconds": 60,
                "maxWorkspaceBytes": 0
              }
            }
            """.formatted(extraFields);
    }

    private static String validRequestWithoutRevision() {
        return """
            {
              "repositoryUrl": "https://example.invalid/project.git",
              "requestId": "request-1",
              "schemaVersion": "schema-v1",
              "buildContext": {
                "buildTool": "gradle",
                "buildId": "build-1",
                "declaredModules": [":app"],
                "attributes": {}
              },
              "workspacePolicy": {
                "ephemeral": false,
                "allowShallowClone": true,
                "allowPartialClone": false,
                "allowSparseCheckout": false,
                "timeoutSeconds": 60,
                "maxWorkspaceBytes": 0
              }
            }
            """;
    }

    private static String validRequestWithout(String fieldName) {
        return switch (fieldName) {
            case "buildContext" -> """
                {
                  "repositoryUrl": "https://example.invalid/project.git",
                  "branch": "main",
                  "requestId": "request-1",
                  "schemaVersion": "schema-v1",
                  "workspacePolicy": {
                    "ephemeral": false,
                    "allowShallowClone": false,
                    "allowPartialClone": false,
                    "allowSparseCheckout": false,
                    "timeoutSeconds": 60,
                    "maxWorkspaceBytes": 0
                  }
                }
                """;
            case "workspacePolicy" -> """
                {
                  "repositoryUrl": "https://example.invalid/project.git",
                  "branch": "main",
                  "requestId": "request-1",
                  "schemaVersion": "schema-v1",
                  "buildContext": {
                    "buildTool": "gradle",
                    "buildId": "build-1",
                    "declaredModules": [":app"],
                    "attributes": {}
                  }
                }
                """;
            default -> throw new IllegalArgumentException("unsupported test field: " + fieldName);
        };
    }

    private static String validRequestWithRepositoryUrl(String repositoryUrl) {
        return """
            {
              "repositoryUrl": "%s",
              "branch": "main",
              "requestId": "request-1",
              "schemaVersion": "schema-v1",
              "buildContext": {
                "buildTool": "gradle",
                "buildId": "build-1",
                "declaredModules": [":app"],
                "attributes": {}
              },
              "workspacePolicy": {
                "ephemeral": false,
                "allowShallowClone": false,
                "allowPartialClone": false,
                "allowSparseCheckout": false,
                "timeoutSeconds": 60,
                "maxWorkspaceBytes": 0
              }
            }
            """.formatted(repositoryUrl);
    }

    private static String validRequestWithWorkspacePolicy(String policyFields) {
        return """
            {
              "repositoryUrl": "https://example.invalid/project.git",
              "branch": "main",
              "requestId": "request-1",
              "schemaVersion": "schema-v1",
              "buildContext": {
                "buildTool": "gradle",
                "buildId": "build-1",
                "declaredModules": [":app"],
                "attributes": {}
              },
              "workspacePolicy": {
                %s
              }
            }
            """.formatted(policyFields);
    }

    private static String validRequestWithNegativeTimeout() {
        return """
            {
              "repositoryUrl": "https://example.invalid/project.git",
              "branch": "main",
              "requestId": "request-1",
              "schemaVersion": "schema-v1",
              "buildContext": {
                "buildTool": "gradle",
                "buildId": "build-1",
                "declaredModules": [":app"],
                "attributes": {}
              },
              "workspacePolicy": {
                "ephemeral": false,
                "allowShallowClone": false,
                "allowPartialClone": false,
                "allowSparseCheckout": false,
                "timeoutSeconds": -1,
                "maxWorkspaceBytes": 0
              }
            }
            """;
    }

    private static String validRequestWithBuildContext(String replacement) {
        return """
            {
              "repositoryUrl": "https://example.invalid/project.git",
              "branch": "main",
              "requestId": "request-1",
              "schemaVersion": "schema-v1",
              "buildContext": {
                "buildTool": "gradle",
                "buildId": "build-1",
                %s,
                "attributes": {}
              },
              "workspacePolicy": {
                "ephemeral": false,
                "allowShallowClone": false,
                "allowPartialClone": false,
                "allowSparseCheckout": false,
                "timeoutSeconds": 60,
                "maxWorkspaceBytes": 0
              }
            }
            """.formatted(replacement);
    }

    private static String validRequestWithInvalidAttributes() {
        return """
            {
              "repositoryUrl": "https://example.invalid/project.git",
              "branch": "main",
              "requestId": "request-1",
              "schemaVersion": "schema-v1",
              "buildContext": {
                "buildTool": "gradle",
                "buildId": "build-1",
                "declaredModules": [":app"],
                "attributes": {"token": " "}
              },
              "workspacePolicy": {
                "ephemeral": false,
                "allowShallowClone": false,
                "allowPartialClone": false,
                "allowSparseCheckout": false,
                "timeoutSeconds": 60,
                "maxWorkspaceBytes": 0
              }
            }
            """;
    }

    private static String commitOnlyRequest() {
        return """
            {
              "repositoryUrl": "https://example.invalid/project.git",
              "provider": "",
              "commit": "abcdef",
              "requestId": "request-1",
              "schemaVersion": "schema-v1",
              "buildContext": {
                "buildTool": "gradle",
                "buildId": "build-1",
                "declaredModules": [],
                "attributes": {}
              },
              "workspacePolicy": {
                "ephemeral": false,
                "allowShallowClone": false,
                "allowPartialClone": false,
                "allowSparseCheckout": false,
                "timeoutSeconds": 60,
                "maxWorkspaceBytes": 0
              }
            }
            """;
    }

    private static String wildFlyRequest() {
        return """
            {
              "repositoryUrl": "%s",
              "provider": "github",
              "branch": "main",
              "requestId": "wildfly-request-1",
              "schemaVersion": "schema-v1",
              "buildContext": {
                "buildTool": "maven",
                "buildId": "manual-wildfly",
                "rootProjectName": "wildfly",
                "declaredModules": [],
                "attributes": {"scenario": "wildfly-hardening"}
              },
              "workspacePolicy": {
                "ephemeral": false,
                "allowShallowClone": true,
                "allowPartialClone": false,
                "allowSparseCheckout": false,
                "timeoutSeconds": 1200,
                "maxWorkspaceBytes": 0
              }
            }
            """.formatted(WILDFLY_REPOSITORY_URL);
    }

    private static RepositoryAnalysisView view(String analysisRunId, String workspaceId) {
        return new RepositoryAnalysisView(
            new AnalysisRunId(analysisRunId),
            new WorkspaceId(workspaceId),
            "https://example.invalid/project.git",
            Optional.of("main"),
            Optional.empty(),
            "https://example.invalid/project.git",
            "abcdef",
            "CHECKED_OUT",
            AnalysisSessionState.REGISTERED,
            RepositoryAnalysisWorkflow.REPOSITORY_SESSION_REGISTRATION,
            Optional.empty(),
            List.of("src/main/java"),
            List.of("checkout completed")
        );
    }

    private static AnalyzeRepositoryResult result() {
        return new AnalyzeRepositoryResult(
            new AnalysisRunId("analysis-1"),
            new WorkspaceId("workspace-1"),
            new CheckoutResult(
                "https://mirror.example.invalid/project.git",
                Optional.of("main"),
                Optional.empty(),
                "abcdef",
                List.of(new SourceRoot("src/main/java")),
                "CHECKED_OUT",
                List.of("checkout completed")
            ),
            "Repository analysis session registered"
        );
    }

    private static final class RecordingIngestionUseCase implements RepositoryAnalysisIngestionUseCase {
        private AnalyzeRepositoryCommand command;
        private RuntimeException error;

        @Override
        public AnalyzeRepositoryResult analyze(AnalyzeRepositoryCommand command) {
            this.command = command;
            if (error != null) {
                throw error;
            }
            return result();
        }
    }

    private static final class RecordingQueryUseCase implements RepositoryAnalysisQueryUseCase {
        private List<RepositoryAnalysisView> analyses = List.of();
        private List<RepositoryAnalysisWorkspaceView> workspaces = List.of();
        private RuntimeException error;

        @Override
        public List<RepositoryAnalysisView> listRepositoryAnalyses() {
            if (error != null) {
                throw error;
            }
            return analyses;
        }

        @Override
        public Optional<RepositoryAnalysisView> findRepositoryAnalysis(AnalysisRunId analysisRunId) {
            assertNotNull(analysisRunId);
            return analyses.stream()
                .filter(view -> view.analysisRunId().equals(analysisRunId))
                .findFirst();
        }

        @Override
        public List<RepositoryAnalysisWorkspaceView> listWorkspaces() {
            return workspaces;
        }

        @Override
        public Optional<RepositoryAnalysisWorkspaceView> findWorkspace(WorkspaceId workspaceId) {
            assertNotNull(workspaceId);
            return workspaces.stream()
                .filter(view -> view.workspaceId().equals(workspaceId))
                .findFirst();
        }
    }
}
