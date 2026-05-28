package de.burger.forensics.analytics.services.analysisstore.adapter.out.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind;
import de.burger.forensics.analytics.btmgeneration.v1.BtmGenerationServiceGrpc;
import de.burger.forensics.analytics.btmgeneration.v1.GenerateBtmRulesRequest;
import de.burger.forensics.analytics.btmgeneration.v1.GenerateBtmRulesResponse;
import de.burger.forensics.analytics.joerncpganalysis.v1.AnalyzeJoernCpgRequest;
import de.burger.forensics.analytics.joerncpganalysis.v1.AnalyzeJoernCpgResponse;
import de.burger.forensics.analytics.joerncpganalysis.v1.JoernCpgAnalysisServiceGrpc;
import de.burger.forensics.analytics.joerncpganalysis.v1.MaterializeJoernWorkspaceRequest;
import de.burger.forensics.analytics.joerncpganalysis.v1.MaterializeJoernWorkspaceResponse;
import de.burger.forensics.analytics.repositoryanalysis.v1.AnalyzeSourceSnapshotWithJavaAstRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.JavaAstHandoffResponse;
import de.burger.forensics.analytics.repositoryanalysis.v1.PrepareRepositoryRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.PrepareRepositoryResponse;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryAnalysisServiceGrpc;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryPreparation;
import de.burger.forensics.analytics.services.analysisstore.application.port.RepositoryAnalysisWorkerPort;
import de.burger.forensics.analytics.services.analysisstore.application.port.WorkerOwnerApiUnavailableException;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactCategory;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJobId;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisRunId;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactByteAccess;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactByteCustody;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactReference;
import de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain;
import de.burger.forensics.analytics.services.analysisstore.domain.RepositoryToBtmOrchestrationDomain;
import de.burger.forensics.analytics.services.analysisstore.domain.SourceSnapshotId;
import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisStoreWorkerGrpcClientTest {
    private Server server;
    private ManagedChannel channel;

    @AfterEach
    void stopGrpc() {
        if (channel != null) {
            channel.shutdownNow();
            channel = null;
        }
        if (server != null) {
            server.shutdownNow();
            server = null;
        }
    }

    @Test
    void mapsRepositoryAnalysisPrepareAndJavaAstHandoffThroughOwnerApi() throws Exception {
        var service = new CapturingRepositoryAnalysisService();
        var client = repositoryClient(service);

        var result = client.prepareAndAnalyzeJavaAst(command(), new AnalysisJobId("job-java-ast"));

        assertEquals("request-1-repository-prepare", service.prepareRequest.getRequestId());
        assertEquals("https://example.org/repository.git", service.prepareRequest.getRepository().getRemoteUrl());
        assertTrue(service.prepareRequest.getRevision().getBranchRequired());
        assertEquals("main", service.prepareRequest.getRevision().getBranch());
        assertEquals(104_857_600, service.handoffRequest.getHandoffPolicy().getMaxSourceBytes());
        assertEquals("job-java-ast", service.handoffRequest.getAnalysisJobId());
        assertEquals(new SourceSnapshotId("snapshot-1"), result.sourceSnapshotId());
        assertEquals(1, result.sourceRoots().size());
        assertTrue(result.hasJoernReadyPackages());
        assertEquals(RepositoryAnalysisWorkerPort.BuildOutputProducer.ARTIFACTORY, result.buildOutputPackage()
            .buildOutputResolution()
            .selectedProducer());
        assertEquals("source-facts.json", result.sourceFactArtifact().artifact().path());
        assertEquals("repository-analysis", result.attributes().get("owner"));
    }

    @Test
    void mapsRepositoryAnalysisStatusFailuresToOwnerUnavailable() throws Exception {
        var client = repositoryClient(new FailingRepositoryAnalysisService());

        var failure = assertThrows(
            WorkerOwnerApiUnavailableException.class,
            () -> client.prepareAndAnalyzeJavaAst(command(), new AnalysisJobId("job-java-ast"))
        );

        assertEquals("Repository Analysis owner API is unavailable with status UNAVAILABLE", failure.getMessage());
    }

    @Test
    void mapsJoernMaterializationAndAnalysisThroughOwnerApi() throws Exception {
        var service = new CapturingJoernCpgAnalysisService();
        var client = joernClient(service);

        var result = client.analyze(command(), new AnalysisJobId("job-joern"), repositoryAnalysisResult());

        assertEquals("request-1-joern-materialize", service.materializeRequest.getRequestId());
        assertEquals("src/main/java", service.materializeRequest.getSourceRoots(0).getRelativePath());
        assertEquals("source-package.zip", service.materializeRequest.getSourcePackage().getPackageArtifact().getReference());
        assertEquals("request-1-joern-analyze", service.analyzeRequest.getRequestId());
        assertEquals(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_JOERN_ANALYSIS, service.analyzeRequest.getWorkerKind());
        assertEquals("ghcr.io/joernio/joern@sha256:" + "a".repeat(64), service.analyzeRequest.getPolicy().getJoernImageReference());
        assertEquals("analysis-store-default-v1", service.analyzeRequest.getPolicy().getQueryBundleVersion());
        assertEquals(AnalysisCompleteness.INCOMPLETE, result.completeness());
        assertEquals(1, result.semanticArtifacts().size());
        assertEquals("joern-cpg/cpg.bin.zip", result.semanticArtifacts().getFirst().artifact().path());
        assertEquals("joern", result.attributes().get("owner"));
    }

    @Test
    void mapsJoernStatusFailuresToOwnerUnavailable() throws Exception {
        var client = joernClient(new FailingJoernCpgAnalysisService());

        var failure = assertThrows(
            WorkerOwnerApiUnavailableException.class,
            () -> client.analyze(command(), new AnalysisJobId("job-joern"), repositoryAnalysisResult())
        );

        assertEquals("Joern CPG owner API is unavailable with status UNAVAILABLE", failure.getMessage());
    }

    @Test
    void mapsBtmGenerationThroughOwnerApi() throws Exception {
        var service = new CapturingBtmGenerationService();
        var client = btmClient(service);

        var result = client.generate(
            command(),
            new AnalysisJobId("job-btm"),
            new SourceSnapshotId("snapshot-1"),
            List.of(sourceFactArtifact()),
            List.of(semanticArtifact()),
            AnalysisCompleteness.INCOMPLETE,
            targetSelection(),
            List.of(target())
        );

        assertEquals("request-1-btm-generate", service.request.getRequestId());
        assertEquals(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_BTM_GENERATION, service.request.getWorkerKind());
        assertEquals(1, service.request.getFacts().getSourceFactArtifactsCount());
        assertEquals(1, service.request.getFacts().getSemanticArtifactsCount());
        assertEquals(1, service.request.getFacts().getTargetsCount());
        assertEquals(4096, service.request.getPolicy().getMaxArtifactBytes());
        assertEquals(AnalysisCompleteness.COMPLETE, result.completeness());
        assertEquals(2, result.generatedArtifacts().size());
        assertEquals("btm/snapshot-1-rules.btm", result.generatedArtifacts().getFirst().artifact().path());
        assertEquals("btm", result.attributes().get("owner"));
    }

    @Test
    void mapsBtmGenerationStatusFailuresToOwnerUnavailable() throws Exception {
        var client = btmClient(new FailingBtmGenerationService());

        var failure = assertThrows(
            WorkerOwnerApiUnavailableException.class,
            () -> client.generate(
                command(),
                new AnalysisJobId("job-btm"),
                new SourceSnapshotId("snapshot-1"),
                List.of(sourceFactArtifact()),
                List.of(),
                AnalysisCompleteness.COMPLETE,
                targetSelection(),
                List.of(target())
            )
        );

        assertEquals("BTM Generation owner API is unavailable with status UNAVAILABLE", failure.getMessage());
    }

    @Test
    void validatesClientConstructors() {
        assertThrows(IllegalArgumentException.class, () -> new RepositoryAnalysisGrpcClient("example.com", 9092, 1));
        assertThrows(IllegalArgumentException.class, () -> new JavaAstSourceFactArtifactClient("example.com", 9093, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new JoernCpgAnalysisGrpcClient(
            "example.com",
            9094,
            1,
            "ghcr.io/joernio/joern@sha256:" + "a".repeat(64),
            "bundle"
        ));
        assertThrows(IllegalArgumentException.class, () -> new BtmGenerationGrpcClient("example.com", 9095, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryAnalysisGrpcClient(
            RepositoryAnalysisServiceGrpc.newBlockingStub(InProcessChannelBuilder.forName("missing").directExecutor().build()),
            0
        ));
        assertThrows(IllegalArgumentException.class, () -> new JoernCpgAnalysisGrpcClient(
            JoernCpgAnalysisServiceGrpc.newBlockingStub(InProcessChannelBuilder.forName("missing").directExecutor().build()),
            1,
            "",
            "bundle"
        ));
        assertThrows(IllegalArgumentException.class, () -> new BtmGenerationGrpcClient(
            BtmGenerationServiceGrpc.newBlockingStub(InProcessChannelBuilder.forName("missing").directExecutor().build()),
            1,
            0
        ));
    }

    @Test
    void coversRepositoryAnalysisGrpcContractEnumMappings() throws Exception {
        for (var completeness : de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.values()) {
            callStatic(
                RepositoryAnalysisGrpcClient.class,
                "completeness",
                new Class<?>[] { de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.class },
                completeness
            );
        }
        for (var completeness : de.burger.forensics.analytics.repositoryanalysis.v1.SourceSnapshotCompleteness.values()) {
            callStatic(
                RepositoryAnalysisGrpcClient.class,
                "completeness",
                new Class<?>[] { de.burger.forensics.analytics.repositoryanalysis.v1.SourceSnapshotCompleteness.class },
                completeness
            );
        }
        for (var severity : de.burger.forensics.analytics.repositoryanalysis.v1.DiagnosticSeverity.values()) {
            callStatic(
                RepositoryAnalysisGrpcClient.class,
                "severity",
                new Class<?>[] { de.burger.forensics.analytics.repositoryanalysis.v1.DiagnosticSeverity.class },
                severity
            );
        }
        for (var availability : de.burger.forensics.analytics.repositoryanalysis.v1.PackageAvailability.values()) {
            callStatic(
                RepositoryAnalysisGrpcClient.class,
                "availability",
                new Class<?>[] { de.burger.forensics.analytics.repositoryanalysis.v1.PackageAvailability.class },
                availability
            );
        }
        for (var producer : de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducer.values()) {
            callStatic(
                RepositoryAnalysisGrpcClient.class,
                "producer",
                new Class<?>[] { de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducer.class },
                producer
            );
        }
        for (var status : de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducerStatus.values()) {
            callStatic(
                RepositoryAnalysisGrpcClient.class,
                "producerStatus",
                new Class<?>[] { de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducerStatus.class },
                status
            );
        }
    }

    @Test
    void coversJoernGrpcContractEnumMappings() throws Exception {
        for (var availability : RepositoryAnalysisWorkerPort.PackageAvailability.values()) {
            callStatic(
                JoernCpgAnalysisGrpcClient.class,
                "availability",
                new Class<?>[] { RepositoryAnalysisWorkerPort.PackageAvailability.class },
                availability
            );
        }
        for (var completeness : AnalysisCompleteness.values()) {
            callStatic(
                JoernCpgAnalysisGrpcClient.class,
                "sourceCompleteness",
                new Class<?>[] { AnalysisCompleteness.class },
                completeness
            );
        }
        for (var completeness : de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.values()) {
            callStatic(
                JoernCpgAnalysisGrpcClient.class,
                "completeness",
                new Class<?>[] { de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.class },
                completeness
            );
        }
        for (var severity : de.burger.forensics.analytics.joerncpganalysis.v1.DiagnosticSeverity.values()) {
            callStatic(
                JoernCpgAnalysisGrpcClient.class,
                "severity",
                new Class<?>[] { de.burger.forensics.analytics.joerncpganalysis.v1.DiagnosticSeverity.class },
                severity
            );
        }
        for (var custody : ArtifactByteCustody.values()) {
            callStatic(
                JoernCpgAnalysisGrpcClient.class,
                "byteCustody",
                new Class<?>[] { ArtifactByteCustody.class },
                custody
            );
        }
        for (var producer : RepositoryAnalysisWorkerPort.BuildOutputProducer.values()) {
            callStatic(
                JoernCpgAnalysisGrpcClient.class,
                "buildOutputProducer",
                new Class<?>[] { RepositoryAnalysisWorkerPort.BuildOutputProducer.class },
                producer
            );
        }
        for (var status : RepositoryAnalysisWorkerPort.BuildOutputProducerStatus.values()) {
            callStatic(
                JoernCpgAnalysisGrpcClient.class,
                "buildOutputProducerStatus",
                new Class<?>[] { RepositoryAnalysisWorkerPort.BuildOutputProducerStatus.class },
                status
            );
        }
    }

    @Test
    void coversBtmGenerationGrpcContractEnumMappings() throws Exception {
        for (var completeness : AnalysisCompleteness.values()) {
            callStatic(
                BtmGenerationGrpcClient.class,
                "toProto",
                new Class<?>[] { AnalysisCompleteness.class },
                completeness
            );
        }
        for (var completeness : de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.values()) {
            callStatic(
                BtmGenerationGrpcClient.class,
                "completeness",
                new Class<?>[] { de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.class },
                completeness
            );
        }
        for (var category : AnalysisArtifactCategory.values()) {
            callStatic(
                BtmGenerationGrpcClient.class,
                "category",
                new Class<?>[] { AnalysisArtifactCategory.class },
                category
            );
        }
        for (var category : de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory.values()) {
            if (category == de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_UNSPECIFIED
                || category == de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory.UNRECOGNIZED) {
                assertThrows(InvocationTargetException.class, () -> callStatic(
                    BtmGenerationGrpcClient.class,
                    "category",
                    new Class<?>[] { de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory.class },
                    category
                ));
            } else {
                callStatic(
                    BtmGenerationGrpcClient.class,
                    "category",
                    new Class<?>[] { de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory.class },
                    category
                );
            }
        }
        for (var custody : ArtifactByteCustody.values()) {
            callStatic(
                BtmGenerationGrpcClient.class,
                "byteCustody",
                new Class<?>[] { ArtifactByteCustody.class },
                custody
            );
        }
        for (var probeKind : InstrumentationTargetPlanningDomain.ProbeKind.values()) {
            callStatic(
                BtmGenerationGrpcClient.class,
                "probeKind",
                new Class<?>[] { InstrumentationTargetPlanningDomain.ProbeKind.class },
                probeKind
            );
        }
        for (var severity : de.burger.forensics.analytics.btmgeneration.v1.DiagnosticSeverity.values()) {
            callStatic(
                BtmGenerationGrpcClient.class,
                "severity",
                new Class<?>[] { de.burger.forensics.analytics.btmgeneration.v1.DiagnosticSeverity.class },
                severity
            );
        }
    }

    private RepositoryAnalysisGrpcClient repositoryClient(RepositoryAnalysisServiceGrpc.RepositoryAnalysisServiceImplBase service)
        throws IOException {
        start(service);
        return new RepositoryAnalysisGrpcClient(RepositoryAnalysisServiceGrpc.newBlockingStub(channel), 3);
    }

    private JoernCpgAnalysisGrpcClient joernClient(JoernCpgAnalysisServiceGrpc.JoernCpgAnalysisServiceImplBase service)
        throws IOException {
        start(service);
        return new JoernCpgAnalysisGrpcClient(
            JoernCpgAnalysisServiceGrpc.newBlockingStub(channel),
            3,
            "ghcr.io/joernio/joern@sha256:" + "a".repeat(64),
            "analysis-store-default-v1"
        );
    }

    private BtmGenerationGrpcClient btmClient(BtmGenerationServiceGrpc.BtmGenerationServiceImplBase service)
        throws IOException {
        start(service);
        return new BtmGenerationGrpcClient(BtmGenerationServiceGrpc.newBlockingStub(channel), 3, 4096);
    }

    private void start(BindableService service) throws IOException {
        var serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(service)
            .build()
            .start();
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
    }

    private static Object callStatic(Class<?> type, String methodName, Class<?>[] parameterTypes, Object... arguments)
        throws Exception {
        var method = type.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(null, arguments);
    }

    private static RepositoryToBtmOrchestrationDomain.StartRepositoryToBtmCommand command() {
        return new RepositoryToBtmOrchestrationDomain.StartRepositoryToBtmCommand(
            new RepositoryToBtmOrchestrationDomain.OrchestrationMetadata(
                "request-1",
                "schema-v1",
                "correlation-1",
                new AnalysisRunId("run-1")
            ),
            new RepositoryToBtmOrchestrationDomain.RepositoryReference("https://example.org/repository.git", "github"),
            new RepositoryToBtmOrchestrationDomain.RevisionSelector("main", ""),
            new RepositoryToBtmOrchestrationDomain.WorkspacePolicy(false, true, false, false, 30, 104_857_600),
            new RepositoryToBtmOrchestrationDomain.BuildContext("gradle", "build-1", "demo", List.of("app"), Map.of()),
            List.of(RepositoryToBtmOrchestrationDomain.RequestedOutput.BTM_RULES),
            Map.of("tenant", "demo")
        );
    }

    private static RepositoryAnalysisWorkerPort.RepositoryAnalysisResult repositoryAnalysisResult() {
        return new RepositoryAnalysisWorkerPort.RepositoryAnalysisResult(
            new AnalysisRunId("run-1"),
            new AnalysisJobId("job-java-ast"),
            new SourceSnapshotId("snapshot-1"),
            List.of(new RepositoryAnalysisWorkerPort.SourceRoot("src/main/java", "java")),
            packageDescriptor("repository-analysis-service", "source-package.zip"),
            packageDescriptor("build-artifact-worker-service", "build-output.zip"),
            sourceFactArtifact(),
            AnalysisCompleteness.COMPLETE,
            List.of(),
            Map.of()
        );
    }

    private static RepositoryAnalysisWorkerPort.PackageDescriptor packageDescriptor(String owner, String packagePath) {
        return new RepositoryAnalysisWorkerPort.PackageDescriptor(
            RepositoryAnalysisWorkerPort.PackageAvailability.AVAILABLE,
            new ArtifactReference(packagePath.replace(".zip", "-manifest.json"), "application/json", "c".repeat(64), 64),
            new ArtifactReference(packagePath, "application/zip", "d".repeat(64), 1024),
            "package-descriptor-v1",
            owner,
            new ArtifactByteAccess(
                owner,
                owner + ".v1.DownloadPackage",
                packagePath,
                ArtifactByteCustody.PRODUCER_RETAINED
            ),
            AnalysisCompleteness.COMPLETE,
            new RepositoryAnalysisWorkerPort.BuildOutputResolution(
                List.of(new RepositoryAnalysisWorkerPort.BuildOutputProducerCandidate(
                    RepositoryAnalysisWorkerPort.BuildOutputProducer.ARTIFACTORY,
                    RepositoryAnalysisWorkerPort.BuildOutputProducerStatus.AVAILABLE,
                    "artifact-store:demo",
                    List.of()
                )),
                RepositoryAnalysisWorkerPort.BuildOutputProducer.ARTIFACTORY,
                false,
                List.of()
            ),
            "gradle"
        );
    }

    private static AnalysisArtifactReference sourceFactArtifact() {
        return artifact(
            "source-facts.json",
            "application/vnd.forensic-analytics.java-ast-source-facts.v1+json",
            AnalysisArtifactCategory.STATIC,
            "java-ast-analysis-service",
            "java-ast-analysis.v1.JavaAstAnalysisService.GetSourceFactArtifactBytes"
        );
    }

    private static AnalysisArtifactReference semanticArtifact() {
        return artifact(
            "joern-cpg/cpg.bin.zip",
            "application/vnd.forensic-analytics.joern-cpg.v1+zip",
            AnalysisArtifactCategory.STATIC,
            "joern-cpg-analysis-service",
            "joern-cpg-analysis.v1.JoernCpgAnalysisService.DownloadSemanticArtifact"
        );
    }

    private static AnalysisArtifactReference generatedArtifact(String path) {
        return artifact(
            path,
            "application/vnd.forensic-analytics.btm-rules.v1+btm",
            AnalysisArtifactCategory.GENERATED,
            "btm-generation-service",
            "btm-generation.v1.BtmArtifactDeliveryService.DownloadBtmArtifacts"
        );
    }

    private static AnalysisArtifactReference artifact(
        String path,
        String type,
        AnalysisArtifactCategory category,
        String owner,
        String contract
    ) {
        return new AnalysisArtifactReference(
            new ArtifactReference(path, type, "a".repeat(64), 256),
            category,
            owner,
            "schema-v1",
            AnalysisCompleteness.COMPLETE,
            new ArtifactByteAccess(owner, contract, path, ArtifactByteCustody.PRODUCER_RETAINED)
        );
    }

    private static InstrumentationTargetPlanningDomain.InstrumentationTargetSelection targetSelection() {
        return new InstrumentationTargetPlanningDomain.InstrumentationTargetSelection(
            "selection-1",
            "analysis-store-service",
            "policy-v1",
            "f".repeat(64),
            AnalysisCompleteness.COMPLETE,
            InstrumentationTargetPlanningDomain.DETERMINISTIC_ORDER,
            "correlation-1",
            1
        );
    }

    private static InstrumentationTargetPlanningDomain.InstrumentationTarget target() {
        return new InstrumentationTargetPlanningDomain.InstrumentationTarget(
            "target-1",
            "fact-1",
            "",
            "src/main/java/a/A.java",
            "a.A",
            "run",
            "a.A#run()",
            12,
            InstrumentationTargetPlanningDomain.ProbeKind.METHOD_ENTRY,
            "source-facts.json",
            "",
            0,
            AnalysisCompleteness.COMPLETE,
            "source-code"
        );
    }

    private static de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactReference protoArtifact(
        AnalysisArtifactReference artifact
    ) {
        return de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactReference.newBuilder()
            .setArtifact(de.burger.forensics.analytics.analysisjob.v1.ArtifactReference.newBuilder()
                .setPath(artifact.artifact().path())
                .setType(artifact.artifact().type())
                .setSha256(artifact.artifact().sha256())
                .setSizeBytes(artifact.artifact().sizeBytes()))
            .setCategory(switch (artifact.category()) {
                case STATIC -> de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC;
                case RUNTIME -> de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_RUNTIME;
                case PROJECTION ->
                    de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_PROJECTION;
                case GENERATED ->
                    de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_GENERATED;
            })
            .setProducerService(artifact.producerService())
            .setSchemaVersion(artifact.schemaVersion())
            .setCompleteness(de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE)
            .setByteAccess(de.burger.forensics.analytics.analysisjob.v1.ArtifactByteAccess.newBuilder()
                .setOwnerService(artifact.byteAccess().ownerService())
                .setRetrievalContract(artifact.byteAccess().retrievalContract())
                .setRetrievalReference(artifact.byteAccess().retrievalReference())
                .setByteCustody(de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_PRODUCER_RETAINED))
            .build();
    }

    private static de.burger.forensics.analytics.repositoryanalysis.v1.ArtifactReference repositoryArtifact(String path) {
        return de.burger.forensics.analytics.repositoryanalysis.v1.ArtifactReference.newBuilder()
            .setReference(path)
            .setType(path.endsWith(".zip") ? "application/zip" : "application/json")
            .setSha256("b".repeat(64))
            .setSizeBytes(512)
            .build();
    }

    private static de.burger.forensics.analytics.analysisjob.v1.ArtifactByteAccess protoByteAccess(String owner, String path) {
        return de.burger.forensics.analytics.analysisjob.v1.ArtifactByteAccess.newBuilder()
            .setOwnerService(owner)
            .setRetrievalContract(owner + ".v1.DownloadPackage")
            .setRetrievalReference(path)
            .setByteCustody(de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_PRODUCER_RETAINED)
            .build();
    }

    private static de.burger.forensics.analytics.repositoryanalysis.v1.SourcePackageDescriptor protoSourcePackage() {
        return de.burger.forensics.analytics.repositoryanalysis.v1.SourcePackageDescriptor.newBuilder()
            .setAvailability(de.burger.forensics.analytics.repositoryanalysis.v1.PackageAvailability.PACKAGE_AVAILABILITY_AVAILABLE)
            .setManifestArtifact(repositoryArtifact("source-package-manifest.json"))
            .setPackageArtifact(repositoryArtifact("source-package.zip"))
            .setSchemaVersion("source-package-v1")
            .setProducerService("repository-analysis-service")
            .setByteAccess(protoByteAccess("repository-analysis-service", "source-package.zip"))
            .setCompleteness(de.burger.forensics.analytics.repositoryanalysis.v1.SourceSnapshotCompleteness.SOURCE_SNAPSHOT_COMPLETENESS_COMPLETE)
            .build();
    }

    private static de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputPackageDescriptor protoBuildOutputPackage() {
        return de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputPackageDescriptor.newBuilder()
            .setAvailability(de.burger.forensics.analytics.repositoryanalysis.v1.PackageAvailability.PACKAGE_AVAILABILITY_AVAILABLE)
            .setManifestArtifact(repositoryArtifact("build-output-manifest.json"))
            .setPackageArtifact(repositoryArtifact("build-output.zip"))
            .setSchemaVersion("build-output-v1")
            .setProducerService("build-artifact-worker-service")
            .setByteAccess(protoByteAccess("build-artifact-worker-service", "build-output.zip"))
            .setCompleteness(de.burger.forensics.analytics.repositoryanalysis.v1.SourceSnapshotCompleteness.SOURCE_SNAPSHOT_COMPLETENESS_COMPLETE)
            .setResolution(de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputResolution.newBuilder()
                .setSelectedProducer(de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducer.BUILD_OUTPUT_PRODUCER_ARTIFACTORY)
                .addCandidates(de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducerCandidate.newBuilder()
                    .setProducer(de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducer.BUILD_OUTPUT_PRODUCER_ARTIFACTORY)
                    .setStatus(de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducerStatus.BUILD_OUTPUT_PRODUCER_STATUS_AVAILABLE)
                    .setReference("artifact-store:demo")))
            .setBuildSystem("gradle")
            .build();
    }

    private static final class CapturingRepositoryAnalysisService
        extends RepositoryAnalysisServiceGrpc.RepositoryAnalysisServiceImplBase {
        private PrepareRepositoryRequest prepareRequest;
        private AnalyzeSourceSnapshotWithJavaAstRequest handoffRequest;

        @Override
        public void prepareRepository(
            PrepareRepositoryRequest request,
            StreamObserver<PrepareRepositoryResponse> responseObserver
        ) {
            prepareRequest = request;
            responseObserver.onNext(PrepareRepositoryResponse.newBuilder()
                .setPreparation(RepositoryPreparation.newBuilder()
                    .setAnalysisRunId("run-1")
                    .setSourceSnapshotId("snapshot-1")
                    .setWorkspaceId("workspace-1")
                    .setRepository(request.getRepository())
                    .setRequestedRevision(request.getRevision())
                    .setCheckout(de.burger.forensics.analytics.repositoryanalysis.v1.CheckoutResult.newBuilder()
                        .setStatus(de.burger.forensics.analytics.repositoryanalysis.v1.CheckoutStatus.CHECKOUT_STATUS_CHECKED_OUT)
                        .setResolvedCommit("a".repeat(40))
                        .setRequestedBranch("main"))
                    .setSourceSnapshot(de.burger.forensics.analytics.repositoryanalysis.v1.SourceSnapshot.newBuilder()
                        .setSourceSnapshotId("snapshot-1")
                        .setCompleteness(de.burger.forensics.analytics.repositoryanalysis.v1.SourceSnapshotCompleteness.SOURCE_SNAPSHOT_COMPLETENESS_COMPLETE)
                        .addSourceRoots(de.burger.forensics.analytics.repositoryanalysis.v1.SourceRoot.newBuilder()
                            .setRelativePath("src/main/java")
                            .setLanguage("java"))
                        .setSourcePackage(protoSourcePackage())
                        .setBuildOutputPackage(protoBuildOutputPackage()))
                    .setWorkspaceStatus(de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryWorkspaceStatus.REPOSITORY_WORKSPACE_STATUS_CHECKED_OUT))
                .build());
            responseObserver.onCompleted();
        }

        @Override
        public void analyzeSourceSnapshotWithJavaAst(
            AnalyzeSourceSnapshotWithJavaAstRequest request,
            StreamObserver<JavaAstHandoffResponse> responseObserver
        ) {
            handoffRequest = request;
            responseObserver.onNext(JavaAstHandoffResponse.newBuilder()
                .setAnalysisRunId("run-1")
                .setAnalysisJobId(request.getAnalysisJobId())
                .setSourceSnapshotId("snapshot-1")
                .setCompleteness(de.burger.forensics.analytics.repositoryanalysis.v1.SourceSnapshotCompleteness.SOURCE_SNAPSHOT_COMPLETENESS_COMPLETE)
                .setSourceFactArtifact(protoArtifact(sourceFactArtifact()))
                .putSafeAttributes("owner", "repository-analysis")
                .build());
            responseObserver.onCompleted();
        }
    }

    private static final class FailingRepositoryAnalysisService
        extends RepositoryAnalysisServiceGrpc.RepositoryAnalysisServiceImplBase {
        @Override
        public void prepareRepository(
            PrepareRepositoryRequest request,
            StreamObserver<PrepareRepositoryResponse> responseObserver
        ) {
            responseObserver.onError(Status.UNAVAILABLE.asRuntimeException());
        }
    }

    private static final class CapturingJoernCpgAnalysisService
        extends JoernCpgAnalysisServiceGrpc.JoernCpgAnalysisServiceImplBase {
        private MaterializeJoernWorkspaceRequest materializeRequest;
        private AnalyzeJoernCpgRequest analyzeRequest;

        @Override
        public void materializeSourceSnapshot(
            MaterializeJoernWorkspaceRequest request,
            StreamObserver<MaterializeJoernWorkspaceResponse> responseObserver
        ) {
            materializeRequest = request;
            responseObserver.onNext(MaterializeJoernWorkspaceResponse.newBuilder()
                .setAnalysisRunId(request.getAnalysisRunId())
                .setAnalysisJobId(request.getAnalysisJobId())
                .setSourceSnapshotId(request.getSourceSnapshotId())
                .setWorkspace(de.burger.forensics.analytics.joerncpganalysis.v1.SourceWorkspace.newBuilder()
                    .setWorkspaceId("joern-workspace-1")
                    .addSourceRoots(request.getSourceRoots(0))
                    .addInputArtifacts(protoArtifact(sourceFactArtifact())))
                .build());
            responseObserver.onCompleted();
        }

        @Override
        public void analyzeSourceSnapshot(
            AnalyzeJoernCpgRequest request,
            StreamObserver<AnalyzeJoernCpgResponse> responseObserver
        ) {
            analyzeRequest = request;
            responseObserver.onNext(AnalyzeJoernCpgResponse.newBuilder()
                .setAnalysisRunId(request.getAnalysisRunId())
                .setAnalysisJobId(request.getAnalysisJobId())
                .setSourceSnapshotId(request.getSourceSnapshotId())
                .setCompleteness(de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.ANALYSIS_COMPLETENESS_INCOMPLETE)
                .addSemanticArtifacts(protoArtifact(semanticArtifact()))
                .addDiagnostics(de.burger.forensics.analytics.joerncpganalysis.v1.JoernCpgDiagnostic.newBuilder()
                    .setCode("JOERN_DATAFLOW_SKIPPED")
                    .setMessage("dataflow was not requested")
                    .setSeverity(de.burger.forensics.analytics.joerncpganalysis.v1.DiagnosticSeverity.DIAGNOSTIC_SEVERITY_WARNING)
                    .setAffectsCompleteness(true))
                .putSafeAttributes("owner", "joern")
                .build());
            responseObserver.onCompleted();
        }
    }

    private static final class FailingJoernCpgAnalysisService
        extends JoernCpgAnalysisServiceGrpc.JoernCpgAnalysisServiceImplBase {
        @Override
        public void materializeSourceSnapshot(
            MaterializeJoernWorkspaceRequest request,
            StreamObserver<MaterializeJoernWorkspaceResponse> responseObserver
        ) {
            responseObserver.onError(Status.UNAVAILABLE.asRuntimeException());
        }
    }

    private static final class CapturingBtmGenerationService
        extends BtmGenerationServiceGrpc.BtmGenerationServiceImplBase {
        private GenerateBtmRulesRequest request;

        @Override
        public void generateBtmRules(
            GenerateBtmRulesRequest request,
            StreamObserver<GenerateBtmRulesResponse> responseObserver
        ) {
            this.request = request;
            responseObserver.onNext(GenerateBtmRulesResponse.newBuilder()
                .setAnalysisRunId(request.getAnalysisRunId())
                .setAnalysisJobId(request.getAnalysisJobId())
                .setSourceSnapshotId(request.getSourceSnapshotId())
                .setCompleteness(de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE)
                .addGeneratedArtifacts(protoArtifact(generatedArtifact("btm/snapshot-1-rules.btm")))
                .addGeneratedArtifacts(protoArtifact(generatedArtifact("btm/snapshot-1-manifest.json")))
                .putSafeAttributes("owner", "btm")
                .build());
            responseObserver.onCompleted();
        }
    }

    private static final class FailingBtmGenerationService
        extends BtmGenerationServiceGrpc.BtmGenerationServiceImplBase {
        @Override
        public void generateBtmRules(
            GenerateBtmRulesRequest request,
            StreamObserver<GenerateBtmRulesResponse> responseObserver
        ) {
            responseObserver.onError(Status.UNAVAILABLE.asRuntimeException());
        }
    }
}
