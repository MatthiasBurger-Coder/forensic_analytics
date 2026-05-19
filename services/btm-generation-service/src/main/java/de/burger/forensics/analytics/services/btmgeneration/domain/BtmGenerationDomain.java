package de.burger.forensics.analytics.services.btmgeneration.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class BtmGenerationDomain {
    public static final String PRODUCER_SERVICE = "btm-generation-service";
    public static final String GENERATOR_VERSION = "btm-generation-service-0.1.0";
    public static final String DETERMINISTIC_SORT = "target_id_probe_kind_ascending";
    public static final String BTM_RULE_ARTIFACT_TYPE = "application/vnd.forensic-analytics.btm-rules.v1+btm";
    public static final String BTM_MANIFEST_ARTIFACT_TYPE = "application/vnd.forensic-analytics.btm-rule-manifest.v1+json";
    public static final String BTM_DELIVERY_CONTRACT =
        "de.burger.forensics.analytics.btmgeneration.v1.BtmArtifactDeliveryService.DownloadBtmArtifacts";
    public static final int MAX_DELIVERY_CHUNK_BYTES = 1_048_576;
    public static final long MAX_DELIVERY_TOTAL_BYTES = 1_073_741_824L;
    private static final Pattern WINDOWS_DRIVE_PATH = Pattern.compile("^[A-Za-z]:.*");
    private static final Set<String> SENSITIVE_ATTRIBUTE_NAMES = Set.of(
        "authorization",
        "credential",
        "credentials",
        "password",
        "secret",
        "token"
    );

    private BtmGenerationDomain() {
    }

    public static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    public static String optionalText(String value) {
        return value == null ? "" : value.trim();
    }

    public static String requireRelativePath(String value, String name) {
        var text = requireText(value, name).replace('\\', '/');
        if (isUnsafePath(text)) {
            throw new IllegalArgumentException(name + " must be a safe relative path");
        }
        return text;
    }

    public static String optionalRelativePath(String value, String name) {
        var text = optionalText(value).replace('\\', '/');
        if (!text.isBlank() && isUnsafePath(text)) {
            throw new IllegalArgumentException(name + " must be a safe relative path");
        }
        return text;
    }

    public static String requireArtifactPath(String value, String name) {
        var text = requirePublicReference(value, name);
        if (text.contains("://")) {
            throw new IllegalArgumentException(name + " must be an opaque artifact key or relative artifact path");
        }
        return text;
    }

    public static String optionalArtifactPath(String value, String name) {
        var text = optionalText(value).replace('\\', '/');
        if (!text.isBlank() && (isUnsafePath(text) || text.contains("://") || hasUnsafePathSegment(text))) {
            throw new IllegalArgumentException(name + " must be an opaque artifact key or relative artifact path");
        }
        return text;
    }

    public static String requirePublicReference(String value, String name) {
        var text = requireText(value, name).replace('\\', '/');
        if (isUnsafePath(text) || hasUnsafePathSegment(text) || text.contains("://")
            || text.contains("\n") || text.contains("\r")) {
            throw new IllegalArgumentException(name + " must not be a private path, URI or unsafe public reference");
        }
        return text;
    }

    public static Map<String, String> safeAttributes(Map<String, String> attributes) {
        var copy = Map.copyOf(Objects.requireNonNull(attributes, "safe attributes must not be null"));
        copy.forEach(BtmGenerationDomain::requireSafeAttribute);
        return copy;
    }

    public static String sha256(String content) {
        return sha256(content.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }

    public static String stableRuleId(SourceSnapshotId sourceSnapshotId, RuleTarget target, String ruleSchemaVersion) {
        return "btm-rule:" + sha256("btm-rule-v1\0"
            + sourceSnapshotId.value() + "\0"
            + target.targetId() + "\0"
            + target.probeKind().name() + "\0"
            + ruleSchemaVersion);
    }

    public static String factsFingerprint(SourceSnapshotId sourceSnapshotId, DeliveredFacts facts) {
        return sha256("facts-v1\n"
            + sourceSnapshotId.value() + "\n"
            + facts.targetSelection().canonical() + "\n"
            + artifactFingerprint(facts.sourceFactArtifacts()) + "\n"
            + artifactFingerprint(facts.semanticArtifacts()) + "\n"
            + facts.targets().stream().map(RuleTarget::canonical).collect(Collectors.joining("\n")));
    }

    public static String policyFingerprint(BtmGenerationPolicy policy) {
        return sha256(String.join("\n",
            "policy-v1",
            Integer.toString(policy.maxTargets()),
            Long.toString(policy.maxArtifactBytes()),
            Long.toString(policy.timeoutSeconds()),
            policy.ruleSchemaVersion(),
            Boolean.toString(policy.failOnIncompleteFacts())
        ));
    }

    public static String generationFingerprint(List<GeneratedRule> rules, List<BtmDiagnostic> diagnostics) {
        var rulePart = rules.stream().map(GeneratedRule::canonical).collect(Collectors.joining("\n"));
        var diagnosticPart = diagnostics.stream().map(BtmDiagnostic::canonical).collect(Collectors.joining("\n"));
        return sha256("generation-v1\n" + rulePart + "\n" + diagnosticPart);
    }

    private static String artifactFingerprint(List<AnalysisArtifactReference> artifacts) {
        return artifacts.stream()
            .map(AnalysisArtifactReference::canonical)
            .collect(Collectors.joining("\n"));
    }

    private static boolean isUnsafePath(String text) {
        return text.startsWith("/")
            || text.startsWith("file:")
            || text.contains("//")
            || WINDOWS_DRIVE_PATH.matcher(text).matches()
            || Arrays.asList(text.split("/")).contains("..");
    }

    private static boolean hasUnsafePathSegment(String text) {
        return Arrays.asList(text.split("/")).stream()
            .anyMatch(part -> part.isBlank() || part.equals(".") || part.equals(".."));
    }

    private static void requireSafeAttribute(String key, String value) {
        requireText(key, "safe attribute key");
        requireText(value, "safe attribute value");
        var normalizedKey = key.toLowerCase(Locale.ROOT);
        if (SENSITIVE_ATTRIBUTE_NAMES.stream().anyMatch(normalizedKey::contains)
            || value.startsWith("file:")
            || value.contains("://")
            || value.contains("\\")
            || isUnsafePath(value.replace('\\', '/'))) {
            throw new IllegalArgumentException("safe attributes must not contain secrets or local paths");
        }
    }

    public record RequestMetadata(
        String requestId,
        String idempotencyKey,
        String schemaVersion,
        String correlationId,
        AnalysisRunId analysisRunId,
        AnalysisJobId analysisJobId,
        SourceSnapshotId sourceSnapshotId,
        String workerVersion,
        Map<String, String> safeAttributes
    ) {
        public RequestMetadata {
            requestId = requireText(requestId, "request id");
            idempotencyKey = requireText(idempotencyKey, "idempotency key");
            schemaVersion = requireText(schemaVersion, "schema version");
            correlationId = requireText(correlationId, "correlation id");
            analysisRunId = Objects.requireNonNull(analysisRunId, "analysis run id must not be null");
            analysisJobId = Objects.requireNonNull(analysisJobId, "analysis job id must not be null");
            sourceSnapshotId = Objects.requireNonNull(sourceSnapshotId, "source snapshot id must not be null");
            workerVersion = requireText(workerVersion, "worker version");
            safeAttributes = BtmGenerationDomain.safeAttributes(safeAttributes);
        }
    }

    public record AnalysisRunId(String value) {
        public AnalysisRunId {
            value = requireText(value, "analysis run id");
        }
    }

    public record AnalysisJobId(String value) {
        public AnalysisJobId {
            value = requireText(value, "analysis job id");
        }
    }

    public record SourceSnapshotId(String value) {
        public SourceSnapshotId {
            value = requireText(value, "source snapshot id");
        }
    }

    public record ArtifactReference(String path, String type, String sha256, long sizeBytes) {
        public ArtifactReference {
            path = requireArtifactPath(path, "artifact path");
            type = requireText(type, "artifact type");
            sha256 = requireText(sha256, "artifact sha256").toLowerCase(Locale.ROOT);
            if (!sha256.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("artifact checksum must be a SHA-256 hex value");
            }
            if (sizeBytes < 0) {
                throw new IllegalArgumentException("artifact size must not be negative");
            }
        }

        String canonical() {
            return String.join("|", path, type, sha256, Long.toString(sizeBytes));
        }
    }

    public record ArtifactByteAccess(
        String ownerService,
        String retrievalContract,
        String retrievalReference,
        ArtifactByteCustody byteCustody
    ) {
        public ArtifactByteAccess {
            ownerService = requireText(ownerService, "byte access owner service");
            retrievalContract = requireText(retrievalContract, "byte access retrieval contract");
            retrievalReference = requirePublicReference(retrievalReference, "byte access retrieval reference");
            byteCustody = Objects.requireNonNull(byteCustody, "byte custody must not be null");
            if (byteCustody == ArtifactByteCustody.UNKNOWN) {
                throw new IllegalArgumentException("byte custody must be specified");
            }
        }

        String canonical() {
            return String.join("|", ownerService, retrievalContract, retrievalReference, byteCustody.name());
        }
    }

    public enum ArtifactByteCustody {
        PRODUCER_RETAINED,
        SCOPED_OBJECT_ACCESS,
        EXPLICIT_HANDOFF,
        UNKNOWN
    }

    public record AnalysisArtifactReference(
        ArtifactReference artifact,
        AnalysisArtifactCategory category,
        String producerService,
        String schemaVersion,
        AnalysisCompleteness completeness,
        ArtifactByteAccess byteAccess
    ) {
        public AnalysisArtifactReference {
            artifact = Objects.requireNonNull(artifact, "artifact must not be null");
            category = Objects.requireNonNull(category, "artifact category must not be null");
            producerService = requireText(producerService, "producer service");
            schemaVersion = requireText(schemaVersion, "artifact schema version");
            completeness = Objects.requireNonNull(completeness, "artifact completeness must not be null");
            byteAccess = Objects.requireNonNull(byteAccess, "byte access must not be null");
        }

        String canonical() {
            return artifact.canonical() + "|" + category + "|" + producerService + "|" + schemaVersion + "|" + completeness
                + "|" + byteAccess.canonical();
        }
    }

    public enum AnalysisArtifactCategory {
        STATIC,
        RUNTIME,
        PROJECTION,
        GENERATED,
        UNKNOWN
    }

    public enum AnalysisCompleteness {
        COMPLETE,
        INCOMPLETE,
        UNKNOWN
    }

    public enum DiagnosticSeverity {
        INFO,
        WARNING,
        ERROR
    }

    public enum ProbeKind {
        METHOD_ENTRY,
        METHOD_EXIT,
        THROW,
        UNKNOWN
    }

    public record BtmGenerationPolicy(
        int maxTargets,
        long maxArtifactBytes,
        long timeoutSeconds,
        String ruleSchemaVersion,
        boolean failOnIncompleteFacts
    ) {
        public BtmGenerationPolicy {
            if (maxTargets < 1 || maxTargets > 100_000) {
                throw new IllegalArgumentException("max targets must be between 1 and 100000");
            }
            if (maxArtifactBytes < 1 || maxArtifactBytes > 1_073_741_824L) {
                throw new IllegalArgumentException("max artifact bytes must be between 1 and 1073741824");
            }
            if (timeoutSeconds < 1 || timeoutSeconds > 86_400) {
                throw new IllegalArgumentException("timeout seconds must be between 1 and 86400");
            }
            ruleSchemaVersion = requireText(ruleSchemaVersion, "rule schema version");
        }
    }

    public record DeliveredFacts(
        List<AnalysisArtifactReference> sourceFactArtifacts,
        List<AnalysisArtifactReference> semanticArtifacts,
        List<RuleTarget> targets,
        AnalysisCompleteness inputCompleteness,
        TargetSelection targetSelection
    ) {
        public DeliveredFacts {
            sourceFactArtifacts = sortedArtifacts(sourceFactArtifacts);
            semanticArtifacts = sortedArtifacts(semanticArtifacts);
            targets = sortedTargets(targets);
            inputCompleteness = Objects.requireNonNull(inputCompleteness, "input completeness must not be null");
            targetSelection = Objects.requireNonNull(targetSelection, "target selection must not be null");
        }

        private static List<AnalysisArtifactReference> sortedArtifacts(List<AnalysisArtifactReference> artifacts) {
            return List.copyOf(Objects.requireNonNull(artifacts, "artifacts must not be null")).stream()
                .sorted(Comparator.comparing(reference -> reference.artifact().path()))
                .toList();
        }

        private static List<RuleTarget> sortedTargets(List<RuleTarget> targets) {
            return List.copyOf(Objects.requireNonNull(targets, "targets must not be null")).stream()
                .sorted(Comparator.comparing(RuleTarget::targetId).thenComparing(target -> target.probeKind().name()))
                .toList();
        }
    }

    public record TargetSelection(
        String selectionId,
        String ownerService,
        String policyVersion,
        String selectionFingerprint,
        AnalysisCompleteness completeness,
        String deterministicOrder,
        String correlationId,
        int targetCount
    ) {
        public TargetSelection {
            selectionId = requireText(selectionId, "selection id");
            ownerService = requireText(ownerService, "selection owner service");
            policyVersion = requireText(policyVersion, "selection policy version");
            selectionFingerprint = requireText(selectionFingerprint, "selection fingerprint");
            completeness = Objects.requireNonNull(completeness, "selection completeness must not be null");
            deterministicOrder = requireText(deterministicOrder, "selection deterministic order");
            correlationId = requireText(correlationId, "selection correlation id");
            if (targetCount < 0) {
                throw new IllegalArgumentException("selection target count must not be negative");
            }
        }

        String canonical() {
            return String.join("|",
                selectionId,
                ownerService,
                policyVersion,
                selectionFingerprint,
                completeness.name(),
                deterministicOrder,
                correlationId,
                Integer.toString(targetCount)
            );
        }
    }

    public record RuleTarget(
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
        public RuleTarget {
            targetId = requireText(targetId, "target id");
            sourceFactId = requireText(sourceFactId, "source fact id");
            semanticNodeId = optionalText(semanticNodeId);
            relativePath = optionalRelativePath(relativePath, "target relative path");
            fullyQualifiedClassName = optionalText(fullyQualifiedClassName);
            methodName = optionalText(methodName);
            signature = optionalText(signature);
            probeKind = Objects.requireNonNull(probeKind, "probe kind must not be null");
            sourceFactArtifactReference = requirePublicReference(sourceFactArtifactReference, "source fact artifact reference");
            semanticArtifactReference = optionalText(semanticArtifactReference);
            if (!semanticArtifactReference.isBlank()) {
                semanticArtifactReference = requirePublicReference(semanticArtifactReference, "semantic artifact reference");
            }
            if (orderIndex < 0) {
                throw new IllegalArgumentException("target order index must not be negative");
            }
            completeness = Objects.requireNonNull(completeness, "target completeness must not be null");
            sensitivity = requireText(sensitivity, "target sensitivity");
        }

        public boolean canGenerateRule() {
            return !relativePath.isBlank()
                && !fullyQualifiedClassName.isBlank()
                && !methodName.isBlank()
                && !signature.isBlank()
                && lineNumber > 0
                && probeKind != ProbeKind.UNKNOWN;
        }

        public String canonical() {
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

    public record GenerateBtmRulesCommand(
        RequestMetadata metadata,
        BtmGenerationPolicy policy,
        DeliveredFacts facts
    ) {
        public GenerateBtmRulesCommand {
            metadata = Objects.requireNonNull(metadata, "metadata must not be null");
            policy = Objects.requireNonNull(policy, "policy must not be null");
            facts = Objects.requireNonNull(facts, "facts must not be null");
        }
    }

    public record GeneratedRule(
        String ruleId,
        RuleTarget target
    ) {
        public GeneratedRule {
            ruleId = requireText(ruleId, "rule id");
            target = Objects.requireNonNull(target, "target must not be null");
        }

        String canonical() {
            return ruleId + "|" + target.canonical();
        }
    }

    public record BtmDiagnostic(
        String code,
        String message,
        DiagnosticSeverity severity,
        SourceSnapshotId sourceSnapshotId,
        String targetId,
        String artifactPath,
        boolean retryable,
        boolean affectsCompleteness
    ) {
        public BtmDiagnostic {
            code = requireText(code, "diagnostic code");
            message = requireText(message, "diagnostic message");
            severity = Objects.requireNonNull(severity, "diagnostic severity must not be null");
            sourceSnapshotId = Objects.requireNonNull(sourceSnapshotId, "source snapshot id must not be null");
            targetId = optionalText(targetId);
            artifactPath = optionalArtifactPath(artifactPath, "diagnostic artifact path");
        }

        String canonical() {
            return String.join("|",
                code,
                message,
                severity.name(),
                sourceSnapshotId.value(),
                targetId,
                artifactPath,
                Boolean.toString(retryable),
                Boolean.toString(affectsCompleteness)
            );
        }
    }

    public record BtmGenerationSummary(
        int receivedTargetCount,
        int generatedRuleCount,
        int skippedTargetCount,
        int sourceFactArtifactCount,
        int semanticArtifactCount,
        String producerService,
        String generatorVersion,
        String ruleSchemaVersion
    ) {
        public BtmGenerationSummary {
            if (receivedTargetCount < 0 || generatedRuleCount < 0 || skippedTargetCount < 0) {
                throw new IllegalArgumentException("BTM generation counts must not be negative");
            }
            producerService = requireText(producerService, "producer service");
            generatorVersion = requireText(generatorVersion, "generator version");
            ruleSchemaVersion = requireText(ruleSchemaVersion, "rule schema version");
        }
    }

    public record ReproducibilityMetadata(
        String factsFingerprint,
        String policyFingerprint,
        String generationFingerprint,
        String generatorVersion,
        String deterministicSort
    ) {
        public ReproducibilityMetadata {
            factsFingerprint = requireText(factsFingerprint, "facts fingerprint");
            policyFingerprint = requireText(policyFingerprint, "policy fingerprint");
            generationFingerprint = requireText(generationFingerprint, "generation fingerprint");
            generatorVersion = requireText(generatorVersion, "generator version");
            deterministicSort = requireText(deterministicSort, "deterministic sort");
        }
    }

    public record BtmArtifactWriteRequest(
        RequestMetadata metadata,
        BtmGenerationPolicy policy,
        List<GeneratedRule> rules,
        List<BtmDiagnostic> diagnostics,
        BtmGenerationSummary summary,
        ReproducibilityMetadata reproducibility,
        AnalysisCompleteness completeness,
        TargetSelection targetSelection
    ) {
        public BtmArtifactWriteRequest {
            metadata = Objects.requireNonNull(metadata, "metadata must not be null");
            policy = Objects.requireNonNull(policy, "policy must not be null");
            rules = List.copyOf(Objects.requireNonNull(rules, "rules must not be null"));
            diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics must not be null"));
            summary = Objects.requireNonNull(summary, "summary must not be null");
            reproducibility = Objects.requireNonNull(reproducibility, "reproducibility metadata must not be null");
            completeness = Objects.requireNonNull(completeness, "completeness must not be null");
            targetSelection = Objects.requireNonNull(targetSelection, "target selection must not be null");
        }
    }

    public record BtmArtifactDeliveryMetadata(
        String requestId,
        String idempotencyKey,
        String schemaVersion,
        String correlationId,
        AnalysisRunId analysisRunId,
        AnalysisJobId analysisJobId,
        Map<String, String> safeAttributes
    ) {
        public BtmArtifactDeliveryMetadata {
            requestId = requireText(requestId, "request id");
            idempotencyKey = requireText(idempotencyKey, "idempotency key");
            schemaVersion = requireText(schemaVersion, "schema version");
            correlationId = requireText(correlationId, "correlation id");
            analysisRunId = Objects.requireNonNull(analysisRunId, "analysis run id must not be null");
            analysisJobId = Objects.requireNonNull(analysisJobId, "analysis job id must not be null");
            safeAttributes = BtmGenerationDomain.safeAttributes(safeAttributes);
        }
    }

    public record BtmArtifactDeliveryCommand(
        BtmArtifactDeliveryMetadata metadata,
        int maxChunkBytes,
        long maxTotalBytes,
        List<String> artifactReferences,
        List<AnalysisArtifactReference> acceptedGeneratedArtifacts
    ) {
        public BtmArtifactDeliveryCommand {
            metadata = Objects.requireNonNull(metadata, "delivery metadata must not be null");
            if (maxChunkBytes < 1 || maxChunkBytes > MAX_DELIVERY_CHUNK_BYTES) {
                throw new IllegalArgumentException("max chunk bytes must be between 1 and " + MAX_DELIVERY_CHUNK_BYTES);
            }
            if (maxTotalBytes < 1 || maxTotalBytes > MAX_DELIVERY_TOTAL_BYTES) {
                throw new IllegalArgumentException("max total bytes must be between 1 and " + MAX_DELIVERY_TOTAL_BYTES);
            }
            artifactReferences = List.copyOf(Objects.requireNonNull(artifactReferences, "artifact references must not be null")).stream()
                .map(reference -> requirePublicReference(reference, "artifact reference"))
                .toList();
            acceptedGeneratedArtifacts = List.copyOf(Objects.requireNonNull(acceptedGeneratedArtifacts, "accepted generated artifacts must not be null"));
            if (acceptedGeneratedArtifacts.isEmpty()) {
                throw new IllegalArgumentException("accepted generated artifacts are required");
            }
        }
    }

    public enum BtmArtifactKind {
        RULE_FILE,
        MANIFEST
    }

    public record ReadableBtmArtifact(
        AnalysisArtifactReference reference,
        BtmArtifactKind kind
    ) {
        public ReadableBtmArtifact {
            reference = Objects.requireNonNull(reference, "artifact reference must not be null");
            kind = Objects.requireNonNull(kind, "artifact kind must not be null");
        }
    }

    public record BtmArtifactDescriptor(
        String artifactReference,
        BtmArtifactKind artifactKind,
        String relativePath,
        String sha256,
        long sizeBytes,
        int chunkCount,
        String contentType
    ) {
        public BtmArtifactDescriptor {
            artifactReference = requirePublicReference(artifactReference, "artifact reference");
            artifactKind = Objects.requireNonNull(artifactKind, "artifact kind must not be null");
            relativePath = requireArtifactPath(relativePath, "artifact relative path");
            sha256 = requireText(sha256, "artifact sha256").toLowerCase(Locale.ROOT);
            if (!sha256.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("artifact checksum must be a SHA-256 hex value");
            }
            if (sizeBytes < 0 || chunkCount < 0) {
                throw new IllegalArgumentException("artifact descriptor counts must not be negative");
            }
            contentType = requireText(contentType, "artifact content type");
        }

        public String canonical() {
            return String.join("|",
                artifactReference,
                artifactKind.name(),
                relativePath,
                sha256,
                Long.toString(sizeBytes),
                Integer.toString(chunkCount),
                contentType
            );
        }
    }

    public record BtmDeliveryManifest(
        String manifestId,
        AnalysisRunId analysisRunId,
        AnalysisJobId analysisJobId,
        SourceSnapshotId sourceSnapshotId,
        AnalysisCompleteness completeness,
        List<BtmArtifactDescriptor> artifacts,
        long totalSizeBytes,
        String manifestSha256,
        String deliveryOrder,
        ReproducibilityMetadata reproducibility,
        TargetSelection targetSelection
    ) {
        public BtmDeliveryManifest {
            manifestId = requireText(manifestId, "delivery manifest id");
            analysisRunId = Objects.requireNonNull(analysisRunId, "analysis run id must not be null");
            analysisJobId = Objects.requireNonNull(analysisJobId, "analysis job id must not be null");
            sourceSnapshotId = Objects.requireNonNull(sourceSnapshotId, "source snapshot id must not be null");
            completeness = Objects.requireNonNull(completeness, "delivery completeness must not be null");
            artifacts = List.copyOf(Objects.requireNonNull(artifacts, "delivery descriptors must not be null"));
            if (totalSizeBytes < 0) {
                throw new IllegalArgumentException("delivery total size must not be negative");
            }
            manifestSha256 = requireText(manifestSha256, "delivery manifest sha256").toLowerCase(Locale.ROOT);
            if (!manifestSha256.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("delivery manifest checksum must be a SHA-256 hex value");
            }
            deliveryOrder = requireText(deliveryOrder, "delivery order");
            reproducibility = Objects.requireNonNull(reproducibility, "reproducibility metadata must not be null");
            targetSelection = Objects.requireNonNull(targetSelection, "target selection must not be null");
        }
    }

    public record StoredBtmArtifactManifest(
        AnalysisRunId analysisRunId,
        AnalysisJobId analysisJobId,
        SourceSnapshotId sourceSnapshotId,
        AnalysisCompleteness completeness,
        List<ArtifactReference> generatedArtifacts,
        ReproducibilityMetadata reproducibility,
        TargetSelection targetSelection
    ) {
        public StoredBtmArtifactManifest {
            analysisRunId = Objects.requireNonNull(analysisRunId, "analysis run id must not be null");
            analysisJobId = Objects.requireNonNull(analysisJobId, "analysis job id must not be null");
            sourceSnapshotId = Objects.requireNonNull(sourceSnapshotId, "source snapshot id must not be null");
            completeness = Objects.requireNonNull(completeness, "stored manifest completeness must not be null");
            generatedArtifacts = List.copyOf(Objects.requireNonNull(generatedArtifacts, "stored manifest artifacts must not be null"));
            reproducibility = Objects.requireNonNull(reproducibility, "reproducibility metadata must not be null");
            targetSelection = Objects.requireNonNull(targetSelection, "target selection must not be null");
        }
    }

    public record BtmArtifactDeliveryPlan(
        BtmDeliveryManifest manifest,
        List<ReadableBtmArtifact> artifacts,
        int chunkBytes
    ) {
        public BtmArtifactDeliveryPlan {
            manifest = Objects.requireNonNull(manifest, "delivery manifest must not be null");
            artifacts = List.copyOf(Objects.requireNonNull(artifacts, "delivery artifacts must not be null"));
            if (chunkBytes < 1 || chunkBytes > MAX_DELIVERY_CHUNK_BYTES) {
                throw new IllegalArgumentException("delivery chunk bytes are outside the supported range");
            }
        }
    }

    public record GeneratedBtmArtifacts(List<AnalysisArtifactReference> artifacts, long totalBytes) {
        public GeneratedBtmArtifacts {
            artifacts = List.copyOf(Objects.requireNonNull(artifacts, "artifacts must not be null"));
            if (totalBytes < 0) {
                throw new IllegalArgumentException("artifact byte count must not be negative");
            }
        }
    }

    public record GenerateBtmRulesResult(
        RequestMetadata metadata,
        AnalysisCompleteness completeness,
        List<AnalysisArtifactReference> generatedArtifacts,
        List<GeneratedRule> generatedRules,
        BtmGenerationSummary summary,
        ReproducibilityMetadata reproducibility,
        List<BtmDiagnostic> diagnostics,
        TargetSelection targetSelection
    ) {
        public GenerateBtmRulesResult {
            metadata = Objects.requireNonNull(metadata, "metadata must not be null");
            completeness = Objects.requireNonNull(completeness, "completeness must not be null");
            generatedArtifacts = List.copyOf(Objects.requireNonNull(generatedArtifacts, "generated artifacts must not be null"));
            generatedRules = List.copyOf(Objects.requireNonNull(generatedRules, "generated rules must not be null"));
            summary = Objects.requireNonNull(summary, "summary must not be null");
            reproducibility = Objects.requireNonNull(reproducibility, "reproducibility metadata must not be null");
            diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics must not be null"));
            targetSelection = Objects.requireNonNull(targetSelection, "target selection must not be null");
        }
    }
}
