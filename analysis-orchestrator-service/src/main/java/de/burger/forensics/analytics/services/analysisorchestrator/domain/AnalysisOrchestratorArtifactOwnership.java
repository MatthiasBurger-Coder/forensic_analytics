package de.burger.forensics.analytics.services.analysisorchestrator.domain;

import java.util.Locale;

public final class AnalysisOrchestratorArtifactOwnership {
    public static final String SERVICE_NAME = "analysis-orchestrator-service";

    private AnalysisOrchestratorArtifactOwnership() {
    }

    public static String requireExternalProducer(String producerService) {
        var producer = RequiredText.require(producerService, "producerService");
        if (isAnalysisOrchestratorService(producer)) {
            throw new IllegalArgumentException("producerService must not be analysis-orchestrator-service");
        }
        return producer;
    }

    public static String requireExternalByteOwner(String ownerService) {
        var owner = RequiredText.require(ownerService, "ownerService");
        if (isAnalysisOrchestratorService(owner)) {
            throw new IllegalArgumentException("ownerService must not be analysis-orchestrator-service");
        }
        return owner;
    }

    private static boolean isAnalysisOrchestratorService(String serviceName) {
        return SERVICE_NAME.equals(serviceName.toLowerCase(Locale.ROOT));
    }
}
