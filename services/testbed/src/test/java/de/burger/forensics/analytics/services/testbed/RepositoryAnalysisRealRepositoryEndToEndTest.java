package de.burger.forensics.analytics.services.testbed;

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
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class RepositoryAnalysisRealRepositoryEndToEndTest {
    private static final String FIXED_GIT_DATE = "2026-05-21T00:00:00Z";

    @TempDir
    Path tempDir;

    @Test
    void pluginStyleGrpcRequestChecksOutRealRepositoryFixtureAndCleansWorkspace() throws Exception {
        assumeTrue(gitIsAvailable(), "git executable is required for repository E2E checkout tests");
        var sourceRepository = createRealGitRepository();
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
            assertFalse(response.getWorkspaceId().getValue().contains(tempDir.toString()));
            assertEquals("CHECKED_OUT", response.getCheckoutResult().getCheckoutStatus());
            assertEquals("main", response.getCheckoutResult().getRequestedBranch());
            assertEquals(commitHash, response.getCheckoutResult().getRequestedCommit());
            assertEquals(commitHash, response.getCheckoutResult().getResolvedCommit());
            assertEquals(List.of("checkout mode: full clone"), response.getCheckoutResult().getDiagnosticsList());
            assertEquals(2, response.getCheckoutResult().getDetectedSourceRootsCount());
            assertTrue(response.getCheckoutResult().getDetectedSourceRootsList().stream()
                .anyMatch(root -> root.endsWith("repository/service-a/src/main/java")));
            assertTrue(response.getCheckoutResult().getDetectedSourceRootsList().stream()
                .anyMatch(root -> root.endsWith("repository/service-b/src/main/java")));
            assertFalse(Files.exists(Path.of(workspacePort.preparedWorkspace().path().value())
                .resolve("repository")
                .resolve("build-was-run")));

            var storedSession = analysisSessionRepository.findById(
                new AnalysisRunId(response.getAnalysisSessionId().getValue())
            ).orElseThrow();
            assertEquals("real-repository-e2e-request", storedSession.requestId());
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
                .putAttributes("fixture", "real-repository-template"))
            .setBranch(BranchReference.newBuilder()
                .setName("main")
                .setRequired(true))
            .setCommit(CommitReference.newBuilder()
                .setHash(commitHash)
                .setRequired(true))
            .setWorkspacePolicy(WorkspacePolicy.newBuilder()
                .setEphemeral(true)
                .setAllowShallowClone(false)
                .setAllowPartialClone(false)
                .setAllowSparseCheckout(false)
                .setTimeoutSeconds(30)
                .setMaxWorkspaceBytes(0))
            .setBuildContext(BuildContext.newBuilder()
                .setBuildTool("gradle")
                .setBuildId("real-repository-e2e-build")
                .setRootProjectName("forensic-real-e2e")
                .addDeclaredModules(":service-a")
                .addDeclaredModules(":service-b")
                .putAllAttributes(Map.of("fixture", "real-repository-template", "network", "none")))
            .setRequestId("real-repository-e2e-request")
            .setSchemaVersion("workspace-grpc-v1")
            .build();
    }

    private Path createRealGitRepository() throws IOException, InterruptedException, URISyntaxException {
        var repository = Files.createDirectories(tempDir.resolve("real-repository"));
        copyFixture(repository);
        git(repository, "init");
        git(repository, "checkout", "-B", "main");
        git(repository, "config", "user.email", "forensic-analytics@example.invalid");
        git(repository, "config", "user.name", "Forensic Analytics Test");
        git(repository, "config", "core.autocrlf", "false");
        git(repository, "config", "core.eol", "lf");
        git(repository, "add", ".");
        gitWithFixedMetadata(repository, "commit", "-m", "Add real repository e2e fixture");
        return repository;
    }

    private void copyFixture(Path targetDirectory) throws IOException, URISyntaxException {
        var fixtureUri = Objects.requireNonNull(
            getClass().getResource("/repository-e2e/real-repository-template"),
            "real repository fixture resource must exist"
        ).toURI();
        var fixtureDirectory = Path.of(fixtureUri);
        try (var paths = Files.find(fixtureDirectory, Integer.MAX_VALUE, (path, attributes) -> true)) {
            for (var source : paths.toList()) {
                var relative = fixtureDirectory.relativize(source);
                var target = targetDirectory.resolve(relative.toString()).normalize();
                if (!target.startsWith(targetDirectory)) {
                    throw new IOException("Fixture copy target escapes repository directory: " + target);
                }
                var attributes = Files.readAttributes(source, BasicFileAttributes.class);
                if (attributes.isDirectory()) {
                    Files.createDirectories(target);
                } else if (attributes.isRegularFile()) {
                    Files.createDirectories(target.getParent());
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static boolean gitIsAvailable() {
        try {
            var process = new ProcessBuilder("git", "--version")
                .redirectErrorStream(true)
                .start();
            return process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static String git(Path workingDirectory, String... arguments) throws IOException, InterruptedException {
        return runGit(workingDirectory, Map.of(), arguments);
    }

    private static String gitWithFixedMetadata(Path workingDirectory, String... arguments)
        throws IOException, InterruptedException {
        return runGit(workingDirectory, Map.of(
            "GIT_AUTHOR_DATE", FIXED_GIT_DATE,
            "GIT_COMMITTER_DATE", FIXED_GIT_DATE
        ), arguments);
    }

    private static String runGit(Path workingDirectory, Map<String, String> environment, String... arguments)
        throws IOException, InterruptedException {
        var command = new java.util.ArrayList<String>();
        command.add("git");
        command.addAll(List.of(arguments));
        var processBuilder = new ProcessBuilder(command)
            .directory(workingDirectory.toFile())
            .redirectErrorStream(true);
        processBuilder.environment().putAll(environment);
        var process = processBuilder.start();
        var completed = process.waitFor(30, TimeUnit.SECONDS);
        var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(completed, () -> "git command timed out: " + command);
        assertEquals(0, process.exitValue(), () -> "git command failed: " + command + System.lineSeparator() + output);
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
