package de.burger.forensics.analytics.ingestion.grpc.validator;

import de.burger.forensics.analytics.ingestion.v1.StartAnalysisSessionRequest;

public final class StartAnalysisSessionRequestValidator {
    public void validate(StartAnalysisSessionRequest request) {
        if (!request.hasBuildIdentity()) {
            throw new ValidationException("buildIdentity must be present");
        }
        if (!request.hasPluginIdentity()) {
            throw new ValidationException("pluginIdentity must be present");
        }

        var buildIdentity = request.getBuildIdentity();
        RequiredFields.nonBlank(buildIdentity.getProjectId(), "projectId");
        RequiredFields.nonBlank(buildIdentity.getRepositoryUrl(), "repositoryUrl");
        RequiredFields.nonBlank(buildIdentity.getCommitHash(), "commitHash");
        RequiredFields.nonBlank(buildIdentity.getBuildId(), "buildId");

        var pluginIdentity = request.getPluginIdentity();
        RequiredFields.nonBlank(pluginIdentity.getPluginName(), "pluginName");
        RequiredFields.nonBlank(pluginIdentity.getPluginVersion(), "pluginVersion");
        RequiredFields.nonBlank(request.getSchemaVersion(), "schemaVersion");
    }
}
