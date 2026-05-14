package de.burger.forensics.analytics.persistence;

import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.analysis.AnalysisSession;
import de.burger.forensics.analytics.domain.analysis.BuildContext;
import de.burger.forensics.analytics.domain.repository.BranchReference;
import de.burger.forensics.analytics.domain.repository.CheckoutResult;
import de.burger.forensics.analytics.domain.repository.CommitReference;
import de.burger.forensics.analytics.domain.repository.RepositoryReference;
import de.burger.forensics.analytics.domain.workspace.WorkspaceCleanupPolicy;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import de.burger.forensics.analytics.domain.workspace.WorkspacePolicy;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryAnalysisSessionRepositoryTest {
    private final InMemoryAnalysisSessionRepository repository = new InMemoryAnalysisSessionRepository();

    @Test
    void storesAnalysisSessionsBySessionId() {
        var session = session("analysis-1");

        repository.save(session);

        assertEquals(session, repository.findById(new AnalysisRunId("analysis-1")).orElseThrow());
        assertTrue(repository.findById(new AnalysisRunId("missing")).isEmpty());
    }

    @Test
    void replacesSessionWithSameId() {
        var first = session("analysis-1");
        var second = AnalysisSession.registered(
            first.id(),
            "request-2",
            first.schemaVersion(),
            first.buildContext(),
            first.repository(),
            first.branch(),
            first.commit(),
            first.workspacePolicy(),
            first.workspaceId(),
            first.checkoutResult()
        );

        repository.save(first);
        repository.save(second);

        assertEquals(second, repository.findById(first.id()).orElseThrow());
    }

    @Test
    void requiresInputs() {
        assertThrows(NullPointerException.class, () -> repository.save(null));
        assertThrows(NullPointerException.class, () -> repository.findById(null));
    }

    private static AnalysisSession session(String id) {
        return AnalysisSession.registered(
            new AnalysisRunId(id),
            "request-1",
            "schema-v1",
            new BuildContext("gradle", "build-1", Optional.of("project"), List.of(":app"), Map.of()),
            new RepositoryReference("https://example.invalid/project.git", Optional.of("github"), Map.of()),
            new BranchReference(Optional.of("main"), true),
            new CommitReference(Optional.empty(), false),
            new WorkspacePolicy(
                true,
                false,
                false,
                false,
                Duration.ofSeconds(60),
                0,
                WorkspaceCleanupPolicy.DELETE_ON_COMPLETION
            ),
            new WorkspaceId("workspace-1"),
            new CheckoutResult(
                "https://example.invalid/project.git",
                Optional.of("main"),
                Optional.empty(),
                "abcdef",
                List.of(),
                "CHECKED_OUT",
                List.of()
            )
        );
    }
}
