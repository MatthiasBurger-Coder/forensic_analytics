package de.burger.forensics.analytics.ingestion.grpc.validator;

import de.burger.forensics.analytics.ingestion.v1.AnalyzeRepositoryRequest;
import de.burger.forensics.analytics.ingestion.v1.BuildContext;
import de.burger.forensics.analytics.ingestion.v1.WorkspacePolicy;

import java.util.Map;

public final class AnalyzeRepositoryRequestValidator {
    public void validate(AnalyzeRepositoryRequest request) {
        RequiredFields.present(request.hasRepository(), "repository");
        RequiredFields.present(request.hasWorkspacePolicy(), "workspacePolicy");
        RequiredFields.present(request.hasBuildContext(), "buildContext");
        RequiredFields.nonBlank(request.getRequestId(), "requestId");
        RequiredFields.nonBlank(request.getSchemaVersion(), "schemaVersion");

        validateRepository(request);
        validateRevision(request);
        validateWorkspacePolicy(request.getWorkspacePolicy());
        validateBuildContext(request.getBuildContext());
    }

    private void validateRepository(AnalyzeRepositoryRequest request) {
        var repository = request.getRepository();
        RequiredFields.nonBlank(repository.getRemoteUrl(), "repository.remoteUrl");
        RequiredFields.nonBlank(repository.getProvider(), "repository.provider");
        validateAttributes(repository.getAttributesMap(), "repository.attributes");
    }

    private void validateRevision(AnalyzeRepositoryRequest request) {
        var branchName = request.hasBranch() ? request.getBranch().getName() : "";
        var commitHash = request.hasCommit() ? request.getCommit().getHash() : "";
        if (branchName.isBlank() && commitHash.isBlank()) {
            throw new ValidationException("branch.name or commit.hash must be present");
        }
        if (request.hasBranch()) {
            RequiredFields.notBlankWhenPresent(branchName, "branch.name");
            if (request.getBranch().getRequired()) {
                RequiredFields.nonBlank(branchName, "branch.name");
            }
        }
        if (request.hasCommit()) {
            RequiredFields.notBlankWhenPresent(commitHash, "commit.hash");
            if (request.getCommit().getRequired()) {
                RequiredFields.nonBlank(commitHash, "commit.hash");
            }
        }
    }

    private void validateWorkspacePolicy(WorkspacePolicy workspacePolicy) {
        RequiredFields.nonNegative(workspacePolicy.getTimeoutSeconds(), "workspacePolicy.timeoutSeconds");
        RequiredFields.nonNegative(workspacePolicy.getMaxWorkspaceBytes(), "workspacePolicy.maxWorkspaceBytes");
    }

    private void validateBuildContext(BuildContext buildContext) {
        RequiredFields.nonBlank(buildContext.getBuildTool(), "buildContext.buildTool");
        RequiredFields.nonBlank(buildContext.getBuildId(), "buildContext.buildId");
        RequiredFields.nonBlank(buildContext.getRootProjectName(), "buildContext.rootProjectName");
        buildContext.getDeclaredModulesList()
            .forEach(module -> RequiredFields.nonBlank(module, "buildContext.declaredModules"));
        validateAttributes(buildContext.getAttributesMap(), "buildContext.attributes");
    }

    private void validateAttributes(Map<String, String> attributes, String fieldName) {
        attributes.forEach((key, value) -> {
            RequiredFields.nonBlank(key, fieldName + ".key");
            RequiredFields.nonBlank(value, fieldName + "[" + key + "]");
        });
    }
}
