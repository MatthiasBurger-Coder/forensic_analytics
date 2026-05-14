package de.burger.forensics.analytics.domain.analysis;

import de.burger.forensics.analytics.domain.repository.BranchReference;
import de.burger.forensics.analytics.domain.repository.CheckoutResult;
import de.burger.forensics.analytics.domain.repository.CommitReference;
import de.burger.forensics.analytics.domain.repository.RepositoryReference;
import de.burger.forensics.analytics.domain.workspace.WorkspaceCleanupPolicy;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import de.burger.forensics.analytics.domain.workspace.WorkspacePolicy;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnalysisSessionTest {
    @Test
    void registeredSessionKeepsRepositoryWorkspaceAndCheckoutProvenance() {
        var session = AnalysisSession.registered(
            new AnalysisRunId("analysis-1"),
            "request-1",
            "schema-v1",
            buildContext(),
            repository(),
            branch(),
            commit(),
            workspacePolicy(),
            new WorkspaceId("workspace-1"),
            checkoutResult()
        );

        assertEquals(new AnalysisRunId("analysis-1"), session.id());
        assertEquals("request-1", session.requestId());
        assertEquals("schema-v1", session.schemaVersion());
        assertEquals(buildContext(), session.buildContext());
        assertEquals(repository(), session.repository());
        assertEquals(branch(), session.branch());
        assertEquals(commit(), session.commit());
        assertEquals(workspacePolicy(), session.workspacePolicy());
        assertEquals(new WorkspaceId("workspace-1"), session.workspaceId());
        assertEquals(checkoutResult(), session.checkoutResult());
        assertEquals(AnalysisSessionState.REGISTERED, session.state());
    }

    @Test
    void buildContextKeepsMissingRootProjectNameExplicitAndCopiesMetadata() {
        var modules = new java.util.ArrayList<>(List.of(":app"));
        var attributes = new HashMap<String, String>();
        attributes.put("z", "last");
        attributes.put("a", "first");

        var context = new BuildContext("gradle", "build-1", Optional.empty(), modules, attributes);
        modules.add(":mutated");
        attributes.put("b", "mutated");

        assertEquals(Optional.empty(), context.rootProjectName());
        assertEquals(List.of(":app"), context.declaredModules());
        assertEquals(List.of("a", "z"), List.copyOf(context.attributes().keySet()));
    }

    @Test
    void rejectsIncompleteSessionProvenance() {
        assertThrows(
            IllegalArgumentException.class,
            () -> AnalysisSession.registered(
                new AnalysisRunId("analysis-1"),
                " ",
                "schema-v1",
                buildContext(),
                repository(),
                branch(),
                commit(),
                workspacePolicy(),
                new WorkspaceId("workspace-1"),
                checkoutResult()
            )
        );
        assertThrows(
            NullPointerException.class,
            () -> AnalysisSession.registered(
                null,
                "request-1",
                "schema-v1",
                buildContext(),
                repository(),
                branch(),
                commit(),
                workspacePolicy(),
                new WorkspaceId("workspace-1"),
                checkoutResult()
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new BuildContext(" ", "build-1", Optional.empty(), List.of(), Map.of())
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new BuildContext("gradle", "build-1", Optional.of(" "), List.of(), Map.of())
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new BuildContext("gradle", "build-1", Optional.empty(), List.of(" "), Map.of())
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new BuildContext("gradle", "build-1", Optional.empty(), List.of(), Map.of(" ", "value"))
        );
    }

    private static RepositoryReference repository() {
        return new RepositoryReference("https://example.invalid/project.git", Optional.of("github"), Map.of());
    }

    private static BranchReference branch() {
        return new BranchReference(Optional.of("main"), true);
    }

    private static CommitReference commit() {
        return new CommitReference(Optional.of("abcdef"), false);
    }

    private static BuildContext buildContext() {
        return new BuildContext("gradle", "build-1", Optional.of("project"), List.of(":app"), Map.of());
    }

    private static WorkspacePolicy workspacePolicy() {
        return new WorkspacePolicy(
            false,
            false,
            false,
            false,
            Duration.ofSeconds(60),
            0,
            WorkspaceCleanupPolicy.RETAIN_FOR_REVIEW
        );
    }

    private static CheckoutResult checkoutResult() {
        return new CheckoutResult(
            "https://example.invalid/project.git",
            Optional.of("main"),
            Optional.of("abcdef"),
            "abcdef",
            List.of(),
            "CHECKED_OUT",
            List.of()
        );
    }
}
