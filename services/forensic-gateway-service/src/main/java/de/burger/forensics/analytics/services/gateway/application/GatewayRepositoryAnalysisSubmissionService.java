package de.burger.forensics.analytics.services.gateway.application;

import de.burger.forensics.analytics.services.gateway.application.port.RepositoryAnalysisPreparationPort;
import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.Diagnostic;
import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.RepositoryPreparationCommand;
import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.RepositoryToBtmSubmission;
import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.SubmissionRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class GatewayRepositoryAnalysisSubmissionService {
    private final RepositoryAnalysisPreparationPort preparationPort;
    private final Map<String, IdempotentSubmission> submissions = new HashMap<>();

    public GatewayRepositoryAnalysisSubmissionService(RepositoryAnalysisPreparationPort preparationPort) {
        this.preparationPort = Objects.requireNonNull(preparationPort, "preparation port must not be null");
    }

    public synchronized RepositoryToBtmSubmission submit(SubmissionRequest request) {
        var fingerprint = request.fingerprint();
        var replay = submissions.get(request.idempotencyKey());
        if (replay != null) {
            return replay.sameFingerprintOrThrow(fingerprint);
        }

        var analysisRunId = request.analysisRunId();
        var preparation = preparationPort.prepare(new RepositoryPreparationCommand(analysisRunId, request));
        var diagnostics = List.of(
            Diagnostic.info("SOURCE_SNAPSHOT_PREPARED", "Repository source snapshot was prepared"),
            Diagnostic.info("BTM_DELIVERY_PENDING", "BTM delivery is not ready in this slice")
        );
        var submission = new RepositoryToBtmSubmission(
            analysisRunId,
            "ACCEPTED",
            "/repository-analyses/" + analysisRunId,
            "/repository-analyses/" + analysisRunId + "/jobs",
            "BTM_DELIVERY_NOT_READY",
            "BtmArtifactDeliveryService",
            request.correlationId(),
            merge(diagnostics, preparation.diagnostics())
        );
        submissions.put(request.idempotencyKey(), new IdempotentSubmission(fingerprint, submission));
        return submission;
    }

    private static List<Diagnostic> merge(List<Diagnostic> gatewayDiagnostics, List<Diagnostic> downstreamDiagnostics) {
        var merged = new java.util.ArrayList<Diagnostic>();
        merged.addAll(gatewayDiagnostics);
        merged.addAll(downstreamDiagnostics);
        return List.copyOf(merged);
    }

    private record IdempotentSubmission(String fingerprint, RepositoryToBtmSubmission submission) {
        private RepositoryToBtmSubmission sameFingerprintOrThrow(String requestedFingerprint) {
            if (!fingerprint.equals(requestedFingerprint)) {
                throw new GatewayIdempotencyConflictException("idempotency key was reused with different input");
            }
            return submission;
        }
    }
}
