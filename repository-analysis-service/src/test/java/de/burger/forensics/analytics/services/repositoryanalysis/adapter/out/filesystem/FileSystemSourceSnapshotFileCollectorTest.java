package de.burger.forensics.analytics.services.repositoryanalysis.adapter.out.filesystem;

import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceRoot;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceSnapshotHandoffPolicy;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.WorkspaceId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.sha256Hex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileSystemSourceSnapshotFileCollectorTest {
    @TempDir
    Path tempDir;

    @Test
    void collectsBoundedJavaFilesWithoutWorkspacePaths() throws Exception {
        var repository = repositoryRoot("workspace-1");
        Files.createDirectories(repository.resolve("src/main/java/a"));
        Files.createDirectories(repository.resolve("src/test/java/a"));
        Files.writeString(repository.resolve("src/main/java/a/B.java"), "package a; class B {}");
        Files.writeString(repository.resolve("src/main/java/a/A.java"), "package a; class A {}");
        Files.writeString(repository.resolve("src/test/java/a/ATest.java"), "package a; class ATest {}");
        Files.writeString(repository.resolve("README.md"), "ignored");

        var files = collector().collect(
            new WorkspaceId("workspace-1"),
            List.of(new SourceRoot("src/main/java", "java")),
            new SourceSnapshotHandoffPolicy(10, 10_000, 30)
        );

        assertEquals(List.of("a/A.java", "a/B.java"), files.stream().map(file -> file.relativePath()).toList());
        assertEquals("src/main/java", files.getFirst().sourceRoot());
        assertEquals(sha256Hex("package a; class A {}"), files.getFirst().sha256());
        assertFalse(files.getFirst().sourcePath().contains(tempDir.toString().replace('\\', '/')));
    }

    @Test
    void rejectsBoundsAndIgnoresSymlinkedFiles() throws Exception {
        var repository = repositoryRoot("workspace-2");
        Files.createDirectories(repository.resolve("src/main/java/a"));
        Files.writeString(repository.resolve("src/main/java/a/A.java"), "package a; class A {}");
        var outside = tempDir.resolve("outside.java");
        Files.writeString(outside, "class Outside {}");
        Files.createSymbolicLink(repository.resolve("src/main/java/a/Linked.java"), outside);

        var files = collector().collect(
            new WorkspaceId("workspace-2"),
            List.of(new SourceRoot("src/main/java", "java")),
            new SourceSnapshotHandoffPolicy(10, 10_000, 30)
        );

        assertEquals(List.of("a/A.java"), files.stream().map(file -> file.relativePath()).toList());
        assertThrows(IllegalArgumentException.class, () -> collector().collect(
            new WorkspaceId("workspace-2"),
            List.of(new SourceRoot("src/main/java", "java")),
            new SourceSnapshotHandoffPolicy(1, 5, 30)
        ));
    }

    @Test
    void rejectsMaxFileCountSeparately() throws Exception {
        var repository = repositoryRoot("workspace-3");
        Files.createDirectories(repository.resolve("src/main/java/a"));
        Files.writeString(repository.resolve("src/main/java/a/A.java"), "package a; class A {}");
        Files.writeString(repository.resolve("src/main/java/a/B.java"), "package a; class B {}");

        var error = assertThrows(IllegalArgumentException.class, () -> collector().collect(
            new WorkspaceId("workspace-3"),
            List.of(new SourceRoot("src/main/java", "java")),
            new SourceSnapshotHandoffPolicy(1, 10_000, 30)
        ));

        assertEquals("source file count exceeds handoff policy", error.getMessage());
    }

    @Test
    void rejectsOversizedSourceFileBeforeReadingUtf8Content() throws Exception {
        var repository = repositoryRoot("workspace-6");
        Files.createDirectories(repository.resolve("src/main/java/a"));
        Files.write(repository.resolve("src/main/java/a/Oversized.java"), new byte[] {(byte) 0xC3, (byte) 0x28});

        var error = assertThrows(IllegalArgumentException.class, () -> collector().collect(
            new WorkspaceId("workspace-6"),
            List.of(new SourceRoot("src/main/java", "java")),
            new SourceSnapshotHandoffPolicy(10, 1, 30)
        ));

        assertEquals("source byte count exceeds handoff policy", error.getMessage());
    }

    @Test
    void rejectsEmptySnapshotsFromUnsupportedOrUnavailableSourceRootsWithoutLeakingPaths() throws Exception {
        var repository = repositoryRoot("workspace-4");
        Files.createDirectories(repository.resolve("src/main/java/a"));
        Files.writeString(repository.resolve("src/main/java/a/A.java"), "package a; class A {}");

        var unsupportedLanguage = assertThrows(IllegalStateException.class, () -> collector().collect(
            new WorkspaceId("workspace-4"),
            List.of(new SourceRoot("src/main/java", "kotlin")),
            new SourceSnapshotHandoffPolicy(10, 10_000, 30)
        ));
        var unavailableRoot = assertThrows(IllegalStateException.class, () -> collector().collect(
            new WorkspaceId("workspace-4"),
            List.of(new SourceRoot("src/generated/java", "java")),
            new SourceSnapshotHandoffPolicy(10, 10_000, 30)
        ));

        assertEquals("Source snapshot contains no Java source files", unsupportedLanguage.getMessage());
        assertEquals("Source snapshot contains no Java source files", unavailableRoot.getMessage());
        assertFalse(unavailableRoot.getMessage().contains(tempDir.toString()));
    }

    @Test
    void wrapsUnreadableConfiguredRootWithoutLeakingPaths() {
        var missingRootCollector = new FileSystemSourceSnapshotFileCollector(tempDir.resolve("missing-root"));

        var error = assertThrows(IllegalStateException.class, () -> missingRootCollector.collect(
            new WorkspaceId("workspace-5"),
            List.of(new SourceRoot("src/main/java", "java")),
            new SourceSnapshotHandoffPolicy(10, 10_000, 30)
        ));

        assertEquals("Source snapshot files could not be collected", error.getMessage());
        assertFalse(error.getMessage().contains(tempDir.toString()));
    }

    @Test
    void rejectsUnavailableOrEscapedWorkspacesWithoutLeakingPaths() {
        var error = assertThrows(IllegalStateException.class, () -> collector().collect(
            new WorkspaceId("workspace-missing"),
            List.of(new SourceRoot("src/main/java", "java")),
            new SourceSnapshotHandoffPolicy(10, 10_000, 30)
        ));

        assertTrue(error.getMessage().contains("Repository workspace is not available"));
        assertFalse(error.getMessage().contains(tempDir.toString()));
    }

    private Path repositoryRoot(String workspaceId) throws Exception {
        var repository = tempDir.resolve(workspaceId).resolve("repository");
        Files.createDirectories(repository);
        return repository;
    }

    private FileSystemSourceSnapshotFileCollector collector() {
        return new FileSystemSourceSnapshotFileCollector(tempDir);
    }
}
