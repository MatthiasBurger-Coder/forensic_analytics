package de.burger.forensics.analytics.ingestion.grpc.mapper;

import de.burger.forensics.analytics.application.ingestion.command.UploadAnalysisDataCommand;
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
            envelope.getPayloadType(),
            envelope.getPayload().toByteArray()
        );
    }
}
