package de.burger.forensics.analytics.adapter.repository.source;

import de.burger.forensics.analytics.domain.repository.RepositoryMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalRepositorySourceAdapterTest {
    @TempDir
    Path tempDir;

    @Test
    void resolvesFileUriToMainJavaSourceRoots() throws Exception {
        var repository = Files.createDirectories(tempDir.resolve("project"));
        var moduleB = Files.createDirectories(repository.resolve("module-b/src/main/java"));
        var moduleA = Files.createDirectories(repository.resolve("module-a/src/main/java"));
        Files.createDirectories(repository.resolve("build/generated/src/main/java"));

        var source = new LocalRepositorySourceAdapter(tempDir).resolve(metadata(repository.toUri().toString()));

        assertEquals(metadata(repository.toUri().toString()), source.metadata());
        assertEquals(
            List.of(moduleA.toAbsolutePath().normalize().toString(), moduleB.toAbsolutePath().normalize().toString()),
            source.sourceRoots()
        );
    }

    @Test
    void resolvesRelativeLocalPathAgainstBaseDirectory() throws Exception {
        var workspace = Files.createDirectories(tempDir.resolve("workspace"));
        var repository = Files.createDirectories(workspace.resolve("project"));
        var sourceRoot = Files.createDirectories(repository.resolve("src/main/java"));

        var source = new LocalRepositorySourceAdapter(workspace).resolve(metadata("project"));

        assertEquals(List.of(sourceRoot.toAbsolutePath().normalize().toString()), source.sourceRoots());
    }

    @Test
    void fallsBackToRepositoryDirectoryWhenNoMainJavaSourceRootExists() throws Exception {
        var repository = Files.createDirectories(tempDir.resolve("project"));

        var source = new LocalRepositorySourceAdapter(tempDir).resolve(metadata(repository.toString()));

        assertEquals(List.of(repository.toAbsolutePath().normalize().toString()), source.sourceRoots());
    }

    @Test
    void rejectsUnsupportedRemoteRepositoryLocations() {
        var adapter = new LocalRepositorySourceAdapter(tempDir);

        assertThrows(IllegalArgumentException.class, () -> adapter.resolve(metadata("https://example.test/project.git")));
        assertThrows(IllegalArgumentException.class, () -> adapter.resolve(metadata("ssh://example.test/project.git")));
    }

    @Test
    void rejectsMissingOrFileRepositoryLocations() throws Exception {
        var adapter = new LocalRepositorySourceAdapter(tempDir);
        var regularFile = Files.writeString(tempDir.resolve("README.md"), "text");

        assertThrows(IllegalArgumentException.class, () -> adapter.resolve(metadata("missing")));
        assertThrows(IllegalArgumentException.class, () -> adapter.resolve(metadata(regularFile.toString())));
    }

    @Test
    void requiresInputs() {
        assertThrows(NullPointerException.class, () -> new LocalRepositorySourceAdapter(null));
        assertThrows(NullPointerException.class, () -> new LocalRepositorySourceAdapter(tempDir).resolve(null));
    }

    private static RepositoryMetadata metadata(String repositoryLocation) {
        return new RepositoryMetadata("project-a", repositoryLocation, "main", "abcdef");
    }
}
