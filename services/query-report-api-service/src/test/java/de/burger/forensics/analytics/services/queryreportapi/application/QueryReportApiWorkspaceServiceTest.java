package de.burger.forensics.analytics.services.queryreportapi.application;

import de.burger.forensics.analytics.services.queryreportapi.application.port.RepositoryWorkspaceOwnerPort;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.Diagnostic;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.BranchRefreshResponse;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.CreateWorkspaceRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.GetWorkspaceRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.RefreshWorkspaceBranchRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.RepositoryIdentity;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceBranchResponse;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceFacadeConfiguration;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceMetadataRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceMetadataResponse;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspacePolicy;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryReportApiWorkspaceServiceTest {
    @Test
    void previewsRepositoryMetadataIdempotentlyInsideFacade() {
        var owner = new RecordingWorkspacePort();
        var service = service(owner);

        var first = service.previewMetadata(
            "request-1",
            "idem-1",
            "correlation-1",
            "https://example.com/acme/demo.git"
        );
        var replay = service.previewMetadata(
            "request-2",
            "idem-1",
            "correlation-1",
            "https://example.com/acme/demo.git"
        );

        assertEquals(first, replay);
        assertEquals(1, owner.previewCalls.get());
        assertEquals("query-report-workspace.v1", owner.lastMetadataRequest.schemaVersion());
        assertEquals(60, owner.lastMetadataRequest.metadataTimeoutSeconds());

        assertThrows(
            QueryReportApiIdempotencyConflictException.class,
            () -> service.previewMetadata(
                "request-3",
                "idem-1",
                "correlation-1",
                "https://example.com/acme/other.git"
            )
        );
        assertEquals(1, owner.previewCalls.get());
    }

    @Test
    void delegatesCreateGetAndRefreshWithConfiguredSchemaAndRefreshPolicy() {
        var owner = new RecordingWorkspacePort();
        var service = service(owner);
        var policy = new WorkspacePolicy(false, true, false, false, 60, 1_073_741_824L);

        var created = service.create(
            "request-create",
            "idem-create",
            "correlation-1",
            "https://example.com/acme/demo.git",
            "main",
            policy
        );
        var loaded = service.get("request-get", "correlation-2", "workspace-0001");
        var refreshed = service.refresh(
            "request-refresh",
            "idem-refresh",
            "correlation-3",
            "workspace-0001",
            "workspace-branch-0001"
        );

        assertEquals("workspace-0001", created.workspaceId());
        assertEquals("workspace-0001", loaded.workspaceId());
        assertEquals("UP_TO_DATE", refreshed.status());
        assertEquals("query-report-workspace.v1", owner.lastCreateRequest.schemaVersion());
        assertEquals("query-report-workspace.v1", owner.lastRefreshRequest.schemaVersion());
        assertEquals(policy, owner.lastCreateRequest.workspacePolicy());
        assertEquals(policy, owner.lastRefreshRequest.workspacePolicy());
        assertEquals("workspace-0001", owner.lastGetRequest.workspaceId());
    }

    private static QueryReportApiWorkspaceService service(RecordingWorkspacePort owner) {
        return new QueryReportApiWorkspaceService(
            owner,
            new WorkspaceFacadeConfiguration(
                "query-report-workspace.v1",
                60,
                new WorkspacePolicy(false, true, false, false, 60, 1_073_741_824L)
            )
        );
    }

    private static final class RecordingWorkspacePort implements RepositoryWorkspaceOwnerPort {
        private final AtomicInteger previewCalls = new AtomicInteger();
        private WorkspaceMetadataRequest lastMetadataRequest;
        private CreateWorkspaceRequest lastCreateRequest;
        private GetWorkspaceRequest lastGetRequest;
        private RefreshWorkspaceBranchRequest lastRefreshRequest;

        @Override
        public WorkspaceMetadataResponse previewMetadata(WorkspaceMetadataRequest request) {
            previewCalls.incrementAndGet();
            lastMetadataRequest = request;
            return new WorkspaceMetadataResponse(
                "example.com/acme/demo",
                "example.com",
                "acme",
                "demo",
                "demo",
                "main",
                List.of(Diagnostic.info("METADATA_READY", "Repository metadata loaded"))
            );
        }

        @Override
        public WorkspaceResponse create(CreateWorkspaceRequest request) {
            lastCreateRequest = request;
            return workspace();
        }

        @Override
        public WorkspaceResponse get(GetWorkspaceRequest request) {
            lastGetRequest = request;
            return workspace();
        }

        @Override
        public BranchRefreshResponse refresh(RefreshWorkspaceBranchRequest request) {
            lastRefreshRequest = request;
            return new BranchRefreshResponse(
                "workspace-branch-0001",
                "main",
                "UP_TO_DATE",
                false,
                null,
                "abcdef1",
                "source-snapshot-0001",
                List.of(Diagnostic.info("BRANCH_READY", "Branch already up to date"))
            );
        }

        private static WorkspaceResponse workspace() {
            return new WorkspaceResponse(
                "workspace-0001",
                "demo",
                new RepositoryIdentity(
                    "example.com/acme/demo",
                    "https://example.com/acme/demo.git",
                    "example.com",
                    "acme",
                    "demo",
                    "main"
                ),
                List.of(new WorkspaceBranchResponse(
                    "workspace-branch-0001",
                    "main",
                    "CHECKED_OUT",
                    "abcdef1",
                    "source-snapshot-0001",
                    List.of("src/main/java"),
                    List.of(Diagnostic.info("CHECKOUT_READY", "Checkout completed"))
                )),
                "CHECKED_OUT",
                List.of(Diagnostic.info("WORKSPACE_READY", "Workspace loaded"))
            );
        }
    }
}
