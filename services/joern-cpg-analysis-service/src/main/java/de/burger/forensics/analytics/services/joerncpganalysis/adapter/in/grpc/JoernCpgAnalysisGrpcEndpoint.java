package de.burger.forensics.analytics.services.joerncpganalysis.adapter.in.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactCategory;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisArtifactReference;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisCompleteness;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisJobId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisRunId;
import de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind;
import de.burger.forensics.analytics.analysisjob.v1.ArtifactReference;
import de.burger.forensics.analytics.analysisjob.v1.SourceSnapshotId;
import de.burger.forensics.analytics.joerncpganalysis.v1.AnalyzeJoernCpgRequest;
import de.burger.forensics.analytics.joerncpganalysis.v1.AnalyzeJoernCpgResponse;
import de.burger.forensics.analytics.joerncpganalysis.v1.DiagnosticSeverity;
import de.burger.forensics.analytics.joerncpganalysis.v1.JoernMaterializationPolicy;
import de.burger.forensics.analytics.joerncpganalysis.v1.JoernCpgAnalysisServiceGrpc;
import de.burger.forensics.analytics.joerncpganalysis.v1.JoernCpgDiagnostic;
import de.burger.forensics.analytics.joerncpganalysis.v1.JoernCpgPolicy;
import de.burger.forensics.analytics.joerncpganalysis.v1.JoernCpgSummary;
import de.burger.forensics.analytics.joerncpganalysis.v1.MaterializeJoernWorkspaceRequest;
import de.burger.forensics.analytics.joerncpganalysis.v1.MaterializeJoernWorkspaceResponse;
import de.burger.forensics.analytics.joerncpganalysis.v1.OperationStatus;
import de.burger.forensics.analytics.joerncpganalysis.v1.SourceRoot;
import de.burger.forensics.analytics.joerncpganalysis.v1.SourceWorkspace;
import de.burger.forensics.analytics.services.joerncpganalysis.application.JoernCpgAnalysisApplicationService;
import de.burger.forensics.analytics.services.joerncpganalysis.application.JoernCpgAnalysisTimeoutException;
import de.burger.forensics.analytics.services.joerncpganalysis.application.JoernCpgArtifactException;
import de.burger.forensics.analytics.services.joerncpganalysis.application.JoernRuntimeUnavailableException;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgCommand;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalyzeJoernCpgResult;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.MaterializationMetadata;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.MaterializedPackageDescriptor;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.MaterializeJoernWorkspaceCommand;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.MaterializeJoernWorkspaceResult;
import de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.RequestMetadata;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Objects;

import static de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.requireText;

public final class JoernCpgAnalysisGrpcEndpoint extends JoernCpgAnalysisServiceGrpc.JoernCpgAnalysisServiceImplBase {
    private static final Map<de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody, de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.ArtifactByteCustody> BYTE_CUSTODIES = Map.of(
        de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_PRODUCER_RETAINED,
        de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.ArtifactByteCustody.PRODUCER_RETAINED,
        de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_SCOPED_OBJECT_ACCESS,
        de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.ArtifactByteCustody.SCOPED_OBJECT_ACCESS,
        de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_EXPLICIT_HANDOFF,
        de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.ArtifactByteCustody.EXPLICIT_HANDOFF
    );
    private static final Map<de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.ArtifactByteCustody, de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody> BYTE_CUSTODY_PROTOS = Map.of(
        de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.ArtifactByteCustody.PRODUCER_RETAINED,
        de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_PRODUCER_RETAINED,
        de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.ArtifactByteCustody.SCOPED_OBJECT_ACCESS,
        de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_SCOPED_OBJECT_ACCESS,
        de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.ArtifactByteCustody.EXPLICIT_HANDOFF,
        de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_EXPLICIT_HANDOFF
    );

    private final JoernCpgAnalysisApplicationService applicationService;

    public JoernCpgAnalysisGrpcEndpoint(JoernCpgAnalysisApplicationService applicationService) {
        this.applicationService = Objects.requireNonNull(applicationService, "application service must not be null");
    }

