package de.burger.forensics.analytics.services.analysisstore.adapter.out.grpc;

import de.burger.forensics.analytics.repositoryanalysis.v1.AnalyzeSourceSnapshotWithJavaAstRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputPackageDescriptor;
import de.burger.forensics.analytics.repositoryanalysis.v1.DiagnosticSeverity;
import de.burger.forensics.analytics.repositoryanalysis.v1.JavaAstHandoffResponse;
import de.burger.forensics.analytics.repositoryanalysis.v1.PrepareRepositoryRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryAnalysisServiceGrpc;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryPreparation;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryReference;
import de.burger.forensics.analytics.repositoryanalysis.v1.RevisionSelector;
import de.burger.forensics.analytics.repositoryanalysis.v1.SourcePackageDescriptor;
import de.burger.forensics.analytics.repositoryanalysis.v1.SourceSnapshotCompleteness;
import de.burger.forensics.analytics.repositoryanalysis.v1.WorkspacePolicy;
import de.burger.forensics.analytics.services.analysisstore.application.port.RepositoryAnalysisWorkerPort;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactCategory;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJobId;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisRunId;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactByteAccess;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactByteCustody;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactReference;
import de.burger.forensics.analytics.services.analysisstore.domain.RepositoryToBtmOrchestrationDomain.StartRepositoryToBtmCommand;
import de.burger.forensics.analytics.services.analysisstore.domain.SourceSnapshotId;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import de.burger.forensics.analytics.services.analysisstore.application.port.RepositoryAnalysisWorkerPort.BuildOutputResolution;
import de.burger.forensics.analytics.services.analysisstore.application.port.RepositoryAnalysisWorkerPort.BuildOutputProducerCandidate;
import de.burger.forensics.analytics.services.analysisstore.application.port.RepositoryAnalysisWorkerPort.PackageAvailability;
import de.burger.forensics.analytics.services.analysisstore.application.port.RepositoryAnalysisWorkerPort.PackageDescriptor;
import de.burger.forensics.analytics.services.analysisstore.application.port.RepositoryAnalysisWorkerPort.RepositoryAnalysisResult;
import de.burger.forensics.analytics.services.analysisstore.application.port.RepositoryAnalysisWorkerPort.SourceRoot;
import de.burger.forensics.analytics.services.analysisstore.application.port.RepositoryAnalysisWorkerPort.WorkerDiagnostic;
import de.burger.forensics.analytics.services.analysisstore.application.port.RepositoryAnalysisWorkerPort.WorkerDiagnosticSeverity;
import de.burger.forensics.analytics.services.analysisstore.application.port.WorkerOwnerApiUnavailableException;

public final class RepositoryAnalysisGrpcClient implements RepositoryAnalysisWorkerPort, AutoCloseable {
    private final ManagedChannel channel;
    private final RepositoryAnalysisServiceGrpc.RepositoryAnalysisServiceBlockingStub stub;
    private final long deadlineSeconds;

    public RepositoryAnalysisGrpcClient(String host, int port, long deadlineSeconds) {
        this(ManagedChannelBuilder
            .forAddress(TrustedPlaintextGrpcTargets.requireTrustedHost(host, "repository analysis gRPC host"), port)
            .usePlaintext()
            .build(), deadlineSeconds);
    }

    RepositoryAnalysisGrpcClient(
        RepositoryAnalysisServiceGrpc.RepositoryAnalysisServiceBlockingStub stub,
        long deadlineSeconds
    ) {
        this.channel = null;
        this.stub = Objects.requireNonNull(stub, "stub must not be null");
        this.deadlineSeconds = requirePositive(deadlineSeconds);
    }

    private RepositoryAnalysisGrpcClient(ManagedChannel channel, long deadlineSeconds) {
        this.channel = Objects.requireNonNull(channel, "channel must not be null");
        this.stub = RepositoryAnalysisServiceGrpc.newBlockingStub(channel);
        this.deadlineSeconds = requirePositive(deadlineSeconds);
    }

