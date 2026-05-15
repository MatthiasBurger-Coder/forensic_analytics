package de.burger.forensics.analytics.bootstrap;

import de.burger.forensics.analytics.application.ingestion.command.RepositoryCheckoutRequest;
import de.burger.forensics.analytics.application.ingestion.command.WorkspacePreparationRequest;
import de.burger.forensics.analytics.application.ingestion.port.RepositoryCheckoutPort;
import de.burger.forensics.analytics.application.ingestion.port.WorkspacePreparationPort;
import de.burger.forensics.analytics.domain.repository.CheckoutResult;
import de.burger.forensics.analytics.domain.workspace.PreparedWorkspace;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceLease;
import de.burger.forensics.analytics.domain.workspace.WorkspacePath;
import de.burger.forensics.analytics.domain.workspace.WorkspacePreparationStatus;
import de.burger.forensics.analytics.persistence.InMemoryAnalysisSessionRepository;
import de.burger.forensics.analytics.persistence.InMemoryIngestionSessionRepository;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RestApiServerBootstrapFactoryTest {
    @Test
    void createsRestServerWithSharedBackendComponentsWithoutStartingIt() throws Exception {
        var port = availablePort();
        var components = ForensicAnalyticsBackendComponents.create(
            new InMemoryIngestionSessionRepository(),
            new InMemoryAnalysisSessionRepository(),
            new RecordingWorkspacePreparationPort(),
            new RecordingRepositoryCheckoutPort()
        );
        var server = new RestApiServerBootstrapFactory().create(
            new RestApiServerSettings(true, "127.0.0.1", port),
            components
        );
        try {
            assertEquals(port, server.port());
        } finally {
            server.stop();
        }
    }

    private static int availablePort() throws Exception {
        try (var socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static final class RecordingWorkspacePreparationPort implements WorkspacePreparationPort {
        @Override
        public PreparedWorkspace prepare(WorkspacePreparationRequest request) {
            return new PreparedWorkspace(
                new WorkspaceId("workspace-1"),
                WorkspacePreparationStatus.READY,
                new WorkspacePath("/tmp/workspace-1"),
                new WorkspaceLease(
                    request.analysisSessionId().value(),
                    Instant.parse("2026-05-14T12:00:00Z"),
                    Optional.empty(),
                    WorkspacePreparationStatus.READY
                ),
                List.of("Workspace created")
            );
        }

        @Override
        public PreparedWorkspace cleanup(PreparedWorkspace workspace) {
            return workspace;
        }
    }

    private static final class RecordingRepositoryCheckoutPort implements RepositoryCheckoutPort {
        @Override
        public CheckoutResult checkout(RepositoryCheckoutRequest request) {
            return new CheckoutResult(
                request.repository().remoteUrl(),
                request.branch().name(),
                request.commit().hash(),
                "abcdef",
                List.of(),
                "CHECKED_OUT",
                List.of()
            );
        }
    }
}