    @Override
    public void materializeSourceSnapshot(
        MaterializeJoernWorkspaceRequest request,
        StreamObserver<MaterializeJoernWorkspaceResponse> responseObserver
    ) {
        try {
            requireMaterializationRequest(request);
            var result = applicationService.materialize(materializeCommand(request));
            responseObserver.onNext(materializationResponse(result));
            responseObserver.onCompleted();
        } catch (RuntimeException error) {
            responseObserver.onError(status(error).asRuntimeException());
        }
    }

    @Override
    public void analyzeSourceSnapshot(
        AnalyzeJoernCpgRequest request,
        StreamObserver<AnalyzeJoernCpgResponse> responseObserver
    ) {
        try {
            requireJoernWorker(request);
            var result = applicationService.analyze(command(request));
            responseObserver.onNext(response(result));
            responseObserver.onCompleted();
        } catch (RuntimeException error) {
            responseObserver.onError(status(error).asRuntimeException());
        }
    }

    private static void requireMaterializationRequest(MaterializeJoernWorkspaceRequest request) {
        requireText(request.getRequestId(), "request id");
        if (!request.hasAnalysisRunId() || !request.hasAnalysisJobId() || !request.hasSourceSnapshotId()) {
            throw new IllegalArgumentException("analysis run, job and source snapshot ids are required");
        }
        if (request.getSourceRootsList().isEmpty()) {
            throw new IllegalArgumentException("source roots are required");
        }
        if (!request.hasSourcePackage()) {
            throw new IllegalArgumentException("source package descriptor is required");
        }
        if (!request.hasBuildOutputPackage()) {
            throw new IllegalArgumentException("build-output package descriptor is required");
        }
        if (!request.hasPolicy()) {
            throw new IllegalArgumentException("Joern materialization policy is required");
        }
    }

    private static void requireJoernWorker(AnalyzeJoernCpgRequest request) {
        requireText(request.getRequestId(), "request id");
        if (request.getWorkerKind() != AnalysisWorkerKind.ANALYSIS_WORKER_KIND_JOERN_ANALYSIS) {
            throw new IllegalArgumentException("worker kind must be JOERN_ANALYSIS");
        }
        if (!request.hasPolicy()) {
            throw new IllegalArgumentException("Joern CPG policy is required");
        }
        if (!request.hasWorkspace()) {
            throw new IllegalArgumentException("source workspace is required");
        }
    }

    private static MaterializeJoernWorkspaceCommand materializeCommand(MaterializeJoernWorkspaceRequest request) {
        return new MaterializeJoernWorkspaceCommand(
            new MaterializationMetadata(
                request.getRequestId(),
                request.getIdempotencyKey(),
                request.getSchemaVersion(),
                request.getCorrelationId(),
                new de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisRunId(
                    request.getAnalysisRunId().getValue()
                ),
                new de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisJobId(
                    request.getAnalysisJobId().getValue()
                ),
                new de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.SourceSnapshotId(
                    request.getSourceSnapshotId().getValue()
                ),
                request.getSafeAttributesMap()
            ),
            request.getSourceRootsList().stream().map(JoernCpgAnalysisGrpcEndpoint::sourceRoot).toList(),
            sourcePackage(request.getSourcePackage()),
            buildOutputPackage(request.getBuildOutputPackage()),
            materializationPolicy(request.getPolicy())
        );
    }

    private static AnalyzeJoernCpgCommand command(AnalyzeJoernCpgRequest request) {
        return new AnalyzeJoernCpgCommand(
            new RequestMetadata(
                request.getRequestId(),
                request.getIdempotencyKey(),
                request.getSchemaVersion(),
                request.getCorrelationId(),
                new de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisRunId(
                    request.getAnalysisRunId().getValue()
                ),
                new de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisJobId(
                    request.getAnalysisJobId().getValue()
                ),
                new de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.SourceSnapshotId(
                    request.getSourceSnapshotId().getValue()
                ),
                request.getWorkerVersion(),
                request.getSafeAttributesMap()
            ),
            policy(request.getPolicy()),
            workspace(request.getWorkspace())
        );
    }

