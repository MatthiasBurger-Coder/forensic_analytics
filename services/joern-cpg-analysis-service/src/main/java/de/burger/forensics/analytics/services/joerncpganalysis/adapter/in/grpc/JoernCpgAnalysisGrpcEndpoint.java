package de.burger.forensics.analytics.services.joerncpganalysis.adapter.in.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactReference;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisRunId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind;
import de.burger.forensics.analytics.analysisjob.v1.ArtifactReference;
import de.burger.forensics.analytics.analysisjob.v1.SourceSnapshotId;
import de.burger.forensics.analytics.joerncpganalysis.v1.AnalyzeJoernCpgRequest;
import de.burger.forensics.analytics.joerncpganalysis.v1.AnalyzeJoernCpgResponse;
import de.burger.forensics.analytics.joerncpganalysis.v1.DiagnosticSeverity;
import de.burger.forensics.analytics.joerncpganalysis.v1.JoernCpgAnalysisServiceGrpc;
import de.burger.forensics.analytics.joerncpganalysis.v1.JoernCpgDiagnostic;
import de.burger.forensics.analytics.joerncpganalysis.v1.JoernCpgPolicy;
import de.burger.forensics.analytics.joerncpganalysis.v1.JoernCpgSummary;
import de.burger.forensics.analytics.joerncpganalysis.v1.OperationStatus;
import de.burger.forensics.analytics.joerncpganalysis.v1.SourceRoot;
import de.burger.forensics.analytics.joerncpganalysis.v1.SourceWorkspace;
import de.burger.forensics.analytics.services.joerncpganalysis.application.JoernCpgAnalysisApplicationService;
import de.burger.forensics.analytics.services.joerncpganalysis.application.JoernCpgAnalysisTimeoutException;
import de.burger.forensics.analytics.services.joerncpganalysis.application.JoernCpgArtifactException;
import de.burger.forensics.analytics.services.joerncpganalysis.application.JoernRuntimeUnavailableException;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgCommand;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgResult;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.RequestMetadata;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.io.UncheckedIOException;
import java.util.Objects;

import static de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.requireText;

public final class JoernCpgAnalysisGrpcEndpoint extends JoernCpgAnalysisServiceGrpc.JoernCpgAnalysisServiceImplBase {
    private final JoernCpgAnalysisApplicationService applicationService;

    public JoernCpgAnalysisGrpcEndpoint(JoernCpgAnalysisApplicationService applicationService) {
        this.applicationService = Objects.requireNonNull(applicationService, "application service must not be null");
    }

    @Override
    public void analyzeSourceSnapshot(
        AnalyzeJoernCpgRequest request,
        StreamObserver<AnalyzeJoernCpgResponse> responseObserver
    ) {
        try {
            requireJoernWorker(request);
            var result = applicationService.analyze(command(request));
            responseObserver.onNext(response(result));
            responseObserver.onCompleted();
        } catch (RuntimeException error) {
            responseObserver.onError(status(error).asRuntimeException());
        }
    }

    private static void requireJoernWorker(AnalyzeJoernCpgRequest request) {
        requireText(request.getRequestId(), "request id");
        if (request.getWorkerKind() != AnalysisWorkerKind.ANALYSIS_WORKER_KIND_JOERN_ANALYSIS) {
            throw new IllegalArgumentException("worker kind must be JOERN_ANALYSIS");
        }
        if (!request.hasPolicy()) {
            throw new IllegalArgumentException("Joern CPG policy is required");
        }
        if (!request.hasWorkspace()) {
            throw new IllegalArgumentException("source workspace is required");
        }
    }

