package de.burger.forensics.analytics.services.queryreportapi.application;

import de.burger.forensics.analytics.services.queryreportapi.application.port.RepositoryAnalysisOwnerPort;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.RepositoryToBtmSubmission;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.RepositoryToBtmStatus;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.StatusRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.SubmissionRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class QueryReportApiRepositoryAnalysisSubmissionService {
    private final RepositoryAnalysisOwnerPort ownerPort;
    private final Map<String, IdempotentSubmission> submissions = new HashMap<>();

    public QueryReportApiRepositoryAnalysisSubmissionService(RepositoryAnalysisOwnerPort ownerPort) {
        this.ownerPort = Objects.requireNonNull(ownerPort, "repository analysis owner port must not be null");
    }

    public synchronized RepositoryToBtmSubmission submit(SubmissionRequest request) {
        var fingerprint = request.fingerprint();
        var replay = submissions.get(request.idempotencyKey());
        if (replay != null) {
            return replay.sameFingerprintOrThrow(fingerprint);
        }

        var submission = ownerPort.start(request);
        submissions.put(request.idempotencyKey(), new IdempotentSubmission(fingerprint, submission));
        return submission;
    }

    public RepositoryToBtmStatus status(StatusRequest request) {
        return ownerPort.status(Objects.requireNonNull(request, "status request must not be null"));
    }

    private record IdempotentSubmission(String fingerprint, RepositoryToBtmSubmission submission) {
        private RepositoryToBtmSubmission sameFingerprintOrThrow(String requestedFingerprint) {
            if (!fingerprint.equals(requestedFingerprint)) {
                throw new QueryReportApiIdempotencyConflictException("idempotency key was reused with different input");
            }
            return submission;
        }
    }
}