    private static de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.JoernMaterializationPolicy materializationPolicy(
        JoernMaterializationPolicy policy
    ) {
        return new de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.JoernMaterializationPolicy(
            policy.getMaxSourceRoots(),
            policy.getMaxWorkspaceBytes(),
            policy.getMaxArtifactBytes(),
            policy.getMaxArchiveDepth(),
            policy.getRejectSymlinks(),
            policy.getRejectHardlinks(),
            policy.getRejectDeviceFiles(),
            policy.getRejectDuplicatePaths()
        );
    }

    private static de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.JoernCpgPolicy policy(
        JoernCpgPolicy policy
    ) {
        return new de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.JoernCpgPolicy(
            policy.getMaxSourceRoots(),
            policy.getMaxWorkspaceBytes(),
            policy.getMaxArtifactBytes(),
            policy.getTimeoutSeconds(),
            policy.getJoernImageReference(),
            policy.getQueryBundleVersion(),
            policy.getRequireCallgraph(),
            policy.getRequireControlflow(),
            policy.getRequireDataflow()
        );
    }

    private static MaterializedPackageDescriptor sourcePackage(
        de.burger.forensics.analytics.repositoryanalysis.v1.SourcePackageDescriptor descriptor
    ) {
        if (!descriptor.hasPackageArtifact()) {
            throw new IllegalArgumentException("source package artifact is required");
        }
        return new MaterializedPackageDescriptor(
            "source package",
            availability(descriptor.getAvailability()),
            artifact(descriptor.getManifestArtifact()),
            artifact(descriptor.getPackageArtifact()),
            descriptor.getProducerService(),
            descriptor.getSchemaVersion(),
            completeness(descriptor.getCompleteness()),
            byteAccess(descriptor.getByteAccess())
        );
    }

    private static MaterializedPackageDescriptor buildOutputPackage(
        de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputPackageDescriptor descriptor
    ) {
        if (!descriptor.hasPackageArtifact()) {
            throw new IllegalArgumentException("build-output package artifact is required");
        }
        return new MaterializedPackageDescriptor(
            "build-output package",
            availability(descriptor.getAvailability()),
            artifact(descriptor.getManifestArtifact()),
            artifact(descriptor.getPackageArtifact()),
            descriptor.getProducerService(),
            descriptor.getSchemaVersion(),
            completeness(descriptor.getCompleteness()),
            byteAccess(descriptor.getByteAccess())
        );
    }

    private static de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.ArtifactReference artifact(
        de.burger.forensics.analytics.repositoryanalysis.v1.ArtifactReference artifact
    ) {
        return new de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.ArtifactReference(
            artifact.getReference(),
            artifact.getType(),
            artifact.getSha256(),
            artifact.getSizeBytes()
        );
    }

    private static de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.SourceWorkspace workspace(
        SourceWorkspace workspace
    ) {
        return new de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.SourceWorkspace(
            workspace.getWorkspaceId(),
            workspace.getSourceRootsList().stream().map(JoernCpgAnalysisGrpcEndpoint::sourceRoot).toList(),
            workspace.getInputArtifactsList().stream().map(JoernCpgAnalysisGrpcEndpoint::artifact).toList()
        );
    }

    private static de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.SourceRoot sourceRoot(
        SourceRoot sourceRoot
    ) {
        return new de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.SourceRoot(
            sourceRoot.getRelativePath(),
            sourceRoot.getLanguage()
        );
    }

    private static SourceRoot sourceRoot(
        de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.SourceRoot sourceRoot
    ) {
        return SourceRoot.newBuilder()
            .setRelativePath(sourceRoot.relativePath())
            .setLanguage(sourceRoot.language())
            .build();
    }

