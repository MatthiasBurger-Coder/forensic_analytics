package de.burger.forensics.analytics.services.javaastanalysis.adapter.in.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisRunId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind;
import de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody;
import de.burger.forensics.analytics.analysisjob.v1.SourceSnapshotId;
import de.burger.forensics.analytics.javaastanalysis.v1.AnalyzeSourceSnapshotRequest;
import de.burger.forensics.analytics.javaastanalysis.v1.DiagnosticSeverity;
import de.burger.forensics.analytics.javaastanalysis.v1.GetSourceFactArtifactBytesRequest;
import de.burger.forensics.analytics.javaastanalysis.v1.JavaAstAnalysisServiceGrpc;
import de.burger.forensics.analytics.javaastanalysis.v1.JavaSourceFile;
import de.burger.forensics.analytics.javaastanalysis.v1.ScanPolicy;
import de.burger.forensics.analytics.javaastanalysis.v1.SourceRoot;
import de.burger.forensics.analytics.services.javaastanalysis.adapter.out.filesystem.FileSystemAstResultArtifactWriter;
import de.burger.forensics.analytics.services.javaastanalysis.adapter.out.javaparser.JavaParserSourceScannerAdapter;
import de.burger.forensics.analytics.services.javaastanalysis.application.JavaAstAnalysisApplicationService;
import de.burger.forensics.analytics.services.javaastanalysis.application.JavaAstAnalysisTimeoutException;
import de.burger.forensics.analytics.services.javaastanalysis.application.port.AstResultArtifactReaderPort;
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

        assertEquals("ANALYZED_INCOMPLETE", response.getStatus().getCode());
        assertEquals("snapshot-1", response.getSourceSnapshotId().getValue());
        assertEquals("java-ast-analysis-service", response.getSourceFactArtifact().getProducerService());
        assertEquals("java-ast-analysis-service", response.getSourceFactArtifact().getByteAccess().getOwnerService());
        assertEquals(
            FileSystemAstResultArtifactWriter.BYTE_RETRIEVAL_CONTRACT,
            response.getSourceFactArtifact().getByteAccess().getRetrievalContract()
        );
        assertEquals(response.getSourceFactArtifact().getArtifact().getPath(), response.getSourceFactArtifact().getByteAccess().getRetrievalReference());
        assertEquals(ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_PRODUCER_RETAINED, response.getSourceFactArtifact().getByteAccess().getByteCustody());
        assertEquals(1, response.getSummary().getSourceFactCount());
        assertEquals(List.of("SYMBOL_RESOLUTION_NOT_CONFIGURED"), response.getDiagnosticsList().stream().map(diagnostic -> diagnostic.getCode()).toList());

        var bytes = stub.getSourceFactArtifactBytes(bytesRequest(response.getSourceFactArtifact()));

        assertEquals("SOURCE_FACT_ARTIFACT_BYTES_RETRIEVED", bytes.getStatus().getCode());
        assertEquals(response.getSourceFactArtifact().getArtifact().getPath(), bytes.getSourceFactArtifact().getArtifact().getPath());
        assertEquals(response.getSourceFactArtifact().getArtifact().getSha256(), bytes.getSha256());
        assertEquals(response.getSourceFactArtifact().getArtifact().getSizeBytes(), bytes.getSizeBytes());
        assertEquals(
            FileSystemAstResultArtifactWriter.BYTE_RETRIEVAL_CONTRACT,
            bytes.getSourceFactArtifact().getByteAccess().getRetrievalContract()
        );
        assertEquals(bytes.getSha256(), sha256(bytes.getContent().toByteArray()));

        var complete = stub.analyzeSourceSnapshot(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS, false));
        var parseError = stub.analyzeSourceSnapshot(request(
            AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS,
            false,
            "class Broken { void fail( }"
        ));

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
        }, new FileSystemAstResultArtifactWriter(tempDir)), request -> {
            throw new IllegalStateException("Source fact artifact is not available");
        });
        var timeout = assertThrows(
            StatusRuntimeException.class,
            () -> stub.analyzeSourceSnapshot(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS, false))
        );
        assertEquals(Status.Code.DEADLINE_EXCEEDED, timeout.getStatus().getCode());
        assertEquals("Java AST analysis timed out", timeout.getStatus().getDescription());
    }

    @Test
    void mapsAllDomainEnumVariantsAtGrpcBoundary() throws Exception {
        startServerWithApplicationService(new JavaAstAnalysisApplicationService(
            command -> new de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.JavaAstScanResult(
                List.of(new JavaSourceFact(
                    "fact-1",
                    "java-method",
                    new de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.SourceLocation(
                        "src/main/java/a/A.java",
                        "a.A",
                        "run",
                        12,
                        1
                    ),
                    "a.A#run()",
                    "AST method a.A#run()",
                    de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.EvidenceKind.STATIC_SOURCE_FACT
                )),
                List.of(
                    JavaAstDiagnostic.info(
                        new de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.SourceSnapshotId("snapshot-1"),
                        "INFO",
                        "info"
                    ),
                    JavaAstDiagnostic.warning(
                        new de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.SourceSnapshotId("snapshot-1"),
                        "WARNING",
                        "warning",
                        "",
                        0,
                        0,
                        false
                    ),
                    new JavaAstDiagnostic(
                        "ERROR",
                        "error",
                        de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.DiagnosticSeverity.ERROR,
                        new de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.SourceSnapshotId("snapshot-1"),
                        "",
                        0,
                        0,
                        false,
                        false
                    )
                ),
                new ScanSummary(1, 1, 0, 0, 1, "fixture", "1")
            ),
            (metadata, sourceFacts, diagnostics, summary) -> domainArtifact(
                de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.ArtifactByteCustody.SCOPED_OBJECT_ACCESS,
                de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisCompleteness.UNKNOWN
            )
        ), request -> {
            throw new IllegalStateException("Source fact artifact is not available");
        });

        var scoped = stub.analyzeSourceSnapshot(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS, false));

        assertEquals(
            ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_SCOPED_OBJECT_ACCESS,
            scoped.getSourceFactArtifact().getByteAccess().getByteCustody()
        );
        assertEquals(
            de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNKNOWN,
            scoped.getSourceFactArtifact().getCompleteness()
        );
        assertEquals(DiagnosticSeverity.DIAGNOSTIC_SEVERITY_INFO, scoped.getDiagnostics(0).getSeverity());
        assertEquals(DiagnosticSeverity.DIAGNOSTIC_SEVERITY_WARNING, scoped.getDiagnostics(1).getSeverity());
        assertEquals(DiagnosticSeverity.DIAGNOSTIC_SEVERITY_ERROR, scoped.getDiagnostics(2).getSeverity());

        stopServer();
        startServerWithApplicationService(new JavaAstAnalysisApplicationService(
            command -> new de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.JavaAstScanResult(
                List.of(),
                List.of(),
                new ScanSummary(1, 1, 0, 0, 0, "fixture", "1")
            ),
            (metadata, sourceFacts, diagnostics, summary) -> domainArtifact(
                de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.ArtifactByteCustody.EXPLICIT_HANDOFF,
                de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisCompleteness.COMPLETE
            )
        ), request -> {
            throw new IllegalStateException("Source fact artifact is not available");
        });

        var handoff = stub.analyzeSourceSnapshot(request(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_AST_ANALYSIS, false));

        assertEquals(
            ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_EXPLICIT_HANDOFF,
            handoff.getSourceFactArtifact().getByteAccess().getByteCustody()
        );
    }

    private void startServer(AstResultArtifactWriterPort writer) throws Exception {
        var reader = writer instanceof AstResultArtifactReaderPort artifactReader
            ? artifactReader
            : (AstResultArtifactReaderPort) request -> {
                throw new IllegalStateException("Source fact artifact is not available");
            };
        startServerWithApplicationService(new JavaAstAnalysisApplicationService(new JavaParserSourceScannerAdapter(), writer), reader);
    }

    private void startServerWithApplicationService(
        JavaAstAnalysisApplicationService applicationService,
        AstResultArtifactReaderPort artifactReader
    ) throws Exception {
        var serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(new JavaAstAnalysisGrpcEndpoint(applicationService, artifactReader))
            .build()
            .start();
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        stub = JavaAstAnalysisServiceGrpc.newBlockingStub(channel);
    }

    private static GetSourceFactArtifactBytesRequest bytesRequest(
        de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactReference artifact
    ) {
        return GetSourceFactArtifactBytesRequest.newBuilder()
            .setRequestId("request-bytes-1")
            .setCorrelationId("correlation-1")
            .setAnalysisRunId(AnalysisRunId.newBuilder().setValue("run-1"))
            .setAnalysisJobId(AnalysisJobId.newBuilder().setValue("job-1"))
            .setSourceSnapshotId(SourceSnapshotId.newBuilder().setValue("snapshot-1"))
            .setRetrievalReference(artifact.getByteAccess().getRetrievalReference())
            .setExpectedSha256(artifact.getArtifact().getSha256())
            .setExpectedSizeBytes(artifact.getArtifact().getSizeBytes())
            .setMaxBytes(10_000)
            .setSchemaVersion(artifact.getSchemaVersion())
            .putSafeAttributes("tenant", "demo")
            .build();
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

    private static de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisArtifactReference domainArtifact(
        de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.ArtifactByteCustody byteCustody,
        de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisCompleteness completeness
    ) {
        return new de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisArtifactReference(
            new de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.ArtifactReference(
                "java-ast/source-facts.json",
                "application/json",
                "a".repeat(64),
                42
            ),
            de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisArtifactCategory.STATIC,
            "java-ast-analysis-service",
            "schema-v1",
            completeness,
            new de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.ArtifactByteAccess(
                "java-ast-analysis-service",
                FileSystemAstResultArtifactWriter.BYTE_RETRIEVAL_CONTRACT,
                "java-ast/source-facts.json",
                byteCustody
            )
        );
    }
}
