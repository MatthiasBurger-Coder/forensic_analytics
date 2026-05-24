package de.burger.forensics.analytics.services.queryreportapi.domain;

import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceBranchResponse;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceMetadataRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryReportApiWorkspaceTest {
    @Test
    void rejectsSpecialUseRepositoryTargetsAlignedWithPublicContract() {
        List.of(
            "https://localhost/acme/demo.git",
            "https://localhost./acme/demo.git",
            "https://example.local/acme/demo.git",
            "https://example.test/acme/demo.git",
            "https://test/acme/demo.git",
            "https://example.invalid/acme/demo.git",
            "https://invalid/acme/demo.git",
            "https://example.invalid./acme/demo.git",
            "https://example.example/acme/demo.git",
            "https://example/acme/demo.git",
            "https://10.0.0.1/acme/demo.git",
            "https://169.254.169.254/acme/demo.git",
            "https://192.31.196.1/acme/demo.git",
            "https://192.52.193.1/acme/demo.git",
            "https://192.175.48.1/acme/demo.git",
            "https://[0:0:0:0:0:0:0:0]/acme/demo.git",
            "https://[0:0:0:0:0:0:0:1]/acme/demo.git",
            "https://[0000:0000:0000:0000:0000:0000:0000:0001]/acme/demo.git",
            "https://[100::1]/acme/demo.git",
            "https://[64:ff9b::1]/acme/demo.git",
            "https://[0064:ff9b::1]/acme/demo.git",
            "https://[64:ff9b:1::1]/acme/demo.git",
            "https://[2001::1]/acme/demo.git",
            "https://[100:0:0:1::1]/acme/demo.git",
            "https://[2001:db8::1]/acme/demo.git",
            "https://[2001:0db8::1]/acme/demo.git",
            "https://[2002::1]/acme/demo.git",
            "https://[3fff::1]/acme/demo.git",
            "https://[5f00::1]/acme/demo.git",
            "https://[ff00::1]/acme/demo.git"
        ).forEach(url -> assertThrows(
            IllegalArgumentException.class,
            () -> metadataRequest(url),
            () -> "Repository URL should be rejected: " + url
        ));
    }

    @Test
    void rejectsDotSegmentsInSourceRoots() {
        List.of(".", "..", "src/.", "src/..", "src/../main", "src/./main")
            .forEach(sourceRoot -> assertThrows(
                IllegalArgumentException.class,
                () -> branchWithSourceRoot(sourceRoot),
                () -> "Source root should be rejected: " + sourceRoot
            ));

        assertDoesNotThrow(() -> branchWithSourceRoot("src/main/java"));
    }

    private static WorkspaceMetadataRequest metadataRequest(String repositoryUrl) {
        return new WorkspaceMetadataRequest(
            "request-1",
            "idem-1",
            "query-report-workspace.v1",
            "correlation-1",
            repositoryUrl,
            60
        );
    }

    private static WorkspaceBranchResponse branchWithSourceRoot(String sourceRoot) {
        return new WorkspaceBranchResponse(
            "workspace-branch-0001",
            "main",
            "CHECKED_OUT",
            "abcdef1",
            "source-snapshot-0001",
            List.of(sourceRoot),
            List.of()
        );
    }
}
