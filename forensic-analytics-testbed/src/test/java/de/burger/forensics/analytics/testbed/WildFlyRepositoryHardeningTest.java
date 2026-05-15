package de.burger.forensics.analytics.testbed;

import de.burger.forensics.analytics.adapter.repository.source.FileSystemWorkspacePreparationAdapter;
import de.burger.forensics.analytics.adapter.repository.source.LocalRepositorySourceAdapter;
import de.burger.forensics.analytics.application.ingestion.command.WorkspacePreparationRequest;
import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.repository.RepositoryMetadata;
import de.burger.forensics.analytics.domain.workspace.WorkspaceCleanupPolicy;
import de.burger.forensics.analytics.domain.workspace.WorkspacePolicy;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class WildFlyRepositoryHardeningTest {
    private static final String WILDFLY_REMOTE_URL = "https://github.com/wildfly/wildfly.git";
    private static final String HARDENING_ENABLED = "FORENSIC_ANALYTICS_WILDFLY_HARDENING";
    private static final String HARDENING_BRANCH = "FORENSIC_ANALYTICS_WILDFLY_BRANCH";
    private static final String HARDENING_COMMIT = "FORENSIC_ANALYTICS_WILDFLY_COMMIT";
    private static final String HARDENING_TIMEOUT_SECONDS = "FORENSIC_ANALYTICS_WILDFLY_TIMEOUT_SECONDS";
    private static final String HARDENING_MIN_FREE_BYTES = "FORENSIC_ANALYTICS_WILDFLY_MIN_FREE_BYTES";
    private static final String HARDENING_REPORT_DIR = "FORENSIC_ANALYTICS_WILDFLY_REPORT_DIR";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(20);
    private static final long DEFAULT_MIN_FREE_BYTES = 5L * 1024L * 1024L * 1024L;

    @TempDir
    Path tempDir;

    @Test
    @Tag("hardening")
    void clonesChecksOutMeasuresSourceRootsAndCleansWildFlyWhenExplicitlyEnabled() throws Exception {
        assumeTrue(
            "true".equalsIgnoreCase(env(HARDENING_ENABLED).orElse("")),
            "Set " + HARDENING_ENABLED + "=true to run the external WildFly hardening scenario."
        );
        var target = hardeningTarget();
        var timeout = configuredDuration();
        var minimumFreeBytes = configuredLong(HARDENING_MIN_FREE_BYTES, DEFAULT_MIN_FREE_BYTES);
        var workspaceRoot = Files.createDirectories(tempDir.resolve("wildfly-workspaces"));
        assumeTrue(
            Files.getFileStore(workspaceRoot).getUsableSpace() >= minimumFreeBytes,
            () -> "WildFly hardening requires at least " + minimumFreeBytes + " bytes of free workspace disk."
        );

        var metrics = new LinkedHashMap<String, String>();
        metrics.put("repositoryUrl", WILDFLY_REMOTE_URL);
        metrics.put("requestedBranch", target.branch().orElse("UNSPECIFIED"));
        metrics.put("requestedCommit", target.commit().orElse("UNSPECIFIED"));
        metrics.put("timeoutSeconds", String.valueOf(timeout.toSeconds()));
        metrics.put("minimumFreeBytes", String.valueOf(minimumFreeBytes));

        var workspaceAdapter = new FileSystemWorkspacePreparationAdapter(workspaceRoot);
        var workspace = measured(metrics, "workspacePreparationDurationMillis", () -> workspaceAdapter.prepare(
            new WorkspacePreparationRequest(
                AnalysisRunId.deterministic("wildfly-hardening|" + target.identity()),
                new WorkspacePolicy(
                    true,
                    false,
                    false,
                    false,
                    timeout,
                    0L,
                    WorkspaceCleanupPolicy.DELETE_ON_COMPLETION
                )
            )
        ));
        var workspacePath = Path.of(workspace.path().value()).toAbsolutePath().normalize();
        var repositoryDirectory = workspacePath.resolve("repository").normalize();
        metrics.put("workspacePath", workspacePath.toString());

        try {
            measured(metrics, "cloneDurationMillis", () -> {
                git(workspacePath, timeout, List.of(
                    "git",
                    "-c",
                    "core.hooksPath=/dev/null",
                    "clone",
                    "--quiet",
                    "--no-tags",
                    "--",
                    WILDFLY_REMOTE_URL,
                    repositoryDirectory.toString()
                ));
                return null;
            });
            target.branch().ifPresent(branch -> measuredUnchecked(metrics, "branchCheckoutDurationMillis", () -> git(
                repositoryDirectory,
                timeout,
                gitCommand("checkout", "--quiet", "--force", branch)
            )));
            target.commit().ifPresent(commit -> measuredUnchecked(metrics, "commitCheckoutDurationMillis", () -> git(
                repositoryDirectory,
                timeout,
                gitCommand("checkout", "--quiet", "--force", commit)
            )));

            var resolvedCommit = measured(metrics, "commitResolutionDurationMillis", () -> git(
                repositoryDirectory,
                timeout,
                gitCommand("rev-parse", "HEAD")
            )).strip();
            var resolvedRemoteUrl = measured(metrics, "remoteResolutionDurationMillis", () -> git(
                repositoryDirectory,
                timeout,
                gitCommand("remote", "get-url", "origin")
            )).strip();
            var repositorySource = measured(metrics, "sourceRootDetectionDurationMillis", () ->
                new LocalRepositorySourceAdapter(workspacePath).resolve(new RepositoryMetadata(
                    "wildfly-hardening",
                    repositoryDirectory.toString(),
                    target.branch().orElse("UNSPECIFIED"),
                    resolvedCommit
                ))
            );
            var size = measured(metrics, "workspaceMeasurementDurationMillis", () -> repositorySize(repositoryDirectory));

            metrics.put("resolvedRemoteUrl", resolvedRemoteUrl);
            metrics.put("resolvedCommit", resolvedCommit);
            metrics.put("detectedSourceRootCount", String.valueOf(repositorySource.sourceRoots().size()));
            metrics.put("fileCount", String.valueOf(size.fileCount()));
            metrics.put("workspaceSizeBytes", String.valueOf(size.totalBytes()));

            assertFalse(resolvedCommit.isBlank());
            assertFalse(repositorySource.sourceRoots().isEmpty());
            assertTrue(size.fileCount() > 0, "WildFly checkout should contain repository files.");
        } catch (Exception e) {
            metrics.put("failure", e.getClass().getSimpleName() + ": " + e.getMessage());
            throw e;
        } finally {
            measured(metrics, "cleanupDurationMillis", () -> {
                var cleaned = workspaceAdapter.cleanup(workspace);
                metrics.put("cleanupStatus", cleaned.status().name());
                return null;
            });
            writeReport(metrics);
        }

        assertTrue(Files.notExists(workspacePath), "Ephemeral WildFly hardening workspace should be cleaned.");
    }

    private static HardeningTarget hardeningTarget() {
        var branch = env(HARDENING_BRANCH);
        var commit = env(HARDENING_COMMIT);
        assumeTrue(
            branch.isPresent() || commit.isPresent(),
            "Set " + HARDENING_BRANCH + " or " + HARDENING_COMMIT + " to avoid guessing the WildFly checkout target."
        );
        branch.ifPresent(value -> requireSafeGitReference(value, HARDENING_BRANCH));
        commit.ifPresent(value -> requireSafeGitReference(value, HARDENING_COMMIT));
        return new HardeningTarget(branch, commit);
    }

    private static Optional<String> env(String name) {
        return Optional.ofNullable(System.getenv(name)).filter(value -> !value.isBlank());
    }

    private static Duration configuredDuration() {
        return env(HARDENING_TIMEOUT_SECONDS)
            .map(value -> Duration.ofSeconds(Long.parseLong(value)))
            .orElse(DEFAULT_TIMEOUT);
    }

    private static long configuredLong(String name, long defaultValue) {
        return env(name).map(Long::parseLong).orElse(defaultValue);
    }

    private static void requireSafeGitReference(String value, String fieldName) {
        if (value.startsWith("-") || value.contains("\n") || value.contains("\r")) {
            throw new IllegalArgumentException(fieldName + " must be a plain Git reference.");
        }
    }

    private static List<String> gitCommand(String... arguments) {
        var command = new ArrayList<String>();
        command.add("git");
        command.add("-c");
        command.add("core.hooksPath=/dev/null");
        command.addAll(List.of(arguments));
        return List.copyOf(command);
    }

    private static String git(Path workingDirectory, Duration timeout, List<String> command)
        throws IOException, InterruptedException {
        var commandLine = List.copyOf(command);
        var process = new ProcessBuilder(commandLine)
            .directory(workingDirectory.toFile())
            .redirectErrorStream(true)
            .start();
        var completed = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (!completed) {
            process.destroyForcibly();
            throw new IOException("Git command timed out: " + redactedCommand(commandLine));
        }
        if (process.exitValue() != 0) {
            throw new IOException(
                "Git command failed: " + redactedCommand(commandLine) + System.lineSeparator() + output.strip()
            );
        }
        return output;
    }

    private static String redactedCommand(List<String> commandLine) {
        if (commandLine.contains("clone")) {
            return "git clone <repository-url> <workspace>";
        }
        return String.join(" ", commandLine);
    }

    private static RepositorySize repositorySize(Path repositoryDirectory) throws IOException {
        var fileCount = new LongAdder();
        var totalBytes = new LongAdder();
        try (var paths = Files.walk(repositoryDirectory)) {
            paths.filter(Files::isRegularFile).forEach(path -> {
                fileCount.increment();
                try {
                    totalBytes.add(Files.size(path));
                } catch (IOException e) {
                    throw new UncheckedIOException("Failed to measure " + path + ".", e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
        return new RepositorySize(fileCount.sum(), totalBytes.sum());
    }

    private static void writeReport(Map<String, String> metrics) throws IOException {
        var reportDirectory = env(HARDENING_REPORT_DIR)
            .map(Path::of)
            .orElse(Path.of("build", "reports", "wildfly-hardening"))
            .toAbsolutePath()
            .normalize();
        Files.createDirectories(reportDirectory);
        var lines = metrics.entrySet().stream()
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .toList();
        Files.write(
            reportDirectory.resolve("wildfly-hardening-metrics.txt"),
            lines,
            StandardCharsets.UTF_8
        );
    }

    private static void measuredUnchecked(Map<String, String> metrics, String metricName, ThrowingRunnable operation) {
        try {
            measured(metrics, metricName, () -> {
                operation.run();
                return null;
            });
        } catch (Exception e) {
            throw new IllegalStateException("Failed to run measured operation " + metricName + ".", e);
        }
    }

    private static <T> T measured(Map<String, String> metrics, String metricName, ThrowingSupplier<T> operation)
        throws Exception {
        var started = System.nanoTime();
        try {
            return operation.get();
        } finally {
            metrics.put(metricName, String.valueOf(Duration.ofNanos(System.nanoTime() - started).toMillis()));
        }
    }

    private record HardeningTarget(Optional<String> branch, Optional<String> commit) {
        private String identity() {
            return branch.orElse("NO_BRANCH") + "|" + commit.orElse("NO_COMMIT");
        }
    }

    private record RepositorySize(long fileCount, long totalBytes) {
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