    private static AnalyzeJoernCpgCommand command(AnalyzeJoernCpgRequest request) {
        return new AnalyzeJoernCpgCommand(
            new RequestMetadata(
                request.getRequestId(),
                request.getIdempotencyKey(),
                request.getSchemaVersion(),
                request.getCorrelationId(),
                new de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisRunId(
                    request.getAnalysisRunId().getValue()
                ),
                new de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisJobId(
                    request.getAnalysisJobId().getValue()
                ),
                new de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.SourceSnapshotId(
                    request.getSourceSnapshotId().getValue()
                ),
                request.getWorkerVersion(),
                request.getSafeAttributesMap()
            ),
            policy(request.getPolicy()),
            workspace(request.getWorkspace())
        );
    }

    private static de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.JoernCpgPolicy policy(
        JoernCpgPolicy policy
    ) {
        return new de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.JoernCpgPolicy(
            policy.getMaxSourceRoots(),
            policy.getMaxWorkspaceBytes(),
            policy.getMaxArtifactBytes(),
            policy.getTimeoutSeconds(),
            policy.getJoernImageReference(),
            policy.getQueryBundleVersion(),
            policy.getRequireCallgraph(),
            policy.getRequireControlflow(),
            policy.getRequireDataflow()
        );
    }

    private static de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.SourceWorkspace workspace(
        SourceWorkspace workspace
    ) {
        return new de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.SourceWorkspace(
            workspace.getWorkspaceId(),
            workspace.getSourceRootsList().stream().map(JoernCpgAnalysisGrpcEndpoint::sourceRoot).toList(),
            workspace.getInputArtifactsList().stream().map(JoernCpgAnalysisGrpcEndpoint::artifact).toList()
        );
    }

    private static de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.SourceRoot sourceRoot(
        SourceRoot sourceRoot
    ) {
        return new de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.SourceRoot(
            sourceRoot.getRelativePath(),
            sourceRoot.getLanguage()
        );
    }

    private static de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisArtifactReference artifact(
        AnalysisArtifactReference reference
    ) {
        var artifact = reference.getArtifact();
        return new de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisArtifactReference(
            new de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.ArtifactReference(
                artifact.getPath(),
                artifact.getType(),
                artifact.getSha256(),
                artifact.getSizeBytes()
            ),
            de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisArtifactCategory.STATIC,
            reference.getProducerService(),
            reference.getSchemaVersion(),
            completeness(reference.getCompleteness())
        );
    }

    private static AnalyzeJoernCpgResponse response(AnalyzeJoernCpgResult result) {
        var builder = AnalyzeJoernCpgResponse.newBuilder()
            .setStatus(status("ANALYZED", "Joern CPG analysis completed", result))
            .setAnalysisRunId(AnalysisRunId.newBuilder().setValue(result.metadata().analysisRunId().value()))
            .setAnalysisJobId(AnalysisJobId.newBuilder().setValue(result.metadata().analysisJobId().value()))
            .setSourceSnapshotId(SourceSnapshotId.newBuilder().setValue(result.metadata().sourceSnapshotId().value()))
            .setCompleteness(completeness(result.completeness()))
            .setSummary(summary(result.summary()))
            .putAllSafeAttributes(result.metadata().safeAttributes());
        result.semanticArtifacts().forEach(artifact -> builder.addSemanticArtifacts(artifact(artifact)));
        result.diagnostics().forEach(diagnostic -> builder.addDiagnostics(diagnostic(diagnostic)));
        return builder.build();
    }

    private static OperationStatus status(String code, String message, AnalyzeJoernCpgResult result) {
        var builder = OperationStatus.newBuilder()
            .setCode(result.completeness()
                == de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisCompleteness.COMPLETE
                    ? code
                    : "ANALYZED_INCOMPLETE")
            .setMessage(message)
            .setRetryable(false)
            .setCorrelationId(result.metadata().correlationId());
        result.diagnostics().forEach(diagnostic -> builder.addDiagnostics(diagnostic(diagnostic)));
        return builder.build();
    }

