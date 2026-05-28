package de.burger.forensics.analytics.services.analysisstore.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class InstrumentationTargetPlanningDomain {
    public static final String OWNER_SERVICE = "analysis-store-service";
    public static final String DETERMINISTIC_ORDER = "source_path_line_signature_fact_probe_ascending";
    private static final String SUPPORTED_FACT_TYPE = "java-method";

    private InstrumentationTargetPlanningDomain() {
    }

    public static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }

    public static String stableTargetId(SourceSnapshotId sourceSnapshotId, String policyVersion, AcceptedStaticSourceFact fact, ProbeKind probeKind) {
        return "instrumentation-target:" + sha256(String.join("\0",
            "instrumentation-target-v1",
            sourceSnapshotId.value(),
            policyVersion,
            fact.factId(),
            fact.signature(),
            probeKind.name()
        ));
    }

    public static String stableSelectionId(SourceSnapshotId sourceSnapshotId, String selectionFingerprint) {
        return "instrumentation-selection:" + sha256(String.join("\0",
            "instrumentation-selection-v1",
            sourceSnapshotId.value(),
            selectionFingerprint
        ));
    }

    public static String fingerprint(
        SourceSnapshotId sourceSnapshotId,
        String policyVersion,
        List<AnalysisArtifactReference> sourceFactArtifacts,
        List<AnalysisArtifactReference> semanticArtifacts,
        List<InstrumentationTarget> targets,
        List<TargetPlanningDiagnostic> diagnostics
    ) {
        return sha256(String.join("\n",
            "instrumentation-selection-fingerprint-v1",
            sourceSnapshotId.value(),
            policyVersion,
            artifacts(sourceFactArtifacts),
            artifacts(semanticArtifacts),
            targets.stream().map(InstrumentationTarget::canonical).collect(Collectors.joining("\n")),
            diagnostics.stream().map(TargetPlanningDiagnostic::canonical).collect(Collectors.joining("\n"))
        ));
    }

    public static boolean isSupportedFactType(String factType) {
        return SUPPORTED_FACT_TYPE.equals(factType);
    }

    private static String artifacts(List<AnalysisArtifactReference> artifacts) {
        return artifacts.stream()
            .map(artifact -> String.join("|",
                artifact.artifact().path(),
                artifact.artifact().type(),
                artifact.artifact().sha256(),
                Long.toString(artifact.artifact().sizeBytes()),
                artifact.category().name(),
                artifact.producerService(),
                artifact.schemaVersion(),
                artifact.completeness().name(),
                artifact.byteAccess().ownerService(),
                artifact.byteAccess().retrievalContract(),
                artifact.byteAccess().retrievalReference(),
                artifact.byteAccess().byteCustody().name()
            ))
            .collect(Collectors.joining("\n"));
    }

    public record TargetPlanningMetadata(
        String requestId,
        String schemaVersion,
        String correlationId,
        AnalysisRunId analysisRunId,
        AnalysisJobId analysisJobId,
        SourceSnapshotId sourceSnapshotId,
        Map<String, String> attributes
    ) {
        public TargetPlanningMetadata {
            requestId = RequiredText.require(requestId, "requestId");
            schemaVersion = RequiredText.require(schemaVersion, "schemaVersion");
            correlationId = RequiredText.require(correlationId, "correlationId");
            analysisRunId = Objects.requireNonNull(analysisRunId, "analysisRunId must not be null");
            analysisJobId = Objects.requireNonNull(analysisJobId, "analysisJobId must not be null");
            sourceSnapshotId = Objects.requireNonNull(sourceSnapshotId, "sourceSnapshotId must not be null");
            attributes = safeAttributes(attributes);
        }
    }

    public record InstrumentationTargetPolicy(
        int maxTargets,
        List<ProbeKind> probeKinds,
        boolean requireSemanticArtifacts,
        String sensitivity
    ) {
        public InstrumentationTargetPolicy {
            if (maxTargets < 1 || maxTargets > 100_000) {
                throw new IllegalArgumentException("maxTargets must be between 1 and 100000");
            }
            probeKinds = List.copyOf(Objects.requireNonNull(probeKinds, "probeKinds must not be null"));
            if (probeKinds.isEmpty() || probeKinds.stream().anyMatch(kind -> kind == ProbeKind.UNKNOWN)) {
                throw new IllegalArgumentException("probeKinds must contain only supported probe kinds");
            }
            probeKinds = probeKinds.stream()
                .distinct()
                .sorted(Comparator.comparing(ProbeKind::name))
                .toList();
            sensitivity = RequiredText.require(sensitivity, "sensitivity");
        }
    }

    public record AcceptedStaticSourceFact(
        String factId,
        String factType,
        StaticSourceLocation location,
        String signature,
        String sourceFactArtifactReference,
        AnalysisCompleteness completeness
    ) {
        public AcceptedStaticSourceFact {
            factId = RequiredText.require(factId, "factId");
            factType = RequiredText.require(factType, "factType");
            location = Objects.requireNonNull(location, "location must not be null");
            signature = RequiredText.require(signature, "signature");
            sourceFactArtifactReference = ArtifactByteAccess.requirePublicReference(
                sourceFactArtifactReference,
                "sourceFactArtifactReference"
            );
            completeness = Objects.requireNonNull(completeness, "completeness must not be null");
        }
    }

    public record StaticSourceLocation(
        String sourcePath,
        String fullyQualifiedClassName,
        String methodName,
        int lineNumber,
        int columnNumber
    ) {
        public StaticSourceLocation {
            sourcePath = ArtifactByteAccess.requirePublicReference(sourcePath, "sourcePath");
            fullyQualifiedClassName = RequiredText.require(fullyQualifiedClassName, "fullyQualifiedClassName");
            methodName = RequiredText.require(methodName, "methodName");
            if (lineNumber < 1) {
                throw new IllegalArgumentException("lineNumber must be positive");
            }
            if (columnNumber < 0) {
                throw new IllegalArgumentException("columnNumber must not be negative");
            }
        }
    }

    public record PlanInstrumentationTargetsCommand(
        TargetPlanningMetadata metadata,
        String policyVersion,
        InstrumentationTargetPolicy policy,
        List<AcceptedStaticSourceFact> staticFacts,
        List<AnalysisArtifactReference> sourceFactArtifacts,
        List<AnalysisArtifactReference> semanticArtifacts
    ) {
        public PlanInstrumentationTargetsCommand {
            metadata = Objects.requireNonNull(metadata, "metadata must not be null");
            policyVersion = RequiredText.require(policyVersion, "policyVersion");
            policy = Objects.requireNonNull(policy, "policy must not be null");
            staticFacts = sortedFacts(staticFacts);
            sourceFactArtifacts = staticArtifacts(sourceFactArtifacts, "sourceFactArtifacts");
            semanticArtifacts = staticArtifacts(semanticArtifacts, "semanticArtifacts");
            if (sourceFactArtifacts.isEmpty()) {
                throw new IllegalArgumentException("sourceFactArtifacts must not be empty");
            }
        }

        private static List<AcceptedStaticSourceFact> sortedFacts(List<AcceptedStaticSourceFact> facts) {
            return List.copyOf(Objects.requireNonNull(facts, "staticFacts must not be null")).stream()
                .sorted(Comparator
                    .comparing((AcceptedStaticSourceFact fact) -> fact.location().sourcePath())
                    .thenComparingInt(fact -> fact.location().lineNumber())
                    .thenComparing(AcceptedStaticSourceFact::signature)
                    .thenComparing(AcceptedStaticSourceFact::factId))
                .toList();
        }

        private static List<AnalysisArtifactReference> staticArtifacts(List<AnalysisArtifactReference> artifacts, String fieldName) {
            return List.copyOf(Objects.requireNonNull(artifacts, fieldName + " must not be null")).stream()
                .peek(artifact -> {
                    if (artifact.category() != AnalysisArtifactCategory.STATIC) {
                        throw new IllegalArgumentException(fieldName + " must contain only static artifact references");
                    }
                })
                .sorted(Comparator.comparing(AnalysisArtifactReference::path))
                .toList();
        }
    }

    public record InstrumentationTargetSelection(
        String selectionId,
        String ownerService,
        String policyVersion,
        String selectionFingerprint,
        AnalysisCompleteness completeness,
        String deterministicOrder,
        String correlationId,
        int targetCount
    ) {
        public InstrumentationTargetSelection {
            selectionId = RequiredText.require(selectionId, "selectionId");
            ownerService = RequiredText.require(ownerService, "ownerService");
            policyVersion = RequiredText.require(policyVersion, "policyVersion");
            selectionFingerprint = RequiredText.require(selectionFingerprint, "selectionFingerprint");
            completeness = Objects.requireNonNull(completeness, "completeness must not be null");
            deterministicOrder = RequiredText.require(deterministicOrder, "deterministicOrder");
            correlationId = RequiredText.require(correlationId, "correlationId");
            if (targetCount < 0) {
                throw new IllegalArgumentException("targetCount must not be negative");
            }
        }
    }

    public record InstrumentationTarget(
        String targetId,
        String sourceFactId,
        String semanticNodeId,
        String relativePath,
        String fullyQualifiedClassName,
        String methodName,
        String signature,
        int lineNumber,
        ProbeKind probeKind,
        String sourceFactArtifactReference,
        String semanticArtifactReference,
        int orderIndex,
        AnalysisCompleteness completeness,
        String sensitivity
    ) {
        public InstrumentationTarget {
            targetId = RequiredText.require(targetId, "targetId");
            sourceFactId = RequiredText.require(sourceFactId, "sourceFactId");
            semanticNodeId = semanticNodeId == null ? "" : semanticNodeId.strip();
            relativePath = ArtifactByteAccess.requirePublicReference(relativePath, "relativePath");
            fullyQualifiedClassName = RequiredText.require(fullyQualifiedClassName, "fullyQualifiedClassName");
            methodName = RequiredText.require(methodName, "methodName");
            signature = RequiredText.require(signature, "signature");
            if (lineNumber < 1) {
                throw new IllegalArgumentException("lineNumber must be positive");
            }
            probeKind = Objects.requireNonNull(probeKind, "probeKind must not be null");
            if (probeKind == ProbeKind.UNKNOWN) {
                throw new IllegalArgumentException("probeKind must be supported");
            }
            sourceFactArtifactReference = ArtifactByteAccess.requirePublicReference(
                sourceFactArtifactReference,
                "sourceFactArtifactReference"
            );
            semanticArtifactReference = semanticArtifactReference == null || semanticArtifactReference.isBlank()
                ? ""
                : ArtifactByteAccess.requirePublicReference(semanticArtifactReference, "semanticArtifactReference");
            if (orderIndex < 0) {
                throw new IllegalArgumentException("orderIndex must not be negative");
            }
            completeness = Objects.requireNonNull(completeness, "completeness must not be null");
            sensitivity = RequiredText.require(sensitivity, "sensitivity");
        }

        String canonical() {
            return String.join("|",
                targetId,
                sourceFactId,
                semanticNodeId,
                relativePath,
                fullyQualifiedClassName,
                methodName,
                signature,
                Integer.toString(lineNumber),
                probeKind.name(),
                sourceFactArtifactReference,
                semanticArtifactReference,
                Integer.toString(orderIndex),
                completeness.name(),
                sensitivity
            );
        }
    }

    public record TargetPlanningDiagnostic(
        String code,
        String message,
        DiagnosticSeverity severity,
        SourceSnapshotId sourceSnapshotId,
        String sourceFactId,
        String artifactPath,
        boolean retryable,
        boolean affectsCompleteness
    ) {
        public TargetPlanningDiagnostic {
            code = RequiredText.require(code, "code");
            message = RequiredText.require(message, "message");
            severity = Objects.requireNonNull(severity, "severity must not be null");
            sourceSnapshotId = Objects.requireNonNull(sourceSnapshotId, "sourceSnapshotId must not be null");
            sourceFactId = sourceFactId == null ? "" : sourceFactId.strip();
            artifactPath = artifactPath == null || artifactPath.isBlank()
                ? ""
                : ArtifactByteAccess.requirePublicReference(artifactPath, "artifactPath");
        }

        String canonical() {
            return String.join("|",
                code,
                message,
                severity.name(),
                sourceSnapshotId.value(),
                sourceFactId,
                artifactPath,
                Boolean.toString(retryable),
                Boolean.toString(affectsCompleteness)
            );
        }
    }

    public record PlanInstrumentationTargetsResult(
        TargetPlanningMetadata metadata,
        AnalysisCompleteness completeness,
        InstrumentationTargetSelection selection,
        List<InstrumentationTarget> targets,
        List<TargetPlanningDiagnostic> diagnostics
    ) {
        public PlanInstrumentationTargetsResult {
            metadata = Objects.requireNonNull(metadata, "metadata must not be null");
            completeness = Objects.requireNonNull(completeness, "completeness must not be null");
            selection = Objects.requireNonNull(selection, "selection must not be null");
            targets = List.copyOf(Objects.requireNonNull(targets, "targets must not be null"));
            diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics must not be null"));
        }
    }

    public enum ProbeKind {
        METHOD_ENTRY,
        METHOD_EXIT,
        THROW,
        UNKNOWN
    }

    public enum DiagnosticSeverity {
        INFO,
        WARNING,
        ERROR
    }

    private static Map<String, String> safeAttributes(Map<String, String> attributes) {
        var sorted = new TreeMap<String, String>();
        Objects.requireNonNull(attributes, "attributes must not be null").forEach((key, value) -> {
            var safeKey = RequiredText.require(key, "attribute key");
            var safeValue = RequiredText.require(value, "attribute value");
            var normalizedKey = safeKey.toLowerCase(Locale.ROOT);
            if (normalizedKey.contains("secret")
                || normalizedKey.contains("token")
                || normalizedKey.contains("password")
                || normalizedKey.contains("credential")
                || safeValue.toLowerCase(Locale.ROOT).startsWith("file:")
                || safeValue.contains("://")
                || safeValue.startsWith("/")
                || safeValue.contains("\\")
                || safeValue.contains("..")
                || safeValue.matches("^[A-Za-z]:.*")) {
                throw new IllegalArgumentException("attributes must not contain secrets, URIs or private paths");
            }
            sorted.put(safeKey, safeValue);
        });
        return Collections.unmodifiableMap(new LinkedHashMap<>(sorted));
    }
}
