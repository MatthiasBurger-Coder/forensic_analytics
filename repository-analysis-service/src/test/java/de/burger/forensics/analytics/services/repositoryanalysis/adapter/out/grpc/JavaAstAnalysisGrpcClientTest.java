package de.burger.forensics.analytics.services.repositoryanalysis.adapter.out.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactReference;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisRunId;
import de.burger.forensics.analytics.analysisjob.v1.ArtifactByteAccess;
import de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody;
import de.burger.forensics.analytics.analysisjob.v1.ArtifactReference;
import de.burger.forensics.analytics.analysisjob.v1.SourceSnapshotId;
import de.burger.forensics.analytics.javaastanalysis.v1.AnalyzeSourceSnapshotRequest;
import de.burger.forensics.analytics.javaastanalysis.v1.AnalyzeSourceSnapshotResponse;
import de.burger.forensics.analytics.javaastanalysis.v1.DiagnosticSeverity;
import de.burger.forensics.analytics.javaastanalysis.v1.JavaAstAnalysisServiceGrpc;
import de.burger.forensics.analytics.javaastanalysis.v1.JavaAstDiagnostic;
import de.burger.forensics.analytics.javaastanalysis.v1.OperationStatus;
import de.burger.forensics.analytics.javaastanalysis.v1.ScanSummary;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.JavaAstAnalysisHandoffCommand;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceRoot;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceSnapshotCompleteness;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceSnapshotHandoffPolicy;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceSnapshotSourceFile;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.sha256Hex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaAstAnalysisGrpcClientTest {
    private Server server;
    private ManagedChannel channel;

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
    void mapsBoundedInlineSourceSnapshotRequestAndResponse() throws Exception {
        var service = new CapturingJavaAstService();
        var client = startClient(service);

        var result = client.analyze(command());

        assertEquals("request-ast", service.request.getRequestId());
        assertEquals("idempotency-ast", service.request.getIdempotencyKey());
        assertEquals("correlation-1", service.request.getCorrelationId());
        assertEquals("run-1", service.request.getAnalysisRunId().getValue());
        assertEquals("job-ast-1", service.request.getAnalysisJobId().getValue());
        assertEquals("snapshot-1", service.request.getSourceSnapshotId().getValue());
        assertEquals("src/main/java", service.request.getSourceRoots(0).getRelativePath());
        assertEquals("a/A.java", service.request.getSourceFiles(0).getRelativePath());
        assertEquals(sha256Hex("package a; class A {}"), service.request.getSourceFiles(0).getSha256());
        assertTrue(service.request.getScanPolicy().getEmitSymbolResolutionDiagnostics());
        assertEquals(SourceSnapshotCompleteness.INCOMPLETE, result.completeness());
        assertEquals("java-ast/snapshot-1-source-facts.json", result.sourceFactArtifact().reference());
        assertEquals("java-ast-analysis-service", result.sourceFactArtifactProducerService());
        assertEquals("schema-v1", result.sourceFactArtifactSchemaVersion());
        assertEquals("java-ast-analysis-service", result.sourceFactArtifactByteAccess().ownerService());
        assertEquals(
            "java-ast-analysis.v1.JavaAstAnalysisService.GetSourceFactArtifactBytes",
            result.sourceFactArtifactByteAccess().retrievalContract()
        );
        assertEquals("java-ast/snapshot-1-source-facts.json", result.sourceFactArtifactByteAccess().retrievalReference());
        assertEquals("SYMBOL_RESOLUTION_NOT_CONFIGURED", result.diagnostics().getFirst().code());
    }

    @Test
    void mapsGrpcFailureWithoutLeakingInternalDescription() throws Exception {
        var client = startClient(new FailingJavaAstService());

        var failure = assertThrows(IllegalStateException.class, () -> client.analyze(command()));

        assertEquals("Java AST analysis service failed with status FAILED_PRECONDITION", failure.getMessage());
    }

    @Test
    void mapsCompleteAndDiagnosticSeverityBranches() throws Exception {
        var client = startClient(new VariantJavaAstService(
            AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE,
            List.of(
                DiagnosticSeverity.DIAGNOSTIC_SEVERITY_ERROR,
                DiagnosticSeverity.DIAGNOSTIC_SEVERITY_INFO
            )
        ));

        var result = client.analyze(command());

        assertEquals(SourceSnapshotCompleteness.COMPLETE, result.completeness());
        assertEquals(
            de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.DiagnosticSeverity.ERROR,
            result.diagnostics().get(0).severity()
        );
        assertEquals(
            de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.DiagnosticSeverity.INFO,
            result.diagnostics().get(1).severity()
        );
    }

    @Test
    void mapsUnknownCompletenessForUnspecifiedWorkerResponse() throws Exception {
        var client = startClient(new VariantJavaAstService(
            AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNSPECIFIED,
            List.of(DiagnosticSeverity.DIAGNOSTIC_SEVERITY_WARNING)
        ));

        var result = client.analyze(command());

        assertEquals(SourceSnapshotCompleteness.UNKNOWN, result.completeness());
    }

    @Test
    void mapsScopedAndExplicitJavaAstByteCustodyBranches() throws Exception {
        var scopedClient = startClient(new VariantJavaAstService(
            AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE,
            List.of(DiagnosticSeverity.DIAGNOSTIC_SEVERITY_WARNING),
            ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_SCOPED_OBJECT_ACCESS
        ));

        var scoped = scopedClient.analyze(command());

        assertEquals(
            de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.ArtifactByteCustody.SCOPED_OBJECT_ACCESS,
            scoped.sourceFactArtifactByteAccess().byteCustody()
        );

        stopServer();
        var explicitClient = startClient(new VariantJavaAstService(
            AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE,
            List.of(DiagnosticSeverity.DIAGNOSTIC_SEVERITY_WARNING),
            ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_EXPLICIT_HANDOFF
        ));

        var explicit = explicitClient.analyze(command());

        assertEquals(
            de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.ArtifactByteCustody.EXPLICIT_HANDOFF,
            explicit.sourceFactArtifactByteAccess().byteCustody()
        );
    }

    @Test
    void rejectsUnverifiedJavaAstSourceFactArtifactMetadata() throws Exception {
        var nonStaticClient = startClient(new InvalidArtifactJavaAstService(
            AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_RUNTIME,
            "java-ast-analysis-service",
            "java-ast-analysis-service",
            "java-ast-analysis.v1.JavaAstAnalysisService.GetSourceFactArtifactBytes"
        ));

        var nonStaticFailure = assertThrows(IllegalArgumentException.class, () -> nonStaticClient.analyze(command()));
        assertEquals("Java AST source fact artifact must be static", nonStaticFailure.getMessage());

        stopServer();
        var wrongProducerClient = startClient(new InvalidArtifactJavaAstService(
            AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC,
            "other-service",
            "java-ast-analysis-service",
            "java-ast-analysis.v1.JavaAstAnalysisService.GetSourceFactArtifactBytes"
        ));

        var wrongProducerFailure = assertThrows(IllegalArgumentException.class, () -> wrongProducerClient.analyze(command()));
        assertEquals("Java AST source fact artifact must be owned by Java AST Analysis", wrongProducerFailure.getMessage());

        stopServer();
        var wrongOwnerClient = startClient(new InvalidArtifactJavaAstService(
            AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC,
            "java-ast-analysis-service",
            "other-service",
            "java-ast-analysis.v1.JavaAstAnalysisService.GetSourceFactArtifactBytes"
        ));

        var wrongOwnerFailure = assertThrows(IllegalArgumentException.class, () -> wrongOwnerClient.analyze(command()));
        assertEquals("Java AST source fact artifact must be owned by Java AST Analysis", wrongOwnerFailure.getMessage());

        stopServer();
        var wrongContractClient = startClient(new InvalidArtifactJavaAstService(
            AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC,
            "java-ast-analysis-service",
            "java-ast-analysis-service",
            "other.v1.Bytes"
        ));

        var wrongContractFailure = assertThrows(IllegalArgumentException.class, () -> wrongContractClient.analyze(command()));
        assertEquals("Java AST source fact artifact retrieval contract is not verified", wrongContractFailure.getMessage());
    }

    @Test
    void rejectsUnspecifiedJavaAstByteCustody() throws Exception {
        var client = startClient(new UnspecifiedByteCustodyJavaAstService());

        var failure = assertThrows(IllegalArgumentException.class, () -> client.analyze(command()));

        assertEquals("Java AST source fact artifact byte custody must be specified", failure.getMessage());
    }

    @Test
    void closesManagedChannelClient() {
        var client = new JavaAstAnalysisGrpcClient("127.0.0.1", 1, 1);

        client.close();
    }

    private JavaAstAnalysisGrpcClient startClient(JavaAstAnalysisServiceGrpc.JavaAstAnalysisServiceImplBase service) throws Exception {
        var serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(service)
            .build()
            .start();
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        return new JavaAstAnalysisGrpcClient(JavaAstAnalysisServiceGrpc.newBlockingStub(channel), 5);
    }

    private static JavaAstAnalysisHandoffCommand command() {
        return new JavaAstAnalysisHandoffCommand(
            "request-ast",
            "idempotency-ast",
            "schema-v1",
            "correlation-1",
            new de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.AnalysisRunId("run-1"),
            new de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.AnalysisJobId("job-ast-1"),
            new de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceSnapshotId("snapshot-1"),
            "java-ast-analysis-service-v1",
            new SourceSnapshotHandoffPolicy(10, 10_000, 30),
            List.of(new SourceRoot("src/main/java", "java")),
            List.of(new SourceSnapshotSourceFile(
                "src/main/java",
                "a/A.java",
                "package a; class A {}",
                sha256Hex("package a; class A {}"),
                "package a; class A {}".getBytes(java.nio.charset.StandardCharsets.UTF_8).length
            )),
            Map.of("tenant", "demo")
        );
    }

    private static final class CapturingJavaAstService extends JavaAstAnalysisServiceGrpc.JavaAstAnalysisServiceImplBase {
        private AnalyzeSourceSnapshotRequest request;

        @Override
        public void analyzeSourceSnapshot(
            AnalyzeSourceSnapshotRequest request,
            StreamObserver<AnalyzeSourceSnapshotResponse> responseObserver
        ) {
            this.request = request;
            responseObserver.onNext(AnalyzeSourceSnapshotResponse.newBuilder()
                .setStatus(OperationStatus.newBuilder()
                    .setCode("ANALYZED_INCOMPLETE")
                    .setMessage("Java AST analysis completed")
                    .setCorrelationId(request.getCorrelationId()))
                .setAnalysisRunId(AnalysisRunId.newBuilder().setValue(request.getAnalysisRunId().getValue()))
                .setAnalysisJobId(AnalysisJobId.newBuilder().setValue(request.getAnalysisJobId().getValue()))
                .setSourceSnapshotId(SourceSnapshotId.newBuilder().setValue(request.getSourceSnapshotId().getValue()))
                .setCompleteness(AnalysisCompleteness.ANALYSIS_COMPLETENESS_INCOMPLETE)
                .setSourceFactArtifact(AnalysisArtifactReference.newBuilder()
                    .setArtifact(ArtifactReference.newBuilder()
                        .setPath("java-ast/snapshot-1-source-facts.json")
                        .setType("application/vnd.forensic-analytics.java-ast-source-facts.v1+json")
                        .setSha256("c".repeat(64))
                        .setSizeBytes(100))
                    .setCategory(AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC)
                    .setProducerService("java-ast-analysis-service")
                    .setSchemaVersion("schema-v1")
                    .setCompleteness(AnalysisCompleteness.ANALYSIS_COMPLETENESS_INCOMPLETE)
                    .setByteAccess(byteAccess()))
                .setSummary(ScanSummary.newBuilder()
                    .setReceivedFileCount(1)
                    .setParsedFileCount(1)
                    .setSourceFactCount(1)
                    .setParser("JavaParser")
                    .setParserVersion("3.27.1"))
                .addDiagnostics(JavaAstDiagnostic.newBuilder()
                    .setCode("SYMBOL_RESOLUTION_NOT_CONFIGURED")
                    .setMessage("unresolved symbols remain explicit")
                    .setSeverity(DiagnosticSeverity.DIAGNOSTIC_SEVERITY_WARNING)
                    .setSourceSnapshotId("snapshot-1")
                    .setAffectsCompleteness(true))
                .putSafeAttributes("tenant", "demo")
                .build());
            responseObserver.onCompleted();
        }
    }

    private static final class VariantJavaAstService extends JavaAstAnalysisServiceGrpc.JavaAstAnalysisServiceImplBase {
        private final AnalysisCompleteness completeness;
        private final List<DiagnosticSeverity> severities;
        private final ArtifactByteCustody custody;

        private VariantJavaAstService(AnalysisCompleteness completeness, List<DiagnosticSeverity> severities) {
            this(completeness, severities, ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_PRODUCER_RETAINED);
        }

        private VariantJavaAstService(
            AnalysisCompleteness completeness,
            List<DiagnosticSeverity> severities,
            ArtifactByteCustody custody
        ) {
            this.completeness = completeness;
            this.severities = severities;
            this.custody = custody;
        }

        @Override
        public void analyzeSourceSnapshot(
            AnalyzeSourceSnapshotRequest request,
            StreamObserver<AnalyzeSourceSnapshotResponse> responseObserver
        ) {
            var builder = AnalyzeSourceSnapshotResponse.newBuilder()
                .setStatus(OperationStatus.newBuilder()
                    .setCode("ANALYZED")
                    .setMessage("Java AST analysis completed")
                    .setCorrelationId(request.getCorrelationId()))
                .setAnalysisRunId(AnalysisRunId.newBuilder().setValue(request.getAnalysisRunId().getValue()))
                .setAnalysisJobId(AnalysisJobId.newBuilder().setValue(request.getAnalysisJobId().getValue()))
                .setSourceSnapshotId(SourceSnapshotId.newBuilder().setValue(request.getSourceSnapshotId().getValue()))
                .setCompleteness(completeness)
                .setSourceFactArtifact(AnalysisArtifactReference.newBuilder()
                    .setArtifact(ArtifactReference.newBuilder()
                        .setPath("java-ast/snapshot-1-source-facts.json")
                        .setType("application/vnd.forensic-analytics.java-ast-source-facts.v1+json")
                        .setSha256("d".repeat(64))
                        .setSizeBytes(100))
                    .setCategory(AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC)
                    .setProducerService("java-ast-analysis-service")
                    .setSchemaVersion("schema-v1")
                    .setCompleteness(completeness)
                    .setByteAccess(byteAccess(custody)))
                .setSummary(ScanSummary.newBuilder()
                    .setReceivedFileCount(1)
                    .setParsedFileCount(1)
                    .setSourceFactCount(1)
                    .setParser("JavaParser")
                    .setParserVersion("3.27.1"));
            severities.forEach(severity -> builder.addDiagnostics(JavaAstDiagnostic.newBuilder()
                .setCode("DIAGNOSTIC_" + severity.getNumber())
                .setMessage("diagnostic severity branch")
                .setSeverity(severity)
                .setSourceSnapshotId("snapshot-1")
                .setAffectsCompleteness(true)));
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    private static final class InvalidArtifactJavaAstService extends JavaAstAnalysisServiceGrpc.JavaAstAnalysisServiceImplBase {
        private final AnalysisArtifactCategory category;
        private final String producerService;
        private final String byteOwnerService;
        private final String retrievalContract;

        private InvalidArtifactJavaAstService(
            AnalysisArtifactCategory category,
            String producerService,
            String byteOwnerService,
            String retrievalContract
        ) {
            this.category = category;
            this.producerService = producerService;
            this.byteOwnerService = byteOwnerService;
            this.retrievalContract = retrievalContract;
        }

        @Override
        public void analyzeSourceSnapshot(
            AnalyzeSourceSnapshotRequest request,
            StreamObserver<AnalyzeSourceSnapshotResponse> responseObserver
        ) {
            responseObserver.onNext(AnalyzeSourceSnapshotResponse.newBuilder()
                .setStatus(OperationStatus.newBuilder()
                    .setCode("ANALYZED")
                    .setMessage("Java AST analysis completed")
                    .setCorrelationId(request.getCorrelationId()))
                .setAnalysisRunId(AnalysisRunId.newBuilder().setValue(request.getAnalysisRunId().getValue()))
                .setAnalysisJobId(AnalysisJobId.newBuilder().setValue(request.getAnalysisJobId().getValue()))
                .setSourceSnapshotId(SourceSnapshotId.newBuilder().setValue(request.getSourceSnapshotId().getValue()))
                .setCompleteness(AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE)
                .setSourceFactArtifact(AnalysisArtifactReference.newBuilder()
                    .setArtifact(ArtifactReference.newBuilder()
                        .setPath("java-ast/snapshot-1-source-facts.json")
                        .setType("application/vnd.forensic-analytics.java-ast-source-facts.v1+json")
                        .setSha256("f".repeat(64))
                        .setSizeBytes(100))
                    .setCategory(category)
                    .setProducerService(producerService)
                    .setSchemaVersion("schema-v1")
                    .setCompleteness(AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE)
                    .setByteAccess(ArtifactByteAccess.newBuilder()
                        .setOwnerService(byteOwnerService)
                        .setRetrievalContract(retrievalContract)
                        .setRetrievalReference("java-ast/snapshot-1-source-facts.json")
                        .setByteCustody(ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_PRODUCER_RETAINED)))
                .setSummary(ScanSummary.newBuilder()
                    .setReceivedFileCount(1)
                    .setParsedFileCount(1)
                    .setSourceFactCount(1)
                    .setParser("JavaParser")
                    .setParserVersion("3.27.1"))
                .build());
            responseObserver.onCompleted();
        }
    }

    private static final class FailingJavaAstService extends JavaAstAnalysisServiceGrpc.JavaAstAnalysisServiceImplBase {
        @Override
        public void analyzeSourceSnapshot(
            AnalyzeSourceSnapshotRequest request,
            StreamObserver<AnalyzeSourceSnapshotResponse> responseObserver
        ) {
            responseObserver.onError(new StatusRuntimeException(
                Status.FAILED_PRECONDITION.withDescription("failed at /private/workspace")
            ));
        }
    }

    private static final class UnspecifiedByteCustodyJavaAstService extends JavaAstAnalysisServiceGrpc.JavaAstAnalysisServiceImplBase {
        @Override
        public void analyzeSourceSnapshot(
            AnalyzeSourceSnapshotRequest request,
            StreamObserver<AnalyzeSourceSnapshotResponse> responseObserver
        ) {
            responseObserver.onNext(AnalyzeSourceSnapshotResponse.newBuilder()
                .setStatus(OperationStatus.newBuilder()
                    .setCode("ANALYZED")
                    .setMessage("Java AST analysis completed")
                    .setCorrelationId(request.getCorrelationId()))
                .setAnalysisRunId(AnalysisRunId.newBuilder().setValue(request.getAnalysisRunId().getValue()))
                .setAnalysisJobId(AnalysisJobId.newBuilder().setValue(request.getAnalysisJobId().getValue()))
                .setSourceSnapshotId(SourceSnapshotId.newBuilder().setValue(request.getSourceSnapshotId().getValue()))
                .setCompleteness(AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE)
                .setSourceFactArtifact(AnalysisArtifactReference.newBuilder()
                    .setArtifact(ArtifactReference.newBuilder()
                        .setPath("java-ast/snapshot-1-source-facts.json")
                        .setType("application/vnd.forensic-analytics.java-ast-source-facts.v1+json")
                        .setSha256("e".repeat(64))
                        .setSizeBytes(100))
                    .setCategory(AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC)
                    .setProducerService("java-ast-analysis-service")
                    .setSchemaVersion("schema-v1")
                    .setCompleteness(AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE)
                    .setByteAccess(ArtifactByteAccess.newBuilder()
                        .setOwnerService("java-ast-analysis-service")
                        .setRetrievalContract("java-ast-analysis.v1.JavaAstAnalysisService.GetSourceFactArtifactBytes")
                        .setRetrievalReference("java-ast/snapshot-1-source-facts.json")))
                .setSummary(ScanSummary.newBuilder()
                    .setReceivedFileCount(1)
                    .setParsedFileCount(1)
                    .setSourceFactCount(1)
                    .setParser("JavaParser")
                    .setParserVersion("3.27.1"))
                .build());
            responseObserver.onCompleted();
        }
    }

    private static ArtifactByteAccess byteAccess() {
        return byteAccess(ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_PRODUCER_RETAINED);
    }

    private static ArtifactByteAccess byteAccess(ArtifactByteCustody custody) {
        return ArtifactByteAccess.newBuilder()
            .setOwnerService("java-ast-analysis-service")
            .setRetrievalContract("java-ast-analysis.v1.JavaAstAnalysisService.GetSourceFactArtifactBytes")
            .setRetrievalReference("java-ast/snapshot-1-source-facts.json")
            .setByteCustody(custody)
            .build();
    }
}
