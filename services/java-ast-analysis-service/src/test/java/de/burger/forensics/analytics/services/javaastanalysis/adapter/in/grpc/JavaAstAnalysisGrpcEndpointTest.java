package de.burger.forensics.analytics.services.javaastanalysis.adapter.in.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisRunId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind;
import de.burger.forensics.analytics.analysisjob.v1.SourceSnapshotId;
import de.burger.forensics.analytics.javaastanalysis.v1.AnalyzeSourceSnapshotRequest;
import de.burger.forensics.analytics.javaastanalysis.v1.JavaAstAnalysisServiceGrpc;
import de.burger.forensics.analytics.javaastanalysis.v1.JavaSourceFile;
import de.burger.forensics.analytics.javaastanalysis.v1.ScanPolicy;
import de.burger.forensics.analytics.javaastanalysis.v1.SourceRoot;
import de.burger.forensics.analytics.services.javaastanalysis.adapter.out.filesystem.FileSystemAstResultArtifactWriter;
import de.burger.forensics.analytics.services.javaastanalysis.adapter.out.javaparser.JavaParserSourceScannerAdapter;
import de.burger.forensics.analytics.services.javaastanalysis.application.JavaAstAnalysisApplicationService;
import de.burger.forensics.analytics.services.javaastanalysis.application.JavaAstAnalysisTimeoutException;
import de.burger.forensics.analytics.services.javaastanalysis.application.port.AstResultArtifactWriterPort;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.JavaAstDiagnostic;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.JavaSourceFact;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.RequestMetadata;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.ScanSummary;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.sha256;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JavaAstAnalysisGrpcEndpointTest {
    @TempDir
    Path tempDir;

    private Server server;
    private ManagedChannel channel;
    private JavaAstAnalysisServiceGrpc.JavaAstAnalysisServiceBlockingStub stub;

    @AfterEach
    void stopServer() {
        if (channel != null) {
            channel.shutdownNow();
        }
        if (server != null) {
            server.shutdownNow();
        }
    }

    @Test
    void analyzesSourceSnapshotThroughGrpcBoundary() throws Exception {
        startServer(new FileSystemAstResultArtifactWriter(tempDir));

        var response = stub.analyzeSourceSnapshot(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS, true));
        var complete = stub.analyzeSourceSnapshot(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS, false));
        var parseError = stub.analyzeSourceSnapshot(request(
            AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS,
            false,
            "class Broken { void fail( }"
        ));

        assertEquals("ANALYZED_INCOMPLETE", response.getStatus().getCode());
        assertEquals("snapshot-1", response.getSourceSnapshotId().getValue());
        assertEquals("java-ast-analysis-service", response.getSourceFactArtifact().getProducerService());
        assertEquals(1, response.getSummary().getSourceFactCount());
        assertEquals(List.of("SYMBOL_RESOLUTION_NOT_CONFIGURED"), response.getDiagnosticsList().stream().map(diagnostic -> diagnostic.getCode()).toList());
        assertEquals("ANALYZED", complete.getStatus().getCode());
        assertEquals("JAVA_PARSE_ERROR", parseError.getDiagnostics(0).getCode());
    }

    @Test
    void mapsInvalidWorkerKindAndInternalFailuresToGrpcStatuses() throws Exception {
        startServer(new FileSystemAstResultArtifactWriter(tempDir));

        var invalid = assertThrows(
            StatusRuntimeException.class,
            () -> stub.analyzeSourceSnapshot(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_REPORT, false))
        );
        assertEquals(Status.Code.INVALID_ARGUMENT, invalid.getStatus().getCode());
        assertEquals("Invalid Java AST analysis request", invalid.getStatus().getDescription());

        var noPolicy = assertThrows(
            StatusRuntimeException.class,
            () -> stub.analyzeSourceSnapshot(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS, false).toBuilder()
                .clearScanPolicy()
                .build())
        );
        assertEquals(Status.Code.INVALID_ARGUMENT, noPolicy.getStatus().getCode());

        stopServer();
        startServer((metadata, sourceFacts, diagnostics, summary) -> {
            throw new UncheckedIOException(new java.io.IOException("cannot write /private/source"));
        });
        var failure = assertThrows(
            StatusRuntimeException.class,
            () -> stub.analyzeSourceSnapshot(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS, false))
        );
        assertEquals(Status.Code.FAILED_PRECONDITION, failure.getStatus().getCode());
        assertEquals("Java AST analysis artifact write failed", failure.getStatus().getDescription());

        stopServer();
        startServer((metadata, sourceFacts, diagnostics, summary) -> {
            throw new IllegalStateException("unexpected /private/source failure");
        });
        var unexpected = assertThrows(
            StatusRuntimeException.class,
            () -> stub.analyzeSourceSnapshot(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS, false))
        );
        assertEquals(Status.Code.FAILED_PRECONDITION, unexpected.getStatus().getCode());
        assertEquals("Java AST analysis failed", unexpected.getStatus().getDescription());

        stopServer();
        startServerWithApplicationService(new JavaAstAnalysisApplicationService(command -> {
            throw new JavaAstAnalysisTimeoutException("timeout");
        }, new FileSystemAstResultArtifactWriter(tempDir)));
        var timeout = assertThrows(
            StatusRuntimeException.class,
            () -> stub.analyzeSourceSnapshot(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS, false))
        );
        assertEquals(Status.Code.DEADLINE_EXCEEDED, timeout.getStatus().getCode());
        assertEquals("Java AST analysis timed out", timeout.getStatus().getDescription());
    }

    private void startServer(AstResultArtifactWriterPort writer) throws Exception {
        startServerWithApplicationService(new JavaAstAnalysisApplicationService(new JavaParserSourceScannerAdapter(), writer));
    }

    private void startServerWithApplicationService(JavaAstAnalysisApplicationService applicationService) throws Exception {
        var serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(new JavaAstAnalysisGrpcEndpoint(applicationService))
            .build()
            .start();
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        stub = JavaAstAnalysisServiceGrpc.newBlockingStub(channel);
    }

    private static AnalyzeSourceSnapshotRequest request(AnalysisWorkerKind workerKind, boolean symbolDiagnostics) {
        return request(workerKind, symbolDiagnostics, "package a; class A { void run() {} }");
    }

    private static AnalyzeSourceSnapshotRequest request(
        AnalysisWorkerKind workerKind,
        boolean symbolDiagnostics,
        String content
    ) {
        return AnalyzeSourceSnapshotRequest.newBuilder()
            .setRequestId("request-1")
            .setIdempotencyKey("idempotency-1")
            .setSchemaVersion("java-ast-analysis-v1")
            .setCorrelationId("correlation-1")
            .setWorkerKind(workerKind)
            .setAnalysisRunId(AnalysisRunId.newBuilder().setValue("run-1"))
            .setAnalysisJobId(AnalysisJobId.newBuilder().setValue("job-1"))
            .setSourceSnapshotId(SourceSnapshotId.newBuilder().setValue("snapshot-1"))
            .setWorkerVersion("java-ast-analysis-service-test")
            .setScanPolicy(ScanPolicy.newBuilder()
                .setMaxFiles(10)
                .setMaxSourceBytes(10_000)
                .setTimeoutSeconds(60)
                .setEmitSymbolResolutionDiagnostics(symbolDiagnostics))
            .addSourceRoots(SourceRoot.newBuilder()
                .setRelativePath("src/main/java")
                .setLanguage("java"))
            .addSourceFiles(JavaSourceFile.newBuilder()
                .setSourceRoot("src/main/java")
                .setRelativePath("a/A.java")
                .setContentUtf8(content)
                .setSha256(sha256(content))
                .setSizeBytes(content.getBytes(StandardCharsets.UTF_8).length))
            .putSafeAttributes("tenant", "demo")
            .build();
    }
}
