package de.burger.forensics.analytics.services.cliclient.domain;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CliClientSubmissionCommandTest {
    @Test
    void acceptsCommitOnlyAndPreservesTrailingGatewaySlash() {
        var command = command(
            URI.create("https://gateway.example/api/"),
            "https://example.com/acme/demo.git",
            "",
            "abc123",
            List.of("BTM_RULES"),
            List.of(":app"),
            3_600L,
            107_374_182_400L
        );

        assertEquals("", command.branch());
        assertEquals("abc123", command.commit());
        assertEquals(URI.create("https://gateway.example/api/repository-analyses"), command.repositoryAnalysesUri());
        assertEquals(3_600L, command.timeout().toSeconds());
    }

    @Test
    void rejectsMissingCollectionEvidence() {
        assertValidation("gateway-submit requires at least one --requested-outputs value.", () -> command(
            URI.create("http://gateway.example/api"),
            "https://example.com/acme/demo.git",
            "main",
            "",
            List.of(),
            List.of(":app"),
            60L,
            100_000L
        ));
        assertValidation("gateway-submit requires at least one --declared-modules value.", () -> command(
            URI.create("http://gateway.example/api"),
            "https://example.com/acme/demo.git",
            "main",
            "",
            List.of("BTM_RULES"),
            List.of(),
            60L,
            100_000L
        ));
    }

    @Test
    void rejectsUnsafeGatewayAndRepositoryUris() {
        assertValidation("gateway-submit --gateway must be an http or https URL.", () -> command(
            URI.create("file:///tmp/api"),
            "https://example.com/acme/demo.git",
            "main",
            "",
            List.of("BTM_RULES"),
            List.of(":app"),
            60L,
            100_000L
        ));
        assertValidation("gateway-submit --gateway must include a host.", () -> command(
            URI.create("http:///api"),
            "https://example.com/acme/demo.git",
            "main",
            "",
            List.of("BTM_RULES"),
            List.of(":app"),
            60L,
            100_000L
        ));
        assertValidation("gateway-submit --gateway must not include user info, query or fragment.", () -> command(
            URI.create("https://user@gateway.example/api"),
            "https://example.com/acme/demo.git",
            "main",
            "",
            List.of("BTM_RULES"),
            List.of(":app"),
            60L,
            100_000L
        ));
        assertValidation("Invalid gateway-submit --repo-url.", () -> command(
            URI.create("http://gateway.example/api"),
            "https://[invalid",
            "main",
            "",
            List.of("BTM_RULES"),
            List.of(":app"),
            60L,
            100_000L
        ));
        assertValidation("gateway-submit --repo-url must not include user information.", () -> command(
            URI.create("http://gateway.example/api"),
            "https://token@example.com/acme/demo.git",
            "main",
            "",
            List.of("BTM_RULES"),
            List.of(":app"),
            60L,
            100_000L
        ));
    }

    @Test
    void rejectsBlankRequiredEvidenceAndUpperBounds() {
        assertValidation("gateway-submit requires --request-id.", () -> command(
            URI.create("http://gateway.example/api"),
            "https://example.com/acme/demo.git",
            "main",
            "",
            List.of("BTM_RULES"),
            List.of(":app"),
            60L,
            100_000L,
            " "
        ));
        assertValidation("gateway-submit --timeout-seconds must be between 1 and 3600.", () -> command(
            URI.create("http://gateway.example/api"),
            "https://example.com/acme/demo.git",
            "main",
            "",
            List.of("BTM_RULES"),
            List.of(":app"),
            3_601L,
            100_000L
        ));
        assertValidation("gateway-submit --max-workspace-bytes must be between 1 and 107374182400.", () -> command(
            URI.create("http://gateway.example/api"),
            "https://example.com/acme/demo.git",
            "main",
            "",
            List.of("BTM_RULES"),
            List.of(":app"),
            60L,
            107_374_182_401L
        ));
    }

    private static void assertValidation(String expectedMessage, ThrowingRunnable runnable) {
        var error = assertThrows(CliClientValidationException.class, runnable::run);
        assertEquals(expectedMessage, error.getMessage());
    }

    private static CliClientSubmissionCommand command(
        URI gateway,
        String repositoryUrl,
        String branch,
        String commit,
        List<String> requestedOutputs,
        List<String> declaredModules,
        long timeoutSeconds,
        long maxWorkspaceBytes
    ) {
        return command(gateway, repositoryUrl, branch, commit, requestedOutputs, declaredModules, timeoutSeconds, maxWorkspaceBytes, "request-1");
    }

    private static CliClientSubmissionCommand command(
        URI gateway,
        String repositoryUrl,
        String branch,
        String commit,
        List<String> requestedOutputs,
        List<String> declaredModules,
        long timeoutSeconds,
        long maxWorkspaceBytes,
        String requestId
    ) {
        return new CliClientSubmissionCommand(
            gateway,
            repositoryUrl,
            branch,
            commit,
            requestId,
            "gateway.v1",
            requestedOutputs,
            "github",
            "gradle",
            "build-1",
            "demo",
            declaredModules,
            "correlation-1",
            "idem-1",
            timeoutSeconds,
            maxWorkspaceBytes,
            true
        );
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run();
    }
}
