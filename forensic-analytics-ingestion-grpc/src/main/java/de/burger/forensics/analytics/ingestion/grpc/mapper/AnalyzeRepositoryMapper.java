package de.burger.forensics.analytics.ingestion.grpc.mapper;

import de.burger.forensics.analytics.application.ingestion.command.AnalyzeRepositoryCommand;
import de.burger.forensics.analytics.application.ingestion.command.BuildContextCommand;
import de.burger.forensics.analytics.application.ingestion.result.AnalyzeRepositoryResult;
import de.burger.forensics.analytics.domain.repository.BranchReference;
import de.burger.forensics.analytics.domain.repository.CheckoutResult;
import de.burger.forensics.analytics.domain.repository.CommitReference;
import de.burger.forensics.analytics.domain.repository.RepositoryReference;
import de.burger.forensics.analytics.domain.repository.SourceRoot;
import de.burger.forensics.analytics.domain.workspace.WorkspaceCleanupPolicy;
import de.burger.forensics.analytics.domain.workspace.WorkspacePolicy;
import de.burger.forensics.analytics.ingestion.v1.AnalysisSessionId;
import de.burger.forensics.analytics.ingestion.v1.AnalyzeRepositoryRequest;
import de.burger.forensics.analytics.ingestion.v1.AnalyzeRepositoryResponse;
import de.burger.forensics.analytics.ingestion.v1.WorkspaceId;

import java.time.Duration;
import java.util.Optional;

public final class AnalyzeRepositoryMapper {
    public AnalyzeRepositoryCommand toCommand(AnalyzeRepositoryRequest request) {
        return new AnalyzeRepositoryCommand(
            new RepositoryReference(
                request.getRepository().getRemoteUrl(),
                optionalText(request.getRepository().getProvider()),
                request.getRepository().getAttributesMap()
            ),
            new BranchReference(optionalText(request.getBranch().getName()), request.getBranch().getRequired()),
            new CommitReference(optionalText(request.getCommit().getHash()), request.getCommit().getRequired()),
            new WorkspacePolicy(
                request.getWorkspacePolicy().getEphemeral(),
                request.getWorkspacePolicy().getAllowShallowClone(),
                request.getWorkspacePolicy().getAllowPartialClone(),
                request.getWorkspacePolicy().getAllowSparseCheckout(),
                Duration.ofSeconds(request.getWorkspacePolicy().getTimeoutSeconds()),
                request.getWorkspacePolicy().getMaxWorkspaceBytes(),
                request.getWorkspacePolicy().getEphemeral()
                    ? WorkspaceCleanupPolicy.DELETE_ON_COMPLETION
                    : WorkspaceCleanupPolicy.RETAIN_FOR_REVIEW
            ),
            new BuildContextCommand(
                request.getBuildContext().getBuildTool(),
                request.getBuildContext().getBuildId(),
                request.getBuildContext().getRootProjectName(),
                request.getBuildContext().getDeclaredModulesList(),
                request.getBuildContext().getAttributesMap()
            ),
            request.getRequestId(),
            request.getSchemaVersion()
        );
    }

    public AnalyzeRepositoryResponse toProto(AnalyzeRepositoryResult result) {
        return AnalyzeRepositoryResponse.newBuilder()
            .setAnalysisSessionId(AnalysisSessionId.newBuilder().setValue(result.analysisSessionId().value()))
            .setWorkspaceId(WorkspaceId.newBuilder().setValue(result.workspaceId().value()))
            .setCheckoutResult(toProto(result.checkoutResult()))
            .setMessage(result.message())
            .build();
    }

    private de.burger.forensics.analytics.ingestion.v1.CheckoutResult toProto(CheckoutResult checkoutResult) {
        var builder = de.burger.forensics.analytics.ingestion.v1.CheckoutResult.newBuilder()
            .setResolvedRemoteUrl(checkoutResult.resolvedRemoteUrl())
            .setResolvedCommit(checkoutResult.resolvedCommit())
            .setCheckoutStatus(checkoutResult.checkoutStatus())
            .addAllDetectedSourceRoots(checkoutResult.detectedSourceRoots().stream().map(SourceRoot::path).toList())
            .addAllDiagnostics(checkoutResult.diagnostics());
        checkoutResult.requestedBranch().ifPresent(builder::setRequestedBranch);
        checkoutResult.requestedCommit().ifPresent(builder::setRequestedCommit);
        return builder.build();
    }

    private static Optional<String> optionalText(String value) {
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
    }
}
