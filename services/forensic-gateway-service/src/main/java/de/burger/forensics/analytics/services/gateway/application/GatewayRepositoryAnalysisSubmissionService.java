package de.burger.forensics.analytics.services.gateway.application;

import de.burger.forensics.analytics.services.gateway.application.port.RepositoryToBtmOrchestrationPort;
import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.RepositoryToBtmSubmission;
import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.StatusRequest;
import de.burger.forensics.analytics.services.gateway.domain.GatewayRepositoryAnalysis.SubmissionRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class GatewayRepositoryAnalysisSubmissionService {
    private final RepositoryToBtmOrchestrationPort orchestrationPort;
    private final Map<String, IdempotentSubmission> submissions = new HashMap<>();

    public GatewayRepositoryAnalysisSubmissionService(RepositoryToBtmOrchestrationPort orchestrationPort) {
        this.orchestrationPort = Objects.requireNonNull(orchestrationPort, "orchestration port must not be null");
    }

    public synchronized RepositoryToBtmSubmission submit(SubmissionRequest request) {
        var fingerprint = request.fingerprint();
        var replay = submissions.get(request.idempotencyKey());
        if (replay != null) {
            return replay.sameFingerprintOrThrow(fingerprint);
        }

        var submission = orchestrationPort.start(request);
        submissions.put(request.idempotencyKey(), new IdempotentSubmission(fingerprint, submission));
        return submission;
    }

    public RepositoryToBtmSubmission status(StatusRequest request) {
        return orchestrationPort.status(Objects.requireNonNull(request, "status request must not be null"));
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
