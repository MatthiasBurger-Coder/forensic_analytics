package de.burger.forensics.analytics.domain.repository;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryCheckoutDomainModelTest {
    @Test
    void repositoryRequestKeepsExplicitRevisionReferences() {
        var repository = new RepositoryReference(
            "https://example.invalid/project.git",
            Optional.of("github"),
            Map.of("visibility", "public")
        );
        var branch = new BranchReference(Optional.of("main"), true);
        var commit = new CommitReference(Optional.empty(), false);

        assertEquals("https://example.invalid/project.git", repository.remoteUrl());
        assertEquals(Optional.of("github"), repository.provider());
        assertEquals("public", repository.attributes().get("visibility"));
        assertTrue(branch.isSpecified());
        assertEquals(Optional.empty(), commit.hash());
        assertEquals(List.of("visibility"), List.copyOf(repository.attributes().keySet()));
    }

    @Test
    void checkoutResultPreservesRequestedAndResolvedStateDeterministically() {
        var sourceRoots = new ArrayList<SourceRoot>();
        sourceRoots.add(new SourceRoot("/workspace/project/src/test/java"));
        sourceRoots.add(new SourceRoot("/workspace/project/src/main/java"));
        var diagnostics = new ArrayList<>(List.of("checkout mode: full clone"));

        var result = new CheckoutResult(
            "https://example.invalid/project.git",
            Optional.of("main"),
            Optional.of("abcdef"),
            "abcdef123456",
            sourceRoots,
            "CHECKED_OUT",
            diagnostics
        );
        sourceRoots.add(new SourceRoot("/workspace/project/generated"));
        diagnostics.add("mutated");

        assertEquals(Optional.of("main"), result.requestedBranch());
        assertEquals(Optional.of("abcdef"), result.requestedCommit());
        assertEquals("abcdef123456", result.resolvedCommit());
        assertEquals(
            List.of(
                new SourceRoot("/workspace/project/src/main/java"),
                new SourceRoot("/workspace/project/src/test/java")
            ),
            result.detectedSourceRoots()
        );
        assertEquals(List.of("checkout mode: full clone"), result.diagnostics());
    }

    @Test
    void repositoryReferenceCopiesAttributesInStableOrder() {
        var attributes = new HashMap<String, String>();
        attributes.put("z", "last");
        attributes.put("a", "first");

        var repository = new RepositoryReference("https://example.invalid/project.git", Optional.empty(), attributes);
        attributes.put("b", "mutated");

        assertEquals(List.of("a", "z"), List.copyOf(repository.attributes().keySet()));
        assertThrows(UnsupportedOperationException.class, () -> repository.attributes().put("c", "blocked"));
    }

    @Test
    void rejectsMissingCheckoutValues() {
        assertThrows(IllegalArgumentException.class, () -> new RepositoryReference(" ", Optional.empty(), Map.of()));
        assertThrows(NullPointerException.class, () -> new RepositoryReference("repo", null, Map.of()));
        assertThrows(NullPointerException.class, () -> new RepositoryReference("repo", Optional.empty(), null));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryReference("repo", Optional.empty(), Map.of(" ", "value")));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryReference("repo", Optional.empty(), Map.of("key", " ")));
        assertThrows(IllegalArgumentException.class, () -> new BranchReference(Optional.of(" "), false));
        assertThrows(IllegalArgumentException.class, () -> new CommitReference(Optional.of(" "), false));
        assertThrows(IllegalArgumentException.class, () -> new SourceRoot(" "));
        assertThrows(
            IllegalArgumentException.class,
            () -> new CheckoutResult(
                "repo",
                Optional.empty(),
                Optional.empty(),
                "commit",
                List.of(),
                " ",
                List.of()
            )
        );
    }
}