    private static AnalysisArtifactReference artifact(
        de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisArtifactReference reference
    ) {
        return AnalysisArtifactReference.newBuilder()
            .setArtifact(ArtifactReference.newBuilder()
                .setPath(reference.artifact().path())
                .setType(reference.artifact().type())
                .setSha256(reference.artifact().sha256())
                .setSizeBytes(reference.artifact().sizeBytes()))
            .setCategory(AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC)
            .setProducerService(reference.producerService())
            .setSchemaVersion(reference.schemaVersion())
            .setCompleteness(completeness(reference.completeness()))
            .build();
    }

    private static JoernCpgSummary summary(
        de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.JoernCpgSummary summary
    ) {
        return JoernCpgSummary.newBuilder()
            .setSourceRootCount(summary.sourceRootCount())
            .setProducedArtifactCount(summary.producedArtifactCount())
            .setMissingArtifactCount(summary.missingArtifactCount())
            .setJoernVersion(summary.joernVersion())
            .setJoernImageReference(summary.joernImageReference())
            .setQueryBundleVersion(summary.queryBundleVersion())
            .setProducerService(summary.producerService())
            .setSchemaVersion(summary.schemaVersion())
            .build();
    }

    private static JoernCpgDiagnostic diagnostic(
        de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.JoernCpgDiagnostic diagnostic
    ) {
        return JoernCpgDiagnostic.newBuilder()
            .setCode(diagnostic.code())
            .setMessage(diagnostic.message())
            .setSeverity(severity(diagnostic.severity()))
            .setSourceSnapshotId(diagnostic.sourceSnapshotId().value())
            .setArtifactPath(diagnostic.artifactPath())
            .setRetryable(diagnostic.retryable())
            .setAffectsCompleteness(diagnostic.affectsCompleteness())
            .build();
    }

    private static DiagnosticSeverity severity(
        de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.DiagnosticSeverity severity
    ) {
        return switch (severity) {
            case INFO -> DiagnosticSeverity.DIAGNOSTIC_SEVERITY_INFO;
            case WARNING -> DiagnosticSeverity.DIAGNOSTIC_SEVERITY_WARNING;
            case ERROR -> DiagnosticSeverity.DIAGNOSTIC_SEVERITY_ERROR;
        };
    }

    private static AnalysisCompleteness completeness(
        de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisCompleteness completeness
    ) {
        return switch (completeness) {
            case COMPLETE -> AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE;
            case INCOMPLETE -> AnalysisCompleteness.ANALYSIS_COMPLETENESS_INCOMPLETE;
            case UNKNOWN -> AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNKNOWN;
        };
    }

    private static de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisCompleteness completeness(
        AnalysisCompleteness completeness
    ) {
        return switch (completeness) {
            case ANALYSIS_COMPLETENESS_COMPLETE ->
                de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisCompleteness.COMPLETE;
            case ANALYSIS_COMPLETENESS_INCOMPLETE ->
                de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisCompleteness.INCOMPLETE;
            case ANALYSIS_COMPLETENESS_UNKNOWN, ANALYSIS_COMPLETENESS_UNSPECIFIED, UNRECOGNIZED ->
                de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisCompleteness.UNKNOWN;
        };
    }

    private static Status status(RuntimeException error) {
        return switch (error) {
            case IllegalArgumentException ignored -> Status.INVALID_ARGUMENT.withDescription("Invalid Joern CPG analysis request");
            case NullPointerException ignored -> Status.INVALID_ARGUMENT.withDescription("Invalid Joern CPG analysis request");
            case JoernCpgAnalysisTimeoutException ignored -> Status.DEADLINE_EXCEEDED.withDescription("Joern CPG analysis timed out");
            case JoernRuntimeUnavailableException ignored -> Status.UNAVAILABLE.withDescription("Joern runtime unavailable");
            case JoernCpgArtifactException ignored -> Status.FAILED_PRECONDITION.withDescription("Joern artifact collection failed");
            case UncheckedIOException ignored -> Status.FAILED_PRECONDITION.withDescription("Joern artifact collection failed");
            default -> Status.FAILED_PRECONDITION.withDescription("Joern CPG analysis failed");
        };
    }
}
