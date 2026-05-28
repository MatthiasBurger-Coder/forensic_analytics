package de.burger.forensics.analytics.services.ingestion.adapter.in.grpc;

import de.burger.forensics.analytics.ingestion.v1.AnalysisDataEnvelope;
import de.burger.forensics.analytics.ingestion.v1.AnalysisPayloadDescriptor;
import de.burger.forensics.analytics.ingestion.v1.AnalysisPayloadKind;
import de.burger.forensics.analytics.ingestion.v1.BuildIdentity;
import de.burger.forensics.analytics.ingestion.v1.PluginIdentity;
import de.burger.forensics.analytics.ingestion.v1.StartAnalysisSessionRequest;

import java.util.Map;

public final class ForensicIngestionRequestValidator {
    public void validate(StartAnalysisSessionRequest request) {
        RequiredFields.present(request.hasBuildIdentity(), "buildIdentity");
        RequiredFields.present(request.hasPluginIdentity(), "pluginIdentity");
        validate(request.getBuildIdentity());
        validate(request.getPluginIdentity());
        RequiredFields.nonBlank(request.getSchemaVersion(), "schemaVersion");
    }

    public void validate(AnalysisDataEnvelope envelope) {
        RequiredFields.nonBlank(envelope.getSessionId(), "sessionId");
        RequiredFields.present(envelope.hasBuildIdentity(), "buildIdentity");
        RequiredFields.present(envelope.hasModuleIdentity(), "moduleIdentity");
        RequiredFields.present(envelope.hasPluginIdentity(), "pluginIdentity");
        RequiredFields.nonBlank(envelope.getSchemaVersion(), "schemaVersion");
        RequiredFields.present(envelope.hasPayloadDescriptor(), "payloadDescriptor");
        validate(envelope.getBuildIdentity());
        validate(envelope.getPluginIdentity());
        RequiredFields.nonBlank(envelope.getModuleIdentity().getModuleName(), "moduleIdentity.moduleName");
        RequiredFields.nonBlank(envelope.getModuleIdentity().getModulePath(), "moduleIdentity.modulePath");
        validate(envelope.getPayloadDescriptor());
        RequiredFields.nonEmpty(envelope.getPayload(), "payload");
    }

    public void validateComplete(String sessionId) {
        RequiredFields.nonBlank(sessionId, "sessionId");
    }

    public void validateAbort(String sessionId, String reason) {
        RequiredFields.nonBlank(sessionId, "sessionId");
        RequiredFields.nonBlank(reason, "reason");
    }

    private void validate(BuildIdentity identity) {
        RequiredFields.nonBlank(identity.getProjectId(), "buildIdentity.projectId");
        RequiredFields.nonBlank(identity.getRepositoryUrl(), "buildIdentity.repositoryUrl");
        RequiredFields.nonBlank(identity.getCommitHash(), "buildIdentity.commitHash");
        RequiredFields.nonBlank(identity.getBuildId(), "buildIdentity.buildId");
    }

    private void validate(PluginIdentity identity) {
        RequiredFields.nonBlank(identity.getPluginName(), "pluginIdentity.pluginName");
        RequiredFields.nonBlank(identity.getPluginVersion(), "pluginIdentity.pluginVersion");
    }

    private void validate(AnalysisPayloadDescriptor descriptor) {
        RequiredFields.nonBlank(descriptor.getPayloadId(), "payloadDescriptor.payloadId");
        RequiredFields.nonBlank(descriptor.getContentType(), "payloadDescriptor.contentType");
        if (
            descriptor.getKind() == AnalysisPayloadKind.ANALYSIS_PAYLOAD_KIND_UNSPECIFIED
                || descriptor.getKind() == AnalysisPayloadKind.UNRECOGNIZED
        ) {
            throw new ValidationException("payloadDescriptor.kind must be specified");
        }
        validateAttributes(descriptor.getAttributesMap(), "payloadDescriptor.attributes");
    }

    private void validateAttributes(Map<String, String> attributes, String fieldName) {
        attributes.forEach((key, value) -> {
            RequiredFields.nonBlank(key, fieldName + ".key");
            RequiredFields.nonBlank(value, fieldName + "[" + key + "]");
        });
    }
}
