package de.burger.forensics.analytics.services.btmgeneration.adapter.in.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactReference;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisRunId;
import de.burger.forensics.analytics.analysisjob.v1.ArtifactByteAccess;
import de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody;
import de.burger.forensics.analytics.analysisjob.v1.ArtifactReference;
import de.burger.forensics.analytics.btmgeneration.v1.BtmArtifactDeliveryMessage;
import de.burger.forensics.analytics.btmgeneration.v1.BtmArtifactDeliveryServiceGrpc;
import de.burger.forensics.analytics.btmgeneration.v1.BtmArtifactKind;
import de.burger.forensics.analytics.btmgeneration.v1.DownloadBtmArtifactsRequest;
import de.burger.forensics.analytics.services.btmgeneration.adapter.out.filesystem.FileSystemBtmArtifactReader;
import de.burger.forensics.analytics.services.btmgeneration.adapter.out.filesystem.FileSystemBtmArtifactWriter;
import de.burger.forensics.analytics.services.btmgeneration.application.BtmArtifactDeliveryApplicationService;
import de.burger.forensics.analytics.services.btmgeneration.application.BtmArtifactDeliveryException;
import de.burger.forensics.analytics.services.btmgeneration.application.port.BtmArtifactReaderPort;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BtmArtifactWriteRequest;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BtmGenerationPolicy;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BtmGenerationSummary;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.GeneratedBtmArtifacts;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.GeneratedRule;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ProbeKind;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ReadableBtmArtifact;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ReproducibilityMetadata;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.RequestMetadata;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.RuleTarget;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.SourceSnapshotId;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.StoredBtmArtifactManifest;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.TargetSelection;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BTM_DELIVERY_CONTRACT;
import static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.PRODUCER_SERVICE;
import static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.sha256;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BtmArtifactDeliveryGrpcEndpointTest {
    @TempDir
    Path tempDir;

    private Server server;
    private ManagedChannel channel;
    private BtmArtifactDeliveryServiceGrpc.BtmArtifactDeliveryServiceBlockingStub stub;

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
    void streamsManifestFirstThenDeterministicBoundedChunks() throws Exception {
        var artifacts = writeArtifacts();
        startServer();

        var first = deliver(request(artifacts, 32, 100_000));
        var second = deliver(request(artifacts, 32, 100_000));

        assertEquals(first, second);
        assertTrue(first.getFirst().hasManifest());
        assertTrue(first.getLast().hasStatus());
        assertEquals("DELIVERED", first.getLast().getStatus().getCode());
        assertEquals("snapshot-1", first.getFirst().getManifest().getSourceSnapshotId().getValue());
        assertEquals("selection-1", first.getFirst().getManifest().getTargetSelection().getSelectionId());
        assertEquals(2, first.getFirst().getManifest().getArtifactsCount());
        assertEquals(BtmArtifactKind.BTM_ARTIFACT_KIND_MANIFEST, first.getFirst().getManifest().getArtifacts(0).getArtifactKind());
        assertEquals(BtmArtifactKind.BTM_ARTIFACT_KIND_RULE_FILE, first.getFirst().getManifest().getArtifacts(1).getArtifactKind());

        var reconstructed = reconstruct(first);
        for (var artifact : artifacts.artifacts()) {
            var expected = Files.readAllBytes(tempDir.resolve(artifact.artifact().path()));
            assertEquals(sha256(expected), sha256(reconstructed.get(artifact.artifact().path()).toByteArray()));
            assertEquals(expected.length, reconstructed.get(artifact.artifact().path()).size());
        }
        first.stream()
            .filter(BtmArtifactDeliveryMessage::hasChunk)
            .forEach(message -> assertTrue(message.getChunk().getData().size() <= 32));
        assertChunkMetadata(first);
    }

    @Test
    void requestedRuleSubsetStillStreamsSelectedManifestBytes() throws Exception {
        var artifacts = writeArtifacts();
        startServer();

        var messages = deliver(request(artifacts, 32, 100_000).toBuilder()
            .addArtifactReferences(artifacts.artifacts().getFirst().artifact().path())
            .build());

        assertEquals(2, messages.getFirst().getManifest().getArtifactsCount());
        assertEquals(BtmArtifactKind.BTM_ARTIFACT_KIND_MANIFEST, messages.getFirst().getManifest().getArtifacts(0).getArtifactKind());
        assertEquals(BtmArtifactKind.BTM_ARTIFACT_KIND_RULE_FILE, messages.getFirst().getManifest().getArtifacts(1).getArtifactKind());
        var reconstructed = reconstruct(messages);
        assertTrue(reconstructed.containsKey(artifacts.artifacts().get(1).artifact().path()));
        assertTrue(reconstructed.containsKey(artifacts.artifacts().getFirst().artifact().path()));
        assertEquals("DELIVERED", messages.getLast().getStatus().getCode());
    }

    @Test
    void rejectsInvalidBoundsUnavailableArtifactsAndMetadataMismatch() throws Exception {
        var artifacts = writeArtifacts();
        startServer();

        var invalidChunk = assertThrows(StatusRuntimeException.class, () -> deliver(request(artifacts, 0, 100_000)));
        assertEquals(Status.Code.INVALID_ARGUMENT, invalidChunk.getStatus().getCode());

        var tooLarge = assertThrows(StatusRuntimeException.class, () -> deliver(request(artifacts, 32, artifacts.totalBytes() - 1)));
        assertEquals(Status.Code.RESOURCE_EXHAUSTED, tooLarge.getStatus().getCode());

        var wrongChecksum = assertThrows(StatusRuntimeException.class, () -> deliver(request(withWrongChecksum(artifacts), 32, 100_000)));
        assertEquals(Status.Code.FAILED_PRECONDITION, wrongChecksum.getStatus().getCode());

        var unaccepted = assertThrows(StatusRuntimeException.class, () -> deliver(request(artifacts, 32, 100_000).toBuilder()
            .addArtifactReferences("btm/not-accepted.btm")
            .build()));
        assertEquals(Status.Code.FAILED_PRECONDITION, unaccepted.getStatus().getCode());

        Files.delete(tempDir.resolve(artifacts.artifacts().getFirst().artifact().path()));
        var missing = assertThrows(StatusRuntimeException.class, () -> deliver(request(artifacts, 32, 100_000)));
        assertEquals(Status.Code.NOT_FOUND, missing.getStatus().getCode());
    }

    @Test
    void mapsMissingByteAccessReaderFailuresAndChangedSizeDuringStreaming() throws Exception {
        var artifacts = writeArtifacts();
        startServer();

        var missingByteAccess = assertThrows(
            StatusRuntimeException.class,
            () -> deliver(request(artifacts, 32, 100_000).toBuilder()
                .setAcceptedGeneratedArtifacts(0, artifact(artifacts.artifacts().getFirst()).toBuilder().clearByteAccess())
                .build())
        );
        assertEquals(Status.Code.INVALID_ARGUMENT, missingByteAccess.getStatus().getCode());

        stopServer();
        startServer(new BtmArtifactDeliveryApplicationService(new ThrowingReader(
            new BtmArtifactDeliveryException(
                BtmArtifactDeliveryException.Reason.INVALID_REQUEST,
                "invalid"
            )
        )));
        var invalid = assertThrows(StatusRuntimeException.class, () -> deliver(request(artifacts, 32, 100_000)));
        assertEquals(Status.Code.INVALID_ARGUMENT, invalid.getStatus().getCode());

        stopServer();
        startServer(new BtmArtifactDeliveryApplicationService(new ThrowingReader(new IllegalStateException("unexpected"))));
        var unexpected = assertThrows(StatusRuntimeException.class, () -> deliver(request(artifacts, 32, 100_000)));
        assertEquals(Status.Code.FAILED_PRECONDITION, unexpected.getStatus().getCode());

        stopServer();
        startServer(new BtmArtifactDeliveryApplicationService(new ShortStreamReader(artifacts)));
        var changed = assertThrows(StatusRuntimeException.class, () -> deliver(request(artifacts, 32, 100_000)));
        assertEquals(Status.Code.FAILED_PRECONDITION, changed.getStatus().getCode());

        stopServer();
        startServer(new BtmArtifactDeliveryApplicationService(new SameSizeChangedStreamReader(artifacts)));
        var checksumChanged = assertThrows(StatusRuntimeException.class, () -> deliver(request(artifacts, 32, 100_000)));
        assertEquals(Status.Code.FAILED_PRECONDITION, checksumChanged.getStatus().getCode());
    }

    @Test
    void respectsCancelledAndNotReadyServerObservers() {
        var artifacts = writeArtifacts();
        var reader = new CountingReader(new FileSystemBtmArtifactReader(tempDir));
        var endpoint = new BtmArtifactDeliveryGrpcEndpoint(new BtmArtifactDeliveryApplicationService(reader));

        var cancelled = new CapturingServerObserver(true, true);
        endpoint.downloadBtmArtifacts(request(artifacts, 32, 100_000), cancelled);
        assertEquals(0, reader.calls());
        assertTrue(cancelled.messages.isEmpty());
        assertEquals(null, cancelled.error);
        assertEquals(false, cancelled.completed);

        var notReady = new CapturingServerObserver(false, false);
        endpoint.downloadBtmArtifacts(request(artifacts, 32, 100_000), notReady);
        assertEquals(0, reader.calls());
        assertTrue(notReady.messages.isEmpty());
        assertEquals(false, notReady.completed);

        notReady.ready = true;
        notReady.onReady.run();
        assertEquals("DELIVERED", notReady.messages.getLast().getStatus().getCode());
        assertEquals(true, notReady.completed);
    }

    @Test
    void streamsThroughPlainObserverAndMapsPlainObserverFailures() {
        var artifacts = writeArtifacts();
        var endpoint = new BtmArtifactDeliveryGrpcEndpoint(new BtmArtifactDeliveryApplicationService(new FileSystemBtmArtifactReader(tempDir)));
        var observer = new CapturingObserver();

        endpoint.downloadBtmArtifacts(request(artifacts, 32, 100_000), observer);

        assertEquals("DELIVERED", observer.messages.getLast().getStatus().getCode());
        assertEquals(true, observer.completed);
        assertChunkMetadata(observer.messages);

        var throwingEndpoint = new BtmArtifactDeliveryGrpcEndpoint(new BtmArtifactDeliveryApplicationService(new ThrowingReadReader(artifacts)));
        var failed = new CapturingObserver();
        throwingEndpoint.downloadBtmArtifacts(request(artifacts, 32, 100_000), failed);
        assertEquals(Status.Code.FAILED_PRECONDITION, Status.fromThrowable(failed.error).getCode());

        var nullRequest = new CapturingObserver();
        endpoint.downloadBtmArtifacts(null, nullRequest);
        assertEquals(Status.Code.INVALID_ARGUMENT, Status.fromThrowable(nullRequest.error).getCode());
    }

    private void startServer() throws Exception {
        startServer(new BtmArtifactDeliveryApplicationService(new FileSystemBtmArtifactReader(tempDir)));
    }

    private void startServer(BtmArtifactDeliveryApplicationService applicationService) throws Exception {
        var serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(new BtmArtifactDeliveryGrpcEndpoint(applicationService))
            .build()
            .start();
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        stub = BtmArtifactDeliveryServiceGrpc.newBlockingStub(channel);
    }

    private List<BtmArtifactDeliveryMessage> deliver(DownloadBtmArtifactsRequest request) {
        var messages = new ArrayList<BtmArtifactDeliveryMessage>();
        stub.downloadBtmArtifacts(request).forEachRemaining(messages::add);
        return messages;
    }

    private static Map<String, ByteArrayOutputStream> reconstruct(List<BtmArtifactDeliveryMessage> messages) {
        var reconstructed = new LinkedHashMap<String, ByteArrayOutputStream>();
        messages.stream()
            .filter(BtmArtifactDeliveryMessage::hasChunk)
            .forEach(message -> {
                var chunk = message.getChunk();
                assertEquals(sha256(chunk.getData().toByteArray()), chunk.getChunkSha256());
                reconstructed.computeIfAbsent(chunk.getArtifactReference(), ignored -> new ByteArrayOutputStream())
                    .writeBytes(chunk.getData().toByteArray());
            });
        return reconstructed;
    }

    private static void assertChunkMetadata(List<BtmArtifactDeliveryMessage> messages) {
        var manifest = messages.getFirst().getManifest();
        var byArtifact = new LinkedHashMap<String, List<de.burger.forensics.analytics.btmgeneration.v1.BtmArtifactChunk>>();
        messages.stream()
            .filter(BtmArtifactDeliveryMessage::hasChunk)
            .map(BtmArtifactDeliveryMessage::getChunk)
            .forEach(chunk -> byArtifact.computeIfAbsent(chunk.getArtifactReference(), ignored -> new ArrayList<>()).add(chunk));
        for (var descriptor : manifest.getArtifactsList()) {
            var chunks = byArtifact.get(descriptor.getArtifactReference());
            assertEquals(descriptor.getChunkCount(), chunks.size());
            var expectedOffset = 0L;
            for (var index = 0; index < chunks.size(); index++) {
                var chunk = chunks.get(index);
                assertEquals(index, chunk.getChunkIndex());
                assertEquals(expectedOffset, chunk.getByteOffset());
                assertEquals(index == chunks.size() - 1, chunk.getFinalChunk());
                expectedOffset += chunk.getData().size();
            }
            assertEquals(descriptor.getSizeBytes(), expectedOffset);
        }
    }

    private static final class CountingReader implements BtmArtifactReaderPort {
        private final BtmArtifactReaderPort delegate;
        private final AtomicInteger calls = new AtomicInteger();

        private CountingReader(BtmArtifactReaderPort delegate) {
            this.delegate = delegate;
        }

        private int calls() {
            return calls.get();
        }

        @Override
        public StoredBtmArtifactManifest readManifest(
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactReference manifestReference
        ) {
            calls.incrementAndGet();
            return delegate.readManifest(manifestReference);
        }

        @Override
        public void verify(
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactReference artifact
        ) {
            calls.incrementAndGet();
            delegate.verify(artifact);
        }

        @Override
        public InputStream open(ReadableBtmArtifact artifact) {
            calls.incrementAndGet();
            return delegate.open(artifact);
        }
    }

    private GeneratedBtmArtifacts writeArtifacts() {
        return new FileSystemBtmArtifactWriter(tempDir).write(writeRequest());
    }

    private static DownloadBtmArtifactsRequest request(GeneratedBtmArtifacts artifacts, int maxChunkBytes, long maxTotalBytes) {
        var builder = DownloadBtmArtifactsRequest.newBuilder()
            .setRequestId("request-1")
            .setIdempotencyKey("idempotency-1")
            .setSchemaVersion("btm-generation-v1")
            .setCorrelationId("correlation-1")
            .setAnalysisRunId(AnalysisRunId.newBuilder().setValue("run-1"))
            .setAnalysisJobId(AnalysisJobId.newBuilder().setValue("job-1"))
            .setMaxChunkBytes(maxChunkBytes)
            .setMaxTotalBytes(maxTotalBytes);
        artifacts.artifacts().forEach(artifact -> builder.addAcceptedGeneratedArtifacts(artifact(artifact)));
        return builder.build();
    }

    private static GeneratedBtmArtifacts withWrongChecksum(GeneratedBtmArtifacts artifacts) {
        var first = artifacts.artifacts().getFirst();
        var corrupted = new de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactReference(
            new de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ArtifactReference(
                first.artifact().path(),
                first.artifact().type(),
                "b".repeat(64),
                first.artifact().sizeBytes()
            ),
            first.category(),
            first.producerService(),
            first.schemaVersion(),
            first.completeness(),
            first.byteAccess()
        );
        return new GeneratedBtmArtifacts(List.of(corrupted, artifacts.artifacts().get(1)), artifacts.totalBytes());
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
            .setCompleteness(AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE)
            .setByteAccess(ArtifactByteAccess.newBuilder()
                .setOwnerService(PRODUCER_SERVICE)
                .setRetrievalContract(BTM_DELIVERY_CONTRACT)
                .setRetrievalReference(artifact.byteAccess().retrievalReference())
                .setByteCustody(ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_PRODUCER_RETAINED))
            .build();
    }

    private static BtmArtifactWriteRequest writeRequest() {
        var metadata = new RequestMetadata(
            "request-1",
            "idempotency-1",
            "btm-generation-v1",
            "correlation-1",
            new de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisRunId("run-1"),
            new de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisJobId("job-1"),
            new SourceSnapshotId("snapshot-1"),
            "btm-generation-service-test",
            Map.of("tenant", "demo")
        );
        var rule = new GeneratedRule(
            "btm-rule:test",
            new RuleTarget(
                "target-1",
                "fact-1",
                "semantic-1",
                "src/main/java/a/A.java",
                "a.A",
                "run",
                "a.A#run()",
                12,
                ProbeKind.METHOD_ENTRY,
                "source-facts.json",
                "semantic.json",
                0,
                de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisCompleteness.COMPLETE,
                "internal"
            )
        );
        return new BtmArtifactWriteRequest(
            metadata,
            new BtmGenerationPolicy(10, 100_000, 60, "btm-rule-v1", false),
            List.of(rule),
            List.of(),
            new BtmGenerationSummary(1, 1, 0, 1, 1, PRODUCER_SERVICE, "test", "btm-rule-v1"),
            new ReproducibilityMetadata("facts", "policy", "generation", "test", "target_id_probe_kind_ascending"),
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisCompleteness.COMPLETE,
            new TargetSelection(
                "selection-1",
                "analysis-store-service",
                "target-policy-v1",
                "selection-fingerprint",
                de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisCompleteness.COMPLETE,
                "target_id_probe_kind_ascending",
                "correlation-1",
                1
            )
        );
    }

    private static StoredBtmArtifactManifest storedManifest(GeneratedBtmArtifacts artifacts) {
        return new StoredBtmArtifactManifest(
            new de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisRunId("run-1"),
            new de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisJobId("job-1"),
            new SourceSnapshotId("snapshot-1"),
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisCompleteness.COMPLETE,
            List.of(artifacts.artifacts().getFirst().artifact()),
            new ReproducibilityMetadata("facts", "policy", "generation", "test", "target_id_probe_kind_ascending"),
            new TargetSelection(
                "selection-1",
                "analysis-store-service",
                "target-policy-v1",
                "selection-fingerprint",
                de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisCompleteness.COMPLETE,
                "target_id_probe_kind_ascending",
                "correlation-1",
                1
            )
        );
    }

    private record ThrowingReader(RuntimeException error) implements BtmArtifactReaderPort {
        @Override
        public StoredBtmArtifactManifest readManifest(
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactReference manifestReference
        ) {
            throw error;
        }

        @Override
        public void verify(
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactReference artifact
        ) {
        }

        @Override
        public InputStream open(ReadableBtmArtifact artifact) {
            return new ByteArrayInputStream(new byte[0]);
        }
    }

    private record ShortStreamReader(GeneratedBtmArtifacts artifacts) implements BtmArtifactReaderPort {
        @Override
        public StoredBtmArtifactManifest readManifest(
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactReference manifestReference
        ) {
            return storedManifest(artifacts);
        }

        @Override
        public void verify(
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactReference artifact
        ) {
        }

        @Override
        public InputStream open(ReadableBtmArtifact artifact) {
            return new ByteArrayInputStream(new byte[] { 1 });
        }
    }

    private record SameSizeChangedStreamReader(GeneratedBtmArtifacts artifacts) implements BtmArtifactReaderPort {
        @Override
        public StoredBtmArtifactManifest readManifest(
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactReference manifestReference
        ) {
            return storedManifest(artifacts);
        }

        @Override
        public void verify(
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactReference artifact
        ) {
        }

        @Override
        public InputStream open(ReadableBtmArtifact artifact) {
            var changed = new byte[Math.toIntExact(artifact.reference().artifact().sizeBytes())];
            Arrays.fill(changed, (byte) 7);
            return new ByteArrayInputStream(changed);
        }
    }

    private record ThrowingReadReader(GeneratedBtmArtifacts artifacts) implements BtmArtifactReaderPort {
        @Override
        public StoredBtmArtifactManifest readManifest(
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactReference manifestReference
        ) {
            return storedManifest(artifacts);
        }

        @Override
        public void verify(
            de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactReference artifact
        ) {
        }

        @Override
        public InputStream open(ReadableBtmArtifact artifact) {
            return new InputStream() {
                @Override
                public int read() throws java.io.IOException {
                    throw new java.io.IOException("read failed");
                }

                @Override
                public int read(byte[] buffer, int offset, int length) throws java.io.IOException {
                    throw new java.io.IOException("read failed");
                }
            };
        }
    }

    private static final class CapturingObserver implements StreamObserver<BtmArtifactDeliveryMessage> {
        private final List<BtmArtifactDeliveryMessage> messages = new ArrayList<>();
        private Throwable error;
        private boolean completed;

        @Override
        public void onNext(BtmArtifactDeliveryMessage value) {
            messages.add(value);
        }

        @Override
        public void onError(Throwable throwable) {
            error = throwable;
        }

        @Override
        public void onCompleted() {
            completed = true;
        }
    }

    private static final class CapturingServerObserver extends ServerCallStreamObserver<BtmArtifactDeliveryMessage> {
        private final boolean cancelled;
        private boolean ready;
        private final List<BtmArtifactDeliveryMessage> messages = new ArrayList<>();
        private Runnable onReady = () -> {
        };
        private Throwable error;
        private boolean completed;

        private CapturingServerObserver(boolean cancelled, boolean ready) {
            this.cancelled = cancelled;
            this.ready = ready;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void setOnCancelHandler(Runnable onCancelHandler) {
        }

        @Override
        public void setCompression(String compression) {
        }

        @Override
        public boolean isReady() {
            return ready;
        }

        @Override
        public void setOnReadyHandler(Runnable onReadyHandler) {
            onReady = onReadyHandler;
        }

        @Override
        public void disableAutoInboundFlowControl() {
        }

        @Override
        public void request(int count) {
        }

        @Override
        public void setMessageCompression(boolean enable) {
        }

        @Override
        public void onNext(BtmArtifactDeliveryMessage value) {
            messages.add(value);
        }

        @Override
        public void onError(Throwable throwable) {
            error = throwable;
        }

        @Override
        public void onCompleted() {
            completed = true;
        }
    }
}
