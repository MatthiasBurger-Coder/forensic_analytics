package de.burger.forensics.analytics.services.analysisstore.application;

import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJob;
import de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.AcceptedStaticSourceFact;
import de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.DiagnosticSeverity;
import de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.InstrumentationTarget;
import de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.InstrumentationTargetSelection;
import de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.PlanInstrumentationTargetsCommand;
import de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.PlanInstrumentationTargetsResult;
import de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.ProbeKind;
import de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.TargetPlanningDiagnostic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.DETERMINISTIC_ORDER;
import static de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.OWNER_SERVICE;
import static de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.fingerprint;
import static de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.isSupportedFactType;
import static de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.stableSelectionId;
import static de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.stableTargetId;

public final class InstrumentationTargetPlanningApplicationService {
    private final AnalysisJobApplicationService analysisJobs;
    private final java.util.Map<String, StoredOperation> idempotentResults = new java.util.concurrent.ConcurrentHashMap<>();

    public InstrumentationTargetPlanningApplicationService(AnalysisJobApplicationService analysisJobs) {
        this.analysisJobs = Objects.requireNonNull(analysisJobs, "analysisJobs must not be null");
    }

    public synchronized PlanInstrumentationTargetsResult plan(
        String idempotencyKey,
        PlanInstrumentationTargetsCommand command
    ) {
        var verifiedCommand = Objects.requireNonNull(command, "command must not be null");
        var job = analysisJobs.get(verifiedCommand.metadata().analysisJobId());
        validateJobEnvelope(verifiedCommand, job);
        var registeredArtifacts = registeredArtifacts(job);
        var fingerprint = idempotencyFingerprint(verifiedCommand, registeredArtifacts);
        return idempotent(
            idempotencyKey,
            fingerprint,
            () -> planInternal(verifiedCommand, registeredArtifacts)
        );
    }

    private PlanInstrumentationTargetsResult planInternal(
        PlanInstrumentationTargetsCommand command,
        List<AnalysisArtifactReference> registeredArtifacts
    ) {
        var diagnostics = new ArrayList<TargetPlanningDiagnostic>();
        var registeredByPath = artifactsByPath(registeredArtifacts);
        var acceptedSourceArtifacts = acceptedRequestedArtifacts(
            command,
            command.sourceFactArtifacts(),
            registeredByPath,
            "SOURCE_FACT_ARTIFACT_NOT_ACCEPTED",
            "A source fact artifact was not accepted by Analysis Store.",
            diagnostics
        );
        var acceptedSemanticArtifacts = acceptedRequestedArtifacts(
            command,
            command.semanticArtifacts(),
            registeredByPath,
            "SEMANTIC_ARTIFACT_NOT_ACCEPTED",
            "A semantic artifact was not accepted by Analysis Store.",
            diagnostics
        );
        validateInputs(command, acceptedSourceArtifacts, acceptedSemanticArtifacts, diagnostics);

        var acceptedSourceArtifactPaths = acceptedSourceArtifacts.stream()
            .map(AnalysisArtifactReference::path)
            .collect(Collectors.toUnmodifiableSet());
        var targets = new ArrayList<InstrumentationTarget>();
        for (int factIndex = 0; factIndex < command.staticFacts().size(); factIndex++) {
            var fact = command.staticFacts().get(factIndex);
            var nextTargets = targetsForFact(command, fact, acceptedSourceArtifactPaths, diagnostics, targets.size());
            var limitReachedForFact = false;
            for (var target : nextTargets) {
                if (targets.size() >= command.policy().maxTargets()) {
                    diagnostics.add(targetLimitDiagnostic(command, fact));
                    limitReachedForFact = true;
                    break;
                }
                targets.add(target);
            }
            if (limitReachedForFact) {
                break;
            }
            if (targets.size() >= command.policy().maxTargets() && factIndex + 1 < command.staticFacts().size()) {
                diagnostics.add(targetLimitDiagnostic(command, command.staticFacts().get(factIndex + 1)));
                break;
            }
        }

        var completeness = completeness(targets, diagnostics);
        var selectionFingerprint = fingerprint(
            command.metadata().sourceSnapshotId(),
            command.policyVersion(),
            acceptedSourceArtifacts,
            acceptedSemanticArtifacts,
            targets,
            diagnostics
        );
        var selection = new InstrumentationTargetSelection(
            stableSelectionId(command.metadata().sourceSnapshotId(), selectionFingerprint),
            OWNER_SERVICE,
            command.policyVersion(),
            selectionFingerprint,
            completeness,
            DETERMINISTIC_ORDER,
            command.metadata().correlationId(),
            targets.size()
        );
        return new PlanInstrumentationTargetsResult(
            command.metadata(),
            completeness,
            selection,
            targets,
            diagnostics
        );
    }

    private static void validateJobEnvelope(
        PlanInstrumentationTargetsCommand command,
        AnalysisJob job
    ) {
        if (!job.analysisRunId().equals(command.metadata().analysisRunId())) {
            throw new IllegalArgumentException("analysisRunId does not match existing job");
        }
        if (!job.sourceSnapshotId().equals(command.metadata().sourceSnapshotId())) {
            throw new IllegalArgumentException("sourceSnapshotId does not match existing job");
        }
    }

