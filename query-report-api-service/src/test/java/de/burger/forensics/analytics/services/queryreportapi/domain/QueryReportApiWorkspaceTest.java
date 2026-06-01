package de.burger.forensics.analytics.services.queryreportapi.domain;

import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceBranchResponse;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.BranchRefreshResponse;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.CleanupWorkspaceRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.CreateWorkspaceRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.GetWorkspaceRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.ListWorkspacesRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.PublicRepositoryIdentity;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.RefreshWorkspaceBranchRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.RepositoryIdentity;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceCleanupResponse;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceFacadeConfiguration;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceListItemResponse;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceListResponse;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceMetadataRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceMetadataResponse;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspacePolicy;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void validatesWorkspaceRequestPoliciesAndPublicResponseDtos() {
        var policy = policy();
        var repository = repository();
        var branch = branchWithSourceRoot("src/main/java");
        var workspace = new WorkspaceResponse(
            "workspace-0001",
            "demo",
            repository,
            List.of(branch),
            "checked_out",
            null
        );
        var metadata = new WorkspaceMetadataResponse(
            "EXAMPLE.COM/ACME/DEMO",
            "example.com",
            " ",
            "demo",
            "demo",
            " ",
            List.of("main", "release/1.0"),
            null
        );
        var refresh = new BranchRefreshResponse(
            "workspace-branch-0001",
            "main",
            "updated",
            true,
            "ABCDEF1",
            "FEDCBA2",
            "source-snapshot-0001",
            null
        );

        assertEquals("CHECKED_OUT", workspace.status());
        assertEquals(List.of(), workspace.diagnostics());
        assertEquals("example.com/acme/demo", metadata.repositoryKey());
        assertEquals(null, metadata.repositoryOwner());
        assertEquals(null, metadata.defaultBranch());
        assertEquals("abcdef1", refresh.previousCommit());
        assertEquals("fedcba2", refresh.resolvedCommit());
        assertEquals("", new CreateWorkspaceRequest(
            "request-1",
            "idem-1",
            "query-report-workspace.v1",
            "correlation-1",
            "https://example.com/acme/demo.git",
            null,
            policy
        ).selectedBranch());
        assertDoesNotThrow(() -> new WorkspaceFacadeConfiguration("query-report-workspace.v1", 60, policy));
        assertDoesNotThrow(() -> new RefreshWorkspaceBranchRequest(
            "request-1",
            "idem-1",
            "query-report-workspace.v1",
            "correlation-1",
            "workspace-0001",
            "workspace-branch-0001",
            policy
        ));
        assertDoesNotThrow(() -> new GetWorkspaceRequest("request-1", "correlation-1", "workspace-0001"));
    }

    @Test
    void validatesWorkspaceListAndCleanupPublicDtos() {
        var item = new WorkspaceListItemResponse(
            "workspace-0001",
            "demo",
            publicRepository(),
            List.of(branchWithSourceRoot("src/main/java")),
            "checked_out",
            null
        );
        var list = new WorkspaceListResponse(List.of(item), null);
        var emptyList = new WorkspaceListResponse(null, null);
        var cleanup = new WorkspaceCleanupResponse("workspace-0001", "cleaned", null);
        var listRequest = new ListWorkspacesRequest("request-1", "query-report-workspace.v1", "correlation-1", false);
        var cleanupRequest = new CleanupWorkspaceRequest(
            "request-2",
            "idem-1",
            "query-report-workspace.v1",
            "correlation-2",
            "workspace-0001"
        );

        assertEquals("CHECKED_OUT", item.status());
        assertEquals("example.com/acme/demo", item.repository().repositoryKey());
        assertEquals(List.of(), list.diagnostics());
        assertEquals(List.of(), emptyList.items());
        assertEquals("CLEANED", cleanup.status());
        assertEquals(false, listRequest.includeCleaned());
        assertEquals("workspace-0001", cleanupRequest.workspaceId());
        assertThrows(UnsupportedOperationException.class, () -> list.items().add(item));
    }

    @Test
    void rejectsUnsafeWorkspaceRequestPoliciesAndPublicResponseFields() {
        assertThrows(IllegalArgumentException.class, () -> new WorkspaceFacadeConfiguration("schema", 0, policy()));
        assertThrows(IllegalArgumentException.class, () -> new WorkspaceFacadeConfiguration("schema", 3_601, policy()));
        assertThrows(NullPointerException.class, () -> new WorkspaceFacadeConfiguration("schema", 60, null));
        assertThrows(IllegalArgumentException.class, () -> metadataRequest("https://example.com/acme/demo.git", 0));
        assertThrows(IllegalArgumentException.class, () -> metadataRequest("https://example.com/acme/demo.git", 3_601));
        assertThrows(IllegalArgumentException.class, () -> new CreateWorkspaceRequest(
            "request-1",
            "idem-1",
            "query-report-workspace.v1",
            "correlation-1",
            "https://example.com/acme/demo.git",
            " ",
            policy()
        ));
        assertThrows(NullPointerException.class, () -> new CreateWorkspaceRequest(
            "request-1",
            "idem-1",
            "query-report-workspace.v1",
            "correlation-1",
            "https://example.com/acme/demo.git",
            "main",
            null
        ));
        assertThrows(IllegalArgumentException.class, () -> new WorkspacePolicy(true, true, false, false, 60, 100_000));
        assertThrows(IllegalArgumentException.class, () -> new WorkspacePolicy(false, true, true, false, 60, 100_000));
        assertThrows(IllegalArgumentException.class, () -> new WorkspacePolicy(false, true, false, true, 60, 100_000));
        assertThrows(IllegalArgumentException.class, () -> new WorkspacePolicy(false, true, false, false, 0, 100_000));
        assertThrows(IllegalArgumentException.class, () -> new WorkspacePolicy(false, true, false, false, 3_601, 100_000));
        assertThrows(IllegalArgumentException.class, () -> new WorkspacePolicy(false, true, false, false, 60, 0));
        assertThrows(IllegalArgumentException.class, () -> new WorkspacePolicy(false, true, false, false, 60, 107_374_182_401L));
        assertThrows(IllegalArgumentException.class, () -> new WorkspaceMetadataResponse(
            "https://example.com/acme/demo",
            "example.com",
            "acme",
            "demo",
            "demo",
            "main",
            List.of("main"),
            List.of()
        ));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryIdentity(
            "example.com/acme/demo",
            "https://user@example.com/acme/demo.git",
            "example.com",
            "acme",
            "demo",
            "main"
        ));
        assertThrows(IllegalArgumentException.class, () -> new WorkspaceResponse(
            "workspace-0001",
            "https://example.com/acme/demo.git",
            repository(),
            List.of(),
            "READY",
            List.of()
        ));
        assertThrows(IllegalArgumentException.class, () -> new WorkspaceResponse(
            "workspace-0001",
            "demo",
            repository(),
            List.of(),
            "INTERNAL",
            List.of()
        ));
        assertThrows(IllegalArgumentException.class, () -> branchWithBranch("-main"));
        assertThrows(IllegalArgumentException.class, () -> branchWithBranch("feature..main"));
        assertThrows(IllegalArgumentException.class, () -> branchWithBranch("main\nnext"));
        assertThrows(IllegalArgumentException.class, () -> branchWithCommit("not-a-commit"));
        assertThrows(IllegalArgumentException.class, () -> branchWithSourceSnapshot("source-snapshot-/tmp/demo"));
        assertThrows(IllegalArgumentException.class, () -> branchWithSourceRoot("/src/main/java"));
        assertThrows(IllegalArgumentException.class, () -> branchWithSourceRoot("\\src\\main\\java"));
        assertThrows(IllegalArgumentException.class, () -> new BranchRefreshResponse(
            "workspace-branch-0001",
            "main",
            "INTERNAL",
            false,
            null,
            "abcdef1",
            "source-snapshot-0001",
            List.of()
        ));
        assertThrows(IllegalArgumentException.class, () -> new GetWorkspaceRequest("request-1", "correlation-1", "bad"));
        assertThrows(IllegalArgumentException.class, () -> new RefreshWorkspaceBranchRequest(
            "request-1",
            "idem-1",
            "query-report-workspace.v1",
            "correlation-1",
            "workspace-0001",
            "bad",
            policy()
        ));
        assertThrows(IllegalArgumentException.class, () -> new ListWorkspacesRequest(" ", "schema", "correlation-1", false));
        assertThrows(IllegalArgumentException.class, () -> new CleanupWorkspaceRequest(
            "request-1",
            "idem-1",
            "schema",
            "correlation-1",
            "bad"
        ));
        assertThrows(IllegalArgumentException.class, () -> new WorkspaceCleanupResponse(
            "workspace-0001",
            "READY",
            List.of()
        ));
        assertThrows(IllegalArgumentException.class, () -> new WorkspaceListItemResponse(
            "bad",
            "demo",
            publicRepository(),
            List.of(),
            "READY",
            List.of()
        ));
    }

    @Test
    void validatesLeakDetectionRepositoryKeysAndNullablePublicWorkspaceFields() {
        assertEquals(null, new WorkspaceBranchResponse(
            "workspace-branch-0001",
            "main",
            "CHECKING_OUT",
            " ",
            "",
            null,
            null
        ).resolvedCommit());
        assertEquals(null, new BranchRefreshResponse(
            "workspace-branch-0001",
            "main",
            "UP_TO_DATE",
            false,
            "",
            null,
            "",
            null
        ).sourceSnapshotId());
        assertThrows(IllegalArgumentException.class, () -> metadataRequest(null));
        assertThrows(IllegalArgumentException.class, () -> metadataRequest("http://example.com/acme/demo.git"));
        assertThrows(IllegalArgumentException.class, () -> metadataRequest("https://user@example.com/acme/demo.git"));
        assertThrows(IllegalArgumentException.class, () -> metadataRequest("https://example.com/acme/demo.git?token=x"));
        assertThrows(IllegalArgumentException.class, () -> metadataRequest("https://example.com/acme/demo.git#fragment"));

        List.of(
            "",
            "example.com/acme",
            "example.com/acme/demo/extra",
            "example.com/acme/de:mo",
            "example.com/acme/de?mo",
            "example.com/acme/de#mo",
            "example.com/acme/de@mo",
            "example.com/acme\\demo"
        ).forEach(repositoryKey -> assertThrows(
            IllegalArgumentException.class,
            () -> new WorkspaceMetadataResponse(repositoryKey, "example.com", "acme", "demo", "demo", "main", List.of("main"), List.of())
        ));

        List.of(
            "file:/tmp/repo",
            "jdbc:h2:file:./build/repository-source-data/repository-source",
            "/tmp/demo",
            "C:\\Users\\demo\\repo",
            "https://example.com/private.git",
            "http://example.com/private.git",
            "/mnt/d/workspace",
            "/home/demo/repo",
            "/users/demo/repo",
            "/var/lib/forensic-analytics/repository-workspaces/workspace-1",
            "repository-source-data",
            "repository-workspaces",
            "raw stdout from git",
            "raw stderr from git",
            "stdout",
            "stderr",
            "token-value",
            "password-value",
            "secret-value",
            "credential-value",
            "authorization-header"
        ).forEach(leakingValue -> assertThrows(
            IllegalArgumentException.class,
            () -> new WorkspaceMetadataResponse(
                "example.com/acme/demo",
                "example.com",
                "acme",
                "demo",
                leakingValue,
                "main",
                List.of("main"),
                List.of()
            ),
            () -> "Public workspace value should be rejected: " + leakingValue
        ));

        assertThrows(IllegalArgumentException.class, () -> branchWithBranch("C:\\feature"));
        assertThrows(IllegalArgumentException.class, () -> branchWithBranch("feature\rnext"));
        assertThrows(IllegalArgumentException.class, () -> branchWithSourceRoot("C:/src/main/java"));
        assertThrows(IllegalArgumentException.class, () -> new PublicRepositoryIdentity(
            "example.com/acme/demo",
            "/tmp/repository-source-data",
            "acme",
            "demo"
        ));
        assertThrows(IllegalArgumentException.class, () -> new WorkspaceListItemResponse(
            "workspace-0001",
            "https://example.com/acme/demo.git",
            publicRepository(),
            List.of(),
            "READY",
            List.of()
        ));
    }

    private static WorkspaceMetadataRequest metadataRequest(String repositoryUrl) {
        return metadataRequest(repositoryUrl, 60);
    }

    private static WorkspaceMetadataRequest metadataRequest(String repositoryUrl, long timeoutSeconds) {
        return new WorkspaceMetadataRequest(
            "request-1",
            "idem-1",
            "query-report-workspace.v1",
            "correlation-1",
            repositoryUrl,
            timeoutSeconds
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

    private static WorkspaceBranchResponse branchWithBranch(String branch) {
        return new WorkspaceBranchResponse(
            "workspace-branch-0001",
            branch,
            "CHECKED_OUT",
            "abcdef1",
            "source-snapshot-0001",
            List.of("src/main/java"),
            List.of()
        );
    }

    private static WorkspaceBranchResponse branchWithCommit(String commit) {
        return new WorkspaceBranchResponse(
            "workspace-branch-0001",
            "main",
            "CHECKED_OUT",
            commit,
            "source-snapshot-0001",
            List.of("src/main/java"),
            List.of()
        );
    }

    private static WorkspaceBranchResponse branchWithSourceSnapshot(String sourceSnapshotId) {
        return new WorkspaceBranchResponse(
            "workspace-branch-0001",
            "main",
            "CHECKED_OUT",
            "abcdef1",
            sourceSnapshotId,
            List.of("src/main/java"),
            List.of()
        );
    }

    private static RepositoryIdentity repository() {
        return new RepositoryIdentity(
            "example.com/acme/demo",
            "https://example.com/acme/demo.git",
            "example.com",
            "acme",
            "demo",
            "main"
        );
    }

    private static PublicRepositoryIdentity publicRepository() {
        return new PublicRepositoryIdentity(
            "example.com/acme/demo",
            "example.com",
            "acme",
            "demo"
        );
    }

    private static WorkspacePolicy policy() {
        return new WorkspacePolicy(false, true, false, false, 60, 100_000);
    }
}
