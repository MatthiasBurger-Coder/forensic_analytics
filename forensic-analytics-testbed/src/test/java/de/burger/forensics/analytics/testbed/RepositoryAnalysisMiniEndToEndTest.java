package de.burger.forensics.analytics.testbed;

import de.burger.forensics.analytics.adapter.repository.source.FileSystemWorkspacePreparationAdapter;
import de.burger.forensics.analytics.adapter.repository.source.GitRepositoryCheckoutAdapter;
import de.burger.forensics.analytics.application.ingestion.DefaultForensicIngestionUseCase;
import de.burger.forensics.analytics.application.ingestion.DefaultRepositoryAnalysisIngestionUseCase;
import de.burger.forensics.analytics.application.ingestion.command.WorkspacePreparationRequest;
import de.burger.forensics.analytics.application.ingestion.port.WorkspacePreparationPort;
import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.analysis.AnalysisSessionState;
import de.burger.forensics.analytics.domain.workspace.PreparedWorkspace;
import de.burger.forensics.analytics.ingestion.grpc.ForensicIngestionGrpcService;
import de.burger.forensics.analytics.ingestion.v1.AnalyzeRepositoryRequest;
import de.burger.forensics.analytics.ingestion.v1.BranchReference;
import de.burger.forensics.analytics.ingestion.v1.BuildContext;
import de.burger.forensics.analytics.ingestion.v1.CommitReference;
import de.burger.forensics.analytics.ingestion.v1.ForensicIngestionServiceGrpc;
import de.burger.forensics.analytics.ingestion.v1.RepositoryReference;
import de.burger.forensics.analytics.ingestion.v1.WorkspacePolicy;
import de.burger.forensics.analytics.persistence.InMemoryAnalysisSessionRepository;
import de.burger.forensics.analytics.persistence.InMemoryIngestionSessionRepository;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryAnalysisMiniEndToEndTest {
    @TempDir
    Path tempDir;

    @Test
    void pluginStyleGrpcRequestCreatesSessionChecksOutRepositoryAndCleansWorkspace() throws Exception {
        var sourceRepository = createSyntheticGitRepository();
        var commitHash = git(sourceRepository, "rev-parse", "HEAD");
        var workspacePort = new RecordingWorkspacePreparationPort(tempDir.resolve("workspaces"));
        var analysisSessionRepository = new InMemoryAnalysisSessionRepository();
        var service = new ForensicIngestionGrpcService(
            new DefaultForensicIngestionUseCase(new InMemoryIngestionSessionRepository()),
            new DefaultRepositoryAnalysisIngestionUseCase(
                workspacePort,
                new GitRepositoryCheckoutAdapter(),
                analysisSessionRepository
            )
        );
        var serverName = InProcessServerBuilder.generateName();
        Server server = InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(service)
            .build()
            .start();
        ManagedChannel channel = InProcessChannelBuilder.forName(serverName)
            .directExecutor()
            .build();

        try {
            var response = ForensicIngestionServiceGrpc.newBlockingStub(channel)
                .analyzeRepository(request(sourceRepository, commitHash));

            assertFalse(response.getAnalysisSessionId().getValue().isBlank());
            assertFalse(response.getWorkspaceId().getValue().isBlank());
            assertEquals("CHECKED_OUT", response.getCheckoutResult().getCheckoutStatus());
            assertEquals("main", response.getCheckoutResult().getRequestedBranch());
            assertEquals(commitHash, response.getCheckoutResult().getRequestedCommit());
            assertEquals(commitHash, response.getCheckoutResult().getResolvedCommit());
            assertEquals(List.of("checkout mode: full clone"), response.getCheckoutResult().getDiagnosticsList());
            assertEquals(1, response.getCheckoutResult().getDetectedSourceRootsCount());
            assertTrue(response.getCheckoutResult().getDetectedSourceRoots(0).endsWith("repository/src/main/java"));

            var storedSession = analysisSessionRepository.findById(
                new AnalysisRunId(response.getAnalysisSessionId().getValue())
            ).orElseThrow();
            assertEquals("mini-e2e-request", storedSession.requestId());
            assertEquals(response.getWorkspaceId().getValue(), storedSession.workspaceId().value());
            assertEquals(AnalysisSessionState.REGISTERED, storedSession.state());
            assertEquals(commitHash, storedSession.checkoutResult().resolvedCommit());

            var cleaned = workspacePort.cleanup(workspacePort.preparedWorkspace());

            assertEquals(response.getWorkspaceId().getValue(), cleaned.workspaceId().value());
            assertTrue(Files.notExists(Path.of(cleaned.path().value())));
        } finally {
            channel.shutdownNow();
            server.shutdownNow();
        }
    }

    private AnalyzeRepositoryRequest request(Path sourceRepository, String commitHash) {
        return AnalyzeRepositoryRequest.newBuilder()
            .setRepository(RepositoryReference.newBuilder()
                .setRemoteUrl(sourceRepository.toUri().toString())
                .setProvider("local")
                .putAttributes("fixture", "synthetic-mini-repository"))
            .setBranch(BranchReference.newBuilder()
                .setName("main")
                .setRequired(true))
            .setCommit(CommitReference.newBuilder()
                .setHash(commitHash)
                .setRequired(false))
            .setWorkspacePolicy(WorkspacePolicy.newBuilder()
                .setEphemeral(true)
                .setAllowShallowClone(false)
                .setAllowPartialClone(false)
                .setAllowSparseCheckout(false)
                .setTimeoutSeconds(30)
                .setMaxWorkspaceBytes(0))
            .setBuildContext(BuildContext.newBuilder()
                .setBuildTool("gradle")
                .setBuildId("mini-e2e-build")
                .setRootProjectName("mini-project")
                .addDeclaredModules(":")
                .putAttributes("fixture", "synthetic"))
            .setRequestId("mini-e2e-request")
            .setSchemaVersion("workspace-grpc-v1")
            .build();
    }

    private Path createSyntheticGitRepository() throws IOException, InterruptedException {
        var repository = Files.createDirectories(tempDir.resolve("synthetic-mini-repository"));
        var sourceRoot = Files.createDirectories(repository.resolve("src/main/java/com/example"));
        Files.writeString(
            sourceRoot.resolve("MiniApp.java"),
            """
            package com.example;

            final class MiniApp {
                String value() {
                    return "mini";
                }
            }
            """,
            StandardCharsets.UTF_8
        );
        git(repository, "init");
        git(repository, "config", "user.email", "forensic-analytics@example.invalid");
        git(repository, "config", "user.name", "Forensic Analytics Test");
        git(repository, "add", ".");
        git(repository, "commit", "-m", "initial synthetic fixture");
        git(repository, "branch", "-M", "main");
        return repository;
    }

    private static String git(Path workingDirectory, String... arguments) throws IOException, InterruptedException {
        var command = new java.util.ArrayList<String>();
        command.add("git");
        command.addAll(List.of(arguments));
        var process = new ProcessBuilder(command)
            .directory(workingDirectory.toFile())
            .redirectErrorStream(true)
            .start();
        var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        var exitCode = process.waitFor();
        assertEquals(0, exitCode, output);
        return output.strip();
    }

    private static final class RecordingWorkspacePreparationPort implements WorkspacePreparationPort {
        private final WorkspacePreparationPort delegate;
        private PreparedWorkspace preparedWorkspace;

        private RecordingWorkspacePreparationPort(Path workspaceRoot) {
            this.delegate = new FileSystemWorkspacePreparationAdapter(workspaceRoot);
        }

        @Override
        public PreparedWorkspace prepare(WorkspacePreparationRequest request) {
            preparedWorkspace = delegate.prepare(request);
            return preparedWorkspace;
        }

        @Override
        public PreparedWorkspace cleanup(PreparedWorkspace workspace) {
            return delegate.cleanup(workspace);
        }

        private PreparedWorkspace preparedWorkspace() {
            return preparedWorkspace;
        }
    }
}