    private static de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisArtifactReference artifact(
        AnalysisArtifactReference reference
    ) {
        var artifact = reference.getArtifact();
        return new de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisArtifactReference(
            new de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.ArtifactReference(
                artifact.getPath(),
                artifact.getType(),
                artifact.getSha256(),
                artifact.getSizeBytes()
            ),
            category(reference.getCategory()),
            reference.getProducerService(),
            reference.getSchemaVersion(),
            completeness(reference.getCompleteness()),
            byteAccess(reference.getByteAccess())
        );
    }

    private static MaterializeJoernWorkspaceResponse materializationResponse(MaterializeJoernWorkspaceResult result) {
        var builder = MaterializeJoernWorkspaceResponse.newBuilder()
            .setStatus(status("MATERIALIZED", "Joern-owned workspace materialization accepted", result))
            .setAnalysisRunId(AnalysisRunId.newBuilder().setValue(result.metadata().analysisRunId().value()))
            .setAnalysisJobId(AnalysisJobId.newBuilder().setValue(result.metadata().analysisJobId().value()))
            .setSourceSnapshotId(SourceSnapshotId.newBuilder().setValue(result.metadata().sourceSnapshotId().value()))
            .setWorkspace(workspace(result.workspace()))
            .putAllSafeAttributes(result.metadata().safeAttributes());
        result.diagnostics().forEach(diagnostic -> builder.addDiagnostics(diagnostic(diagnostic)));
        return builder.build();
    }

    private static AnalyzeJoernCpgResponse response(AnalyzeJoernCpgResult result) {
        var builder = AnalyzeJoernCpgResponse.newBuilder()
            .setStatus(status("ANALYZED", "Joern CPG analysis completed", result))
            .setAnalysisRunId(AnalysisRunId.newBuilder().setValue(result.metadata().analysisRunId().value()))
            .setAnalysisJobId(AnalysisJobId.newBuilder().setValue(result.metadata().analysisJobId().value()))
            .setSourceSnapshotId(SourceSnapshotId.newBuilder().setValue(result.metadata().sourceSnapshotId().value()))
            .setCompleteness(completeness(result.completeness()))
            .setSummary(summary(result.summary()))
            .putAllSafeAttributes(result.metadata().safeAttributes());
        result.semanticArtifacts().forEach(artifact -> builder.addSemanticArtifacts(artifact(artifact)));
        result.diagnostics().forEach(diagnostic -> builder.addDiagnostics(diagnostic(diagnostic)));
        return builder.build();
    }

    private static OperationStatus status(String code, String message, MaterializeJoernWorkspaceResult result) {
        var builder = OperationStatus.newBuilder()
            .setCode(code)
            .setMessage(message)
            .setRetryable(false)
            .setCorrelationId(result.metadata().correlationId());
        result.diagnostics().forEach(diagnostic -> builder.addDiagnostics(diagnostic(diagnostic)));
        return builder.build();
    }

    private static OperationStatus status(String code, String message, AnalyzeJoernCpgResult result) {
        var builder = OperationStatus.newBuilder()
            .setCode(result.completeness()
                == de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisCompleteness.COMPLETE
                    ? code
                    : "ANALYZED_INCOMPLETE")
            .setMessage(message)
            .setRetryable(false)
            .setCorrelationId(result.metadata().correlationId());
        result.diagnostics().forEach(diagnostic -> builder.addDiagnostics(diagnostic(diagnostic)));
        return builder.build();
    }

    private static SourceWorkspace workspace(
        de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.SourceWorkspace workspace
    ) {
        var builder = SourceWorkspace.newBuilder()
            .setWorkspaceId(workspace.workspaceId());
        workspace.sourceRoots().forEach(sourceRoot -> builder.addSourceRoots(sourceRoot(sourceRoot)));
        workspace.inputArtifacts().forEach(artifact -> builder.addInputArtifacts(artifact(artifact)));
        return builder.build();
    }

