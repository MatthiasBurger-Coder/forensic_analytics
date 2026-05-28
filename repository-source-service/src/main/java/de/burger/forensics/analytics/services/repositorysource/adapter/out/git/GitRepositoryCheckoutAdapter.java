package de.burger.forensics.analytics.services.repositorysource.adapter.out.git;

import de.burger.forensics.analytics.services.repositorysource.application.port.PreparedWorkspace;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryCheckoutPort;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.CheckoutResult;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.CheckoutStatus;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.Diagnostic;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryReference;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RevisionSelector;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspacePolicy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class GitRepositoryCheckoutAdapter implements RepositoryCheckoutPort {
    private final GitCommandRunner runner;
    private final SourceRootDetector sourceRootDetector;
    private final RemoteHostValidator remoteHostValidator;

    public GitRepositoryCheckoutAdapter(GitCommandRunner runner, SourceRootDetector sourceRootDetector) {
        this(runner, sourceRootDetector, RemoteHostValidator.system());
    }

    GitRepositoryCheckoutAdapter(
        GitCommandRunner runner,
        SourceRootDetector sourceRootDetector,
        RemoteHostValidator remoteHostValidator
    ) {
        this.runner = Objects.requireNonNull(runner, "runner must not be null");
        this.sourceRootDetector = Objects.requireNonNull(sourceRootDetector, "source root detector must not be null");
        this.remoteHostValidator = Objects.requireNonNull(remoteHostValidator, "remote host validator must not be null");
    }

    @Override
    public CheckoutResult checkout(
        PreparedWorkspace workspace,
        RepositoryReference repository,
        RevisionSelector revision,
        WorkspacePolicy policy
    ) {
        var startedNanos = System.nanoTime();
        var repoPath = workspace.workspacePath().resolve("checkout").toAbsolutePath().normalize();
        var timeout = Duration.ofSeconds(policy.timeoutSeconds());
        var remoteHost = remoteHostValidator.requirePubliclyRoutable(repository);
        var existingCheckout = Files.isDirectory(repoPath.resolve(".git"));
        if (existingCheckout) {
            runOrThrow(repoPath, timeout, fetchArguments(repository, revision, remoteHost));
        } else {
            runOrThrow(workspace.workspacePath(), timeout, cloneArguments(repository, revision, policy, repoPath, remoteHost));
        }
        if (revision.hasBranch() && (existingCheckout || !policy.allowShallowClone())) {
            var branchRef = existingCheckout ? "refs/remotes/origin/" + revision.branch() : revision.branch();
            runOrThrow(repoPath, timeout, List.of("checkout", "--quiet", "--no-recurse-submodules", branchRef));
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
            runOrThrow(repoPath, timeout, List.of("checkout", "--quiet", "--no-recurse-submodules", "--detach", revision.commit()));
        }
        var resolvedCommit = runOrThrow(repoPath, timeout, List.of("rev-parse", "HEAD")).trimmedOutput();
        enforceWorkspaceQuota(workspace.workspacePath(), policy.maxWorkspaceBytes());
        return new CheckoutResult(
            CheckoutStatus.CHECKED_OUT,
            repository.remoteUrl(),
            resolvedCommit,
            revision.branch(),
            revision.commit(),
            policy.allowShallowClone() && revision.hasBranch() && !revision.hasCommit(),
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos),
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
        Path repoPath,
        RemoteHostValidator.ValidatedRemoteHost remoteHost
    ) {
        var arguments = new ArrayList<String>();
        remoteHost.curlResolveOptions().forEach(resolveOption -> {
            arguments.add("-c");
            arguments.add("http.curloptResolve=" + resolveOption);
        });
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

    private static List<String> fetchArguments(
        RepositoryReference repository,
        RevisionSelector revision,
        RemoteHostValidator.ValidatedRemoteHost remoteHost
    ) {
        var arguments = new ArrayList<String>();
        remoteHost.curlResolveOptions().forEach(resolveOption -> {
            arguments.add("-c");
            arguments.add("http.curloptResolve=" + resolveOption);
        });
        arguments.add("fetch");
        arguments.add("--quiet");
        arguments.add("--no-tags");
        arguments.add("--prune");
        arguments.add("--no-recurse-submodules");
        arguments.add(repository.remoteUrl());
        if (revision.hasBranch()) {
            arguments.add("+refs/heads/" + revision.branch() + ":refs/remotes/origin/" + revision.branch());
        }
        return arguments;
    }
}
