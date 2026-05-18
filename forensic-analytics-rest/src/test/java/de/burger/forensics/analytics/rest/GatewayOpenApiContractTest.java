package de.burger.forensics.analytics.rest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayOpenApiContractTest {
    @Test
    void gatewayContractDefinesAsyncRepositoryToBtmSubmissionAndRequiredMutationIdempotency() throws IOException {
        var contract = Files.readString(findGatewayContract());
        var repositoryPost = section(contract, "  /repository-analyses:", "  /repository-analyses/{analysisRunId}:");
        var idempotencyParameter = section(contract, "    IdempotencyKey:", "    AnalysisRunId:");

        assertContains(repositoryPost, "operationId: startRepositoryToBtmAnalysis");
        assertContains(repositoryPost, "x-implementation-status: planned-initial");
        assertContains(repositoryPost, "- $ref: '#/components/parameters/MutationCorrelationId'");
        assertContains(repositoryPost, "- $ref: '#/components/parameters/IdempotencyKey'");
        assertContains(repositoryPost, "'202':");
        assertContains(repositoryPost, "$ref: '#/components/schemas/RepositoryToBtmSubmission'");

        assertContains(idempotencyParameter, "required: true");
        assertContains(idempotencyParameter, "same request fingerprint returns the same accepted response");
        assertContains(idempotencyParameter, "different request fingerprint returns CONFLICT");

        assertContains(contract, "required: [repositoryUrl, requestId, schemaVersion, requestedOutputs, buildContext, workspacePolicy]");
        assertContains(contract, "$ref: '#/components/schemas/RequestedAnalysisOutput'");
        assertContains(contract, "BTM_RULES");
        assertContains(contract, "RepositoryToBtmSubmission:");
        assertContains(contract, "btmDeliveryStatus:");
        assertContains(contract, "BtmArtifactDeliveryService");
        assertContains(contract, "BTM_DELIVERY_NOT_READY");
        assertContains(contract, "BTM_DELIVERY_READY");
        assertContains(contract, "BTM_DELIVERY_UNAVAILABLE");
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
}