    @Override
    public RepositoryAnalysisResult prepareAndAnalyzeJavaAst(
        StartRepositoryToBtmCommand command,
        AnalysisJobId astAnalysisJobId
    ) {
        try {
            var prepared = stub.withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS)
                .prepareRepository(prepareRequest(command))
                .getPreparation();
            var handoff = stub.withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS)
                .analyzeSourceSnapshotWithJavaAst(handoffRequest(command, astAnalysisJobId, prepared));
            return result(command, astAnalysisJobId, prepared, handoff);
        } catch (StatusRuntimeException error) {
            throw new WorkerOwnerApiUnavailableException("Repository Analysis", error.getStatus().getCode().name());
        }
    }

    private static PrepareRepositoryRequest prepareRequest(StartRepositoryToBtmCommand command) {
        return PrepareRepositoryRequest.newBuilder()
            .setRequestId(command.metadata().requestId() + "-repository-prepare")
            .setIdempotencyKey(command.metadata().requestId() + "-repository-prepare")
            .setSchemaVersion(command.metadata().schemaVersion())
            .setCorrelationId(command.metadata().correlationId())
            .setAnalysisRunId(command.metadata().analysisRunId().value())
            .setRepository(RepositoryReference.newBuilder()
                .setRemoteUrl(command.repository().remoteUrl())
                .setProvider(command.repository().provider())
                .putAllSafeAttributes(command.attributes()))
            .setRevision(RevisionSelector.newBuilder()
                .setBranch(command.revision().branch())
                .setBranchRequired(!command.revision().branch().isBlank())
                .setCommit(command.revision().commit())
                .setCommitRequired(!command.revision().commit().isBlank()))
            .setWorkspacePolicy(WorkspacePolicy.newBuilder()
                .setEphemeral(command.workspacePolicy().ephemeral())
                .setAllowShallowClone(command.workspacePolicy().allowShallowClone())
                .setAllowPartialClone(command.workspacePolicy().allowPartialClone())
                .setAllowSparseCheckout(command.workspacePolicy().allowSparseCheckout())
                .setTimeoutSeconds(command.workspacePolicy().timeoutSeconds())
                .setMaxWorkspaceBytes(command.workspacePolicy().maxWorkspaceBytes()))
            .putAllSafeAttributes(command.attributes())
            .build();
    }

    private static AnalyzeSourceSnapshotWithJavaAstRequest handoffRequest(
        StartRepositoryToBtmCommand command,
        AnalysisJobId astAnalysisJobId,
        RepositoryPreparation prepared
    ) {
        return AnalyzeSourceSnapshotWithJavaAstRequest.newBuilder()
            .setRequestId(command.metadata().requestId() + "-java-ast-handoff")
            .setIdempotencyKey(command.metadata().requestId() + "-java-ast-handoff")
            .setSchemaVersion(command.metadata().schemaVersion())
            .setCorrelationId(command.metadata().correlationId())
            .setAnalysisRunId(command.metadata().analysisRunId().value())
            .setAnalysisJobId(astAnalysisJobId.value())
            .setSourceSnapshotId(prepared.getSourceSnapshotId())
            .setHandoffPolicy(de.burger.forensics.analytics.repositoryanalysis.v1.SourceSnapshotHandoffPolicy.newBuilder()
                .setMaxFiles(100_000)
                .setMaxSourceBytes(command.workspacePolicy().maxWorkspaceBytes())
                .setTimeoutSeconds(command.workspacePolicy().timeoutSeconds()))
            .putAllSafeAttributes(command.attributes())
            .build();
    }

    private static RepositoryAnalysisResult result(
        StartRepositoryToBtmCommand command,
        AnalysisJobId astAnalysisJobId,
        RepositoryPreparation prepared,
        JavaAstHandoffResponse handoff
    ) {
        var snapshot = new SourceSnapshotId(prepared.getSourceSnapshotId());
        var sourceRoots = prepared.getSourceSnapshot().getSourceRootsList().stream()
            .map(sourceRoot -> new SourceRoot(sourceRoot.getRelativePath(), sourceRoot.getLanguage()))
            .toList();
        var diagnostics = new java.util.ArrayList<WorkerDiagnostic>();
        prepared.getDiagnosticsList().stream().map(RepositoryAnalysisGrpcClient::diagnostic).forEach(diagnostics::add);
        handoff.getDiagnosticsList().stream().map(RepositoryAnalysisGrpcClient::diagnostic).forEach(diagnostics::add);
        return new RepositoryAnalysisResult(
            command.metadata().analysisRunId(),
            astAnalysisJobId,
            snapshot,
            sourceRoots,
            packageDescriptor(prepared.getSourceSnapshot().getSourcePackage()),
            packageDescriptor(prepared.getSourceSnapshot().getBuildOutputPackage()),
            sourceFactArtifact(handoff.getSourceFactArtifact()),
            completeness(handoff.getCompleteness()),
            diagnostics,
            handoff.getSafeAttributesMap()
        );
    }

    private static PackageDescriptor packageDescriptor(SourcePackageDescriptor descriptor) {
        return new PackageDescriptor(
            availability(descriptor.getAvailability()),
            artifactOrNull(descriptor.getManifestArtifact()),
            descriptor.hasPackageArtifact() ? artifactOrNull(descriptor.getPackageArtifact()) : null,
            descriptor.getSchemaVersion(),
            descriptor.getProducerService(),
            byteAccess(descriptor.getByteAccess()),
            completeness(descriptor.getCompleteness()),
            BuildOutputResolution.empty(),
            ""
        );
    }

    private static PackageDescriptor packageDescriptor(BuildOutputPackageDescriptor descriptor) {
        return new PackageDescriptor(
            availability(descriptor.getAvailability()),
            descriptor.hasManifestArtifact() ? artifactOrNull(descriptor.getManifestArtifact()) : null,
            descriptor.hasPackageArtifact() ? artifactOrNull(descriptor.getPackageArtifact()) : null,
            descriptor.getSchemaVersion(),
            descriptor.getProducerService(),
            byteAccess(descriptor.getByteAccess()),
            completeness(descriptor.getCompleteness()),
            buildOutputResolution(descriptor),
            descriptor.getBuildSystem()
        );
    }

    private static BuildOutputResolution buildOutputResolution(BuildOutputPackageDescriptor descriptor) {
        var resolution = descriptor.getResolution();
        return new BuildOutputResolution(
            resolution.getCandidatesList().stream()
                .map(RepositoryAnalysisGrpcClient::candidate)
                .toList(),
            producer(resolution.getSelectedProducer()),
            resolution.getTerminalIntegrityFailure(),
            resolution.getDiagnosticsList().stream()
                .map(RepositoryAnalysisGrpcClient::diagnostic)
                .toList()
        );
    }

    private static BuildOutputProducerCandidate candidate(
        de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducerCandidate candidate
    ) {
        return new BuildOutputProducerCandidate(
            producer(candidate.getProducer()),
            producerStatus(candidate.getStatus()),
            candidate.getReference(),
            candidate.getDiagnosticsList().stream().map(RepositoryAnalysisGrpcClient::diagnostic).toList()
        );
    }

    private static ArtifactReference artifactOrNull(de.burger.forensics.analytics.repositoryanalysis.v1.ArtifactReference artifact) {
        if (artifact.getReference().isBlank()) {
            return null;
        }
        return new ArtifactReference(
            artifact.getReference(),
            artifact.getType(),
            artifact.getSha256(),
            artifact.getSizeBytes()
        );
    }

    private static AnalysisArtifactReference sourceFactArtifact(
        de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactReference artifact
    ) {
        return new AnalysisArtifactReference(
            new ArtifactReference(
                artifact.getArtifact().getPath(),
                artifact.getArtifact().getType(),
                artifact.getArtifact().getSha256(),
                artifact.getArtifact().getSizeBytes()
            ),
            AnalysisArtifactCategory.STATIC,
            artifact.getProducerService(),
            artifact.getSchemaVersion(),
            completeness(artifact.getCompleteness()),
            byteAccess(artifact.getByteAccess())
        );
    }

    private static ArtifactByteAccess byteAccess(de.burger.forensics.analytics.analysisjob.v1.ArtifactByteAccess access) {
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

    private static AnalysisCompleteness completeness(
        de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness completeness
    ) {
        return switch (completeness) {
            case ANALYSIS_COMPLETENESS_COMPLETE -> AnalysisCompleteness.COMPLETE;
            case ANALYSIS_COMPLETENESS_INCOMPLETE -> AnalysisCompleteness.INCOMPLETE;
            case ANALYSIS_COMPLETENESS_UNKNOWN, ANALYSIS_COMPLETENESS_UNSPECIFIED, UNRECOGNIZED -> AnalysisCompleteness.UNKNOWN;
        };
    }

    private static AnalysisCompleteness completeness(SourceSnapshotCompleteness completeness) {
        return switch (completeness) {
            case SOURCE_SNAPSHOT_COMPLETENESS_COMPLETE -> AnalysisCompleteness.COMPLETE;
            case SOURCE_SNAPSHOT_COMPLETENESS_INCOMPLETE -> AnalysisCompleteness.INCOMPLETE;
            case SOURCE_SNAPSHOT_COMPLETENESS_UNKNOWN, SOURCE_SNAPSHOT_COMPLETENESS_UNSPECIFIED, UNRECOGNIZED ->
                AnalysisCompleteness.UNKNOWN;
        };
    }

    private static WorkerDiagnostic diagnostic(de.burger.forensics.analytics.repositoryanalysis.v1.Diagnostic diagnostic) {
        return new WorkerDiagnostic(
            diagnostic.getCode(),
            diagnostic.getMessage(),
            severity(diagnostic.getSeverity()),
            false,
            diagnostic.getSeverity() == DiagnosticSeverity.DIAGNOSTIC_SEVERITY_ERROR
        );
    }

    private static WorkerDiagnosticSeverity severity(DiagnosticSeverity severity) {
        return switch (severity) {
            case DIAGNOSTIC_SEVERITY_ERROR -> WorkerDiagnosticSeverity.ERROR;
            case DIAGNOSTIC_SEVERITY_WARNING -> WorkerDiagnosticSeverity.WARNING;
            case DIAGNOSTIC_SEVERITY_INFO, DIAGNOSTIC_SEVERITY_UNSPECIFIED, UNRECOGNIZED -> WorkerDiagnosticSeverity.INFO;
        };
    }

    private static PackageAvailability availability(
        de.burger.forensics.analytics.repositoryanalysis.v1.PackageAvailability availability
    ) {
        return switch (availability) {
            case PACKAGE_AVAILABILITY_AVAILABLE -> PackageAvailability.AVAILABLE;
            case PACKAGE_AVAILABILITY_PENDING -> PackageAvailability.PENDING;
            case PACKAGE_AVAILABILITY_UNAVAILABLE -> PackageAvailability.UNAVAILABLE;
            case PACKAGE_AVAILABILITY_FAILED_INTEGRITY -> PackageAvailability.FAILED_INTEGRITY;
            case PACKAGE_AVAILABILITY_UNSPECIFIED, UNRECOGNIZED -> PackageAvailability.UNAVAILABLE;
        };
    }

    private static RepositoryAnalysisWorkerPort.BuildOutputProducer producer(
        de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducer producer
    ) {
        return switch (producer) {
            case BUILD_OUTPUT_PRODUCER_ARTIFACT_STORE -> RepositoryAnalysisWorkerPort.BuildOutputProducer.ARTIFACT_STORE;
            case BUILD_OUTPUT_PRODUCER_ARTIFACTORY -> RepositoryAnalysisWorkerPort.BuildOutputProducer.ARTIFACTORY;
            case BUILD_OUTPUT_PRODUCER_JENKINS -> RepositoryAnalysisWorkerPort.BuildOutputProducer.JENKINS;
            case BUILD_OUTPUT_PRODUCER_BUILD_ARTIFACT_WORKER ->
                RepositoryAnalysisWorkerPort.BuildOutputProducer.BUILD_ARTIFACT_WORKER;
            case BUILD_OUTPUT_PRODUCER_UNSPECIFIED, UNRECOGNIZED -> RepositoryAnalysisWorkerPort.BuildOutputProducer.UNSPECIFIED;
        };
    }

    private static RepositoryAnalysisWorkerPort.BuildOutputProducerStatus producerStatus(
        de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducerStatus status
    ) {
        return switch (status) {
            case BUILD_OUTPUT_PRODUCER_STATUS_AVAILABLE -> RepositoryAnalysisWorkerPort.BuildOutputProducerStatus.AVAILABLE;
            case BUILD_OUTPUT_PRODUCER_STATUS_NOT_CONFIGURED ->
                RepositoryAnalysisWorkerPort.BuildOutputProducerStatus.NOT_CONFIGURED;
            case BUILD_OUTPUT_PRODUCER_STATUS_MISSING -> RepositoryAnalysisWorkerPort.BuildOutputProducerStatus.MISSING;
            case BUILD_OUTPUT_PRODUCER_STATUS_FALLBACK_PLANNED ->
                RepositoryAnalysisWorkerPort.BuildOutputProducerStatus.FALLBACK_PLANNED;
            case BUILD_OUTPUT_PRODUCER_STATUS_TERMINAL_INTEGRITY_FAILURE ->
                RepositoryAnalysisWorkerPort.BuildOutputProducerStatus.TERMINAL_INTEGRITY_FAILURE;
            case BUILD_OUTPUT_PRODUCER_STATUS_UNSPECIFIED, UNRECOGNIZED ->
                RepositoryAnalysisWorkerPort.BuildOutputProducerStatus.MISSING;
        };
    }

    private static long requirePositive(long value) {
        if (value < 1) {
            throw new IllegalArgumentException("deadlineSeconds must be positive");
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
