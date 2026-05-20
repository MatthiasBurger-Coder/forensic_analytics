package de.burger.forensics.analytics.services.repositoryanalysis.adapter.out.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisRunId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind;
import de.burger.forensics.analytics.analysisjob.v1.SourceSnapshotId;
import de.burger.forensics.analytics.javaastanalysis.v1.AnalyzeSourceSnapshotRequest;
import de.burger.forensics.analytics.javaastanalysis.v1.DiagnosticSeverity;
import de.burger.forensics.analytics.javaastanalysis.v1.JavaAstAnalysisServiceGrpc;
import de.burger.forensics.analytics.javaastanalysis.v1.JavaSourceFile;
import de.burger.forensics.analytics.javaastanalysis.v1.ScanPolicy;
import de.burger.forensics.analytics.javaastanalysis.v1.SourceRoot;
import de.burger.forensics.analytics.services.repositoryanalysis.application.port.JavaAstAnalysisPort;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.ArtifactByteAccess;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.ArtifactByteCustody;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.ArtifactReference;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.Diagnostic;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.JavaAstAnalysisHandoffCommand;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.JavaAstAnalysisHandoffResult;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.JavaAstScanSummary;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceSnapshotCompleteness;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.concurrent.TimeUnit;

public final class JavaAstAnalysisGrpcClient implements JavaAstAnalysisPort, AutoCloseable {
    private final ManagedChannel channel;
    private final JavaAstAnalysisServiceGrpc.JavaAstAnalysisServiceBlockingStub stub;
    private final long deadlineSeconds;

