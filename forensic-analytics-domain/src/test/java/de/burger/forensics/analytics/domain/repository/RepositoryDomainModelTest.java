package de.burger.forensics.analytics.domain.repository;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RepositoryDomainModelTest {
    @Test
    void storesRepositoryMetadata() {
        var metadata = metadata();

        assertEquals("project-a", metadata.projectId());
        assertEquals("file:///workspace/project", metadata.repositoryLocation());
        assertEquals("main", metadata.branchName());
        assertEquals("abcdef", metadata.commitHash());
    }

    @Test
    void rejectsInvalidRepositoryMetadata() {
        assertThrows(IllegalArgumentException.class, () -> new RepositoryMetadata(null, "repo", "main", "abcdef"));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryMetadata("", "repo", "main", "abcdef"));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryMetadata("project", null, "main", "abcdef"));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryMetadata("project", "", "main", "abcdef"));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryMetadata("project", "repo", null, "abcdef"));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryMetadata("project", "repo", "", "abcdef"));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryMetadata("project", "repo", "main", null));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryMetadata("project", "repo", "main", ""));
    }

    @Test
    void repositorySourceCopiesSourceRoots() {
        var sourceRoots = new ArrayList<String>();
        sourceRoots.add("src/main/java");

        var source = new RepositorySource(metadata(), sourceRoots);
        sourceRoots.add("generated");

        assertEquals(List.of("src/main/java"), source.sourceRoots());
    }

    @Test
    void repositorySourceRejectsMissingFields() {
        assertThrows(NullPointerException.class, () -> new RepositorySource(null, List.of("src/main/java")));
        assertThrows(NullPointerException.class, () -> new RepositorySource(metadata(), null));
    }

    private static RepositoryMetadata metadata() {
        return new RepositoryMetadata("project-a", "file:///workspace/project", "main", "abcdef");
    }
}
