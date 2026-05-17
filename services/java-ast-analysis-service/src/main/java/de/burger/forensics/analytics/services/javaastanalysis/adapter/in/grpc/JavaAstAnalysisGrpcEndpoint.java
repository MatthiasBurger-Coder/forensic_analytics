package de.burger.forensics.analytics.services.javaastanalysis.adapter.in.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactReference;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisRunId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind;
import de.burger.forensics.analytics.analysisjob.v1.ArtifactReference;
import de.burger.forensics.analytics.analysisjob.v1.SourceSnapshotId;
import de.burger.forensics.analytics.javaastanalysis.v1.AnalyzeSourceSnapshotRequest;
import de.burger.forensics.analytics.javaastanalysis.v1.AnalyzeSourceSnapshotResponse;
import de.burger.forensics.analytics.javaastanalysis.v1.DiagnosticSeverity;
import de.burger.forensics.analytics.javaastanalysis.v1.JavaAstAnalysisServiceGrpc;
import de.burger.forensics.analytics.javaastanalysis.v1.JavaAstDiagnostic;
import de.burger.forensics.analytics.javaastanalysis.v1.JavaSourceFile;
import de.burger.forensics.analytics.javaastanalysis.v1.OperationStatus;
import de.burger.forensics.analytics.javaastanalysis.v1.ScanPolicy;
import de.burger.forensics.analytics.javaastanalysis.v1.ScanSummary;
import de.burger.forensics.analytics.javaastanalysis.v1.SourceRoot;
import de.burger.forensics.analytics.services.javaastanalysis.application.JavaAstAnalysisApplicationService;
import de.burger.forensics.analytics.services.javaastanalysis.application.JavaAstAnalysisTimeoutException;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalyzeSourceSnapshotCommand;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalyzeSourceSnapshotResult;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.RequestMetadata;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.io.UncheckedIOException;
import java.util.Objects;

import static de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.requireText;

public final class JavaAstAnalysisGrpcEndpoint extends JavaAstAnalysisServiceGrpc.JavaAstAnalysisServiceImplBase {
    private final JavaAstAnalysisApplicationService applicationService;

    public JavaAstAnalysisGrpcEndpoint(JavaAstAnalysisApplicationService applicationService) {
        this.applicationService = Objects.requireNonNull(applicationService, "application service must not be null");
    }

    @Override
    public void analyzeSourceSnapshot(
        AnalyzeSourceSnapshotRequest request,
        StreamObserver<AnalyzeSourceSnapshotResponse> responseObserver
    ) {
        try {
            requireAstWorker(request);
            var result = applicationService.analyze(command(request));
            responseObserver.onNext(response(result));
            responseObserver.onCompleted();
        } catch (RuntimeException error) {
            responseObserver.onError(status(error).asRuntimeException());
        }
    }

    private static void requireAstWorker(AnalyzeSourceSnapshotRequest request) {
        requireText(request.getRequestId(), "request id");
        if (request.getWorkerKind() != AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS) {
            throw new IllegalArgumentException("worker kind must be AST_ANALYSIS");
        }
        if (!request.hasScanPolicy()) {
            throw new IllegalArgumentException("scan policy is required");
        }
    }

    private static AnalyzeSourceSnapshotCommand command(AnalyzeSourceSnapshotRequest request) {
        return new AnalyzeSourceSnapshotCommand(
            new RequestMetadata(
                request.getRequestId(),
                request.getIdempotencyKey(),
                request.getSchemaVersion(),
                request.getCorrelationId(),
                new de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisRunId(
                    request.getAnalysisRunId().getValue()
                ),
                new de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisJobId(
                    request.getAnalysisJobId().getValue()
                ),
                new de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.SourceSnapshotId(
                    request.getSourceSnapshotId().getValue()
                ),
                request.getWorkerVersion(),
                request.getSafeAttributesMap()
            ),
            scanPolicy(request.getScanPolicy()),
            request.getSourceRootsList().stream().map(JavaAstAnalysisGrpcEndpoint::sourceRoot).toList(),
            request.getSourceFilesList().stream().map(JavaAstAnalysisGrpcEndpoint::sourceFile).toList()
        );
    }

    private static de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.ScanPolicy scanPolicy(
        ScanPolicy policy
    ) {
        return new de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.ScanPolicy(
            policy.getMaxFiles(),
            policy.getMaxSourceBytes(),
            policy.getTimeoutSeconds(),
            policy.getEmitSymbolResolutionDiagnostics()
        );
    }

    private static de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.SourceRoot sourceRoot(
        SourceRoot sourceRoot
    ) {
        return new de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.SourceRoot(
            sourceRoot.getRelativePath(),
            sourceRoot.getLanguage()
        );
    }

    private static de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.JavaSourceFile sourceFile(
        JavaSourceFile sourceFile
    ) {
        return new de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.JavaSourceFile(
            sourceFile.getSourceRoot(),
            sourceFile.getRelativePath(),
            sourceFile.getContentUtf8(),
            sourceFile.getSha256(),
            sourceFile.getSizeBytes()
        );
    }

