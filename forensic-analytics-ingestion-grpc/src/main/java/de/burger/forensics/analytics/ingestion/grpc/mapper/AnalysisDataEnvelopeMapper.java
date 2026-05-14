package de.burger.forensics.analytics.ingestion.grpc.mapper;

import de.burger.forensics.analytics.application.ingestion.command.UploadAnalysisDataCommand;
import de.burger.forensics.analytics.domain.ingestion.AnalysisPayloadDescriptor;
import de.burger.forensics.analytics.ingestion.v1.AnalysisDataEnvelope;

public final class AnalysisDataEnvelopeMapper {
    private final BuildIdentityMapper buildIdentityMapper;
    private final ModuleIdentityMapper moduleIdentityMapper;
    private final PluginIdentityMapper pluginIdentityMapper;

    public AnalysisDataEnvelopeMapper(
        BuildIdentityMapper buildIdentityMapper,
        ModuleIdentityMapper moduleIdentityMapper,
        PluginIdentityMapper pluginIdentityMapper
    ) {
        this.buildIdentityMapper = buildIdentityMapper;
        this.moduleIdentityMapper = moduleIdentityMapper;
        this.pluginIdentityMapper = pluginIdentityMapper;
    }

    public UploadAnalysisDataCommand toCommand(AnalysisDataEnvelope envelope) {
        return new UploadAnalysisDataCommand(
            envelope.getSessionId(),
            buildIdentityMapper.toCommand(envelope.getBuildIdentity()),
            moduleIdentityMapper.toCommand(envelope.getModuleIdentity()),
            pluginIdentityMapper.toCommand(envelope.getPluginIdentity()),
            envelope.getSchemaVersion(),
            toDescriptor(envelope.getPayloadDescriptor()),
            envelope.getPayload().toByteArray()
        );
    }

    private AnalysisPayloadDescriptor toDescriptor(
        de.burger.forensics.analytics.ingestion.v1.AnalysisPayloadDescriptor descriptor
    ) {
        return new AnalysisPayloadDescriptor(
            descriptor.getPayloadId(),
            switch (descriptor.getKind()) {
                case ANALYSIS_PAYLOAD_KIND_SOURCE_FACTS ->
                    de.burger.forensics.analytics.domain.ingestion.AnalysisPayloadKind.SOURCE_FACTS;
                case ANALYSIS_PAYLOAD_KIND_SEMANTIC_ARTIFACTS ->
                    de.burger.forensics.analytics.domain.ingestion.AnalysisPayloadKind.SEMANTIC_ARTIFACTS;
                case ANALYSIS_PAYLOAD_KIND_RULE_ARTIFACTS ->
                    de.burger.forensics.analytics.domain.ingestion.AnalysisPayloadKind.RULE_ARTIFACTS;
                case ANALYSIS_PAYLOAD_KIND_RUNTIME_TRACE ->
                    de.burger.forensics.analytics.domain.ingestion.AnalysisPayloadKind.RUNTIME_TRACE;
                case ANALYSIS_PAYLOAD_KIND_DIAGNOSTIC_REPORT ->
                    de.burger.forensics.analytics.domain.ingestion.AnalysisPayloadKind.DIAGNOSTIC_REPORT;
                default -> throw new IllegalArgumentException("Unsupported analysis payload kind: " + descriptor.getKind());
            },
            descriptor.getContentType(),
            descriptor.getAttributesMap()
        );
    }
}
