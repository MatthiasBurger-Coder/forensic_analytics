package de.burger.forensics.analytics.services.btmgeneration.adapter.in.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactReference;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisRunId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind;
import de.burger.forensics.analytics.analysisjob.v1.ArtifactReference;
import de.burger.forensics.analytics.analysisjob.v1.SourceSnapshotId;
import de.burger.forensics.analytics.btmgeneration.v1.BtmDiagnostic;
import de.burger.forensics.analytics.btmgeneration.v1.BtmGenerationServiceGrpc;
import de.burger.forensics.analytics.btmgeneration.v1.BtmGenerationSummary;
import de.burger.forensics.analytics.btmgeneration.v1.BtmRuleSummary;
import de.burger.forensics.analytics.btmgeneration.v1.DiagnosticSeverity;
import de.burger.forensics.analytics.btmgeneration.v1.GenerateBtmRulesRequest;
import de.burger.forensics.analytics.btmgeneration.v1.GenerateBtmRulesResponse;
import de.burger.forensics.analytics.btmgeneration.v1.InstrumentationTarget;
import de.burger.forensics.analytics.btmgeneration.v1.OperationStatus;
import de.burger.forensics.analytics.btmgeneration.v1.ProbeKind;
import de.burger.forensics.analytics.btmgeneration.v1.ReproducibilityMetadata;
import de.burger.forensics.analytics.services.btmgeneration.application.BtmArtifactException;
import de.burger.forensics.analytics.services.btmgeneration.application.BtmGenerationApplicationService;
import de.burger.forensics.analytics.services.btmgeneration.application.BtmGenerationTimeoutException;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.GenerateBtmRulesCommand;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.GenerateBtmRulesResult;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.RequestMetadata;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.Objects;
import java.util.Map;

import static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.requireText;

public final class BtmGenerationGrpcEndpoint extends BtmGenerationServiceGrpc.BtmGenerationServiceImplBase {
    private static final Map<AnalysisCompleteness, de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisCompleteness> COMPLETENESS_TO_DOMAIN =
        Map.of(
            AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE,
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisCompleteness.COMPLETE,
            AnalysisCompleteness.ANALYSIS_COMPLETENESS_INCOMPLETE,
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisCompleteness.INCOMPLETE,
            AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNKNOWN,
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisCompleteness.UNKNOWN
        );
    private static final Map<de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisCompleteness, AnalysisCompleteness> COMPLETENESS_FROM_DOMAIN =
        Map.of(
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisCompleteness.COMPLETE,
            AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE,
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisCompleteness.INCOMPLETE,
            AnalysisCompleteness.ANALYSIS_COMPLETENESS_INCOMPLETE,
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisCompleteness.UNKNOWN,
            AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNKNOWN
        );
    private static final Map<AnalysisArtifactCategory, de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactCategory> CATEGORY_TO_DOMAIN =
        Map.of(
            AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC,
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactCategory.STATIC,
            AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_RUNTIME,
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactCategory.RUNTIME,
            AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_PROJECTION,
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactCategory.PROJECTION,
            AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_GENERATED,
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactCategory.GENERATED
        );
    private static final Map<ProbeKind, de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ProbeKind> PROBE_TO_DOMAIN =
        Map.of(
            ProbeKind.PROBE_KIND_METHOD_ENTRY,
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ProbeKind.METHOD_ENTRY,
            ProbeKind.PROBE_KIND_METHOD_EXIT,
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ProbeKind.METHOD_EXIT,
            ProbeKind.PROBE_KIND_THROW,
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ProbeKind.THROW
        );
    private static final Map<de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ProbeKind, ProbeKind> PROBE_FROM_DOMAIN =
        Map.of(
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ProbeKind.METHOD_ENTRY,
            ProbeKind.PROBE_KIND_METHOD_ENTRY,
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ProbeKind.METHOD_EXIT,
            ProbeKind.PROBE_KIND_METHOD_EXIT,
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ProbeKind.THROW,
            ProbeKind.PROBE_KIND_THROW,
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ProbeKind.UNKNOWN,
            ProbeKind.PROBE_KIND_UNSPECIFIED
        );
    private static final Map<de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.DiagnosticSeverity, DiagnosticSeverity> SEVERITY_FROM_DOMAIN =
        Map.of(
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.DiagnosticSeverity.INFO,
            DiagnosticSeverity.DIAGNOSTIC_SEVERITY_INFO,
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.DiagnosticSeverity.WARNING,
            DiagnosticSeverity.DIAGNOSTIC_SEVERITY_WARNING,
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.DiagnosticSeverity.ERROR,
            DiagnosticSeverity.DIAGNOSTIC_SEVERITY_ERROR
        );

    private final BtmGenerationApplicationService applicationService;

    public BtmGenerationGrpcEndpoint(BtmGenerationApplicationService applicationService) {
        this.applicationService = Objects.requireNonNull(applicationService, "application service must not be null");
    }

