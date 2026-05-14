package de.burger.forensics.analytics.application.ingestion.command;

import de.burger.forensics.analytics.domain.repository.BranchReference;
import de.burger.forensics.analytics.domain.repository.CommitReference;
import de.burger.forensics.analytics.domain.repository.RepositoryReference;
import de.burger.forensics.analytics.domain.workspace.WorkspaceCleanupPolicy;
import de.burger.forensics.analytics.domain.workspace.WorkspacePolicy;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnalyzeRepositoryCommandTest {
    @Test
    void buildContextTreatsMissingOrBlankRootProjectNameAsUnknown() {
        var missingRoot = new BuildContextCommand("gradle", "build-1", (String) null, List.of(":app"), Map.of());
        var blankRoot = new BuildContextCommand("gradle", "build-1", " ", List.of(":app"), Map.of());

        assertEquals(Optional.empty(), missingRoot.rootProjectName());
        assertEquals(Optional.empty(), blankRoot.rootProjectName());
    }

    @Test
    void buildContextRejectsBlankTextCollectionsAndAttributes() {
        assertThrows(IllegalArgumentException.class, () -> new BuildContextCommand(null, "build-1", "root", List.of(":app"), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new BuildContextCommand(" ", "build-1", "root", List.of(":app"), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new BuildContextCommand("gradle", " ", "root", List.of(":app"), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new BuildContextCommand("gradle", "build-1", Optional.of(" "), List.of(":app"), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new BuildContextCommand("gradle", "build-1", "root", List.of(" "), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new BuildContextCommand("gradle", "build-1", "root", List.of(":app"), Map.of("", "value")));
        assertThrows(IllegalArgumentException.class, () -> new BuildContextCommand("gradle", "build-1", "root", List.of(":app"), Map.of("key", " ")));
    }

    @Test
    void buildContextRejectsNullContainers() {
        assertThrows(NullPointerException.class, () -> new BuildContextCommand("gradle", "build-1", (Optional<String>) null, List.of(":app"), Map.of()));
        assertThrows(NullPointerException.class, () -> new BuildContextCommand("gradle", "build-1", "root", null, Map.of()));
        assertThrows(NullPointerException.class, () -> new BuildContextCommand("gradle", "build-1", "root", List.of(":app"), null));
    }

    @Test
    void buildContextCopiesAttributesInDeterministicOrder() {
        var attributes = new HashMap<String, String>();
        attributes.put("zeta", "last");
        attributes.put("alpha", "first");

        var command = new BuildContextCommand("gradle", "build-1", "root", List.of(":app"), attributes);

        assertEquals(List.of("alpha", "zeta"), List.copyOf(command.attributes().keySet()));
        assertThrows(UnsupportedOperationException.class, () -> command.attributes().put("new", "value"));
    }

    @Test
    void analyzeRepositoryCommandRejectsBlankRequestIdentifiers() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new AnalyzeRepositoryCommand(
                repository(),
                branch(),
                commit(),
                workspacePolicy(),
                buildContext(),
                null,
                "schema-v1"
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new AnalyzeRepositoryCommand(
                repository(),
                branch(),
                commit(),
                workspacePolicy(),
                buildContext(),
                "request-1",
                " "
            )
        );
    }

    private static RepositoryReference repository() {
        return new RepositoryReference("https://example.invalid/project.git", Optional.of("git"), Map.of());
    }

    private static BranchReference branch() {
        return new BranchReference(Optional.of("main"), true);
    }

    private static CommitReference commit() {
        return new CommitReference(Optional.empty(), false);
    }

    private static WorkspacePolicy workspacePolicy() {
        return new WorkspacePolicy(false, false, false, false, Duration.ofSeconds(60), 0L, WorkspaceCleanupPolicy.RETAIN_FOR_REVIEW);
    }

    private static BuildContextCommand buildContext() {
        return new BuildContextCommand("gradle", "build-1", "root", List.of(":app"), Map.of());
    }
}
