package de.burger.forensics.analytics.ingestion.grpc.mapper;

import de.burger.forensics.analytics.application.ingestion.result.AnalyzeRepositoryResult;
import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.repository.CheckoutResult;
import de.burger.forensics.analytics.domain.repository.SourceRoot;
import de.burger.forensics.analytics.domain.workspace.WorkspaceCleanupPolicy;
import de.burger.forensics.analytics.ingestion.v1.AnalyzeRepositoryRequest;
import de.burger.forensics.analytics.ingestion.v1.BranchReference;
import de.burger.forensics.analytics.ingestion.v1.BuildContext;
import de.burger.forensics.analytics.ingestion.v1.CommitReference;
import de.burger.forensics.analytics.ingestion.v1.RepositoryReference;
import de.burger.forensics.analytics.ingestion.v1.WorkspacePolicy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyzeRepositoryMapperTest {
    private final AnalyzeRepositoryMapper mapper = new AnalyzeRepositoryMapper();

    @Test
    void mapsAnalyzeRepositoryRequestToApplicationCommand() {
        var command = mapper.toCommand(validRequest());

        assertEquals("https://example.invalid/repo.git", command.repository().remoteUrl());
        assertEquals(Optional.of("git"), command.repository().provider());
        assertEquals(Optional.of("main"), command.branch().name());
        assertTrue(command.branch().required());
        assertEquals(Optional.of("abcdef"), command.commit().hash());
        assertTrue(command.commit().required());
        assertTrue(command.workspacePolicy().ephemeral());
        assertEquals(WorkspaceCleanupPolicy.DELETE_ON_COMPLETION, command.workspacePolicy().cleanupPolicy());
        assertEquals("gradle", command.buildContext().buildTool());
        assertEquals(List.of(":"), command.buildContext().declaredModules());
        assertEquals("request-1", command.requestId());
        assertEquals("workspace-grpc-v1", command.schemaVersion());
    }

    @Test
    void mapsAnalyzeRepositoryResultToProtoResponse() {
        var response = mapper.toProto(new AnalyzeRepositoryResult(
            new AnalysisRunId("analysis-session-1"),
            new de.burger.forensics.analytics.domain.workspace.WorkspaceId("workspace-1"),
            new CheckoutResult(
                "https://example.invalid/repo.git",
                Optional.of("main"),
                Optional.of("abcdef"),
                "abcdef",
                List.of(new SourceRoot("src/main/java")),
                "CHECKED_OUT",
                List.of("checkout mode: full clone")
            ),
            "Repository analysis session registered"
        ));

        assertEquals("analysis-session-1", response.getAnalysisSessionId().getValue());
        assertEquals("workspace-1", response.getWorkspaceId().getValue());
        assertEquals("https://example.invalid/repo.git", response.getCheckoutResult().getResolvedRemoteUrl());
        assertEquals("main", response.getCheckoutResult().getRequestedBranch());
        assertEquals("abcdef", response.getCheckoutResult().getResolvedCommit());
        assertEquals(List.of("src/main/java"), response.getCheckoutResult().getDetectedSourceRootsList());
        assertEquals(List.of("checkout mode: full clone"), response.getCheckoutResult().getDiagnosticsList());
    }

    private AnalyzeRepositoryRequest validRequest() {
        return AnalyzeRepositoryRequest.newBuilder()
            .setRepository(RepositoryReference.newBuilder()
                .setRemoteUrl("https://example.invalid/repo.git")
                .setProvider("git")
                .putAttributes("hosting", "example"))
            .setBranch(BranchReference.newBuilder()
                .setName("main")
                .setRequired(true))
            .setCommit(CommitReference.newBuilder()
                .setHash("abcdef")
                .setRequired(true))
            .setWorkspacePolicy(WorkspacePolicy.newBuilder()
                .setEphemeral(true)
                .setAllowShallowClone(true)
                .setTimeoutSeconds(60)
                .setMaxWorkspaceBytes(1_048_576))
            .setBuildContext(BuildContext.newBuilder()
                .setBuildTool("gradle")
                .setBuildId("build-1")
                .setRootProjectName("sample")
                .addDeclaredModules(":")
                .putAttributes("java", "25"))
            .setRequestId("request-1")
            .setSchemaVersion("workspace-grpc-v1")
            .build();
    }
}
