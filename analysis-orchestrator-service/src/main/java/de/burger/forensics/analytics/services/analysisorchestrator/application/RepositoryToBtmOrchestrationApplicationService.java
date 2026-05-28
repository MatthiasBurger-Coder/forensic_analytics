package de.burger.forensics.analytics.services.analysisorchestrator.application;

import de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisJobId;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisRunId;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.BtmDeliveryReadiness;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.RepositoryToBtmDiagnostic;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.RepositoryToBtmDiagnosticSeverity;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.RepositoryToBtmOrchestrationState;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.RepositoryToBtmOrchestrationStatus;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.SafeMetadata;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class RepositoryToBtmOrchestrationApplicationService {
    private final ConcurrentHashMap<AnalysisRunId, StoredOperation> runResults = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, StoredOperation> idempotentResults = new ConcurrentHashMap<>();

    public RepositoryToBtmOrchestrationStatus start(RepositoryToBtmStartCommand command) {
        var fingerprint = List.of(
            "repository-to-btm-start",
            command.correlationId(),
            command.analysisRunId(),
            command.schemaVersion(),
            command.remoteUrl(),
            command.provider(),
            command.branch(),
            command.commit(),
            command.workspaceTimeoutSeconds(),
            command.maxWorkspaceBytes(),
            command.buildTool(),
            command.buildId(),
            command.rootProjectName(),
            command.declaredModules(),
            command.requestedOutputs(),
            command.attributes()
        ).toString();
        return idempotent(command.idempotencyKey(), fingerprint, () -> runResults.compute(command.analysisRunId(), (analysisRunId, existing) -> {
            if (existing != null) {
                if (!existing.fingerprint().equals(fingerprint)) {
                    throw new RepositoryToBtmOrchestrationConflictException(analysisRunId.value());
                }
                return existing;
            }
            return new StoredOperation(fingerprint, waitingForRepository(command));
        }).status());
    }

    public RepositoryToBtmOrchestrationStatus get(AnalysisRunId analysisRunId) {
        var stored = runResults.get(Objects.requireNonNull(analysisRunId, "analysisRunId must not be null"));
        return stored == null ? null : stored.status();
    }

    private static RepositoryToBtmOrchestrationStatus waitingForRepository(RepositoryToBtmStartCommand command) {
        return new RepositoryToBtmOrchestrationStatus(
            command.correlationId(),
            command.analysisRunId(),
            new AnalysisJobId("repository-analysis-" + sha256(command.analysisRunId().value()).substring(0, 24)),
            "",
            AnalysisCompleteness.INCOMPLETE,
            RepositoryToBtmOrchestrationState.WAITING_FOR_REPOSITORY,
            BtmDeliveryReadiness.NOT_READY,
            true,
            List.of(new RepositoryToBtmDiagnostic(
                "REPOSITORY_TO_BTM_WAITING_FOR_REPOSITORY",
                "Repository source handoff has not completed; no worker execution has started.",
                RepositoryToBtmDiagnosticSeverity.INFO,
                false,
                true
            )),
            command.attributes()
        );
    }

    private RepositoryToBtmOrchestrationStatus idempotent(
        String idempotencyKey,
        String fingerprint,
        Supplier<RepositoryToBtmOrchestrationStatus> operation
    ) {
        var key = requireText(idempotencyKey, "idempotencyKey");
        var existing = idempotentResults.get(key);
        if (existing != null) {
            if (!existing.fingerprint().equals(fingerprint)) {
                throw new IdempotencyConflictException(key);
            }
            return existing.status();
        }
        var status = operation.get();
        var stored = new StoredOperation(fingerprint, status);
        var previous = idempotentResults.putIfAbsent(key, stored);
        if (previous != null) {
            if (!previous.fingerprint().equals(fingerprint)) {
                throw new IdempotencyConflictException(key);
            }
            return previous.status();
        }
        return status;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.strip();
    }

    private static String sha256(String value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }

    public record RepositoryToBtmStartCommand(
        String idempotencyKey,
        String correlationId,
        String schemaVersion,
        AnalysisRunId analysisRunId,
        String remoteUrl,
        String provider,
        String branch,
        String commit,
        long workspaceTimeoutSeconds,
        long maxWorkspaceBytes,
        String buildTool,
        String buildId,
        String rootProjectName,
        List<String> declaredModules,
        List<String> requestedOutputs,
        Map<String, String> attributes
    ) {
        public RepositoryToBtmStartCommand {
            idempotencyKey = requireText(idempotencyKey, "idempotencyKey");
            correlationId = requireText(correlationId, "correlationId");
            schemaVersion = requireText(schemaVersion, "schemaVersion");
            analysisRunId = Objects.requireNonNull(analysisRunId, "analysisRunId must not be null");
            remoteUrl = requireText(remoteUrl, "remoteUrl");
            provider = requireText(provider, "provider");
            branch = branch == null ? "" : branch.strip();
            commit = commit == null ? "" : commit.strip();
            if (branch.isBlank() && commit.isBlank()) {
                throw new IllegalArgumentException("branch or commit must be provided");
            }
            if (workspaceTimeoutSeconds < 1) {
                throw new IllegalArgumentException("workspaceTimeoutSeconds must be positive");
            }
            if (maxWorkspaceBytes < 1) {
                throw new IllegalArgumentException("maxWorkspaceBytes must be positive");
            }
            buildTool = requireText(buildTool, "buildTool");
            buildId = buildId == null ? "" : buildId.strip();
            rootProjectName = rootProjectName == null ? "" : rootProjectName.strip();
            declaredModules = List.copyOf(Objects.requireNonNull(declaredModules, "declaredModules must not be null"));
            requestedOutputs = List.copyOf(Objects.requireNonNull(requestedOutputs, "requestedOutputs must not be null"));
            attributes = SafeMetadata.safeAttributes(attributes);
        }
    }

    private record StoredOperation(String fingerprint, RepositoryToBtmOrchestrationStatus status) {
    }
}
