package de.burger.forensics.analytics.services.queryreportapi.application.port;

import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.BranchRefreshResponse;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.CleanupWorkspaceRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.CreateWorkspaceRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.GetWorkspaceRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.ListWorkspacesRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.RefreshWorkspaceBranchRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceCleanupResponse;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceListResponse;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceMetadataRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceMetadataResponse;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceResponse;

public interface RepositoryWorkspaceOwnerPort {
    WorkspaceMetadataResponse previewMetadata(WorkspaceMetadataRequest request);

    WorkspaceResponse create(CreateWorkspaceRequest request);

    WorkspaceResponse get(GetWorkspaceRequest request);

    WorkspaceListResponse list(ListWorkspacesRequest request);

    WorkspaceCleanupResponse cleanup(CleanupWorkspaceRequest request);

    BranchRefreshResponse refresh(RefreshWorkspaceBranchRequest request);
}