    private static List<AnalysisArtifactReference> registeredArtifacts(AnalysisJob job) {
        var byPath = new LinkedHashMap<String, AnalysisArtifactReference>();
        java.util.stream.Stream.concat(job.inputArtifacts().stream(), job.outputArtifacts().stream())
            .sorted(Comparator.comparing(AnalysisArtifactReference::path))
            .forEach(artifact -> {
                var previous = byPath.putIfAbsent(artifact.path(), artifact);
                if (previous != null && !previous.equals(artifact)) {
                    throw new IllegalArgumentException("artifact path " + artifact.path() + " conflicts with existing metadata");
                }
            });
        return List.copyOf(byPath.values());
    }

    private static String idempotencyFingerprint(
        PlanInstrumentationTargetsCommand command,
        List<AnalysisArtifactReference> registeredArtifacts
    ) {
        return List.of(
            "planInstrumentationTargets",
            command.metadata().schemaVersion(),
            command.metadata().correlationId(),
            command.metadata().analysisRunId(),
            command.metadata().analysisJobId(),
            command.metadata().sourceSnapshotId(),
            command.metadata().attributes(),
            command.policyVersion(),
            command.policy(),
            command.staticFacts(),
            command.sourceFactArtifacts(),
            command.semanticArtifacts(),
            registeredArtifacts
        ).toString();
    }

    private static Map<String, AnalysisArtifactReference> artifactsByPath(List<AnalysisArtifactReference> artifacts) {
        var byPath = new LinkedHashMap<String, AnalysisArtifactReference>();
        artifacts.forEach(artifact -> byPath.put(artifact.path(), artifact));
        return Map.copyOf(byPath);
    }

    private static List<AnalysisArtifactReference> acceptedRequestedArtifacts(
        PlanInstrumentationTargetsCommand command,
        List<AnalysisArtifactReference> requestedArtifacts,
        Map<String, AnalysisArtifactReference> registeredArtifacts,
        String diagnosticCode,
        String diagnosticMessage,
        List<TargetPlanningDiagnostic> diagnostics
    ) {
        return requestedArtifacts.stream()
            .filter(artifact -> {
                var registered = registeredArtifacts.get(artifact.path());
                if (!artifact.equals(registered)) {
                    diagnostics.add(diagnostic(
                        command,
                        "",
                        artifact.path(),
                        diagnosticCode,
                        diagnosticMessage,
                        DiagnosticSeverity.ERROR,
                        true
                    ));
                    return false;
                }
                return true;
            })
            .toList();
    }

    private static void validateInputs(
        PlanInstrumentationTargetsCommand command,
        List<AnalysisArtifactReference> acceptedSourceArtifacts,
        List<AnalysisArtifactReference> acceptedSemanticArtifacts,
        List<TargetPlanningDiagnostic> diagnostics
    ) {
        if (command.staticFacts().isEmpty()) {
            diagnostics.add(diagnostic(
                command,
                "",
                "",
                "NO_ACCEPTED_STATIC_FACTS",
                "No accepted static source facts were provided for target planning.",
                DiagnosticSeverity.ERROR,
                true
            ));
        }
        acceptedSourceArtifacts.stream()
            .filter(artifact -> artifact.completeness() != AnalysisCompleteness.COMPLETE)
            .forEach(artifact -> diagnostics.add(diagnostic(
                command,
                "",
                artifact.path(),
                "SOURCE_FACT_ARTIFACT_INCOMPLETE",
                "A source fact artifact is not complete.",
                DiagnosticSeverity.WARNING,
                true
            )));
        if (command.policy().requireSemanticArtifacts() && acceptedSemanticArtifacts.isEmpty()) {
            diagnostics.add(diagnostic(
                command,
                "",
                "",
                "SEMANTIC_ARTIFACTS_REQUIRED",
                "The target-planning policy requires accepted semantic artifacts, but none were provided.",
                DiagnosticSeverity.WARNING,
                true
            ));
        }
        acceptedSemanticArtifacts.stream()
            .filter(artifact -> artifact.completeness() != AnalysisCompleteness.COMPLETE)
            .forEach(artifact -> diagnostics.add(diagnostic(
                command,
                "",
                artifact.path(),
                "SEMANTIC_ARTIFACT_INCOMPLETE",
                "A semantic artifact is not complete.",
                DiagnosticSeverity.WARNING,
                true
            )));
    }