    @Override
    public void generateBtmRules(
        GenerateBtmRulesRequest request,
        StreamObserver<GenerateBtmRulesResponse> responseObserver
    ) {
        try {
            requireBtmWorker(request);
            var result = applicationService.generate(command(request));
            responseObserver.onNext(response(result));
            responseObserver.onCompleted();
        } catch (RuntimeException error) {
            responseObserver.onError(status(error).asRuntimeException());
        }
    }

    private static void requireBtmWorker(GenerateBtmRulesRequest request) {
        requireText(request.getRequestId(), "request id");
        if (request.getWorkerKind() != AnalysisWorkerKind.ANALYSIS_WORKER_KIND_BTM_GENERATION) {
            throw new IllegalArgumentException("worker kind must be BTM_GENERATION");
        }
        if (!request.hasPolicy()) {
            throw new IllegalArgumentException("BTM generation policy is required");
        }
        if (!request.hasFacts()) {
            throw new IllegalArgumentException("delivered facts are required");
        }
    }

    private static GenerateBtmRulesCommand command(GenerateBtmRulesRequest request) {
        return new GenerateBtmRulesCommand(
            new RequestMetadata(
                request.getRequestId(),
                request.getIdempotencyKey(),
                request.getSchemaVersion(),
                request.getCorrelationId(),
                new de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisRunId(
                    request.getAnalysisRunId().getValue()
                ),
                new de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisJobId(
                    request.getAnalysisJobId().getValue()
                ),
                new de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.SourceSnapshotId(
                    request.getSourceSnapshotId().getValue()
                ),
                request.getWorkerVersion(),
                request.getSafeAttributesMap()
            ),
            policy(request.getPolicy()),
            facts(request)
        );
    }

    private static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BtmGenerationPolicy policy(
        de.burger.forensics.analytics.btmgeneration.v1.BtmGenerationPolicy policy
    ) {
        return new de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BtmGenerationPolicy(
            policy.getMaxTargets(),
            policy.getMaxArtifactBytes(),
            policy.getTimeoutSeconds(),
            policy.getRuleSchemaVersion(),
            policy.getFailOnIncompleteFacts()
        );
    }

    private static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.DeliveredFacts facts(
        GenerateBtmRulesRequest request
    ) {
        var facts = request.getFacts();
        return new de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.DeliveredFacts(
            facts.getSourceFactArtifactsList().stream().map(BtmGenerationGrpcEndpoint::artifact).toList(),
            facts.getSemanticArtifactsList().stream().map(BtmGenerationGrpcEndpoint::artifact).toList(),
            facts.getTargetsList().stream().map(BtmGenerationGrpcEndpoint::target).toList(),
            completeness(facts.getInputCompleteness())
        );
    }

    private static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.RuleTarget target(
        InstrumentationTarget target
    ) {
        return new de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.RuleTarget(
            target.getTargetId(),
            target.getSourceFactId(),
            target.getSemanticNodeId(),
            target.getRelativePath(),
            target.getFullyQualifiedClassName(),
            target.getMethodName(),
            target.getSignature(),
            target.getLineNumber(),
            probeKind(target.getProbeKind())
        );
    }

    private static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactReference artifact(
        AnalysisArtifactReference reference
    ) {
        return new de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactReference(
            new de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ArtifactReference(
                reference.getArtifact().getPath(),
                reference.getArtifact().getType(),
                reference.getArtifact().getSha256(),
                reference.getArtifact().getSizeBytes()
            ),
            category(reference.getCategory()),
            reference.getProducerService(),
            reference.getSchemaVersion(),
            completeness(reference.getCompleteness())
        );
    }

    private static GenerateBtmRulesResponse response(GenerateBtmRulesResult result) {
        var builder = GenerateBtmRulesResponse.newBuilder()
            .setStatus(status("GENERATED", "BTM generation completed", result))
            .setAnalysisRunId(AnalysisRunId.newBuilder().setValue(result.metadata().analysisRunId().value()))
            .setAnalysisJobId(AnalysisJobId.newBuilder().setValue(result.metadata().analysisJobId().value()))
            .setSourceSnapshotId(SourceSnapshotId.newBuilder().setValue(result.metadata().sourceSnapshotId().value()))
            .setCompleteness(completeness(result.completeness()))
            .setSummary(summary(result.summary()))
            .setReproducibility(reproducibility(result.reproducibility()))
            .putAllSafeAttributes(result.metadata().safeAttributes());
        result.generatedArtifacts().forEach(artifact -> builder.addGeneratedArtifacts(artifact(artifact)));
        result.generatedRules().forEach(rule -> builder.addGeneratedRules(rule(rule)));
        result.diagnostics().forEach(diagnostic -> builder.addDiagnostics(diagnostic(diagnostic)));
        return builder.build();
    }

    private static OperationStatus status(String code, String message, GenerateBtmRulesResult result) {
        var builder = OperationStatus.newBuilder()
            .setCode(result.completeness()
                == de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisCompleteness.COMPLETE
                    ? code
                    : "GENERATED_INCOMPLETE")
            .setMessage(message)
            .setRetryable(false)
            .setCorrelationId(result.metadata().correlationId());
        result.diagnostics().forEach(diagnostic -> builder.addDiagnostics(diagnostic(diagnostic)));
        return builder.build();
    }

