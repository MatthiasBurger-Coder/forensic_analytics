package de.burger.forensics.analytics.rest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayOpenApiContractTest {
    @Test
    void gatewayContractDefinesAsyncRepositoryToBtmSubmissionAndRequiredMutationIdempotency() throws IOException {
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
        assertContains(repositoryStatusGet, "S11 does not define a CLI status command or response mapping.");
        assertContains(repositoryStatusGet, "- $ref: '#/components/parameters/RequiredCorrelationId'");
        assertContains(repositoryStatusGet, "$ref: '#/components/schemas/RepositoryToBtmStatus'");

        assertContains(idempotencyParameter, "required: true");
        assertContains(idempotencyParameter, "same request fingerprint returns the same accepted response");
        assertContains(idempotencyParameter, "different request fingerprint returns CONFLICT");
        assertContains(contract, "    RequiredCorrelationId:");
        assertContains(section(contract, "    RequiredCorrelationId:", "    MutationCorrelationId:"), "required: true");

        assertContains(contract, "required: [repositoryUrl, requestId, schemaVersion, requestedOutputs, buildContext, workspacePolicy]");
        assertContains(contract, "$ref: '#/components/schemas/RequestedAnalysisOutput'");
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
        assertNotContains(contract, "  /workspaces:");
        assertNotContains(contract, "  /workspaces/{workspaceId}:");
        assertNotContains(contract, "workspaceId:");
        assertNotContains(contract, "workspaceName:");
        assertNotContains(contract, "WorkspaceList:");
        assertNotContains(contract, "    Workspace:");
        assertNotContains(contract, "RepositoryAnalysis:");
        assertNotContains(contract, "resolvedRemoteUrl:");
    }

    private static Path findGatewayContract() {
        var current = Path.of("").toAbsolutePath();
        while (current != null) {
            var candidate = current.resolve("contracts/openapi/gateway-api.yaml");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("contracts/openapi/gateway-api.yaml not found from test working directory");
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
}
