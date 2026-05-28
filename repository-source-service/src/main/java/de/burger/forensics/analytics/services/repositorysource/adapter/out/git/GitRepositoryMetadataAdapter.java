package de.burger.forensics.analytics.services.repositorysource.adapter.out.git;

import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryMetadataPort;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryMetadataPreviewPolicy;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryMetadataResolution;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.Diagnostic;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryIdentity;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryReference;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceBranchSelector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class GitRepositoryMetadataAdapter implements RepositoryMetadataPort {
    private final GitCommandRunner runner;
    private final Path metadataRoot;
    private final RemoteHostValidator remoteHostValidator;

    public GitRepositoryMetadataAdapter(GitCommandRunner runner, Path metadataRoot) {
        this(runner, metadataRoot, RemoteHostValidator.system());
    }

    GitRepositoryMetadataAdapter(
        GitCommandRunner runner,
        Path metadataRoot,
        RemoteHostValidator remoteHostValidator
    ) {
        this.runner = Objects.requireNonNull(runner, "runner must not be null");
        this.metadataRoot = Objects.requireNonNull(metadataRoot, "metadata root must not be null");
        this.remoteHostValidator = Objects.requireNonNull(remoteHostValidator, "remote host validator must not be null");
    }

    @Override
    public RepositoryMetadataResolution resolveMetadata(
        RepositoryReference repository,
        RepositoryMetadataPreviewPolicy policy
    ) {
        var remoteHost = remoteHostValidator.requirePubliclyRoutable(repository);
        var timeout = Duration.ofSeconds(policy.timeoutSeconds());
        var workingDirectory = metadataRoot.resolve("metadata-" + UUID.randomUUID()).toAbsolutePath().normalize();
        try {
            var defaultBranch = resolveDefaultBranch(workingDirectory, repository, timeout, remoteHost);
            return defaultBranch
                .map(branch -> new RepositoryMetadataResolution(
                    RepositoryIdentity.from(repository, branch.branch()),
                    true,
                    List.of(branch.fallback()
                        ? Diagnostic.info("DEFAULT_BRANCH_FALLBACK", "Repository default branch fallback selected")
                        : Diagnostic.info("DEFAULT_BRANCH_RESOLVED", "Repository default branch resolved"))
                ))
                .orElseGet(() -> new RepositoryMetadataResolution(
                    RepositoryIdentity.from(repository, ""),
                    false,
                    List.of(Diagnostic.error("DEFAULT_BRANCH_UNRESOLVED", "Repository default branch could not be resolved"))
                ));
        } finally {
            deleteBestEffort(workingDirectory);
        }
    }

    private Optional<DefaultBranchCandidate> resolveDefaultBranch(
        Path workingDirectory,
        RepositoryReference repository,
        Duration timeout,
        RemoteHostValidator.ValidatedRemoteHost remoteHost
    ) {
        var head = runner.run(new GitCommand(
            workingDirectory,
            timeout,
            metadataArguments(remoteHost, "ls-remote", "--symref", "--exit-code", "--", repository.remoteUrl(), "HEAD")
        ));
        if (head.exitCode() == 0) {
            var parsed = parseDefaultBranch(head.output());
            if (parsed.isPresent()) {
                return parsed.map(branch -> new DefaultBranchCandidate(branch, false));
            }
        }
        if (branchExists(workingDirectory, repository, timeout, remoteHost, "main")) {
            return Optional.of(new DefaultBranchCandidate("main", true));
        }
        if (branchExists(workingDirectory, repository, timeout, remoteHost, "master")) {
            return Optional.of(new DefaultBranchCandidate("master", true));
        }
        return Optional.empty();
    }

    private boolean branchExists(
        Path workingDirectory,
        RepositoryReference repository,
        Duration timeout,
        RemoteHostValidator.ValidatedRemoteHost remoteHost,
        String branch
    ) {
        var result = runner.run(new GitCommand(
            workingDirectory,
            timeout,
            metadataArguments(remoteHost, "ls-remote", "--exit-code", "--", repository.remoteUrl(), "refs/heads/" + branch)
        ));
        return result.exitCode() == 0;
    }

    private static Optional<String> parseDefaultBranch(String output) {
        return output.lines()
            .map(String::trim)
            .filter(line -> line.startsWith("ref: refs/heads/"))
            .filter(line -> line.endsWith("HEAD"))
            .map(line -> line.substring("ref: refs/heads/".length()).split("\\s+", 2)[0])
            .filter(branch -> !branch.isBlank())
            .filter(GitRepositoryMetadataAdapter::isSafeBranch)
            .findFirst();
    }

    private static boolean isSafeBranch(String branch) {
        try {
            new RepositoryWorkspaceBranchSelector(branch, "").requireBranch();
            return true;
        } catch (IllegalArgumentException error) {
            return false;
        }
    }

    private static List<String> metadataArguments(
        RemoteHostValidator.ValidatedRemoteHost remoteHost,
        String... arguments
    ) {
        var gitArguments = new ArrayList<String>();
        remoteHost.curlResolveOptions().forEach(resolveOption -> {
            gitArguments.add("-c");
            gitArguments.add("http.curloptResolve=" + resolveOption);
        });
        gitArguments.addAll(List.of(arguments));
        return gitArguments;
    }

    private static void deleteBestEffort(Path workingDirectory) {
        if (!Files.exists(workingDirectory)) {
            return;
        }
        try (var stream = Files.walk(workingDirectory)) {
            var paths = stream.sorted(Comparator.reverseOrder()).toList();
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ignored) {
            // Metadata cleanup failures must not expose local paths or hide the lookup result.
        }
    }

    private record DefaultBranchCandidate(String branch, boolean fallback) {
    }
}