    private static AnalysisArtifactReference artifact(
        de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactReference artifact
    ) {
        return AnalysisArtifactReference.newBuilder()
            .setArtifact(ArtifactReference.newBuilder()
                .setPath(artifact.artifact().path())
                .setType(artifact.artifact().type())
                .setSha256(artifact.artifact().sha256())
                .setSizeBytes(artifact.artifact().sizeBytes()))
            .setCategory(AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_GENERATED)
            .setProducerService(artifact.producerService())
            .setSchemaVersion(artifact.schemaVersion())
            .setCompleteness(completeness(artifact.completeness()))
            .build();
    }

    private static BtmRuleSummary rule(
        de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.GeneratedRule rule
    ) {
        var target = rule.target();
        return BtmRuleSummary.newBuilder()
            .setRuleId(rule.ruleId())
            .setTargetId(target.targetId())
            .setSourceFactId(target.sourceFactId())
            .setRelativePath(target.relativePath())
            .setFullyQualifiedClassName(target.fullyQualifiedClassName())
            .setMethodName(target.methodName())
            .setSignature(target.signature())
            .setLineNumber(target.lineNumber())
            .setProbeKind(probeKind(target.probeKind()))
            .build();
    }

    private static BtmGenerationSummary summary(
        de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BtmGenerationSummary summary
    ) {
        return BtmGenerationSummary.newBuilder()
            .setReceivedTargetCount(summary.receivedTargetCount())
            .setGeneratedRuleCount(summary.generatedRuleCount())
            .setSkippedTargetCount(summary.skippedTargetCount())
            .setSourceFactArtifactCount(summary.sourceFactArtifactCount())
            .setSemanticArtifactCount(summary.semanticArtifactCount())
            .setProducerService(summary.producerService())
            .setGeneratorVersion(summary.generatorVersion())
            .setRuleSchemaVersion(summary.ruleSchemaVersion())
            .build();
    }

    private static ReproducibilityMetadata reproducibility(
        de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ReproducibilityMetadata metadata
    ) {
        return ReproducibilityMetadata.newBuilder()
            .setFactsFingerprint(metadata.factsFingerprint())
            .setPolicyFingerprint(metadata.policyFingerprint())
            .setGenerationFingerprint(metadata.generationFingerprint())
            .setGeneratorVersion(metadata.generatorVersion())
            .setDeterministicSort(metadata.deterministicSort())
            .build();
    }

    private static BtmDiagnostic diagnostic(
        de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BtmDiagnostic diagnostic
    ) {
        return BtmDiagnostic.newBuilder()
            .setCode(diagnostic.code())
            .setMessage(diagnostic.message())
            .setSeverity(severity(diagnostic.severity()))
            .setSourceSnapshotId(diagnostic.sourceSnapshotId().value())
            .setTargetId(diagnostic.targetId())
            .setArtifactPath(diagnostic.artifactPath())
            .setRetryable(diagnostic.retryable())
            .setAffectsCompleteness(diagnostic.affectsCompleteness())
            .build();
    }

    private static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisCompleteness completeness(
        AnalysisCompleteness completeness
    ) {
        return COMPLETENESS_TO_DOMAIN.getOrDefault(
            completeness,
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisCompleteness.UNKNOWN
        );
    }

    private static AnalysisCompleteness completeness(
        de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisCompleteness completeness
    ) {
        return COMPLETENESS_FROM_DOMAIN.get(completeness);
    }

    private static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactCategory category(
        AnalysisArtifactCategory category
    ) {
        return CATEGORY_TO_DOMAIN.getOrDefault(
            category,
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactCategory.UNKNOWN
        );
    }

    private static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ProbeKind probeKind(
        ProbeKind probeKind
    ) {
        return PROBE_TO_DOMAIN.getOrDefault(
            probeKind,
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ProbeKind.UNKNOWN
        );
    }

    private static ProbeKind probeKind(
        de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ProbeKind probeKind
    ) {
        return PROBE_FROM_DOMAIN.get(probeKind);
    }

    private static DiagnosticSeverity severity(
        de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.DiagnosticSeverity severity
    ) {
        return SEVERITY_FROM_DOMAIN.get(severity);
    }

    private static Status status(RuntimeException error) {
        return switch (error) {
            case IllegalArgumentException ignored -> Status.INVALID_ARGUMENT.withDescription("Invalid BTM generation request");
            case NullPointerException ignored -> Status.INVALID_ARGUMENT.withDescription("Invalid BTM generation request");
            case BtmGenerationTimeoutException ignored -> Status.DEADLINE_EXCEEDED.withDescription("BTM generation timed out");
            case BtmArtifactException artifactError -> artifactError.getMessage().contains("output limit")
                ? Status.RESOURCE_EXHAUSTED.withDescription("BTM generation output limit exceeded")
                : Status.FAILED_PRECONDITION.withDescription("BTM artifact write failed");
            default -> Status.FAILED_PRECONDITION.withDescription("BTM generation failed");
        };
    }
}
