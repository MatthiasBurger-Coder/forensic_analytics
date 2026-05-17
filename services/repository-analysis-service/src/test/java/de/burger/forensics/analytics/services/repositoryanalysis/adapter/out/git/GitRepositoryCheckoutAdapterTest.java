package de.burger.forensics.analytics.services.repositoryanalysis.adapter.out.git;

import de.burger.forensics.analytics.services.repositoryanalysis.application.port.PreparedWorkspace;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.CheckoutStatus;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RepositoryReference;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RevisionSelector;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.WorkspaceId;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.WorkspacePolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        var adapter = new GitRepositoryCheckoutAdapter(runner, new SourceRootDetector());

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
        assertTrue(runner.commands.stream().noneMatch(command -> command.arguments().contains("submodule")));
    }

    @Test
    void verifiesBranchCommitReachabilityAndSurfacesGitFailures() {
        var runner = new FakeRunner();
        var adapter = new GitRepositoryCheckoutAdapter(runner, new SourceRootDetector());

        adapter.checkout(
            new PreparedWorkspace(new WorkspaceId("workspace-1"), workspaceRoot),
            REPOSITORY,
            new RevisionSelector("main", true, "abcdef1", true),
            new WorkspacePolicy(true, false, false, false, 60, 100_000)
        );

        assertTrue(runner.commands.stream().anyMatch(command -> command.arguments().contains("merge-base")));

        var failing = new GitRepositoryCheckoutAdapter(new FailingRunner(), new SourceRootDetector());
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
        var adapter = new GitRepositoryCheckoutAdapter(fullBranchRunner, new SourceRootDetector());

        var fullBranch = adapter.checkout(
            new PreparedWorkspace(new WorkspaceId("workspace-full"), workspaceRoot.resolve("full")),
            REPOSITORY,
            new RevisionSelector("main", true, "", false),
            new WorkspacePolicy(true, false, false, false, 60, 100_000)
        );
        var commitRunner = new FakeRunner();
        var commitOnly = new GitRepositoryCheckoutAdapter(commitRunner, new SourceRootDetector()).checkout(
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
        var adapter = new GitRepositoryCheckoutAdapter(new LargeRepositoryRunner(), new SourceRootDetector());

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
        Files.writeString(workspaceRoot.resolve(".git-home"), "0123456789");
        var adapter = new GitRepositoryCheckoutAdapter(new FakeRunner(), new SourceRootDetector());

        var failure = assertThrows(IllegalStateException.class, () -> adapter.checkout(
            new PreparedWorkspace(new WorkspaceId("workspace-1"), workspaceRoot),
            REPOSITORY,
            new RevisionSelector("main", true, "", false),
            new WorkspacePolicy(true, true, false, false, 60, 5)
        ));

        assertEquals("Workspace byte quota exceeded", failure.getMessage());
    }

    @Test
    void sourceRootDetectorFallsBackToOpaqueRootWhenJavaLayoutIsAbsent() throws Exception {
        var repositoryRoot = Files.createDirectory(workspaceRoot.resolve("repository"));

        var roots = new SourceRootDetector().detect(repositoryRoot);

        assertEquals(".", roots.getFirst().relativePath());
        assertEquals("unknown", roots.getFirst().language());
    }

    @Test
    void rejectsInvalidCommandConstructionAndNormalizesNullOutput() {
        assertThrows(IllegalArgumentException.class, () -> new GitCommand(workspaceRoot, java.time.Duration.ofSeconds(1), List.of()));
        assertEquals("", new GitCommandResult(0, null).output());
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
