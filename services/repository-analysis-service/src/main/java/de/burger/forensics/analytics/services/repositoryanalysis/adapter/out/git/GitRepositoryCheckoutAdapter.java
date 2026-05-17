package de.burger.forensics.analytics.services.repositoryanalysis.adapter.out.git;

import de.burger.forensics.analytics.services.repositoryanalysis.application.port.PreparedWorkspace;
import de.burger.forensics.analytics.services.repositoryanalysis.application.port.RepositoryCheckoutPort;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.CheckoutResult;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.CheckoutStatus;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.Diagnostic;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RepositoryReference;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RevisionSelector;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.WorkspacePolicy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class GitRepositoryCheckoutAdapter implements RepositoryCheckoutPort {
    private final GitCommandRunner runner;
    private final SourceRootDetector sourceRootDetector;

    public GitRepositoryCheckoutAdapter(GitCommandRunner runner, SourceRootDetector sourceRootDetector) {
        this.runner = Objects.requireNonNull(runner, "runner must not be null");
        this.sourceRootDetector = Objects.requireNonNull(sourceRootDetector, "source root detector must not be null");
    }

    @Override
    public CheckoutResult checkout(
        PreparedWorkspace workspace,
        RepositoryReference repository,
        RevisionSelector revision,
        WorkspacePolicy policy
    ) {
        var started = Instant.now();
        var repoPath = workspace.workspacePath().resolve("repository").toAbsolutePath().normalize();
        var timeout = Duration.ofSeconds(policy.timeoutSeconds());
        var cloneArguments = cloneArguments(repository, revision, policy, repoPath);
        runOrThrow(workspace.workspacePath(), timeout, cloneArguments);
        if (revision.hasBranch() && !policy.allowShallowClone()) {
            runOrThrow(repoPath, timeout, List.of("checkout", "--quiet", revision.branch()));
        }
        if (revision.hasCommit()) {
            runOrThrow(repoPath, timeout, List.of("rev-parse", "--verify", revision.commit() + "^{commit}"));
            if (revision.hasBranch()) {
                runOrThrow(repoPath, timeout, List.of(
                    "merge-base",
                    "--is-ancestor",
                    revision.commit(),
                    "refs/remotes/origin/" + revision.branch()
                ));
            }
            runOrThrow(repoPath, timeout, List.of("checkout", "--quiet", "--detach", revision.commit()));
        }
        var resolvedCommit = runOrThrow(repoPath, timeout, List.of("rev-parse", "HEAD")).trimmedOutput();
        var resolvedRemote = runOrThrow(repoPath, timeout, List.of("remote", "get-url", "origin")).trimmedOutput();
        enforceWorkspaceQuota(workspace.workspacePath(), policy.maxWorkspaceBytes());
        return new CheckoutResult(
            CheckoutStatus.CHECKED_OUT,
            resolvedRemote,
            resolvedCommit,
            revision.branch(),
            revision.commit(),
            policy.allowShallowClone() && revision.hasBranch() && !revision.hasCommit(),
            Duration.between(started, Instant.now()).toMillis(),
            List.of(Diagnostic.info("GIT_CHECKOUT_COMPLETED", "Repository checkout completed")),
            false,
            false,
            sourceRootDetector.detect(repoPath)
        );
    }

    private GitCommandResult runOrThrow(Path workingDirectory, Duration timeout, List<String> arguments) {
        var result = runner.run(new GitCommand(workingDirectory, timeout, arguments));
        if (result.exitCode() != 0) {
            throw new IllegalStateException("Git command failed with exit code " + result.exitCode());
        }
        return result;
    }

    private static void enforceWorkspaceQuota(Path repositoryRoot, long maxWorkspaceBytes) {
        var total = workspaceSize(repositoryRoot, maxWorkspaceBytes);
        if (total > maxWorkspaceBytes) {
            throw new IllegalStateException("Workspace byte quota exceeded");
        }
    }

    private static long workspaceSize(Path repositoryRoot, long maxWorkspaceBytes) {
        try (var stream = Files.walk(repositoryRoot)) {
            var files = stream
                .filter(Files::isRegularFile)
                .toList();
            long total = 0;
            for (Path file : files) {
                total += Files.size(file);
                if (total > maxWorkspaceBytes) {
                    return total;
                }
            }
            return total;
        } catch (IOException error) {
            throw new IllegalStateException("Failed to measure workspace byte quota");
        }
    }

    private static List<String> cloneArguments(
        RepositoryReference repository,
        RevisionSelector revision,
        WorkspacePolicy policy,
        Path repoPath
    ) {
        var arguments = new ArrayList<String>();
        arguments.add("clone");
        arguments.add("--quiet");
        arguments.add("--no-tags");
        if (policy.allowShallowClone() && revision.hasBranch() && !revision.hasCommit()) {
            arguments.add("--depth");
            arguments.add("1");
            arguments.add("--branch");
            arguments.add(revision.branch());
            arguments.add("--single-branch");
        }
        arguments.add("--");
        arguments.add(repository.remoteUrl());
        arguments.add(repoPath.toString());
        return arguments;
    }
}
