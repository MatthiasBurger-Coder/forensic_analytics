package de.burger.forensics.analytics.adapter.repository.source;

import de.burger.forensics.analytics.application.ingestion.RepositoryCheckoutException;
import de.burger.forensics.analytics.application.ingestion.command.RepositoryCheckoutRequest;
import de.burger.forensics.analytics.application.ingestion.command.WorkspacePreparationRequest;
import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.repository.BranchReference;
import de.burger.forensics.analytics.domain.repository.CommitReference;
import de.burger.forensics.analytics.domain.repository.RepositoryReference;
import de.burger.forensics.analytics.domain.repository.SourceRoot;
import de.burger.forensics.analytics.domain.workspace.PreparedWorkspace;
import de.burger.forensics.analytics.domain.workspace.WorkspaceCleanupPolicy;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import de.burger.forensics.analytics.domain.workspace.WorkspacePolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class GitRepositoryCheckoutAdapterTest {
    @TempDir
    Path tempDir;

    @BeforeEach
    void requireGitExecutable() {
        assumeTrue(gitIsAvailable(), "git executable is required for repository checkout adapter tests");
    }

    @Test
    void clonesMiniRepositoryChecksOutPinnedCommitAndDetectsSourceRoots() throws Exception {
        var fixtureRepository = createMiniRepository();
        var expectedCommit = git(fixtureRepository, "rev-parse", "HEAD").strip();
        var workspace = preparedWorkspace("analysis-1");

        var result = new GitRepositoryCheckoutAdapter().checkout(new RepositoryCheckoutRequest(
            new AnalysisRunId("analysis-1"),
            workspace,
            new RepositoryReference(fixtureRepository.toUri().toString(), Optional.of("local-fixture"), Map.of()),
            new BranchReference(Optional.of("main"), true),
            new CommitReference(Optional.of(expectedCommit), true)
        ));

        var checkoutDirectory = Path.of(workspace.path().value()).resolve("repository").toAbsolutePath().normalize();
        assertEquals(fixtureRepository.toUri().toString(), result.resolvedRemoteUrl());
        assertEquals(Optional.of("main"), result.requestedBranch());
        assertEquals(Optional.of(expectedCommit), result.requestedCommit());
        assertEquals(expectedCommit, result.resolvedCommit());
        assertEquals("CHECKED_OUT", result.checkoutStatus());
        assertEquals(List.of("checkout mode: full clone"), result.diagnostics());
        assertEquals(
            List.of(
                new SourceRoot(checkoutDirectory.resolve("module-a/src/main/java").toString()),
                new SourceRoot(checkoutDirectory.resolve("module-b/src/main/java").toString())
            ),
            result.detectedSourceRoots()
        );
        assertFalse(Files.exists(checkoutDirectory.resolve("build-was-run")));
    }

    @Test
    void rejectsUnsupportedRepositoryUrlsBeforeCloning() throws Exception {
        var workspace = preparedWorkspace("analysis-2");
        var request = new RepositoryCheckoutRequest(
            new AnalysisRunId("analysis-2"),
            workspace,
            new RepositoryReference("ssh://example.invalid/project.git", Optional.empty(), Map.of()),
            new BranchReference(Optional.of("main"), true),
            new CommitReference(Optional.empty(), false)
        );

        assertThrows(RepositoryCheckoutException.class, () -> new GitRepositoryCheckoutAdapter().checkout(request));
        assertFalse(Files.exists(Path.of(workspace.path().value()).resolve("repository")));
    }

    @Test
    void clonesRepositoryFromLocalPathWithoutUriScheme() throws Exception {
        var fixtureRepository = createMiniRepository();
        var expectedCommit = git(fixtureRepository, "rev-parse", "HEAD").strip();
        var workspace = preparedWorkspace("analysis-local-path");

        var result = new GitRepositoryCheckoutAdapter().checkout(new RepositoryCheckoutRequest(
            new AnalysisRunId("analysis-local-path"),
            workspace,
            new RepositoryReference(fixtureRepository.toString(), Optional.empty(), Map.of()),
            new BranchReference(Optional.of("main"), true),
            new CommitReference(Optional.empty(), false)
        ));

        assertEquals(expectedCommit, result.resolvedCommit());
        assertEquals(Optional.empty(), result.requestedCommit());
    }

    @Test
    void rejectsMissingOrAlreadyPopulatedCheckoutWorkspaceBeforeGitRuns() throws Exception {
        var missingWorkspace = new PreparedWorkspace(
            new WorkspaceId("workspace-missing"),
            de.burger.forensics.analytics.domain.workspace.WorkspacePreparationStatus.READY,
            new de.burger.forensics.analytics.domain.workspace.WorkspacePath(tempDir.resolve("missing-workspace").toString()),
            new de.burger.forensics.analytics.domain.workspace.WorkspaceLease(
                "analysis-missing",
                java.time.Instant.parse("2026-05-14T12:00:00Z"),
                Optional.empty(),
                de.burger.forensics.analytics.domain.workspace.WorkspacePreparationStatus.READY
            ),
            List.of("fixture")
        );
        var occupiedWorkspace = preparedWorkspace("analysis-occupied");
        Files.createDirectories(Path.of(occupiedWorkspace.path().value()).resolve("repository"));

        assertThrows(RepositoryCheckoutException.class, () -> new GitRepositoryCheckoutAdapter().checkout(new RepositoryCheckoutRequest(
            new AnalysisRunId("analysis-missing"),
            missingWorkspace,
            new RepositoryReference("https://example.invalid/project.git", Optional.empty(), Map.of()),
            new BranchReference(Optional.of("main"), true),
            new CommitReference(Optional.empty(), false)
        )));
        assertThrows(RepositoryCheckoutException.class, () -> new GitRepositoryCheckoutAdapter().checkout(new RepositoryCheckoutRequest(
            new AnalysisRunId("analysis-occupied"),
            occupiedWorkspace,
            new RepositoryReference("https://example.invalid/project.git", Optional.empty(), Map.of()),
            new BranchReference(Optional.of("main"), true),
            new CommitReference(Optional.empty(), false)
        )));
    }

    @Test
    void rejectsUnsafeRemoteUrlsAndReferencesBeforeCloning() throws Exception {
        var unsafeUrlWorkspace = preparedWorkspace("analysis-unsafe-url");
        assertThrows(RepositoryCheckoutException.class, () -> new GitRepositoryCheckoutAdapter().checkout(new RepositoryCheckoutRequest(
            new AnalysisRunId("analysis-unsafe-url"),
            unsafeUrlWorkspace,
            new RepositoryReference("git@example.invalid:project.git", Optional.empty(), Map.of()),
            new BranchReference(Optional.of("main"), true),
            new CommitReference(Optional.empty(), false)
        )));

        var userInfoWorkspace = preparedWorkspace("analysis-user-info");
        assertThrows(RepositoryCheckoutException.class, () -> new GitRepositoryCheckoutAdapter().checkout(new RepositoryCheckoutRequest(
            new AnalysisRunId("analysis-user-info"),
            userInfoWorkspace,
            new RepositoryReference("https://token@example.invalid/project.git", Optional.empty(), Map.of()),
            new BranchReference(Optional.of("main"), true),
            new CommitReference(Optional.empty(), false)
        )));

        var unsafeBranchWorkspace = preparedWorkspace("analysis-unsafe-branch");
        assertThrows(RepositoryCheckoutException.class, () -> new GitRepositoryCheckoutAdapter().checkout(new RepositoryCheckoutRequest(
            new AnalysisRunId("analysis-unsafe-branch"),
            unsafeBranchWorkspace,
            new RepositoryReference("https://example.invalid/project.git", Optional.empty(), Map.of()),
            new BranchReference(Optional.of("-main"), true),
            new CommitReference(Optional.empty(), false)
        )));

        var unsafeCommitWorkspace = preparedWorkspace("analysis-unsafe-commit");
        assertThrows(RepositoryCheckoutException.class, () -> new GitRepositoryCheckoutAdapter().checkout(new RepositoryCheckoutRequest(
            new AnalysisRunId("analysis-unsafe-commit"),
            unsafeCommitWorkspace,
            new RepositoryReference("https://example.invalid/project.git", Optional.empty(), Map.of()),
            new BranchReference(Optional.of("main"), true),
            new CommitReference(Optional.of("-abcdef"), true)
        )));
    }

    @Test
    void reportsMissingRequestedCommitWithoutRunningRepositoryBuildScripts() throws Exception {
        var fixtureRepository = createMiniRepository();
        var workspace = preparedWorkspace("analysis-3");

        var request = new RepositoryCheckoutRequest(
            new AnalysisRunId("analysis-3"),
            workspace,
            new RepositoryReference(fixtureRepository.toUri().toString(), Optional.empty(), Map.of()),
            new BranchReference(Optional.of("main"), true),
            new CommitReference(Optional.of("0123456789012345678901234567890123456789"), true)
        );

        assertThrows(RepositoryCheckoutException.class, () -> new GitRepositoryCheckoutAdapter().checkout(request));
        assertFalse(Files.exists(Path.of(workspace.path().value()).resolve("repository").resolve("build-was-run")));
    }

    private PreparedWorkspace preparedWorkspace(String analysisId) {
        return new FileSystemWorkspacePreparationAdapter(tempDir.resolve("workspaces"))
            .prepare(new WorkspacePreparationRequest(new AnalysisRunId(analysisId), workspacePolicy()));
    }

    private Path createMiniRepository() throws Exception {
        var repository = Files.createDirectories(tempDir.resolve("fixture-repository-" + System.nanoTime()));
        git(repository, "init");
        git(repository, "checkout", "-B", "main");
        git(repository, "config", "user.name", "Forensic Analytics Test");
        git(repository, "config", "user.email", "forensic-analytics@example.invalid");
        Files.createDirectories(repository.resolve("module-b/src/main/java/com/example"));
        Files.createDirectories(repository.resolve("module-a/src/main/java/com/example"));
        Files.createDirectories(repository.resolve("build/generated/src/main/java/com/example"));
        Files.writeString(
            repository.resolve("module-a/src/main/java/com/example/AppA.java"),
            "package com.example; final class AppA {}" + System.lineSeparator(),
            StandardCharsets.UTF_8
        );
        Files.writeString(
            repository.resolve("module-b/src/main/java/com/example/AppB.java"),
            "package com.example; final class AppB {}" + System.lineSeparator(),
            StandardCharsets.UTF_8
        );
        Files.writeString(
            repository.resolve("build/generated/src/main/java/com/example/Generated.java"),
            "package com.example; final class Generated {}" + System.lineSeparator(),
            StandardCharsets.UTF_8
        );
        Files.writeString(
            repository.resolve("gradlew"),
            "#!/bin/sh" + System.lineSeparator() + "touch build-was-run" + System.lineSeparator(),
            StandardCharsets.UTF_8
        );
        git(repository, "add", ".");
        git(repository, "commit", "-m", "Add synthetic mini repository fixture");
        return repository;
    }

    private static WorkspacePolicy workspacePolicy() {
        return new WorkspacePolicy(
            true,
            false,
            false,
            false,
            Duration.ofSeconds(60),
            0L,
            WorkspaceCleanupPolicy.DELETE_ON_COMPLETION
        );
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

    private static String git(Path workingDirectory, String... arguments) throws Exception {
        var command = new ArrayList<String>();
        command.add("git");
        command.addAll(List.of(arguments));
        var process = new ProcessBuilder(command)
            .directory(workingDirectory.toFile())
            .redirectErrorStream(true)
            .start();
        var completed = process.waitFor(30, TimeUnit.SECONDS);
        var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(completed, () -> "git command timed out: " + command);
        assertEquals(0, process.exitValue(), () -> "git command failed: " + command + System.lineSeparator() + output);
        return output;
    }
}
