package de.burger.forensics.analytics.services.queryreportapi.application;

import de.burger.forensics.analytics.services.queryreportapi.application.port.RepositoryWorkspaceOwnerPort;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.BranchRefreshResponse;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.CreateWorkspaceRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.GetWorkspaceRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.RefreshWorkspaceBranchRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceFacadeConfiguration;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceMetadataRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceMetadataResponse;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspacePolicy;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiWorkspace.WorkspaceResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class QueryReportApiWorkspaceService {
    private final RepositoryWorkspaceOwnerPort ownerPort;
    private final WorkspaceFacadeConfiguration configuration;
    private final Map<String, IdempotentMetadataPreview> metadataPreviews = new HashMap<>();

    public QueryReportApiWorkspaceService(
        RepositoryWorkspaceOwnerPort ownerPort,
        WorkspaceFacadeConfiguration configuration
    ) {
        this.ownerPort = Objects.requireNonNull(ownerPort, "repository workspace owner port must not be null");
        this.configuration = Objects.requireNonNull(configuration, "workspace facade configuration must not be null");
    }

    public synchronized WorkspaceMetadataResponse previewMetadata(
        String requestId,
        String idempotencyKey,
        String correlationId,
        String repositoryUrl
    ) {
        var request = new WorkspaceMetadataRequest(
            requestId,
            idempotencyKey,
            configuration.schemaVersion(),
            correlationId,
            repositoryUrl,
            configuration.metadataTimeoutSeconds()
        );
        var fingerprint = request.fingerprint();
        var replay = metadataPreviews.get(request.idempotencyKey());
        if (replay != null) {
            return replay.sameFingerprintOrThrow(fingerprint);
        }

        var preview = ownerPort.previewMetadata(request);
        metadataPreviews.put(request.idempotencyKey(), new IdempotentMetadataPreview(fingerprint, preview));
        return preview;
    }

    public WorkspaceResponse create(
        String requestId,
        String idempotencyKey,
        String correlationId,
        String repositoryUrl,
        String selectedBranch,
        WorkspacePolicy workspacePolicy
    ) {
        return ownerPort.create(new CreateWorkspaceRequest(
            requestId,
            idempotencyKey,
            configuration.schemaVersion(),
            correlationId,
            repositoryUrl,
            selectedBranch,
            workspacePolicy
        ));
    }

    public WorkspaceResponse get(String requestId, String correlationId, String workspaceId) {
        return ownerPort.get(new GetWorkspaceRequest(requestId, correlationId, workspaceId));
    }

    public BranchRefreshResponse refresh(
        String requestId,
        String idempotencyKey,
        String correlationId,
        String workspaceId,
        String workspaceBranchId
    ) {
        return ownerPort.refresh(new RefreshWorkspaceBranchRequest(
            requestId,
            idempotencyKey,
            configuration.schemaVersion(),
            correlationId,
            workspaceId,
            workspaceBranchId,
            configuration.refreshPolicy()
        ));
    }

    private record IdempotentMetadataPreview(String fingerprint, WorkspaceMetadataResponse response) {
        private WorkspaceMetadataResponse sameFingerprintOrThrow(String requestedFingerprint) {
            if (!fingerprint.equals(requestedFingerprint)) {
                throw new QueryReportApiIdempotencyConflictException("idempotency key was reused with different input");
            }
            return response;
        }
    }
}
