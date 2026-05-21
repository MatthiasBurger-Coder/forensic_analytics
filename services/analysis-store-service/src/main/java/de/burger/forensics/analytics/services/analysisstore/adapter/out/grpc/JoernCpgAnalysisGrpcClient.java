package de.burger.forensics.analytics.services.analysisstore.adapter.out.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisRunId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind;
import de.burger.forensics.analytics.analysisjob.v1.SourceSnapshotId;
import de.burger.forensics.analytics.joerncpganalysis.v1.AnalyzeJoernCpgRequest;
import de.burger.forensics.analytics.joerncpganalysis.v1.DiagnosticSeverity;
import de.burger.forensics.analytics.joerncpganalysis.v1.JoernCpgAnalysisServiceGrpc;
import de.burger.forensics.analytics.joerncpganalysis.v1.JoernCpgPolicy;
import de.burger.forensics.analytics.joerncpganalysis.v1.JoernMaterializationPolicy;
import de.burger.forensics.analytics.joerncpganalysis.v1.MaterializeJoernWorkspaceRequest;
import de.burger.forensics.analytics.services.analysisstore.application.port.JoernSemanticAnalysisPort;
import de.burger.forensics.analytics.services.analysisstore.application.port.RepositoryAnalysisWorkerPort;
import de.burger.forensics.analytics.services.analysisstore.application.port.RepositoryAnalysisWorkerPort.PackageDescriptor;
import de.burger.forensics.analytics.services.analysisstore.application.port.RepositoryAnalysisWorkerPort.RepositoryAnalysisResult;
import de.burger.forensics.analytics.services.analysisstore.application.port.RepositoryAnalysisWorkerPort.WorkerDiagnostic;
import de.burger.forensics.analytics.services.analysisstore.application.port.RepositoryAnalysisWorkerPort.WorkerDiagnosticSeverity;
import de.burger.forensics.analytics.services.analysisstore.application.port.WorkerOwnerApiUnavailableException;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactByteAccess;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactByteCustody;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactReference;
import de.burger.forensics.analytics.services.analysisstore.domain.RepositoryToBtmOrchestrationDomain.StartRepositoryToBtmCommand;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class JoernCpgAnalysisGrpcClient implements JoernSemanticAnalysisPort, AutoCloseable {
    private final ManagedChannel channel;
    private final JoernCpgAnalysisServiceGrpc.JoernCpgAnalysisServiceBlockingStub stub;
    private final long deadlineSeconds;
    private final String joernImageReference;
    private final String queryBundleVersion;

    public JoernCpgAnalysisGrpcClient(
        String host,
        int port,
        long deadlineSeconds,
        String joernImageReference,
        String queryBundleVersion
    ) {
        this(
            ManagedChannelBuilder
                .forAddress(TrustedPlaintextGrpcTargets.requireTrustedHost(host, "Joern CPG gRPC host"), port)
                .usePlaintext()
                .build(),
            deadlineSeconds,
            joernImageReference,
            queryBundleVersion
        );
    }

    JoernCpgAnalysisGrpcClient(
        JoernCpgAnalysisServiceGrpc.JoernCpgAnalysisServiceBlockingStub stub,
        long deadlineSeconds,
        String joernImageReference,
        String queryBundleVersion
    ) {
        this.channel = null;
        this.stub = Objects.requireNonNull(stub, "stub must not be null");
        this.deadlineSeconds = requirePositive(deadlineSeconds);
        this.joernImageReference = requireText(joernImageReference, "joernImageReference");
        this.queryBundleVersion = requireText(queryBundleVersion, "queryBundleVersion");
    }

    private JoernCpgAnalysisGrpcClient(
        ManagedChannel channel,
        long deadlineSeconds,
        String joernImageReference,
        String queryBundleVersion
    ) {
        this.channel = Objects.requireNonNull(channel, "channel must not be null");
        this.stub = JoernCpgAnalysisServiceGrpc.newBlockingStub(channel);
        this.deadlineSeconds = requirePositive(deadlineSeconds);
        this.joernImageReference = requireText(joernImageReference, "joernImageReference");
        this.queryBundleVersion = requireText(queryBundleVersion, "queryBundleVersion");
    }

    @Override
    public JoernAnalysisResult analyze(
        StartRepositoryToBtmCommand command,
        de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJobId joernAnalysisJobId,
        RepositoryAnalysisResult repositoryAnalysis
    ) {
        try {
            var materialized = stub.withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS)
                .materializeSourceSnapshot(materializeRequest(command, joernAnalysisJobId, repositoryAnalysis))
                .getWorkspace();
            var analyzed = stub.withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS)
                .analyzeSourceSnapshot(analyzeRequest(command, joernAnalysisJobId, repositoryAnalysis, materialized));
            return new JoernAnalysisResult(
                command.metadata().analysisRunId(),
                joernAnalysisJobId,
                repositoryAnalysis.sourceSnapshotId(),
                completeness(analyzed.getCompleteness()),
                analyzed.getSemanticArtifactsList().stream()
                    .map(JoernCpgAnalysisGrpcClient::artifact)
                    .toList(),
                analyzed.getDiagnosticsList().stream()
                    .map(JoernCpgAnalysisGrpcClient::diagnostic)
                    .toList(),
                analyzed.getSafeAttributesMap()
            );
        } catch (StatusRuntimeException error) {
            throw new WorkerOwnerApiUnavailableException("Joern CPG", error.getStatus().getCode().name());
        }
    }

    private MaterializeJoernWorkspaceRequest materializeRequest(
        StartRepositoryToBtmCommand command,
        de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJobId joernAnalysisJobId,
        RepositoryAnalysisResult repositoryAnalysis
    ) {
        var builder = MaterializeJoernWorkspaceRequest.newBuilder()
            .setRequestId(command.metadata().requestId() + "-joern-materialize")
            .setIdempotencyKey(command.metadata().requestId() + "-joern-materialize")
            .setSchemaVersion(command.metadata().schemaVersion())
            .setCorrelationId(command.metadata().correlationId())
            .setAnalysisRunId(AnalysisRunId.newBuilder().setValue(command.metadata().analysisRunId().value()))
            .setAnalysisJobId(de.burger.forensics.analytics.analysisjob.v1.AnalysisJobId.newBuilder()
                .setValue(joernAnalysisJobId.value()))
            .setSourceSnapshotId(SourceSnapshotId.newBuilder().setValue(repositoryAnalysis.sourceSnapshotId().value()))
            .setSourcePackage(sourcePackage(repositoryAnalysis.sourcePackage()))
            .setBuildOutputPackage(buildOutputPackage(repositoryAnalysis.buildOutputPackage()))
            .setPolicy(JoernMaterializationPolicy.newBuilder()
                .setMaxSourceRoots(repositoryAnalysis.sourceRoots().size())
                .setMaxWorkspaceBytes(command.workspacePolicy().maxWorkspaceBytes())
                .setMaxArtifactBytes(107_374_182_400L)
                .setMaxArchiveDepth(32)
                .setRejectSymlinks(true)
                .setRejectHardlinks(true)
                .setRejectDeviceFiles(true)
                .setRejectDuplicatePaths(true))
            .putAllSafeAttributes(command.attributes());
        repositoryAnalysis.sourceRoots().forEach(sourceRoot -> builder.addSourceRoots(
            de.burger.forensics.analytics.joerncpganalysis.v1.SourceRoot.newBuilder()
                .setRelativePath(sourceRoot.relativePath())
                .setLanguage(sourceRoot.language())
        ));
        return builder.build();
    }

    private AnalyzeJoernCpgRequest analyzeRequest(
        StartRepositoryToBtmCommand command,
        de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJobId joernAnalysisJobId,
        RepositoryAnalysisResult repositoryAnalysis,
        de.burger.forensics.analytics.joerncpganalysis.v1.SourceWorkspace workspace
    ) {
        return AnalyzeJoernCpgRequest.newBuilder()
            .setRequestId(command.metadata().requestId() + "-joern-analyze")
            .setIdempotencyKey(command.metadata().requestId() + "-joern-analyze")
            .setSchemaVersion(command.metadata().schemaVersion())
            .setCorrelationId(command.metadata().correlationId())
            .setWorkerKind(AnalysisWorkerKind.ANALYSIS_WORKER_KIND_JOERN_ANALYSIS)
            .setAnalysisRunId(AnalysisRunId.newBuilder().setValue(command.metadata().analysisRunId().value()))
            .setAnalysisJobId(de.burger.forensics.analytics.analysisjob.v1.AnalysisJobId.newBuilder()
                .setValue(joernAnalysisJobId.value()))
            .setSourceSnapshotId(SourceSnapshotId.newBuilder().setValue(repositoryAnalysis.sourceSnapshotId().value()))
            .setWorkerVersion("joern-cpg-analysis-v1")
            .setPolicy(JoernCpgPolicy.newBuilder()
                .setMaxSourceRoots(repositoryAnalysis.sourceRoots().size())
                .setMaxWorkspaceBytes(command.workspacePolicy().maxWorkspaceBytes())
                .setMaxArtifactBytes(107_374_182_400L)
                .setTimeoutSeconds(command.workspacePolicy().timeoutSeconds())
                .setJoernImageReference(joernImageReference)
                .setQueryBundleVersion(queryBundleVersion)
                .setRequireCallgraph(true)
                .setRequireControlflow(true)
                .setRequireDataflow(false))
            .setWorkspace(workspace)
            .putAllSafeAttributes(command.attributes())
            .build();
    }

    private static de.burger.forensics.analytics.repositoryanalysis.v1.SourcePackageDescriptor sourcePackage(
        PackageDescriptor descriptor
    ) {
        var builder = de.burger.forensics.analytics.repositoryanalysis.v1.SourcePackageDescriptor.newBuilder()
            .setAvailability(availability(descriptor.availability()))
            .setSchemaVersion(descriptor.schemaVersion())
            .setProducerService(descriptor.producerService())
            .setByteAccess(byteAccess(descriptor.byteAccess()))
            .setCompleteness(sourceCompleteness(descriptor.completeness()));
        if (descriptor.manifestArtifact() != null) {
            builder.setManifestArtifact(repositoryArtifact(descriptor.manifestArtifact()));
        }
        if (descriptor.packageArtifact() != null) {
            builder.setPackageArtifact(repositoryArtifact(descriptor.packageArtifact()));
        }
        return builder.build();
    }

    private static de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputPackageDescriptor buildOutputPackage(
        PackageDescriptor descriptor
    ) {
        var resolution = descriptor.buildOutputResolution();
        var resolutionBuilder = de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputResolution.newBuilder()
            .setSelectedProducer(buildOutputProducer(resolution.selectedProducer()))
            .setTerminalIntegrityFailure(resolution.terminalIntegrityFailure());
        resolution.candidates().forEach(candidate -> resolutionBuilder.addCandidates(
            de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducerCandidate.newBuilder()
                .setProducer(buildOutputProducer(candidate.producer()))
                .setStatus(buildOutputProducerStatus(candidate.status()))
                .setReference(candidate.reference())
        ));
        return de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputPackageDescriptor.newBuilder()
            .setAvailability(availability(descriptor.availability()))
            .setSchemaVersion(descriptor.schemaVersion())
            .setProducerService(descriptor.producerService())
            .setByteAccess(byteAccess(descriptor.byteAccess()))
            .setCompleteness(sourceCompleteness(descriptor.completeness()))
            .setResolution(resolutionBuilder)
            .setBuildSystem(descriptor.buildSystem())
            .setManifestArtifact(repositoryArtifact(descriptor.manifestArtifact()))
            .setPackageArtifact(repositoryArtifact(descriptor.packageArtifact()))
            .build();
    }

    private static de.burger.forensics.analytics.repositoryanalysis.v1.ArtifactReference repositoryArtifact(
        ArtifactReference artifact
    ) {
        return de.burger.forensics.analytics.repositoryanalysis.v1.ArtifactReference.newBuilder()
            .setReference(artifact.path())
            .setType(artifact.type())
            .setSha256(artifact.sha256())
            .setSizeBytes(artifact.sizeBytes())
            .build();
    }

    private static de.burger.forensics.analytics.analysisjob.v1.ArtifactByteAccess byteAccess(ArtifactByteAccess access) {
        return de.burger.forensics.analytics.analysisjob.v1.ArtifactByteAccess.newBuilder()
            .setOwnerService(access.ownerService())
            .setRetrievalContract(access.retrievalContract())
            .setRetrievalReference(access.retrievalReference())
            .setByteCustody(byteCustody(access.byteCustody()))
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
            de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactCategory.STATIC,
            artifact.getProducerService(),
            artifact.getSchemaVersion(),
            completeness(artifact.getCompleteness()),
            artifactByteAccess(artifact.getByteAccess())
        );
    }

    private static WorkerDiagnostic diagnostic(
        de.burger.forensics.analytics.joerncpganalysis.v1.JoernCpgDiagnostic diagnostic
    ) {
        return new WorkerDiagnostic(
            diagnostic.getCode(),
            diagnostic.getMessage(),
            severity(diagnostic.getSeverity()),
            diagnostic.getRetryable(),
            diagnostic.getAffectsCompleteness()
        );
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

    private static de.burger.forensics.analytics.repositoryanalysis.v1.PackageAvailability availability(
        RepositoryAnalysisWorkerPort.PackageAvailability availability
    ) {
        return switch (availability) {
            case AVAILABLE -> de.burger.forensics.analytics.repositoryanalysis.v1.PackageAvailability.PACKAGE_AVAILABILITY_AVAILABLE;
            case PENDING -> de.burger.forensics.analytics.repositoryanalysis.v1.PackageAvailability.PACKAGE_AVAILABILITY_PENDING;
            case UNAVAILABLE ->
                de.burger.forensics.analytics.repositoryanalysis.v1.PackageAvailability.PACKAGE_AVAILABILITY_UNAVAILABLE;
            case FAILED_INTEGRITY ->
                de.burger.forensics.analytics.repositoryanalysis.v1.PackageAvailability.PACKAGE_AVAILABILITY_FAILED_INTEGRITY;
        };
    }

    private static de.burger.forensics.analytics.repositoryanalysis.v1.SourceSnapshotCompleteness sourceCompleteness(
        AnalysisCompleteness completeness
    ) {
        return switch (completeness) {
            case COMPLETE -> de.burger.forensics.analytics.repositoryanalysis.v1.SourceSnapshotCompleteness.SOURCE_SNAPSHOT_COMPLETENESS_COMPLETE;
            case INCOMPLETE ->
                de.burger.forensics.analytics.repositoryanalysis.v1.SourceSnapshotCompleteness.SOURCE_SNAPSHOT_COMPLETENESS_INCOMPLETE;
            case UNKNOWN -> de.burger.forensics.analytics.repositoryanalysis.v1.SourceSnapshotCompleteness.SOURCE_SNAPSHOT_COMPLETENESS_UNKNOWN;
        };
    }

    private static de.burger.forensics.analytics.services.analysisstore.domain.AnalysisCompleteness completeness(
        de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness completeness
    ) {
        return switch (completeness) {
            case ANALYSIS_COMPLETENESS_COMPLETE ->
                de.burger.forensics.analytics.services.analysisstore.domain.AnalysisCompleteness.COMPLETE;
            case ANALYSIS_COMPLETENESS_INCOMPLETE ->
                de.burger.forensics.analytics.services.analysisstore.domain.AnalysisCompleteness.INCOMPLETE;
            case ANALYSIS_COMPLETENESS_UNKNOWN, ANALYSIS_COMPLETENESS_UNSPECIFIED, UNRECOGNIZED ->
                de.burger.forensics.analytics.services.analysisstore.domain.AnalysisCompleteness.UNKNOWN;
        };
    }

    private static WorkerDiagnosticSeverity severity(DiagnosticSeverity severity) {
        return switch (severity) {
            case DIAGNOSTIC_SEVERITY_ERROR -> WorkerDiagnosticSeverity.ERROR;
            case DIAGNOSTIC_SEVERITY_WARNING -> WorkerDiagnosticSeverity.WARNING;
            case DIAGNOSTIC_SEVERITY_INFO, DIAGNOSTIC_SEVERITY_UNSPECIFIED, UNRECOGNIZED -> WorkerDiagnosticSeverity.INFO;
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

    private static de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducer buildOutputProducer(
        RepositoryAnalysisWorkerPort.BuildOutputProducer producer
    ) {
        return switch (producer) {
            case ARTIFACT_STORE ->
                de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducer.BUILD_OUTPUT_PRODUCER_ARTIFACT_STORE;
            case ARTIFACTORY ->
                de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducer.BUILD_OUTPUT_PRODUCER_ARTIFACTORY;
            case JENKINS -> de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducer.BUILD_OUTPUT_PRODUCER_JENKINS;
            case BUILD_ARTIFACT_WORKER ->
                de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducer.BUILD_OUTPUT_PRODUCER_BUILD_ARTIFACT_WORKER;
            case UNSPECIFIED ->
                de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducer.BUILD_OUTPUT_PRODUCER_UNSPECIFIED;
        };
    }

    private static de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducerStatus buildOutputProducerStatus(
        RepositoryAnalysisWorkerPort.BuildOutputProducerStatus status
    ) {
        return switch (status) {
            case AVAILABLE ->
                de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducerStatus.BUILD_OUTPUT_PRODUCER_STATUS_AVAILABLE;
            case NOT_CONFIGURED ->
                de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducerStatus.BUILD_OUTPUT_PRODUCER_STATUS_NOT_CONFIGURED;
            case MISSING ->
                de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducerStatus.BUILD_OUTPUT_PRODUCER_STATUS_MISSING;
            case FALLBACK_PLANNED ->
                de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducerStatus.BUILD_OUTPUT_PRODUCER_STATUS_FALLBACK_PLANNED;
            case TERMINAL_INTEGRITY_FAILURE ->
                de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducerStatus.BUILD_OUTPUT_PRODUCER_STATUS_TERMINAL_INTEGRITY_FAILURE;
        };
    }

    private static long requirePositive(long value) {
        if (value < 1) {
            throw new IllegalArgumentException("deadlineSeconds must be positive");
        }
        return value;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
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