    private static AnalysisArtifactReference artifact(
        de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisArtifactReference reference
    ) {
        return AnalysisArtifactReference.newBuilder()
            .setArtifact(ArtifactReference.newBuilder()
                .setPath(reference.artifact().path())
                .setType(reference.artifact().type())
                .setSha256(reference.artifact().sha256())
                .setSizeBytes(reference.artifact().sizeBytes()))
            .setCategory(AnalysisArtifactCategory.ANALYSIS_ARTIFACT_CATEGORY_STATIC)
            .setProducerService(reference.producerService())
            .setSchemaVersion(reference.schemaVersion())
            .setCompleteness(completeness(reference.completeness()))
            .setByteAccess(byteAccess(reference.byteAccess()))
            .build();
    }

    private static de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisArtifactCategory category(
        AnalysisArtifactCategory category
    ) {
        return switch (category) {
            case ANALYSIS_ARTIFACT_CATEGORY_STATIC ->
                de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisArtifactCategory.STATIC;
            case ANALYSIS_ARTIFACT_CATEGORY_RUNTIME,
                 ANALYSIS_ARTIFACT_CATEGORY_PROJECTION,
                 ANALYSIS_ARTIFACT_CATEGORY_GENERATED,
                 ANALYSIS_ARTIFACT_CATEGORY_UNSPECIFIED,
                 UNRECOGNIZED -> throw new IllegalArgumentException("Joern input artifacts must be static semantic artifacts");
        };
    }

    private static de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.ArtifactByteAccess byteAccess(
        de.burger.forensics.analytics.analysisjob.v1.ArtifactByteAccess byteAccess
    ) {
        return new de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.ArtifactByteAccess(
            byteAccess.getOwnerService(),
            byteAccess.getRetrievalContract(),
            byteAccess.getRetrievalReference(),
            required(BYTE_CUSTODIES.get(byteAccess.getByteCustody()), "artifact byte custody must be specified")
        );
    }

    private static de.burger.forensics.analytics.analysisjob.v1.ArtifactByteAccess byteAccess(
        de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.ArtifactByteAccess byteAccess
    ) {
        return de.burger.forensics.analytics.analysisjob.v1.ArtifactByteAccess.newBuilder()
            .setOwnerService(byteAccess.ownerService())
            .setRetrievalContract(byteAccess.retrievalContract())
            .setRetrievalReference(byteAccess.retrievalReference())
            .setByteCustody(BYTE_CUSTODY_PROTOS.get(byteAccess.byteCustody()))
            .build();
    }

    private static JoernCpgSummary summary(
        de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.JoernCpgSummary summary
    ) {
        return JoernCpgSummary.newBuilder()
            .setSourceRootCount(summary.sourceRootCount())
            .setProducedArtifactCount(summary.producedArtifactCount())
            .setMissingArtifactCount(summary.missingArtifactCount())
            .setJoernVersion(summary.joernVersion())
            .setJoernImageReference(summary.joernImageReference())
            .setQueryBundleVersion(summary.queryBundleVersion())
            .setProducerService(summary.producerService())
            .setSchemaVersion(summary.schemaVersion())
            .build();
    }

    private static JoernCpgDiagnostic diagnostic(
        de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.JoernCpgDiagnostic diagnostic
    ) {
        return JoernCpgDiagnostic.newBuilder()
            .setCode(diagnostic.code())
            .setMessage(diagnostic.message())
            .setSeverity(severity(diagnostic.severity()))
            .setSourceSnapshotId(diagnostic.sourceSnapshotId().value())
            .setArtifactPath(diagnostic.artifactPath())
            .setRetryable(diagnostic.retryable())
            .setAffectsCompleteness(diagnostic.affectsCompleteness())
            .build();
    }

    private static DiagnosticSeverity severity(
        de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.DiagnosticSeverity severity
    ) {
        return switch (severity) {
            case INFO -> DiagnosticSeverity.DIAGNOSTIC_SEVERITY_INFO;
            case WARNING -> DiagnosticSeverity.DIAGNOSTIC_SEVERITY_WARNING;
            case ERROR -> DiagnosticSeverity.DIAGNOSTIC_SEVERITY_ERROR;
        };
    }