    private static List<InstrumentationTarget> targetsForFact(
        PlanInstrumentationTargetsCommand command,
        AcceptedStaticSourceFact fact,
        Set<String> acceptedSourceArtifacts,
        List<TargetPlanningDiagnostic> diagnostics,
        int currentTargetCount
    ) {
        if (!isSupportedFactType(fact.factType())) {
            diagnostics.add(diagnostic(
                command,
                fact.factId(),
                fact.sourceFactArtifactReference(),
                "UNSUPPORTED_STATIC_FACT_TYPE",
                "The static source fact type cannot be used for instrumentation target planning.",
                DiagnosticSeverity.WARNING,
                true
            ));
            return List.of();
        }
        if (!acceptedSourceArtifacts.contains(fact.sourceFactArtifactReference())) {
            diagnostics.add(diagnostic(
                command,
                fact.factId(),
                fact.sourceFactArtifactReference(),
                "SOURCE_FACT_ARTIFACT_NOT_ACCEPTED",
                "The static source fact references an artifact that was not accepted by Analysis Store.",
                DiagnosticSeverity.ERROR,
                true
            ));
            return List.of();
        }
        var semanticMappingIncomplete = command.policy().requireSemanticArtifacts();
        if (semanticMappingIncomplete) {
            diagnostics.add(diagnostic(
                command,
                fact.factId(),
                "",
                "SEMANTIC_NODE_MAPPING_UNAVAILABLE",
                "No verified semantic-node schema is available for this static source fact.",
                DiagnosticSeverity.WARNING,
                true
            ));
        }
        var targetCompleteness = fact.completeness() == AnalysisCompleteness.COMPLETE && !semanticMappingIncomplete
            ? AnalysisCompleteness.COMPLETE
            : AnalysisCompleteness.INCOMPLETE;
        if (fact.completeness() != AnalysisCompleteness.COMPLETE) {
            diagnostics.add(diagnostic(
                command,
                fact.factId(),
                fact.sourceFactArtifactReference(),
                "STATIC_FACT_INCOMPLETE",
                "The static source fact is not complete.",
                DiagnosticSeverity.WARNING,
                true
            ));
        }
        var orderOffset = currentTargetCount;
        return java.util.stream.IntStream.range(0, command.policy().probeKinds().size())
            .mapToObj(index -> target(command, fact, command.policy().probeKinds().get(index), targetCompleteness, orderOffset + index))
            .toList();
    }

    private static TargetPlanningDiagnostic targetLimitDiagnostic(
        PlanInstrumentationTargetsCommand command,
        AcceptedStaticSourceFact fact
    ) {
        return diagnostic(
            command,
            fact.factId(),
            fact.sourceFactArtifactReference(),
            "TARGET_LIMIT_EXCEEDED",
            "Target planning stopped at the configured maximum target count.",
            DiagnosticSeverity.WARNING,
            true
        );
    }

    private static InstrumentationTarget target(
        PlanInstrumentationTargetsCommand command,
        AcceptedStaticSourceFact fact,
        ProbeKind probeKind,
        AnalysisCompleteness completeness,
        int orderIndex
    ) {
        return new InstrumentationTarget(
            stableTargetId(command.metadata().sourceSnapshotId(), command.policyVersion(), fact, probeKind),
            fact.factId(),
            "",
            fact.location().sourcePath(),
            fact.location().fullyQualifiedClassName(),
            fact.location().methodName(),
            fact.signature(),
            fact.location().lineNumber(),
            probeKind,
            fact.sourceFactArtifactReference(),
            "",
            orderIndex,
            completeness,
            command.policy().sensitivity()
        );
    }

    private static AnalysisCompleteness completeness(
        List<InstrumentationTarget> targets,
        List<TargetPlanningDiagnostic> diagnostics
    ) {
        if (targets.isEmpty() && diagnostics.stream().anyMatch(TargetPlanningDiagnostic::affectsCompleteness)) {
            return AnalysisCompleteness.UNKNOWN;
        }
        return diagnostics.stream().anyMatch(TargetPlanningDiagnostic::affectsCompleteness)
            || targets.stream().anyMatch(target -> target.completeness() != AnalysisCompleteness.COMPLETE)
                ? AnalysisCompleteness.INCOMPLETE
                : AnalysisCompleteness.COMPLETE;
    }

    private static TargetPlanningDiagnostic diagnostic(
        PlanInstrumentationTargetsCommand command,
        String sourceFactId,
        String artifactPath,
        String code,
        String message,
        DiagnosticSeverity severity,
        boolean affectsCompleteness
    ) {
        return new TargetPlanningDiagnostic(
            code,
            message,
            severity,
            command.metadata().sourceSnapshotId(),
            sourceFactId,
            artifactPath,
            false,
            affectsCompleteness
        );
    }

    private PlanInstrumentationTargetsResult idempotent(
        String key,
        String fingerprint,
        Supplier<PlanInstrumentationTargetsResult> supplier
    ) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
        var operationKey = "planInstrumentationTargets:" + key.strip();
        var existing = idempotentResults.get(operationKey);
        if (existing != null) {
            if (!existing.fingerprint().equals(fingerprint)) {
                throw new IdempotencyConflictException(key);
            }
            return existing.result();
        }
        var result = supplier.get();
        idempotentResults.put(operationKey, new StoredOperation(fingerprint, result));
        return result;
    }

    private record StoredOperation(String fingerprint, PlanInstrumentationTargetsResult result) {
    }
}
