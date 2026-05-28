package de.burger.forensics.analytics.services.analysisstore.adapter.out.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisRunId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind;
import de.burger.forensics.analytics.analysisjob.v1.SourceSnapshotId;
import de.burger.forensics.analytics.btmgeneration.v1.BtmGenerationPolicy;
import de.burger.forensics.analytics.btmgeneration.v1.BtmGenerationServiceGrpc;
import de.burger.forensics.analytics.btmgeneration.v1.DeliveredAnalysisFacts;
import de.burger.forensics.analytics.btmgeneration.v1.GenerateBtmRulesRequest;
import de.burger.forensics.analytics.btmgeneration.v1.ProbeKind;
import de.burger.forensics.analytics.services.analysisstore.application.port.BtmGenerationWorkerPort;
import de.burger.forensics.analytics.services.analysisstore.application.port.BtmGenerationWorkerPort.BtmGenerationResult;
import de.burger.forensics.analytics.services.analysisstore.application.port.RepositoryAnalysisWorkerPort.WorkerDiagnostic;
import de.burger.forensics.analytics.services.analysisstore.application.port.RepositoryAnalysisWorkerPort.WorkerDiagnosticSeverity;
import de.burger.forensics.analytics.services.analysisstore.application.port.WorkerOwnerApiUnavailableException;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactCategory;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactByteAccess;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactByteCustody;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactReference;
import de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.InstrumentationTarget;
import de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.InstrumentationTargetSelection;
import de.burger.forensics.analytics.services.analysisstore.domain.RepositoryToBtmOrchestrationDomain.StartRepositoryToBtmCommand;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class BtmGenerationGrpcClient implements BtmGenerationWorkerPort, AutoCloseable {
    private final ManagedChannel channel;
    private final BtmGenerationServiceGrpc.BtmGenerationServiceBlockingStub stub;
    private final long deadlineSeconds;
    private final long maxArtifactBytes;

    public BtmGenerationGrpcClient(String host, int port, long deadlineSeconds, long maxArtifactBytes) {
        this(ManagedChannelBuilder
            .forAddress(TrustedPlaintextGrpcTargets.requireTrustedHost(host, "BTM generation gRPC host"), port)
            .usePlaintext()
            .build(), deadlineSeconds, maxArtifactBytes);
    }

    BtmGenerationGrpcClient(
        BtmGenerationServiceGrpc.BtmGenerationServiceBlockingStub stub,
        long deadlineSeconds,
        long maxArtifactBytes
    ) {
        this.channel = null;
        this.stub = Objects.requireNonNull(stub, "stub must not be null");
        this.deadlineSeconds = requirePositive(deadlineSeconds, "deadlineSeconds");
        this.maxArtifactBytes = requirePositive(maxArtifactBytes, "maxArtifactBytes");
    }

    private BtmGenerationGrpcClient(ManagedChannel channel, long deadlineSeconds, long maxArtifactBytes) {
        this.channel = Objects.requireNonNull(channel, "channel must not be null");
        this.stub = BtmGenerationServiceGrpc.newBlockingStub(channel);
        this.deadlineSeconds = requirePositive(deadlineSeconds, "deadlineSeconds");
        this.maxArtifactBytes = requirePositive(maxArtifactBytes, "maxArtifactBytes");
    }

    @Override
    public BtmGenerationResult generate(
        StartRepositoryToBtmCommand command,
        de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJobId btmGenerationJobId,
        de.burger.forensics.analytics.services.analysisstore.domain.SourceSnapshotId sourceSnapshotId,
        List<AnalysisArtifactReference> sourceFactArtifacts,
        List<AnalysisArtifactReference> semanticArtifacts,
        AnalysisCompleteness inputCompleteness,
        InstrumentationTargetSelection targetSelection,
        List<InstrumentationTarget> targets
    ) {
        try {
            var response = stub.withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS)
                .generateBtmRules(request(
                    command,
                    btmGenerationJobId,
                    sourceSnapshotId,
                    sourceFactArtifacts,
                    semanticArtifacts,
                    inputCompleteness,
                    targetSelection,
                    targets
                ));
            return new BtmGenerationResult(
                command.metadata().analysisRunId(),
                btmGenerationJobId,
                sourceSnapshotId,
                completeness(response.getCompleteness()),
                response.getGeneratedArtifactsList().stream()
                    .map(BtmGenerationGrpcClient::artifact)
                    .toList(),
                response.getDiagnosticsList().stream()
                    .map(BtmGenerationGrpcClient::diagnostic)
                    .toList(),
                response.getSafeAttributesMap()
            );
        } catch (StatusRuntimeException error) {
            throw new WorkerOwnerApiUnavailableException("BTM Generation", error.getStatus().getCode().name());
        }
    }

    private GenerateBtmRulesRequest request(
        StartRepositoryToBtmCommand command,
        de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJobId btmGenerationJobId,
        de.burger.forensics.analytics.services.analysisstore.domain.SourceSnapshotId sourceSnapshotId,
        List<AnalysisArtifactReference> sourceFactArtifacts,
        List<AnalysisArtifactReference> semanticArtifacts,
        AnalysisCompleteness inputCompleteness,
        InstrumentationTargetSelection targetSelection,
        List<InstrumentationTarget> targets
    ) {
        var facts = DeliveredAnalysisFacts.newBuilder()
            .setInputCompleteness(toProto(inputCompleteness))
            .setTargetSelection(targetSelection(targetSelection));
        sourceFactArtifacts.forEach(artifact -> facts.addSourceFactArtifacts(artifact(artifact)));
        semanticArtifacts.forEach(artifact -> facts.addSemanticArtifacts(artifact(artifact)));
        targets.forEach(target -> facts.addTargets(target(target)));
        return GenerateBtmRulesRequest.newBuilder()
            .setRequestId(command.metadata().requestId() + "-btm-generate")
            .setIdempotencyKey(command.metadata().requestId() + "-btm-generate")
            .setSchemaVersion(command.metadata().schemaVersion())
            .setCorrelationId(command.metadata().correlationId())
            .setWorkerKind(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_BTM_GENERATION)
            .setAnalysisRunId(AnalysisRunId.newBuilder().setValue(command.metadata().analysisRunId().value()))
            .setAnalysisJobId(AnalysisJobId.newBuilder().setValue(btmGenerationJobId.value()))
            .setSourceSnapshotId(SourceSnapshotId.newBuilder().setValue(sourceSnapshotId.value()))
            .setWorkerVersion("btm-generation-v1")
            .setPolicy(BtmGenerationPolicy.newBuilder()
                .setMaxTargets(targets.size())
                .setMaxArtifactBytes(maxArtifactBytes)
                .setTimeoutSeconds(command.workspacePolicy().timeoutSeconds())
                .setRuleSchemaVersion("btm-rule-v1")
                .setFailOnIncompleteFacts(false))
            .setFacts(facts)
            .putAllSafeAttributes(command.attributes())
            .build();
    }

    private static de.burger.forensics.analytics.btmgeneration.v1.InstrumentationTargetSelection targetSelection(
        InstrumentationTargetSelection selection
    ) {
        return de.burger.forensics.analytics.btmgeneration.v1.InstrumentationTargetSelection.newBuilder()
            .setSelectionId(selection.selectionId())
            .setOwnerService(selection.ownerService())
            .setPolicyVersion(selection.policyVersion())
            .setSelectionFingerprint(selection.selectionFingerprint())
            .setCompleteness(toProto(selection.completeness()))
            .setDeterministicOrder(selection.deterministicOrder())
            .setCorrelationId(selection.correlationId())
            .setTargetCount(selection.targetCount())
            .build();
    }

    private static de.burger.forensics.analytics.btmgeneration.v1.InstrumentationTarget target(
        InstrumentationTarget target
    ) {
        return de.burger.forensics.analytics.btmgeneration.v1.InstrumentationTarget.newBuilder()
            .setTargetId(target.targetId())
            .setSourceFactId(target.sourceFactId())
            .setSemanticNodeId(target.semanticNodeId())
            .setRelativePath(target.relativePath())
            .setFullyQualifiedClassName(target.fullyQualifiedClassName())
            .setMethodName(target.methodName())
            .setSignature(target.signature())
            .setLineNumber(target.lineNumber())
            .setProbeKind(probeKind(target.probeKind()))
            .setSourceFactArtifactReference(target.sourceFactArtifactReference())
            .setSemanticArtifactReference(target.semanticArtifactReference())
            .setOrderIndex(target.orderIndex())
            .setCompleteness(toProto(target.completeness()))
            .setSensitivity(target.sensitivity())
            .build();
    }

    private static de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactReference artifact(
        AnalysisArtifactReference artifact
    ) {
        return de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactReference.newBuilder()
            .setArtifact(de.burger.forensics.analytics.analysisjob.v1.ArtifactReference.newBuilder()
                .setPath(artifact.artifact().path())
                .setType(artifact.artifact().type())
                .setSha256(artifact.artifact().sha256())
                .setSizeBytes(artifact.artifact().sizeBytes()))
            .setCategory(category(artifact.category()))
            .setProducerService(artifact.producerService())
            .setSchemaVersion(artifact.schemaVersion())
            .setCompleteness(toProto(artifact.completeness()))
            .setByteAccess(byteAccess(artifact.byteAccess()))
            .build();
    }

    private static AnalysisArtifactReference artifact(
        de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactReference artifact
    ) {
        return new AnalysisArtifactReference(
            new ArtifactReference(
                artifact.getArtifact().getPath(),
                artifact.getArtifact().getType(),
                artifact.getArtifact().getSha256(),
                artifact.getArtifact().getSizeBytes()
            ),
            category(artifact.getCategory()),
            artifact.getProducerService(),
            artifact.getSchemaVersion(),
            completeness(artifact.getCompleteness()),
            artifactByteAccess(artifact.getByteAccess())
        );
    }

    private static de.burger.forensics.analytics.analysisjob.v1.ArtifactByteAccess byteAccess(ArtifactByteAccess access) {
        return de.burger.forensics.analytics.analysisjob.v1.ArtifactByteAccess.newBuilder()
            .setOwnerService(access.ownerService())
            .setRetrievalContract(access.retrievalContract())
            .setRetrievalReference(access.retrievalReference())
            .setByteCustody(byteCustody(access.byteCustody()))
            .build();
    }

    private static ArtifactByteAccess artifactByteAccess(
        de.burger.forensics.analytics.analysisjob.v1.ArtifactByteAccess access
    ) {
        return new ArtifactByteAccess(
            access.getOwnerService(),
            access.getRetrievalContract(),
            access.getRetrievalReference(),
            switch (access.getByteCustody()) {
                case ARTIFACT_BYTE_CUSTODY_PRODUCER_RETAINED -> ArtifactByteCustody.PRODUCER_RETAINED;
                case ARTIFACT_BYTE_CUSTODY_SCOPED_OBJECT_ACCESS -> ArtifactByteCustody.SCOPED_OBJECT_ACCESS;
                case ARTIFACT_BYTE_CUSTODY_EXPLICIT_HANDOFF -> ArtifactByteCustody.EXPLICIT_HANDOFF;
                case ARTIFACT_BYTE_CUSTODY_UNSPECIFIED, UNRECOGNIZED ->
                    throw new IllegalArgumentException("artifact byte custody must be specified");
            }
        );
    }

    private static WorkerDiagnostic diagnostic(de.burger.forensics.analytics.btmgeneration.v1.BtmDiagnostic diagnostic) {
        return new WorkerDiagnostic(
            diagnostic.getCode(),
            diagnostic.getMessage(),
            severity(diagnostic.getSeverity()),
            diagnostic.getRetryable(),
            diagnostic.getAffectsCompleteness()
        );
    }

    private static de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness toProto(AnalysisCompleteness completeness) {
        return switch (completeness) {
            case COMPLETE -> de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE;
            case INCOMPLETE -> de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.ANALYSIS_COMPLETENESS_INCOMPLETE;
            case UNKNOWN -> de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNKNOWN;
        };
    }

    private static AnalysisCompleteness completeness(
        de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness completeness
    ) {
        return switch (completeness) {
            case ANALYSIS_COMPLETENESS_COMPLETE -> AnalysisCompleteness.COMPLETE;
            case ANALYSIS_COMPLETENESS_INCOMPLETE -> AnalysisCompleteness.INCOMPLETE;
            case ANALYSIS_COMPLETENESS_UNKNOWN, ANALYSIS_COMPLETENESS_UNSPECIFIED, UNRECOGNIZED -> AnalysisCompleteness.UNKNOWN;
        };
    }

    private static de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory category(
        AnalysisArtifactCategory category
    ) {
        return switch (category) {
            case STATIC -> de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC;
            case RUNTIME -> de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_RUNTIME;
            case PROJECTION ->
                de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_PROJECTION;
            case GENERATED ->
                de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_GENERATED;
        };
    }

    private static AnalysisArtifactCategory category(
        de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory category
    ) {
        return switch (category) {
            case ANALYSIS_ARTIFACT_CATEGORY_STATIC -> AnalysisArtifactCategory.STATIC;
            case ANALYSIS_ARTIFACT_CATEGORY_RUNTIME -> AnalysisArtifactCategory.RUNTIME;
            case ANALYSIS_ARTIFACT_CATEGORY_PROJECTION -> AnalysisArtifactCategory.PROJECTION;
            case ANALYSIS_ARTIFACT_CATEGORY_GENERATED -> AnalysisArtifactCategory.GENERATED;
            case ANALYSIS_ARTIFACT_CATEGORY_UNSPECIFIED, UNRECOGNIZED ->
                throw new IllegalArgumentException("artifact category must be specified");
        };
    }

    private static de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody byteCustody(ArtifactByteCustody custody) {
        return switch (custody) {
            case PRODUCER_RETAINED ->
                de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_PRODUCER_RETAINED;
            case SCOPED_OBJECT_ACCESS ->
                de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_SCOPED_OBJECT_ACCESS;
            case EXPLICIT_HANDOFF ->
                de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_EXPLICIT_HANDOFF;
        };
    }

    private static ProbeKind probeKind(
        de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.ProbeKind probeKind
    ) {
        return switch (probeKind) {
            case METHOD_ENTRY -> ProbeKind.PROBE_KIND_METHOD_ENTRY;
            case METHOD_EXIT -> ProbeKind.PROBE_KIND_METHOD_EXIT;
            case THROW -> ProbeKind.PROBE_KIND_THROW;
            case UNKNOWN -> ProbeKind.PROBE_KIND_UNSPECIFIED;
        };
    }

    private static WorkerDiagnosticSeverity severity(de.burger.forensics.analytics.btmgeneration.v1.DiagnosticSeverity severity) {
        return switch (severity) {
            case DIAGNOSTIC_SEVERITY_ERROR -> WorkerDiagnosticSeverity.ERROR;
            case DIAGNOSTIC_SEVERITY_WARNING -> WorkerDiagnosticSeverity.WARNING;
            case DIAGNOSTIC_SEVERITY_INFO, DIAGNOSTIC_SEVERITY_UNSPECIFIED, UNRECOGNIZED -> WorkerDiagnosticSeverity.INFO;
        };
    }

    private static long requirePositive(long value, String name) {
        if (value < 1) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    @Override
    public void close() {
        if (channel != null) {
            channel.shutdownNow();
        }
    }
}