    private static AnalyzeSourceSnapshotResponse response(AnalyzeSourceSnapshotResult result) {
        var status = status("ANALYZED", "Java AST analysis completed", result);
        var builder = AnalyzeSourceSnapshotResponse.newBuilder()
            .setStatus(status)
            .setAnalysisRunId(AnalysisRunId.newBuilder().setValue(result.metadata().analysisRunId().value()))
            .setAnalysisJobId(AnalysisJobId.newBuilder().setValue(result.metadata().analysisJobId().value()))
            .setSourceSnapshotId(SourceSnapshotId.newBuilder().setValue(result.metadata().sourceSnapshotId().value()))
            .setCompleteness(completeness(result.completeness()))
            .setSourceFactArtifact(artifact(result.sourceFactArtifact()))
            .setSummary(summary(result.summary()))
            .putAllSafeAttributes(result.metadata().safeAttributes());
        result.diagnostics().forEach(diagnostic -> builder.addDiagnostics(diagnostic(diagnostic)));
        return builder.build();
    }

    private static OperationStatus status(String code, String message, AnalyzeSourceSnapshotResult result) {
        var builder = OperationStatus.newBuilder()
            .setCode(result.completeness()
                == de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisCompleteness.COMPLETE
                    ? code
                    : "ANALYZED_INCOMPLETE")
            .setMessage(message)
            .setRetryable(false)
            .setCorrelationId(result.metadata().correlationId());
        result.diagnostics().forEach(diagnostic -> builder.addDiagnostics(diagnostic(diagnostic)));
        return builder.build();
    }

    private static AnalysisArtifactReference artifact(
        de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisArtifactReference artifact
    ) {
        return AnalysisArtifactReference.newBuilder()
            .setArtifact(ArtifactReference.newBuilder()
                .setPath(artifact.artifact().path())
                .setType(artifact.artifact().type())
                .setSha256(artifact.artifact().sha256())
                .setSizeBytes(artifact.artifact().sizeBytes()))
            .setCategory(AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC)
            .setProducerService(artifact.producerService())
            .setSchemaVersion(artifact.schemaVersion())
            .setCompleteness(completeness(artifact.completeness()))
            .build();
    }

    private static ScanSummary summary(
        de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.ScanSummary summary
    ) {
        return ScanSummary.newBuilder()
            .setReceivedFileCount(summary.receivedFileCount())
            .setParsedFileCount(summary.parsedFileCount())
            .setSkippedFileCount(summary.skippedFileCount())
            .setParseErrorCount(summary.parseErrorCount())
            .setSourceFactCount(summary.sourceFactCount())
            .setParser(summary.parser())
            .setParserVersion(summary.parserVersion())
            .build();
    }

    private static JavaAstDiagnostic diagnostic(
        de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.JavaAstDiagnostic diagnostic
    ) {
        return JavaAstDiagnostic.newBuilder()
            .setCode(diagnostic.code())
            .setMessage(diagnostic.message())
            .setSeverity(severity(diagnostic.severity()))
            .setSourceSnapshotId(diagnostic.sourceSnapshotId().value())
            .setSourcePath(diagnostic.sourcePath())
            .setLineNumber(diagnostic.lineNumber())
            .setColumnNumber(diagnostic.columnNumber())
            .setRetryable(diagnostic.retryable())
            .setAffectsCompleteness(diagnostic.affectsCompleteness())
            .build();
    }

    private static DiagnosticSeverity severity(
        de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.DiagnosticSeverity severity
    ) {
        return switch (severity) {
            case INFO -> DiagnosticSeverity.DIAGNOSTIC_SEVERITY_INFO;
            case WARNING -> DiagnosticSeverity.DIAGNOSTIC_SEVERITY_WARNING;
            case ERROR -> DiagnosticSeverity.DIAGNOSTIC_SEVERITY_ERROR;
        };
    }

    private static AnalysisCompleteness completeness(
        de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisCompleteness completeness
    ) {
        return switch (completeness) {
            case COMPLETE -> AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE;
            case INCOMPLETE -> AnalysisCompleteness.ANALYSIS_COMPLETENESS_INCOMPLETE;
            case UNKNOWN -> AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNKNOWN;
        };
    }

    private static Status status(RuntimeException error) {
        return switch (error) {
            case IllegalArgumentException ignored -> Status.INVALID_ARGUMENT.withDescription("Invalid Java AST analysis request");
            case NullPointerException ignored -> Status.INVALID_ARGUMENT.withDescription("Invalid Java AST analysis request");
            case JavaAstAnalysisTimeoutException ignored -> Status.DEADLINE_EXCEEDED.withDescription("Java AST analysis timed out");
            case UncheckedIOException ignored -> Status.FAILED_PRECONDITION.withDescription("Java AST analysis artifact write failed");
            default -> Status.FAILED_PRECONDITION.withDescription("Java AST analysis failed");
        };
    }
}
