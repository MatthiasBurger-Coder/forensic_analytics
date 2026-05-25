package de.burger.forensics.analytics.services.queryreportapi.application.port;

import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.BranchRefreshResponse;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.CreateWorkspaceRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.GetWorkspaceRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.RefreshWorkspaceBranchRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceMetadataRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceMetadataResponse;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceResponse;

public interface RepositoryWorkspaceOwnerPort {
    WorkspaceMetadataResponse previewMetadata(WorkspaceMetadataRequest request);

    WorkspaceResponse create(CreateWorkspaceRequest request);

    WorkspaceResponse get(GetWorkspaceRequest request);

    BranchRefreshResponse refresh(RefreshWorkspaceBranchRequest request);
}