    private static AnalysisCompleteness completeness(
        de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisCompleteness completeness
    ) {
        return switch (completeness) {
            case COMPLETE -> AnalysisCompleteness.ANALYSIS_COMPLETENESS_COMPLETE;
            case INCOMPLETE -> AnalysisCompleteness.ANALYSIS_COMPLETENESS_INCOMPLETE;
            case UNKNOWN -> AnalysisCompleteness.ANALYSIS_COMPLETENESS_UNKNOWN;
        };
    }

    private static de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisCompleteness completeness(
        AnalysisCompleteness completeness
    ) {
        return switch (completeness) {
            case ANALYSIS_COMPLETENESS_COMPLETE ->
                de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisCompleteness.COMPLETE;
            case ANALYSIS_COMPLETENESS_INCOMPLETE ->
                de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisCompleteness.INCOMPLETE;
            case ANALYSIS_COMPLETENESS_UNKNOWN, ANALYSIS_COMPLETENESS_UNSPECIFIED, UNRECOGNIZED ->
                de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisCompleteness.UNKNOWN;
        };
    }

    private static de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisCompleteness completeness(
        de.burger.forensics.analytics.repositoryanalysis.v1.SourceSnapshotCompleteness completeness
    ) {
        return switch (completeness) {
            case SOURCE_SNAPSHOT_COMPLETENESS_COMPLETE ->
                de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisCompleteness.COMPLETE;
            case SOURCE_SNAPSHOT_COMPLETENESS_INCOMPLETE ->
                de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisCompleteness.INCOMPLETE;
            case SOURCE_SNAPSHOT_COMPLETENESS_UNKNOWN, SOURCE_SNAPSHOT_COMPLETENESS_UNSPECIFIED, UNRECOGNIZED ->
                de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.AnalysisCompleteness.UNKNOWN;
        };
    }

    private static de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.PackageAvailability availability(
        de.burger.forensics.analytics.repositoryanalysis.v1.PackageAvailability availability
    ) {
        return switch (availability) {
            case PACKAGE_AVAILABILITY_AVAILABLE ->
                de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.PackageAvailability.AVAILABLE;
            case PACKAGE_AVAILABILITY_PENDING ->
                de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.PackageAvailability.PENDING;
            case PACKAGE_AVAILABILITY_UNAVAILABLE ->
                de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.PackageAvailability.UNAVAILABLE;
            case PACKAGE_AVAILABILITY_FAILED_INTEGRITY ->
                de.burger.forensics.analytics.services.joerncpganalysis.domain.JoernCpgAnalysisDomain.PackageAvailability.FAILED_INTEGRITY;
            case PACKAGE_AVAILABILITY_UNSPECIFIED, UNRECOGNIZED ->
                throw new IllegalArgumentException("package availability must be specified");
        };
    }

    private static Status status(RuntimeException error) {
        return switch (error) {
            case IllegalArgumentException ignored -> Status.INVALID_ARGUMENT.withDescription("Invalid Joern CPG analysis request");
            case NullPointerException ignored -> Status.INVALID_ARGUMENT.withDescription("Invalid Joern CPG analysis request");
            case JoernCpgAnalysisTimeoutException ignored -> Status.DEADLINE_EXCEEDED.withDescription("Joern CPG analysis timed out");
            case JoernRuntimeUnavailableException ignored -> Status.UNAVAILABLE.withDescription("Joern runtime unavailable");
            case JoernCpgArtifactException ignored -> Status.FAILED_PRECONDITION.withDescription("Joern artifact collection failed");
            case UncheckedIOException ignored -> Status.FAILED_PRECONDITION.withDescription("Joern artifact collection failed");
            default -> Status.FAILED_PRECONDITION.withDescription("Joern CPG analysis failed");
        };
    }

    private static <T> T required(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
