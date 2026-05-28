package de.burger.forensics.analytics.services.analysisstore.adapter.in.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobServiceGrpc;
import de.burger.forensics.analytics.analysisjob.v1.BtmDeliveryReadiness;
import de.burger.forensics.analytics.analysisjob.v1.GetRepositoryToBtmStatusRequest;
import de.burger.forensics.analytics.analysisjob.v1.ListAnalysisJobsRequest;
import de.burger.forensics.analytics.analysisjob.v1.RepositoryToBtmOrchestrationState;
import de.burger.forensics.analytics.services.analysisstore.adapter.out.memory.InMemoryAnalysisJobRepository;
import de.burger.forensics.analytics.services.analysisstore.application.AnalysisJobApplicationService;
import de.burger.forensics.analytics.services.analysisstore.application.InstrumentationTargetPlanningApplicationService;
import de.burger.forensics.analytics.services.analysisstore.application.RepositoryToBtmOrchestrationApplicationService;
import de.burger.forensics.analytics.services.analysisstore.application.port.BtmGenerationWorkerPort;
import de.burger.forensics.analytics.services.analysisstore.application.port.BtmGenerationWorkerPort.BtmGenerationResult;
import de.burger.forensics.analytics.services.analysisstore.application.port.EvidenceArtifactIntegrityException;
import de.burger.forensics.analytics.services.analysisstore.application.port.JoernSemanticAnalysisPort;
import de.burger.forensics.analytics.services.analysisstore.application.port.RepositoryAnalysisWorkerPort;
import de.burger.forensics.analytics.services.analysisstore.application.port.RepositoryAnalysisWorkerPort.BuildOutputResolution;
import de.burger.forensics.analytics.services.analysisstore.application.port.RepositoryAnalysisWorkerPort.PackageAvailability;
import de.burger.forensics.analytics.services.analysisstore.application.port.RepositoryAnalysisWorkerPort.PackageDescriptor;
import de.burger.forensics.analytics.services.analysisstore.application.port.RepositoryAnalysisWorkerPort.RepositoryAnalysisResult;
import de.burger.forensics.analytics.services.analysisstore.application.port.RepositoryAnalysisWorkerPort.SourceRoot;
import de.burger.forensics.analytics.services.analysisstore.application.port.SourceFactArtifactByteVerifierPort;
import de.burger.forensics.analytics.services.analysisstore.application.port.SourceFactArtifactReaderPort;
import de.burger.forensics.analytics.services.analysisstore.application.port.SourceFactArtifactReaderPort.SourceFactArtifact;
import de.burger.forensics.analytics.services.analysisstore.application.port.WorkerOwnerApiUnavailableException;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactCategory;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJobId;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisRunId;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactByteAccess;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactByteCustody;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactReference;
import de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.AcceptedStaticSourceFact;
import de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.StaticSourceLocation;
import de.burger.forensics.analytics.services.analysisstore.domain.RepositoryToBtmOrchestrationDomain;
import de.burger.forensics.analytics.services.analysisstore.domain.SourceSnapshotId;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryToBtmOwnerApiEndToEndTest {
    private Server server;
    private ManagedChannel channel;
    private AnalysisJobServiceGrpc.AnalysisJobServiceBlockingStub stub;

    @BeforeEach
    void startServer() throws IOException {
        startServer(new FakeSourceFactReader());
    }

    private void startServer(SourceFactArtifactReaderPort sourceFactReader) throws IOException {
        startServer(
            new FakeRepositoryAnalysisWorker(false),
            sourceFactReader,
            JoernSemanticAnalysisPort.unavailable(),
            new FakeBtmGenerationWorker(0)
        );
    }

    private void startServer(
        RepositoryAnalysisWorkerPort repositoryAnalysisWorker,
        SourceFactArtifactReaderPort sourceFactReader,
        JoernSemanticAnalysisPort joernSemanticAnalysis,
        BtmGenerationWorkerPort btmGenerationWorker
    ) throws IOException {
        var serverName = InProcessServerBuilder.generateName();
        var analysisJobs = new AnalysisJobApplicationService(
            new InMemoryAnalysisJobRepository(),
            Clock.fixed(Instant.parse("2026-05-20T08:00:00Z"), ZoneOffset.UTC),
            new AcceptingSourceFactVerifier()
        );
        var targetPlanning = new InstrumentationTargetPlanningApplicationService(analysisJobs);
        var orchestration = new RepositoryToBtmOrchestrationApplicationService(
            analysisJobs,
            targetPlanning,
            repositoryAnalysisWorker,
            sourceFactReader,
            joernSemanticAnalysis,
            btmGenerationWorker
        );
        server = InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(new AnalysisJobGrpcEndpoint(analysisJobs, targetPlanning, orchestration))
            .build()
            .start();
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        stub = AnalysisJobServiceGrpc.newBlockingStub(channel);
    }

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
    void orchestratesRepositorySourceFactsAndDeterministicBtmArtifactsThroughOwnerApi() {
        var started = stub.startRepositoryToBtm(AnalysisJobGrpcEndpointTest.repositoryToBtmRequest().build());
        var status = stub.getRepositoryToBtmStatus(GetRepositoryToBtmStatusRequest.newBuilder()
            .setRequestId("request-status-e2e")
            .setCorrelationId("correlation-status-e2e")
            .setAnalysisRunId(AnalysisJobGrpcEndpointTest.runId())
            .build());
        var jobs = stub.listAnalysisJobs(ListAnalysisJobsRequest.newBuilder()
            .setRequestId("request-list-e2e")
            .setCorrelationId("correlation-1")
            .setAnalysisRunId(AnalysisJobGrpcEndpointTest.runId())
            .build());

        assertEquals(
            RepositoryToBtmOrchestrationState.REPOSITORY_TO_BTM_ORCHESTRATION_STATE_READY_FOR_BTM_DELIVERY,
            started.getState()
        );
        assertEquals(BtmDeliveryReadiness.BTM_DELIVERY_READINESS_READY, started.getBtmDeliveryReadiness());
        assertEquals(
            de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness.ANALYSIS_COMPLETENESS_INCOMPLETE,
            started.getCompleteness()
        );
        assertEquals("snapshot-e2e", started.getSourceSnapshotId().getValue());
        assertEquals(2, started.getAcceptedGeneratedArtifactsCount());
        assertEquals("btm/snapshot-e2e-rules.btm", started.getAcceptedGeneratedArtifacts(0).getArtifact().getPath());
        assertEquals("1", started.getAttributesOrThrow("sourceFactCount"));
        assertEquals("1", started.getAttributesOrThrow("targetCount"));
        assertEquals("2", started.getAttributesOrThrow("generatedArtifactCount"));
        assertTrue(started.getDiagnosticsList().stream()
            .anyMatch(diagnostic -> "JOERN_SKIPPED_UNAVAILABLE_PACKAGE".equals(diagnostic.getCode())));
        assertEquals("correlation-status-e2e", status.getStatus().getCorrelationId());
        assertEquals(started.getAcceptedGeneratedArtifactsList(), status.getAcceptedGeneratedArtifactsList());
        assertEquals(3, jobs.getJobsCount());
        assertFalse(started.toString().contains("/tmp"));
        assertFalse(started.toString().contains("token"));
    }

    @Test
    void includesJoernSemanticArtifactsWhenRepositoryAndBuildPackagesAreReady() throws IOException {
        stopServer();
        startServer(
            new FakeRepositoryAnalysisWorker(true),
            new FakeSourceFactReader(),
            new FakeJoernSemanticAnalysis(),
            new FakeBtmGenerationWorker(1)
        );

        var started = stub.startRepositoryToBtm(AnalysisJobGrpcEndpointTest.repositoryToBtmRequest()
            .setIdempotencyKey("repository-to-btm-joern-ready")
            .build());
        var jobs = stub.listAnalysisJobs(ListAnalysisJobsRequest.newBuilder()
            .setRequestId("request-list-joern-ready")
            .setCorrelationId("correlation-1")
            .setAnalysisRunId(AnalysisJobGrpcEndpointTest.runId())
            .build());

        assertEquals(
            RepositoryToBtmOrchestrationState.REPOSITORY_TO_BTM_ORCHESTRATION_STATE_READY_FOR_BTM_DELIVERY,
            started.getState()
        );
        assertFalse(started.getJoernSkipped());
        assertEquals(BtmDeliveryReadiness.BTM_DELIVERY_READINESS_READY, started.getBtmDeliveryReadiness());
        assertEquals(4, jobs.getJobsCount());
        assertEquals("joern-analysis-" + started.getAttributesOrThrow("repositoryAnalysisJobId").substring("repository-analysis-".length()), started.getAttributesOrThrow("joernAnalysisJobId"));
    }

    @Test
    void reportsArtifactIntegrityFailureWithoutGeneratingBtmArtifacts() throws IOException {
        stopServer();
        startServer(new IntegrityFailingSourceFactReader());

        var started = stub.startRepositoryToBtm(AnalysisJobGrpcEndpointTest.repositoryToBtmRequest()
            .setIdempotencyKey("repository-to-btm-integrity-failure")
            .build());
        var jobs = stub.listAnalysisJobs(ListAnalysisJobsRequest.newBuilder()
            .setRequestId("request-list-integrity")
            .setCorrelationId("correlation-1")
            .setAnalysisRunId(AnalysisJobGrpcEndpointTest.runId())
            .build());

        assertEquals(
            RepositoryToBtmOrchestrationState.REPOSITORY_TO_BTM_ORCHESTRATION_STATE_FAILED,
            started.getState()
        );
        assertEquals(BtmDeliveryReadiness.BTM_DELIVERY_READINESS_UNAVAILABLE, started.getBtmDeliveryReadiness());
        assertEquals("snapshot-e2e", started.getSourceSnapshotId().getValue());
        assertEquals(0, started.getAcceptedGeneratedArtifactsCount());
        assertTrue(started.getDiagnosticsList().stream()
            .anyMatch(diagnostic -> !diagnostic.getRetryable()
                && diagnostic.getAffectsCompleteness()
                && "JAVA_AST_SOURCE_FACT_CHECKSUM_MISMATCH".equals(diagnostic.getCode())));
        assertEquals(2, jobs.getJobsCount());
        assertFalse(started.toString().contains("/tmp"));
        assertFalse(started.toString().contains("token"));
    }

    @Test
    void reportsIncompleteWhenNoInstrumentationTargetsAreAccepted() throws IOException {
        stopServer();
        startServer(new EmptySourceFactReader());

        var started = stub.startRepositoryToBtm(AnalysisJobGrpcEndpointTest.repositoryToBtmRequest()
            .setIdempotencyKey("repository-to-btm-empty-targets")
            .build());

        assertEquals(
            RepositoryToBtmOrchestrationState.REPOSITORY_TO_BTM_ORCHESTRATION_STATE_INCOMPLETE,
            started.getState()
        );
        assertEquals(BtmDeliveryReadiness.BTM_DELIVERY_READINESS_UNAVAILABLE, started.getBtmDeliveryReadiness());
        assertEquals("0", started.getAttributesOrThrow("sourceFactCount"));
        assertEquals("0", started.getAttributesOrThrow("targetCount"));
        assertTrue(started.getDiagnosticsList().stream()
            .anyMatch(diagnostic -> "BTM_TARGETS_UNAVAILABLE".equals(diagnostic.getCode())));
        assertEquals(0, started.getAcceptedGeneratedArtifactsCount());
    }

    @Test
    void reportsSourceFactOwnerUnavailableAfterRepositoryAnalysisWithoutResubmittingRepositoryJob() throws IOException {
        stopServer();
        startServer(new OwnerUnavailableSourceFactReader());

        var started = stub.startRepositoryToBtm(AnalysisJobGrpcEndpointTest.repositoryToBtmRequest()
            .setIdempotencyKey("repository-to-btm-source-fact-owner-unavailable")
            .build());
        var jobs = stub.listAnalysisJobs(ListAnalysisJobsRequest.newBuilder()
            .setRequestId("request-list-source-fact-owner")
            .setCorrelationId("correlation-1")
            .setAnalysisRunId(AnalysisJobGrpcEndpointTest.runId())
            .build());

        assertEquals(
            RepositoryToBtmOrchestrationState.REPOSITORY_TO_BTM_ORCHESTRATION_STATE_INCOMPLETE,
            started.getState()
        );
        assertEquals(BtmDeliveryReadiness.BTM_DELIVERY_READINESS_NOT_READY, started.getBtmDeliveryReadiness());
        assertEquals("snapshot-e2e", started.getSourceSnapshotId().getValue());
        assertEquals(2, jobs.getJobsCount());
        assertEquals("0", started.getAttributesOrThrow("sourceFactCount"));
        assertTrue(started.getDiagnosticsList().stream()
            .anyMatch(diagnostic -> diagnostic.getRetryable()
                && diagnostic.getAffectsCompleteness()
                && "JAVA_AST_SOURCE_FACT_OWNER_API_UNAVAILABLE".equals(diagnostic.getCode())));
    }

    @Test
    void reportsBtmOwnerUnavailableAfterTargetPlanningWithoutResubmittingRepositoryJob() throws IOException {
        stopServer();
        startServer(
            new FakeRepositoryAnalysisWorker(false),
            new FakeSourceFactReader(),
            JoernSemanticAnalysisPort.unavailable(),
            new OwnerUnavailableBtmGenerationWorker()
        );

        var started = stub.startRepositoryToBtm(AnalysisJobGrpcEndpointTest.repositoryToBtmRequest()
            .setIdempotencyKey("repository-to-btm-btm-owner-unavailable")
            .build());
        var jobs = stub.listAnalysisJobs(ListAnalysisJobsRequest.newBuilder()
            .setRequestId("request-list-btm-owner")
            .setCorrelationId("correlation-1")
            .setAnalysisRunId(AnalysisJobGrpcEndpointTest.runId())
            .build());

        assertEquals(
            RepositoryToBtmOrchestrationState.REPOSITORY_TO_BTM_ORCHESTRATION_STATE_INCOMPLETE,
            started.getState()
        );
        assertEquals(BtmDeliveryReadiness.BTM_DELIVERY_READINESS_NOT_READY, started.getBtmDeliveryReadiness());
        assertEquals("snapshot-e2e", started.getSourceSnapshotId().getValue());
        assertEquals(3, jobs.getJobsCount());
        assertEquals("1", started.getAttributesOrThrow("sourceFactCount"));
        assertEquals("1", started.getAttributesOrThrow("targetCount"));
        assertTrue(started.getAttributesMap().containsKey("btmGenerationJobId"));
        assertTrue(started.getDiagnosticsList().stream()
            .anyMatch(diagnostic -> diagnostic.getRetryable()
                && diagnostic.getAffectsCompleteness()
                && "BTM_GENERATION_OWNER_API_UNAVAILABLE".equals(diagnostic.getCode())));
        assertEquals(0, started.getAcceptedGeneratedArtifactsCount());
    }

    private static final class FakeRepositoryAnalysisWorker implements RepositoryAnalysisWorkerPort {
        private final boolean packagesReady;

        private FakeRepositoryAnalysisWorker(boolean packagesReady) {
            this.packagesReady = packagesReady;
        }

        @Override
        public RepositoryAnalysisResult prepareAndAnalyzeJavaAst(
            RepositoryToBtmOrchestrationDomain.StartRepositoryToBtmCommand command,
            AnalysisJobId astAnalysisJobId
        ) {
            return new RepositoryAnalysisResult(
                command.metadata().analysisRunId(),
                astAnalysisJobId,
                new SourceSnapshotId("snapshot-e2e"),
                List.of(new SourceRoot("src/main/java", "java")),
                packageDescriptor("repository-analysis-service", "repository-analysis.v1.SourcePackage", packagesReady),
                packageDescriptor("build-artifact-worker-service", "build-artifact-worker.v1.BuildOutputPackage", packagesReady),
                sourceFactArtifact(),
                AnalysisCompleteness.COMPLETE,
                List.of(),
                Map.of("fixture", "repository-to-btm-e2e")
            );
        }

        private static PackageDescriptor packageDescriptor(String owner, String contract, boolean packagesReady) {
            return new PackageDescriptor(
                packagesReady ? PackageAvailability.AVAILABLE : PackageAvailability.PENDING,
                packagesReady ? new ArtifactReference(owner + "/manifest.json", "application/json", "c".repeat(64), 64) : null,
                packagesReady ? new ArtifactReference(owner + "/package.zip", "application/zip", "d".repeat(64), 512) : null,
                "package-descriptor-v1",
                owner,
                new ArtifactByteAccess(
                    owner,
                    contract,
                    "source-snapshot/snapshot-e2e",
                    ArtifactByteCustody.PRODUCER_RETAINED
                ),
                packagesReady ? AnalysisCompleteness.COMPLETE : AnalysisCompleteness.UNKNOWN,
                BuildOutputResolution.empty(),
                "auto-detect"
            );
        }
    }

    private static final class FakeSourceFactReader implements SourceFactArtifactReaderPort {
        @Override
        public SourceFactArtifact readFacts(
            AnalysisRunId analysisRunId,
            AnalysisJobId analysisJobId,
            SourceSnapshotId sourceSnapshotId,
            String requestId,
            String correlationId,
            de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference artifact,
            Map<String, String> safeAttributes
        ) {
            return new SourceFactArtifact(
                artifact,
                List.of(new AcceptedStaticSourceFact(
                    "fact-1",
                    "java-method",
                    new StaticSourceLocation("src/main/java/a/A.java", "a.A", "run", 12, 1),
                    "a.A#run()",
                    artifact.path(),
                    AnalysisCompleteness.COMPLETE
                )),
                AnalysisCompleteness.COMPLETE,
                List.of()
            );
        }
    }

    private static final class IntegrityFailingSourceFactReader implements SourceFactArtifactReaderPort {
        @Override
        public SourceFactArtifact readFacts(
            AnalysisRunId analysisRunId,
            AnalysisJobId analysisJobId,
            SourceSnapshotId sourceSnapshotId,
            String requestId,
            String correlationId,
            de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference artifact,
            Map<String, String> safeAttributes
        ) {
            throw new EvidenceArtifactIntegrityException(
                "JAVA_AST_SOURCE_FACT_CHECKSUM_MISMATCH",
                "Java AST source fact artifact checksum verification failed."
            );
        }
    }

    private static final class OwnerUnavailableSourceFactReader implements SourceFactArtifactReaderPort {
        @Override
        public SourceFactArtifact readFacts(
            AnalysisRunId analysisRunId,
            AnalysisJobId analysisJobId,
            SourceSnapshotId sourceSnapshotId,
            String requestId,
            String correlationId,
            de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference artifact,
            Map<String, String> safeAttributes
        ) {
            throw new WorkerOwnerApiUnavailableException("Java AST source fact artifact reader");
        }
    }

    private static final class EmptySourceFactReader implements SourceFactArtifactReaderPort {
        @Override
        public SourceFactArtifact readFacts(
            AnalysisRunId analysisRunId,
            AnalysisJobId analysisJobId,
            SourceSnapshotId sourceSnapshotId,
            String requestId,
            String correlationId,
            de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference artifact,
            Map<String, String> safeAttributes
        ) {
            return new SourceFactArtifact(
                artifact,
                List.of(),
                AnalysisCompleteness.COMPLETE,
                List.of()
            );
        }
    }

    private static final class FakeJoernSemanticAnalysis implements JoernSemanticAnalysisPort {
        @Override
        public JoernAnalysisResult analyze(
            RepositoryToBtmOrchestrationDomain.StartRepositoryToBtmCommand command,
            AnalysisJobId joernAnalysisJobId,
            RepositoryAnalysisResult repositoryAnalysis
        ) {
            return new JoernAnalysisResult(
                command.metadata().analysisRunId(),
                joernAnalysisJobId,
                repositoryAnalysis.sourceSnapshotId(),
                AnalysisCompleteness.COMPLETE,
                List.of(generatedStaticArtifact(
                    "joern-cpg/snapshot-e2e-cpg.bin.zip",
                    "application/vnd.forensic-analytics.joern-cpg.v1+zip",
                    "joern-cpg-analysis-service",
                    "joern-cpg-v1"
                )),
                List.of(),
                Map.of("fixture", "joern-e2e")
            );
        }
    }

    private static final class FakeBtmGenerationWorker implements BtmGenerationWorkerPort {
        private final int expectedSemanticArtifactCount;

        private FakeBtmGenerationWorker(int expectedSemanticArtifactCount) {
            this.expectedSemanticArtifactCount = expectedSemanticArtifactCount;
        }

        @Override
        public BtmGenerationResult generate(
            RepositoryToBtmOrchestrationDomain.StartRepositoryToBtmCommand command,
            AnalysisJobId btmGenerationJobId,
            SourceSnapshotId sourceSnapshotId,
            List<de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference> sourceFactArtifacts,
            List<de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference> semanticArtifacts,
            AnalysisCompleteness inputCompleteness,
            de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.InstrumentationTargetSelection targetSelection,
            List<de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.InstrumentationTarget> targets
        ) {
            assertEquals(1, sourceFactArtifacts.size());
            assertEquals(expectedSemanticArtifactCount, semanticArtifacts.size());
            assertEquals(1, targets.size());
            return new BtmGenerationResult(
                command.metadata().analysisRunId(),
                btmGenerationJobId,
                sourceSnapshotId,
                AnalysisCompleteness.COMPLETE,
                List.of(
                    generatedArtifact("btm/snapshot-e2e-rules.btm", "application/vnd.forensic-analytics.btm-rules.v1+btm"),
                    generatedArtifact("btm/snapshot-e2e-manifest.json", "application/vnd.forensic-analytics.btm-rule-manifest.v1+json")
                ),
                List.of(),
                Map.of("fixture", "btm-generation-e2e")
            );
        }
    }

    private static final class OwnerUnavailableBtmGenerationWorker implements BtmGenerationWorkerPort {
        @Override
        public BtmGenerationResult generate(
            RepositoryToBtmOrchestrationDomain.StartRepositoryToBtmCommand command,
            AnalysisJobId btmGenerationJobId,
            SourceSnapshotId sourceSnapshotId,
            List<de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference> sourceFactArtifacts,
            List<de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference> semanticArtifacts,
            AnalysisCompleteness inputCompleteness,
            de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.InstrumentationTargetSelection targetSelection,
            List<de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.InstrumentationTarget> targets
        ) {
            throw new WorkerOwnerApiUnavailableException("BTM Generation", "UNAVAILABLE");
        }
    }

    private static final class AcceptingSourceFactVerifier implements SourceFactArtifactByteVerifierPort {
        @Override
        public boolean supports(de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference artifact) {
            return "java-ast-analysis-service".equals(artifact.byteAccess().ownerService());
        }

        @Override
        public de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference verify(
            AnalysisRunId analysisRunId,
            AnalysisJobId analysisJobId,
            SourceSnapshotId sourceSnapshotId,
            String requestId,
            String correlationId,
            de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference artifact,
            Map<String, String> safeAttributes
        ) {
            return artifact;
        }
    }

    private static de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference sourceFactArtifact() {
        return new de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference(
            new ArtifactReference(
                "java-ast/source-facts-e2e.json",
                "application/vnd.forensic-analytics.java-ast-source-facts.v1+json",
                "a".repeat(64),
                512
            ),
            AnalysisArtifactCategory.STATIC,
            "java-ast-analysis-service",
            "java-ast-source-facts-v1",
            AnalysisCompleteness.COMPLETE,
            new ArtifactByteAccess(
                "java-ast-analysis-service",
                "java-ast-analysis.v1.JavaAstAnalysisService.GetSourceFactArtifactBytes",
                "java-ast/source-facts-e2e.json",
                ArtifactByteCustody.PRODUCER_RETAINED
            )
        );
    }

    private static de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference generatedArtifact(
        String path,
        String type
    ) {
        return new de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference(
            new ArtifactReference(path, type, "b".repeat(64), 128),
            AnalysisArtifactCategory.GENERATED,
            "btm-generation-service",
            "btm-rule-v1",
            AnalysisCompleteness.COMPLETE,
            new ArtifactByteAccess(
                "btm-generation-service",
                "de.burger.forensics.analytics.btmgeneration.v1.BtmArtifactDeliveryService.DownloadBtmArtifacts",
                path,
                ArtifactByteCustody.PRODUCER_RETAINED
            )
        );
    }

    private static de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference generatedStaticArtifact(
        String path,
        String type,
        String owner,
        String schemaVersion
    ) {
        return new de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference(
            new ArtifactReference(path, type, "e".repeat(64), 512),
            AnalysisArtifactCategory.STATIC,
            owner,
            schemaVersion,
            AnalysisCompleteness.COMPLETE,
            new ArtifactByteAccess(
                owner,
                owner + ".v1.DownloadSemanticArtifact",
                path,
                ArtifactByteCustody.PRODUCER_RETAINED
            )
        );
    }
}
