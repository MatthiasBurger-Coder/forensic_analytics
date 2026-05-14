package de.burger.forensics.analytics.adapter.repository.source;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalRepositoryLocationTest {
    @TempDir
    Path tempDir;

    @Test
    void resolvesPlainRelativePath() {
        assertEquals(
            tempDir.resolve("project").toAbsolutePath().normalize(),
            LocalRepositoryLocation.resolve("project", tempDir)
        );
    }

    @Test
    void resolvesPlainAbsolutePath() {
        var absolute = tempDir.resolve("project").toAbsolutePath().normalize();

        assertEquals(absolute, LocalRepositoryLocation.resolve(absolute.toString(), tempDir));
    }

    @Test
    void resolvesFileUri() {
        var repository = tempDir.resolve("project").toAbsolutePath().normalize();

        assertEquals(repository, LocalRepositoryLocation.resolve(repository.toUri().toString(), tempDir));
    }

    @Test
    void rejectsUnsupportedUriSchemes() {
        assertThrows(IllegalArgumentException.class, () -> LocalRepositoryLocation.resolve("https://example.test/project.git", tempDir));
        assertThrows(IllegalArgumentException.class, () -> LocalRepositoryLocation.resolve("git@example.test:project.git", tempDir));
    }

    @Test
    void requiresInputs() {
        assertThrows(NullPointerException.class, () -> LocalRepositoryLocation.resolve("project", null));
        assertThrows(IllegalArgumentException.class, () -> LocalRepositoryLocation.resolve(null, tempDir));
        assertThrows(IllegalArgumentException.class, () -> LocalRepositoryLocation.resolve("", tempDir));
    }
}
