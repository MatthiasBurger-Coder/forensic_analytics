package de.burger.forensics.analytics.services.queryreportapi.adapter.in.http;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayOpenApiContractTest {
    @Test
    void queryReportApiOwnsRepositoryToBtmSubmissionAndStatusContract() throws IOException {
        var contract = Files.readString(findGatewayContract());
        var repositoryListGet = section(contract, "  /repository-analyses:\n    get:", "    post:");
        var repositoryPost = section(contract, "    post:", "  /repository-analyses/{analysisRunId}:");
        var repositoryStatusGet = section(contract, "  /repository-analyses/{analysisRunId}:", "  /repository-analyses/{analysisRunId}/jobs:");
        var idempotencyParameter = section(contract, "    IdempotencyKey:", "    AnalysisRunId:");

        assertContains(repositoryListGet, "operationId: listRepositoryAnalyses");
        assertContains(repositoryListGet, "x-implementation-status: planned-initial");
        assertContains(repositoryPost, "operationId: startRepositoryToBtmAnalysis");
        assertContains(repositoryPost, "x-implementation-status: current-verified");
        assertContains(repositoryPost, "x-cli-contract: contracts/cli/gateway-cli-contract.md");
        assertContains(repositoryPost, "cli-client target public API mode");
        assertContains(repositoryPost, "forensic-analytics-cli gateway-submit predecessor compatibility mode");
        assertContains(repositoryPost, "- $ref: '#/components/parameters/MutationCorrelationId'");
        assertContains(repositoryPost, "- $ref: '#/components/parameters/IdempotencyKey'");
        assertContains(repositoryPost, "'202':");
        assertContains(repositoryPost, "$ref: '#/components/schemas/RepositoryToBtmSubmission'");
        assertContains(repositoryStatusGet, "operationId: getRepositoryAnalysis");
        assertContains(repositoryStatusGet, "x-implementation-status: current-verified");
        assertContains(repositoryStatusGet, "x-cli-contract: contracts/cli/gateway-cli-contract.md");
        assertContains(repositoryStatusGet, "x-cli-contract-status: future-cli-command-required");
        assertContains(repositoryStatusGet, "S09 does not define a CLI status command or response mapping.");
        assertContains(repositoryStatusGet, "- $ref: '#/components/parameters/RequiredCorrelationId'");
        assertContains(repositoryStatusGet, "$ref: '#/components/schemas/RepositoryToBtmStatus'");

        assertContains(idempotencyParameter, "required: true");
        assertContains(idempotencyParameter, "same request fingerprint returns the same accepted response");
        assertContains(idempotencyParameter, "different request fingerprint returns CONFLICT");
        assertContains(contract, "    RequiredCorrelationId:");
        assertContains(section(contract, "    RequiredCorrelationId:", "    MutationCorrelationId:"), "required: true");

        assertContains(contract, "required: [repositoryUrl, requestId, schemaVersion, requestedOutputs, buildContext, workspacePolicy]");
        assertContains(contract, "required: [buildTool, buildId, declaredModules, attributes]");
        assertContains(contract, "$ref: '#/components/schemas/RequestedAnalysisOutput'");
        assertContains(contract, "VALIDATION_ERROR");
        assertContains(contract, "CONFLICT");
        assertContains(contract, "BACKEND_UNAVAILABLE");
        assertContains(contract, "TIMEOUT");
        assertContains(contract, "NOT_FOUND");
        assertContains(contract, "UNEXPECTED_ERROR");
        assertContains(contract, "BTM_RULES");
        assertContains(contract, "RepositoryToBtmSubmission:");
        assertContains(contract, "RepositoryToBtmStatus:");
        assertContains(contract, "PublicDiagnostic:");
        assertContains(contract, "TOKEN|SECRET|PASSWORD|CREDENTIAL|AUTHORIZATION");
        assertContains(contract, "https?://\\S+");
        assertContains(contract, "raw stdout");
        assertContains(contract, "raw stderr");
        assertContains(contract, "btmDeliveryStatus:");
        assertContains(contract, "BtmArtifactDeliveryService");
        assertContains(contract, "BTM_DELIVERY_NOT_READY");
        assertContains(contract, "BTM_DELIVERY_READY");
        assertContains(contract, "BTM_DELIVERY_UNAVAILABLE");
        assertNotContains(contract, "workspaceName:");
        assertNotContains(contract, "RepositoryAnalysis:");
        assertNotContains(contract, "resolvedRemoteUrl:");
    }

    @Test
    void workspaceRoutesArePlannedRepositoryCheckoutContracts() throws IOException {
        var contract = Files.readString(findGatewayContract());
        var metadataPost = section(contract, "  /workspace-metadata:", "  /workspaces:");
        var workspacesRoot = section(contract, "  /workspaces:", "  /workspaces/{workspaceId}:");
        var workspaceListGet = section(workspacesRoot, "    get:", "    post:");
        var workspacePost = workspacesRoot;
        var workspaceById = section(
            contract,
            "  /workspaces/{workspaceId}:",
            "  /workspaces/{workspaceId}/checkout-result:"
        );
        var workspaceGet = section(workspaceById, "    get:", "    delete:");
        var workspaceDelete = workspaceById;
        var workspaceCheckoutResultGet = section(
            contract,
            "  /workspaces/{workspaceId}/checkout-result:",
            "  /workspaces/{workspaceId}/branches/{workspaceBranchId}/refresh:"
        );
        var refreshPost = section(
            contract,
            "  /workspaces/{workspaceId}/branches/{workspaceBranchId}/refresh:",
            "  /repository-analyses:"
        );

        assertContains(contract, "- name: Workspaces");
        assertContains(metadataPost, "operationId: previewRepositoryWorkspaceMetadata");
        assertContains(metadataPost, "x-implementation-status: current-verified");
        assertContains(metadataPost, "enforces the public Idempotency-Key locally");
        assertContains(metadataPost, "configured owner schema version and metadata timeout");
        assertContains(metadataPost, "- $ref: '#/components/parameters/MutationCorrelationId'");
        assertContains(metadataPost, "- $ref: '#/components/parameters/IdempotencyKey'");
        assertContains(metadataPost, "$ref: '#/components/schemas/WorkspaceMetadataRequest'");
        assertContains(metadataPost, "$ref: '#/components/schemas/WorkspaceMetadataResponse'");
        assertContains(metadataPost, "'409':");
        assertContains(metadataPost, "$ref: '#/components/responses/IdempotencyConflict'");

        assertContains(workspaceListGet, "operationId: listRepositoryWorkspaces");
        assertContains(workspaceListGet, "x-implementation-status: planned-initial");
        assertContains(workspaceListGet, "Default responses exclude CLEANED workspaces");
        assertContains(workspaceListGet, "deterministic ordering by workspaceId");
        assertContains(workspaceListGet, "branches[].repositoryBranch");
        assertContains(workspaceListGet, "- $ref: '#/components/parameters/RequiredCorrelationId'");
        assertContains(workspaceListGet, "$ref: '#/components/schemas/RepositoryCheckoutWorkspaceListResponse'");

        assertContains(workspacePost, "operationId: createRepositoryWorkspace");
        assertContains(workspacePost, "x-implementation-status: current-verified");
        assertContains(workspacePost, "repository-source owner gRPC API");
        assertContains(workspacePost, "does not read repository-source storage or workspace paths");
        assertContains(workspacePost, "- $ref: '#/components/parameters/MutationCorrelationId'");
        assertContains(workspacePost, "- $ref: '#/components/parameters/IdempotencyKey'");
        assertContains(workspacePost, "$ref: '#/components/schemas/CreateWorkspaceRequest'");
        assertContains(workspacePost, "$ref: '#/components/schemas/RepositoryCheckoutWorkspaceResponse'");
        assertContains(workspacePost, "'409':");
        assertContains(workspacePost, "$ref: '#/components/responses/IdempotencyConflict'");

        assertContains(workspaceGet, "operationId: getRepositoryWorkspace");
        assertContains(workspaceGet, "x-implementation-status: current-verified");
        assertContains(workspaceGet, "- $ref: '#/components/parameters/RequiredCorrelationId'");
        assertContains(workspaceGet, "- $ref: '#/components/parameters/WorkspaceId'");
        assertContains(workspaceGet, "$ref: '#/components/schemas/RepositoryCheckoutWorkspaceResponse'");

        assertContains(workspaceDelete, "operationId: cleanupRepositoryWorkspace");
        assertContains(workspaceDelete, "x-implementation-status: planned-initial");
        assertContains(workspaceDelete, "safe cleanup");
        assertContains(workspaceDelete, "retains repository-source metadata");
        assertContains(workspaceDelete, "marks the workspace CLEANED");
        assertContains(workspaceDelete, "default list responses hide the cleaned workspace");
        assertContains(workspaceDelete, "- $ref: '#/components/parameters/MutationCorrelationId'");
        assertContains(workspaceDelete, "- $ref: '#/components/parameters/IdempotencyKey'");
        assertContains(workspaceDelete, "- $ref: '#/components/parameters/WorkspaceId'");
        assertContains(workspaceDelete, "$ref: '#/components/schemas/RepositoryCheckoutWorkspaceCleanupResponse'");
        assertContains(workspaceDelete, "'409':");
        assertContains(workspaceDelete, "$ref: '#/components/responses/IdempotencyConflict'");

        assertContains(workspaceCheckoutResultGet, "operationId: waitForRepositoryWorkspaceCheckoutResult");
        assertContains(workspaceCheckoutResultGet, "x-implementation-status: current-verified");
        assertContains(workspaceCheckoutResultGet, "single client request");
        assertContains(workspaceCheckoutResultGet, "CHECKED_OUT or FAILED");
        assertContains(workspaceCheckoutResultGet, "- $ref: '#/components/parameters/RequiredCorrelationId'");
        assertContains(workspaceCheckoutResultGet, "- $ref: '#/components/parameters/WorkspaceId'");
        assertContains(workspaceCheckoutResultGet, "$ref: '#/components/schemas/RepositoryCheckoutWorkspaceResponse'");

        assertContains(refreshPost, "operationId: refreshRepositoryWorkspaceBranch");
        assertContains(refreshPost, "x-implementation-status: current-verified");
        assertContains(refreshPost, "has no request body");
        assertContains(refreshPost, "configured bounded refresh workspace policy");
        assertContains(refreshPost, "- $ref: '#/components/parameters/MutationCorrelationId'");
        assertContains(refreshPost, "- $ref: '#/components/parameters/IdempotencyKey'");
        assertContains(refreshPost, "- $ref: '#/components/parameters/WorkspaceId'");
        assertContains(refreshPost, "- $ref: '#/components/parameters/WorkspaceBranchId'");
        assertContains(refreshPost, "$ref: '#/components/schemas/RepositoryCheckoutBranchRefreshResponse'");
        assertContains(refreshPost, "'409':");
        assertContains(refreshPost, "$ref: '#/components/responses/IdempotencyConflict'");

        assertContains(contract, "    WorkspaceMetadataRequest:");
        assertContains(contract, "    WorkspaceMetadataResponse:");
        assertContains(contract, "    CreateWorkspaceRequest:");
        assertContains(contract, "    RepositoryIdentity:");
        assertContains(contract, "    RepositoryCheckoutWorkspaceResponse:");
        assertContains(contract, "    RepositoryCheckoutWorkspaceListResponse:");
        assertContains(contract, "    RepositoryCheckoutWorkspaceCleanupResponse:");
        assertContains(contract, "    RepositoryCheckoutWorkspaceBranchResponse:");
        assertContains(contract, "    RepositoryCheckoutBranchRefreshResponse:");
        assertContains(contract, "    RepositoryCheckoutWorkspaceStatus:");
        assertContains(contract, "    RepositoryCheckoutBranchStatus:");
        assertContains(contract, "workspaceId:");
        assertContains(contract, "workspaceBranchId:");
        assertContains(contract, "sourceSnapshotId:");
        assertContains(contract, "CHECKED_OUT");
        assertContains(contract, "CLEANED");
        assertContains(contract, "UP_TO_DATE");
        assertContains(contract, "UPDATED");
        assertContains(contract, "The idempotency key was already used with different input.");
        assertContains(contract, "DNS resolution must reject every A/AAAA result");
        assertEquals(6, countOccurrences(contract, "- $ref: '#/components/parameters/IdempotencyKey'"));
        assertEquals(6, countOccurrences(contract, "$ref: '#/components/responses/IdempotencyConflict'"));
        assertNotContains(contract, "workspaceName:");
        assertNotContains(contract, "resolvedRemoteUrl:");
        assertNotContains(contract, "rawStdout");
        assertNotContains(contract, "rawStderr");
        assertNotContains(contract, "filesystemPath");
        assertNotContains(contract, "h2Path");
    }

    @Test
    void repositoryUrlSchemaRejectsPrivateAndSpecialUseTargets() throws IOException {
        var contract = Files.readString(findGatewayContract());
        var repositoryUrlSchema = section(contract, "    HttpsRepositoryUrl:", "    WorkspaceMetadataRequest:");
        var startRepositoryAnalysisRequest = section(contract, "    StartRepositoryAnalysisRequest:", "    BuildContext:");
        var repositoryUrlPattern = Pattern.compile(singleQuotedPattern(repositoryUrlSchema));

        assertContains(repositoryUrlSchema, "DNS resolution must reject every A/AAAA result");
        assertContains(repositoryUrlSchema, "Query strings, fragments, userinfo, local names, SSH and SCP-style remotes are forbidden.");
        assertNotContains(repositoryUrlSchema, "(?i");
        assertContains(startRepositoryAnalysisRequest, "$ref: '#/components/schemas/HttpsRepositoryUrl'");
        assertTrue(repositoryUrlPattern.matcher("https://github.com/wildfly/wildfly.git").matches());
        List.of(
            "http://github.com/wildfly/wildfly.git",
            "git@github.com:wildfly/wildfly.git",
            "https://user@example.com/repo.git",
            "https://example.com/repo.git?token=x",
            "https://example.com/repo.git#main",
            "https://localhost/repo.git",
            "https://localhost./repo.git",
            "https://127.0.0.1./repo.git",
            "https://10.0.0.1/repo.git",
            "https://100.64.0.1/repo.git",
            "https://169.254.169.254./repo.git",
            "https://192.0.2.1/repo.git",
            "https://198.18.0.1/repo.git",
            "https://198.51.100.1/repo.git",
            "https://192.31.196.1/repo.git",
            "https://192.31.196.1./repo.git",
            "https://192.52.193.1/repo.git",
            "https://192.52.193.1./repo.git",
            "https://192.175.48.1/repo.git",
            "https://192.175.48.1./repo.git",
            "https://203.0.113.1/repo.git",
            "https://224.0.0.1/repo.git",
            "https://example.test/repo.git",
            "https://example.local/repo.git",
            "https://example.invalid./repo.git",
            "https://example.example./repo.git",
            "https://[0000:0000:0000:0000:0000:0000:0000:0001]/repo.git",
            "https://[2001:db8::1]/repo.git",
            "https://[2001:0db8::1]/repo.git",
            "https://[0064:ff9b::1]/repo.git",
            "https://[64:ff9b:1::1]/repo.git",
            "https://[100:0:0:1::1]/repo.git",
            "https://[3fff::1]/repo.git",
            "https://[5f00::1]/repo.git",
            "https://[ff00::1]/repo.git"
        ).forEach(repositoryUrl -> assertFalse(
            repositoryUrlPattern.matcher(repositoryUrl).matches(),
            () -> "Repository URL schema accepted unsafe target: " + repositoryUrl
        ));
    }

    @Test
    void publicErrorsAndDiagnosticsUseSafePublicMessages() throws IOException {
        var contract = Files.readString(findGatewayContract());
        var dependencyStatus = section(contract, "    DependencyStatus:", "    Health:");
        var publicDiagnostic = section(contract, "    PublicDiagnostic:", "    Diagnostic:");
        var errorEnvelope = section(contract, "    ErrorEnvelope:", "    AnalysisWorkerKind:");

        assertContains(contract, "    SafePublicMessage:");
        assertContains(contract, "file:|jdbc:|h2|https?://\\S+");
        assertContains(contract, "repository-source-data");
        assertContains(dependencyStatus, "$ref: '#/components/schemas/SafePublicMessage'");
        assertContains(publicDiagnostic, "$ref: '#/components/schemas/SafePublicMessage'");
        assertContains(errorEnvelope, "$ref: '#/components/schemas/SafePublicMessage'");
    }

    @Test
    void contractRootDocumentsQueryReportApiAsTargetAuthority() throws IOException {
        var readme = Files.readString(findRepositoryRoot().resolve("contracts/openapi/README.md"));

        assertContains(readme, "`query-report-api-service`");
        assertContains(readme, "target authority");
        assertContains(readme, "service-local executable OpenAPI contract test");
    }

    private static Path findGatewayContract() {
        return findRepositoryRoot().resolve("contracts/openapi/gateway-api.yaml");
    }

    private static Path findRepositoryRoot() {
        var current = Path.of("").toAbsolutePath();
        while (current != null) {
            var candidate = current.resolve("settings.gradle.kts");
            if (Files.isRegularFile(candidate)) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("repository root not found from test working directory");
    }

    private static String section(String content, String startMarker, String endMarker) {
        var start = content.indexOf(startMarker);
        var end = content.indexOf(endMarker, start + startMarker.length());
        if (start < 0 || end < 0) {
            throw new AssertionError("Cannot find section from " + startMarker + " to " + endMarker);
        }
        return content.substring(start, end);
    }

    private static void assertContains(String content, String expected) {
        assertTrue(content.contains(expected), () -> "Expected contract content to contain: " + expected);
    }

    private static void assertNotContains(String content, String unexpected) {
        assertFalse(content.contains(unexpected), () -> "Expected contract content to not contain: " + unexpected);
    }

    private static String singleQuotedPattern(String content) {
        var marker = "      pattern: '";
        var start = content.indexOf(marker);
        if (start < 0) {
            throw new AssertionError("Cannot find schema pattern");
        }
        var valueStart = start + marker.length();
        var end = content.indexOf("'", valueStart);
        if (end < 0) {
            throw new AssertionError("Cannot find schema pattern end");
        }
        return content.substring(valueStart, end);
    }

    private static int countOccurrences(String content, String expected) {
        var count = 0;
        var index = content.indexOf(expected);
        while (index >= 0) {
            count++;
            index = content.indexOf(expected, index + expected.length());
        }
        return count;
    }
}
