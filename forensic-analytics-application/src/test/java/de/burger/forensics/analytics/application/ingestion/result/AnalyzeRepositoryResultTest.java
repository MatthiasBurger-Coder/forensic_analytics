package de.burger.forensics.analytics.application.ingestion.result;

import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.repository.CheckoutResult;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnalyzeRepositoryResultTest {
    @Test
    void acceptsExplicitCheckoutSummary() {
        var result = new AnalyzeRepositoryResult(
            new AnalysisRunId("analysis-1"),
            new WorkspaceId("workspace-1"),
            checkoutResult(),
            "Repository checkout accepted"
        );

        assertEquals("Repository checkout accepted", result.message());
    }

    @Test
    void rejectsMissingMessage() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new AnalyzeRepositoryResult(new AnalysisRunId("analysis-1"), new WorkspaceId("workspace-1"), checkoutResult(), null)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new AnalyzeRepositoryResult(new AnalysisRunId("analysis-1"), new WorkspaceId("workspace-1"), checkoutResult(), " ")
        );
    }

    private static CheckoutResult checkoutResult() {
        return new CheckoutResult(
            "https://example.invalid/project.git",
            Optional.of("main"),
            Optional.empty(),
            "abcdef",
            List.of(),
            "CHECKED_OUT",
            List.of("checkout mode: full clone")
        );
    }
}
