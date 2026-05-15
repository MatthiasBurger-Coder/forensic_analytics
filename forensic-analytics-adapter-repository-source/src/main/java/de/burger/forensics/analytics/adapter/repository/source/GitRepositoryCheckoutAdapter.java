package de.burger.forensics.analytics.adapter.repository.source;

import de.burger.forensics.analytics.application.ingestion.RepositoryCheckoutException;
import de.burger.forensics.analytics.application.ingestion.command.RepositoryCheckoutRequest;
import de.burger.forensics.analytics.application.ingestion.port.RepositoryCheckoutPort;
import de.burger.forensics.analytics.domain.repository.CheckoutResult;
import de.burger.forensics.analytics.domain.repository.SourceRoot;
import de.burger.forensics.analytics.observability.OperationLogger;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public final class GitRepositoryCheckoutAdapter implements RepositoryCheckoutPort {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);
    private static final Set<String> SUPPORTED_URI_SCHEMES = Set.of("file", "http", "https");
    private static final Pattern URI_SCHEME = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*:.*");
    private static final Pattern SCP_STYLE_REMOTE = Pattern.compile("^[^/\\\\]+@[^:]+:.+");
    private final GitCommandRunner commandRunner;
    private final OperationLogger operationLogger;

    public GitRepositoryCheckoutAdapter() {
        this(new GitCommandRunner(), OperationLogger.system(GitRepositoryCheckoutAdapter.class));
    }

    GitRepositoryCheckoutAdapter(GitCommandRunner commandRunner) {
        this(commandRunner, OperationLogger.system(GitRepositoryCheckoutAdapter.class));
    }

    GitRepositoryCheckoutAdapter(GitCommandRunner commandRunner, OperationLogger operationLogger) {
        this.commandRunner = Objects.requireNonNull(commandRunner, "commandRunner must not be null");
        this.operationLogger = Objects.requireNonNull(operationLogger, "operationLogger must not be null");
    }

    @Override
    public CheckoutResult checkout(RepositoryCheckoutRequest request) {
        var verifiedRequest = Objects.requireNonNull(request, "request must not be null");
        return operationLogger.logged("adapter.repository-source.git-checkout", () -> checkoutVerified(verifiedRequest));
    }

    private CheckoutResult checkoutVerified(RepositoryCheckoutRequest request) {
        var workspacePath = Path.of(request.workspace().path().value()).toAbsolutePath().normalize();
        var repositoryDirectory = workspacePath.resolve("repository").normalize();
        var timeout = commandTimeout(request);
        var diagnostics = new ArrayList<String>();

        requireCheckoutWorkspace(workspacePath, repositoryDirectory);
        requireSupportedRepositoryUrl(request.repository().remoteUrl());
        request.branch().name().ifPresent(branch -> requireSafeGitReference(branch, "branch"));
        request.commit().hash().ifPresent(commit -> requireSafeGitReference(commit, "commit"));

        var shallowBranchHeadClone = shallowBranchHeadClone(request);
        commandRunner.run(workspacePath, timeout, cloneCommand(request, repositoryDirectory, shallowBranchHeadClone));
        if (!shallowBranchHeadClone) {
            request.branch().name().ifPresent(branch -> commandRunner.run(repositoryDirectory, timeout, List.of(
                "git",
                "-c",
                "core.hooksPath=/dev/null",
                "checkout",
                "--quiet",
                "--force",
                branch
            )));
        }
        request.commit().hash().ifPresent(commit -> commandRunner.run(repositoryDirectory, timeout, List.of(
            "git",
            "-c",
            "core.hooksPath=/dev/null",
            "checkout",
            "--quiet",
            "--force",
            commit
        )));

        var resolvedCommit = commandRunner.run(repositoryDirectory, timeout, git("rev-parse", "HEAD")).trimmedOutput();
        request.commit().hash().ifPresent(commit -> verifyRequestedCommit(repositoryDirectory, timeout, commit, resolvedCommit));
        var resolvedRemoteUrl = commandRunner.run(
            repositoryDirectory,
            timeout,
            git("remote", "get-url", "origin")
        ).trimmedOutput();
        var sourceRoots = SourceRootDetector.sourceRoots(repositoryDirectory).stream()
            .map(SourceRoot::new)
            .toList();
        var checkoutMode = shallowBranchHeadClone
            ? "checkout mode: shallow branch head clone"
            : "checkout mode: full clone";
        diagnostics.add(checkoutMode);

        return new CheckoutResult(
            resolvedRemoteUrl,
            request.branch().name(),
            request.commit().hash(),
            resolvedCommit,
            sourceRoots,
            "CHECKED_OUT",
            diagnostics
        );
    }

    private static void requireCheckoutWorkspace(Path workspacePath, Path repositoryDirectory) {
        if (!Files.isDirectory(workspacePath)) {
            throw new RepositoryCheckoutException("Workspace path must point to an existing directory");
        }
        if (!repositoryDirectory.startsWith(workspacePath)) {
            throw new RepositoryCheckoutException("Repository checkout path escapes the workspace");
        }
        if (Files.exists(repositoryDirectory)) {
            throw new RepositoryCheckoutException("Repository checkout directory already exists");
        }
    }

    private static void requireSupportedRepositoryUrl(String remoteUrl) {
        if (SCP_STYLE_REMOTE.matcher(remoteUrl).matches()) {
            throw new RepositoryCheckoutException("SCP-style repository URLs are not supported");
        }
        if (!URI_SCHEME.matcher(remoteUrl).matches()) {
            return;
        }
        var uri = URI.create(remoteUrl);
        if (uri.getUserInfo() != null) {
            throw new RepositoryCheckoutException("Repository URL must not include user information");
        }
        if (!SUPPORTED_URI_SCHEMES.contains(uri.getScheme())) {
            throw new RepositoryCheckoutException("Unsupported repository URL scheme: " + uri.getScheme());
        }
    }

    private static void requireSafeGitReference(String value, String fieldName) {
        if (value.startsWith("-")) {
            throw new RepositoryCheckoutException(fieldName + " reference must not start with an option prefix");
        }
    }

    private void verifyRequestedCommit(Path repositoryDirectory, Duration timeout, String requestedCommit, String resolvedCommit) {
        var expectedCommit = commandRunner.run(
            repositoryDirectory,
            timeout,
            git("rev-parse", "--verify", requestedCommit + "^{commit}")
        ).trimmedOutput();
        if (!resolvedCommit.equals(expectedCommit)) {
            throw new RepositoryCheckoutException("Resolved checkout commit does not match requested commit");
        }
    }

    private static boolean shallowBranchHeadClone(RepositoryCheckoutRequest request) {
        return request.workspacePolicy().allowShallowClone()
            && request.branch().name().isPresent()
            && request.commit().hash().isEmpty();
    }

    private static List<String> cloneCommand(
        RepositoryCheckoutRequest request,
        Path repositoryDirectory,
        boolean shallowBranchHeadClone
    ) {
        var command = new ArrayList<String>();
        command.add("git");
        command.add("-c");
        command.add("core.hooksPath=/dev/null");
        command.add("clone");
        command.add("--quiet");
        command.add("--no-tags");
        if (shallowBranchHeadClone) {
            command.add("--depth");
            command.add("1");
            command.add("--branch");
            command.add(request.branch().name().orElseThrow());
            command.add("--single-branch");
        }
        command.add("--");
        command.add(request.repository().remoteUrl());
        command.add(repositoryDirectory.toString());
        return List.copyOf(command);
    }

    private static List<String> git(String... arguments) {
        var command = new ArrayList<String>();
        command.add("git");
        command.add("-c");
        command.add("core.hooksPath=/dev/null");
        command.addAll(Arrays.asList(arguments));
        return List.copyOf(command);
    }

    private static Duration commandTimeout(RepositoryCheckoutRequest request) {
        var configured = request.workspace().lease().expiresAt()
            .map(expiresAt -> Duration.between(java.time.Instant.now(), expiresAt))
            .orElse(DEFAULT_TIMEOUT);
        if (configured.isNegative() || configured.isZero()) {
            return DEFAULT_TIMEOUT;
        }
        return configured;
    }
}
