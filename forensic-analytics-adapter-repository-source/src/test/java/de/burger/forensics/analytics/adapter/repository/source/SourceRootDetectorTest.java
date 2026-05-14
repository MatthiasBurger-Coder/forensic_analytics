package de.burger.forensics.analytics.adapter.repository.source;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SourceRootDetectorTest {
    @TempDir
    Path tempDir;

    @Test
    void detectsMainJavaSourceRootsAsMetadataOnlyAndSkipsIgnoredDirectories() throws Exception {
        var repository = Files.createDirectories(tempDir.resolve("repository"));
        var moduleB = Files.createDirectories(repository.resolve("module-b/src/main/java"));
        var moduleA = Files.createDirectories(repository.resolve("module-a/src/main/java"));
        Files.createDirectories(repository.resolve(".git/objects/src/main/java"));
        Files.createDirectories(repository.resolve(".gradle/cache/src/main/java"));
        Files.createDirectories(repository.resolve("build/generated/src/main/java"));
        Files.createDirectories(repository.resolve("target/generated/src/main/java"));

        var sourceRoots = SourceRootDetector.sourceRoots(repository);

        assertEquals(
            List.of(
                moduleA.toAbsolutePath().normalize().toString(),
                moduleB.toAbsolutePath().normalize().toString()
            ),
            sourceRoots
        );
    }

    @Test
    void fallsBackToRepositoryDirectoryWhenNoMainJavaSourceRootExists() throws Exception {
        var repository = Files.createDirectories(tempDir.resolve("repository"));

        assertEquals(
            List.of(repository.toAbsolutePath().normalize().toString()),
            SourceRootDetector.sourceRoots(repository)
        );
    }

    @Test
    void ignoresDirectoriesThatOnlyPartiallyMatchMainJavaSourceRootShape() throws Exception {
        var repository = Files.createDirectories(tempDir.resolve("repository"));
        Files.createDirectories(repository.resolve("src/test/java"));
        Files.createDirectories(repository.resolve("src/main/resources"));
        Files.createDirectories(repository.resolve("generated/main/java"));

        assertEquals(
            List.of(repository.toAbsolutePath().normalize().toString()),
            SourceRootDetector.sourceRoots(repository)
        );
    }
}
