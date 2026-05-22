package de.burger.forensics.analytics.services.repositorysource.adapter.out.git;

import de.burger.forensics.analytics.services.repositorysource.application.port.PreparedWorkspace;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.CheckoutStatus;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryReference;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RevisionSelector;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspacePolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class GitRepositoryCheckoutAdapterTest {
    private static final RepositoryReference REPOSITORY = new RepositoryReference(
        "https://example.com/acme/demo.git",
        "github",
        Map.of()
    );
    private static final WorkspacePolicy POLICY = new WorkspacePolicy(true, true, false, false, 60, 100_000);

    @TempDir
    private Path workspaceRoot;

    @Test
    void checksOutBranchAndReportsOnlyRelativeSourceRoots() throws Exception {
        var runner = new FakeRunner();
        var adapter = checkoutAdapter(runner);

        var checkout = adapter.checkout(
            new PreparedWorkspace(new WorkspaceId("workspace-1"), workspaceRoot),
            REPOSITORY,
            new RevisionSelector("main", true, "", false),
            POLICY
        );

        assertEquals(CheckoutStatus.CHECKED_OUT, checkout.status());
        assertEquals("b".repeat(40), checkout.resolvedCommit());
        assertEquals("src/main/java", checkout.sourceRoots().getFirst().relativePath());
        assertTrue(runner.commands.stream().anyMatch(command -> command.arguments().contains("--depth")));
        assertTrue(runner.commands.stream().anyMatch(command ->
            command.arguments().contains("http.curloptResolve=example.com:443:93.184.216.34")
        ));
        assertTrue(runner.commands.stream().anyMatch(command ->
            command.arguments().stream().anyMatch(argument ->
                argument.startsWith("http.curloptResolve=example.com:443:[2001:4860:4860")
            )
        ));
        assertTrue(runner.commands.stream().noneMatch(command -> command.arguments().contains("submodule")));
    }

    @Test
    void verifiesBranchCommitReachabilityAndSurfacesGitFailures() {
        var runner = new FakeRunner();
        var adapter = checkoutAdapter(runner);

        adapter.checkout(
            new PreparedWorkspace(new WorkspaceId("workspace-1"), workspaceRoot),
            REPOSITORY,
            new RevisionSelector("main", true, "abcdef1", true),
            new WorkspacePolicy(true, false, false, false, 60, 100_000)
        );

        assertTrue(runner.commands.stream().anyMatch(command -> command.arguments().contains("merge-base")));

        var failing = checkoutAdapter(new FailingRunner());
        var failure = assertThrows(IllegalStateException.class, () -> failing.checkout(
            new PreparedWorkspace(new WorkspaceId("workspace-2"), workspaceRoot.resolve("other")),
            REPOSITORY,
            new RevisionSelector("main", true, "", false),
            POLICY
        ));
        assertEquals("Git command failed with exit code 1", failure.getMessage());
        assertFalse(failure.getMessage().contains("workspace"));
    }

    @Test
    void supportsFullBranchAndCommitOnlyCheckoutModes() {
        var fullBranchRunner = new FakeRunner();
        var adapter = checkoutAdapter(fullBranchRunner);

        var fullBranch = adapter.checkout(
            new PreparedWorkspace(new WorkspaceId("workspace-full"), workspaceRoot.resolve("full")),
            REPOSITORY,
            new RevisionSelector("main", true, "", false),
            new WorkspacePolicy(true, false, false, false, 60, 100_000)
        );
        var commitRunner = new FakeRunner();
        var commitOnly = checkoutAdapter(commitRunner).checkout(
            new PreparedWorkspace(new WorkspaceId("workspace-commit"), workspaceRoot.resolve("commit")),
            REPOSITORY,
            new RevisionSelector("", false, "abcdef1", true),
            new WorkspacePolicy(true, false, false, false, 60, 100_000)
        );

        assertFalse(fullBranch.shallowClone());
        assertTrue(fullBranchRunner.commands.stream().anyMatch(command -> command.arguments().equals(List.of("checkout", "--quiet", "main"))));
        assertFalse(commitOnly.shallowClone());
        assertTrue(commitRunner.commands.stream().noneMatch(command -> command.arguments().contains("merge-base")));
        assertTrue(commitRunner.commands.stream().anyMatch(command -> command.arguments().equals(List.of("checkout", "--quiet", "--detach", "abcdef1"))));
    }

    @Test
    void enforcesWorkspaceByteQuotaAfterCheckout() {
        var adapter = checkoutAdapter(new LargeRepositoryRunner());

        var failure = assertThrows(IllegalStateException.class, () -> adapter.checkout(
            new PreparedWorkspace(new WorkspaceId("workspace-1"), workspaceRoot),
            REPOSITORY,
            new RevisionSelector("main", true, "", false),
            new WorkspacePolicy(true, true, false, false, 60, 1)
        ));

        assertEquals("Workspace byte quota exceeded", failure.getMessage());
    }

    @Test
    void includesGitHomeInWorkspaceByteQuota() throws Exception {
        Files.writeString(workspaceRoot.resolve(".repository-source-git-home"), "0123456789");
        var adapter = checkoutAdapter(new FakeRunner());

        var failure = assertThrows(IllegalStateException.class, () -> adapter.checkout(
            new PreparedWorkspace(new WorkspaceId("workspace-1"), workspaceRoot),
            REPOSITORY,
            new RevisionSelector("main", true, "", false),
            new WorkspacePolicy(true, true, false, false, 60, 5)
        ));

        assertEquals("Workspace byte quota exceeded", failure.getMessage());
    }

    @Test
    void gitHomeIsAlwaysOwnedByPreparedWorkspace() {
        var repositoryPath = workspaceRoot.resolve("repository");

        assertEquals(
            workspaceRoot.resolve(".repository-source-git-home").toAbsolutePath().normalize(),
            SafeGitCommandRunner.isolatedGitHome(workspaceRoot)
        );
        assertEquals(
            workspaceRoot.resolve(".repository-source-git-home").toAbsolutePath().normalize(),
            SafeGitCommandRunner.isolatedGitHome(repositoryPath)
        );
    }

    @Test
    void sourceRootDetectorFallsBackToOpaqueRootWhenJavaLayoutIsAbsent() throws Exception {
        var repositoryRoot = Files.createDirectory(workspaceRoot.resolve("repository"));

        var roots = new SourceRootDetector().detect(repositoryRoot);

        assertEquals(".", roots.getFirst().relativePath());
        assertEquals("unknown", roots.getFirst().language());
    }

    @Test
    void sourceRootDetectorReturnsDeterministicRelativeRootsAndIgnoresGeneratedDirectories() throws Exception {
        var repositoryRoot = Files.createDirectory(workspaceRoot.resolve("repository"));
        Files.createDirectories(repositoryRoot.resolve("module-b/src/main/java"));
        Files.createDirectories(repositoryRoot.resolve("module-a/src/main/java"));
        Files.createDirectories(repositoryRoot.resolve(".git/src/main/java"));
        Files.createDirectories(repositoryRoot.resolve(".gradle/src/main/java"));
        Files.createDirectories(repositoryRoot.resolve(".idea/src/main/java"));
        Files.createDirectories(repositoryRoot.resolve("build/src/main/java"));
        Files.createDirectories(repositoryRoot.resolve("target/src/main/java"));

        var roots = new SourceRootDetector().detect(repositoryRoot);

        assertEquals(List.of("module-a/src/main/java", "module-b/src/main/java"), roots.stream()
            .map(de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourceRoot::relativePath)
            .toList());
        assertTrue(roots.stream().allMatch(root -> !Path.of(root.relativePath()).isAbsolute()));
    }

    @Test
    void sourceRootDetectorIgnoresSymlinkedJavaRoots() throws Exception {
        var repositoryRoot = Files.createDirectory(workspaceRoot.resolve("repository"));
        var external = Files.createDirectory(workspaceRoot.resolve("external-java"));
        Files.createDirectories(repositoryRoot.resolve("src/main"));
        try {
            Files.createSymbolicLink(repositoryRoot.resolve("src/main/java"), external);
        } catch (UnsupportedOperationException | java.io.IOException error) {
            assumeTrue(false, "symbolic links are not available in this filesystem");
        }

        var roots = new SourceRootDetector().detect(repositoryRoot);

        assertEquals(".", roots.getFirst().relativePath());
        assertEquals("unknown", roots.getFirst().language());
    }

    @Test
    void rejectsRemotesThatResolveToPrivateAddressesBeforeGitRuns() throws Exception {
        var runner = new FakeRunner();
        var adapter = new GitRepositoryCheckoutAdapter(
            runner,
            new SourceRootDetector(),
            new RemoteHostValidator(host -> List.of(InetAddress.getByName("127.0.0.1")))
        );

        var failure = assertThrows(IllegalArgumentException.class, () -> adapter.checkout(
            new PreparedWorkspace(new WorkspaceId("workspace-1"), workspaceRoot),
            REPOSITORY,
            new RevisionSelector("main", true, "", false),
            POLICY
        ));

        assertEquals("repository remote host must resolve to public addresses only", failure.getMessage());
        assertTrue(runner.commands.isEmpty());
    }

    @Test
    void rejectsUnresolvedEmptyAndSpecialUseRemoteAddressesBeforeGitRuns() throws Exception {
        assertRemoteResolutionRejected(
            host -> List.of(),
            "repository remote host must resolve to public addresses only"
        );
        assertRemoteResolutionRejected(
            host -> {
                throw new UnknownHostException(host);
            },
            "repository remote host could not be resolved safely"
        );
        assertRemoteResolutionRejected(
            host -> List.of(InetAddress.getByName("100.64.0.1")),
            "repository remote host must resolve to public addresses only"
        );
        assertRemoteResolutionRejected(
            host -> List.of(InetAddress.getByName("0.0.0.0")),
            "repository remote host must resolve to public addresses only"
        );
        assertRemoteResolutionRejected(
            host -> List.of(InetAddress.getByName("169.254.1.1")),
            "repository remote host must resolve to public addresses only"
        );
        assertRemoteResolutionRejected(
            host -> List.of(InetAddress.getByName("192.168.1.1")),
            "repository remote host must resolve to public addresses only"
        );
        assertRemoteResolutionRejected(
            host -> List.of(InetAddress.getByName("fc00::1")),
            "repository remote host must resolve to public addresses only"
        );
        assertRemoteResolutionRejected(
            host -> List.of(InetAddress.getByName("224.0.0.1")),
            "repository remote host must resolve to public addresses only"
        );
    }

    @Test
    void rejectsInvalidCommandConstructionAndNormalizesNullOutput() {
        assertThrows(IllegalArgumentException.class, () -> new GitCommand(workspaceRoot, java.time.Duration.ofSeconds(1), List.of()));
        assertEquals("", new GitCommandResult(0, null).output());
    }

    private static GitRepositoryCheckoutAdapter checkoutAdapter(GitCommandRunner runner) {
        return new GitRepositoryCheckoutAdapter(
            runner,
            new SourceRootDetector(),
            new RemoteHostValidator(host -> List.of(
                InetAddress.getByName("93.184.216.34"),
                InetAddress.getByName("2001:4860:4860::8888")
            ))
        );
    }

    private void assertRemoteResolutionRejected(RemoteHostResolver resolver, String expectedMessage) {
        var runner = new FakeRunner();
        var adapter = new GitRepositoryCheckoutAdapter(
            runner,
            new SourceRootDetector(),
            new RemoteHostValidator(resolver)
        );

        var failure = assertThrows(IllegalArgumentException.class, () -> adapter.checkout(
            new PreparedWorkspace(new WorkspaceId("workspace-1"), workspaceRoot),
            REPOSITORY,
            new RevisionSelector("main", true, "", false),
            POLICY
        ));

        assertEquals(expectedMessage, failure.getMessage());
        assertTrue(runner.commands.isEmpty());
    }

    private static final class FakeRunner implements GitCommandRunner {
        private final List<GitCommand> commands = new ArrayList<>();

        @Override
        public GitCommandResult run(GitCommand command) {
            commands.add(command);
            if (command.arguments().contains("clone")) {
                var repoPath = Path.of(command.arguments().getLast());
                try {
                    Files.createDirectories(repoPath.resolve("src/main/java"));
                } catch (Exception error) {
                    throw new IllegalStateException(error);
                }
                return new GitCommandResult(0, "");
            }
            if (command.arguments().equals(List.of("rev-parse", "HEAD"))) {
                return new GitCommandResult(0, "b".repeat(40));
            }
            if (command.arguments().equals(List.of("remote", "get-url", "origin"))) {
                return new GitCommandResult(0, REPOSITORY.remoteUrl());
            }
            return new GitCommandResult(0, "");
        }
    }

    private static final class FailingRunner implements GitCommandRunner {
        @Override
        public GitCommandResult run(GitCommand command) {
            return new GitCommandResult(1, "network unavailable");
        }
    }

    private static final class LargeRepositoryRunner implements GitCommandRunner {
        @Override
        public GitCommandResult run(GitCommand command) {
            if (command.arguments().contains("clone")) {
                var repoPath = Path.of(command.arguments().getLast());
                try {
                    Files.createDirectories(repoPath.resolve("src/main/java"));
                    Files.writeString(repoPath.resolve("src/main/java/Large.java"), "0123456789");
                } catch (Exception error) {
                    throw new IllegalStateException(error);
                }
                return new GitCommandResult(0, "");
            }
            if (command.arguments().equals(List.of("rev-parse", "HEAD"))) {
                return new GitCommandResult(0, "b".repeat(40));
            }
            if (command.arguments().equals(List.of("remote", "get-url", "origin"))) {
                return new GitCommandResult(0, REPOSITORY.remoteUrl());
            }
            return new GitCommandResult(0, "");
        }
    }
}