    public JavaAstAnalysisGrpcClient(String host, int port, long deadlineSeconds) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext().build(), deadlineSeconds);
    }

    JavaAstAnalysisGrpcClient(JavaAstAnalysisServiceGrpc.JavaAstAnalysisServiceBlockingStub stub, long deadlineSeconds) {
        this.channel = null;
        this.stub = stub;
        this.deadlineSeconds = deadlineSeconds;
    }

    private JavaAstAnalysisGrpcClient(ManagedChannel channel, long deadlineSeconds) {
        this.channel = channel;
        this.stub = JavaAstAnalysisServiceGrpc.newBlockingStub(channel);
        this.deadlineSeconds = deadlineSeconds;
    }

    @Override
    public JavaAstAnalysisHandoffResult analyze(JavaAstAnalysisHandoffCommand command) {
        try {
            var response = stub
                .withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS)
                .analyzeSourceSnapshot(request(command));
            return new JavaAstAnalysisHandoffResult(
                new de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.AnalysisRunId(
                    response.getAnalysisRunId().getValue()
                ),
                new de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.AnalysisJobId(
                    response.getAnalysisJobId().getValue()
                ),
                new de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceSnapshotId(
                    response.getSourceSnapshotId().getValue()
                ),
                completeness(response.getCompleteness()),
                new ArtifactReference(
                    response.getSourceFactArtifact().getArtifact().getPath(),
                    response.getSourceFactArtifact().getArtifact().getType(),
                    response.getSourceFactArtifact().getArtifact().getSha256(),
                    response.getSourceFactArtifact().getArtifact().getSizeBytes()
                ),
                response.getSourceFactArtifact().getProducerService(),
                response.getSourceFactArtifact().getSchemaVersion(),
                byteAccess(response.getSourceFactArtifact().getByteAccess()),
                new JavaAstScanSummary(
                    response.getSummary().getReceivedFileCount(),
                    response.getSummary().getParsedFileCount(),
                    response.getSummary().getSkippedFileCount(),
                    response.getSummary().getParseErrorCount(),
                    response.getSummary().getSourceFactCount(),
                    response.getSummary().getParser(),
                    response.getSummary().getParserVersion()
                ),
                response.getDiagnosticsList().stream()
                    .map(diagnostic -> new Diagnostic(
                        diagnostic.getCode(),
                        diagnostic.getMessage(),
                        severity(diagnostic.getSeverity())
                    ))
                    .toList(),
                response.getSafeAttributesMap()
            );
        } catch (StatusRuntimeException error) {
            throw new IllegalStateException("Java AST analysis service failed with status "
                + error.getStatus().getCode());
        }
    }

    private static AnalyzeSourceSnapshotRequest request(JavaAstAnalysisHandoffCommand command) {
        var builder = AnalyzeSourceSnapshotRequest.newBuilder()
            .setRequestId(command.requestId())
            .setIdempotencyKey(command.idempotencyKey())
            .setSchemaVersion(command.schemaVersion())
            .setCorrelationId(command.correlationId())
            .setWorkerKind(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS)
            .setAnalysisRunId(AnalysisRunId.newBuilder().setValue(command.analysisRunId().value()))
            .setAnalysisJobId(AnalysisJobId.newBuilder().setValue(command.analysisJobId().value()))
            .setSourceSnapshotId(SourceSnapshotId.newBuilder().setValue(command.sourceSnapshotId().value()))
            .setWorkerVersion(command.workerVersion())
            .setScanPolicy(ScanPolicy.newBuilder()
                .setMaxFiles(command.policy().maxFiles())
                .setMaxSourceBytes(command.policy().maxSourceBytes())
                .setTimeoutSeconds(command.policy().timeoutSeconds())
                .setEmitSymbolResolutionDiagnostics(true))
            .putAllSafeAttributes(command.safeAttributes());
        command.sourceRoots().forEach(sourceRoot -> builder.addSourceRoots(SourceRoot.newBuilder()
            .setRelativePath(sourceRoot.relativePath())
            .setLanguage(sourceRoot.language())));
        command.sourceFiles().forEach(sourceFile -> builder.addSourceFiles(JavaSourceFile.newBuilder()
            .setSourceRoot(sourceFile.sourceRoot())
            .setRelativePath(sourceFile.relativePath())
            .setContentUtf8(sourceFile.contentUtf8())
            .setSha256(sourceFile.sha256())
            .setSizeBytes(sourceFile.sizeBytes())));
        return builder.build();
    }

    private static SourceSnapshotCompleteness completeness(
        de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness completeness
    ) {
        return switch (completeness) {
            case ANALYSIS_COMPLETENESS_COMPLETE -> SourceSnapshotCompleteness.COMPLETE;
            case ANALYSIS_COMPLETENESS_INCOMPLETE -> SourceSnapshotCompleteness.INCOMPLETE;
            default -> SourceSnapshotCompleteness.UNKNOWN;
        };
    }

    private static ArtifactByteAccess byteAccess(
        de.burger.forensics.analytics.analysisjob.v1.ArtifactByteAccess byteAccess
    ) {
        return new ArtifactByteAccess(
            byteAccess.getOwnerService(),
            byteAccess.getRetrievalContract(),
            byteAccess.getRetrievalReference(),
            byteCustody(byteAccess.getByteCustody())
        );
    }

    private static ArtifactByteCustody byteCustody(
        de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody byteCustody
    ) {
        return switch (byteCustody) {
            case ARTIFACT_BYTE_CUSTODY_SCOPED_OBJECT_ACCESS -> ArtifactByteCustody.SCOPED_OBJECT_ACCESS;
            case ARTIFACT_BYTE_CUSTODY_EXPLICIT_HANDOFF -> ArtifactByteCustody.EXPLICIT_HANDOFF;
            default -> ArtifactByteCustody.PRODUCER_RETAINED;
        };
    }

    private static de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.DiagnosticSeverity severity(
        DiagnosticSeverity severity
    ) {
        return switch (severity) {
            case DIAGNOSTIC_SEVERITY_WARNING ->
                de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.DiagnosticSeverity.WARNING;
            case DIAGNOSTIC_SEVERITY_ERROR ->
                de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.DiagnosticSeverity.ERROR;
            default ->
                de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.DiagnosticSeverity.INFO;
        };
    }

    @Override
    public void close() {
        if (channel != null) {
            channel.shutdownNow();
        }
    }
}
