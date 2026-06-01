package de.burger.forensics.analytics.services.repositorysource.adapter.in.grpc;

import de.burger.forensics.analytics.repositoryanalysis.v1.ArtifactReference;
import de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputPackageDescriptor;
import de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducer;
import de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducerCandidate;
import de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducerStatus;
import de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputResolution;
import de.burger.forensics.analytics.repositoryanalysis.v1.CheckoutResult;
import de.burger.forensics.analytics.repositoryanalysis.v1.CheckoutStatus;
import de.burger.forensics.analytics.repositoryanalysis.v1.CleanupRepositoryWorkspaceByIdRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.CleanupRepositoryWorkspaceByIdResponse;
import de.burger.forensics.analytics.repositoryanalysis.v1.CleanupRepositoryWorkspaceRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.CleanupRepositoryWorkspaceResponse;
import de.burger.forensics.analytics.repositoryanalysis.v1.CreateRepositoryWorkspaceRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.CreateRepositoryWorkspaceResponse;
import de.burger.forensics.analytics.repositoryanalysis.v1.DatabaseSettingsValidationStatus;
import de.burger.forensics.analytics.repositoryanalysis.v1.Diagnostic;
import de.burger.forensics.analytics.repositoryanalysis.v1.DiagnosticSeverity;
import de.burger.forensics.analytics.repositoryanalysis.v1.GetRepositoryPreparationRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.GetRepositorySourceDatabaseSettingsRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.GetRepositoryWorkspaceRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.ListRepositoryWorkspacesRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.ListRepositoryWorkspacesResponse;
import de.burger.forensics.analytics.repositoryanalysis.v1.MetadataPreviewPolicy;
import de.burger.forensics.analytics.repositoryanalysis.v1.OperationStatus;
import de.burger.forensics.analytics.repositoryanalysis.v1.PackageAvailability;
import de.burger.forensics.analytics.repositoryanalysis.v1.PrepareRepositoryRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.PrepareRepositoryResponse;
import de.burger.forensics.analytics.repositoryanalysis.v1.PreviewRepositoryWorkspaceMetadataRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.PreviewRepositoryWorkspaceMetadataResponse;
import de.burger.forensics.analytics.repositoryanalysis.v1.RefreshRepositoryWorkspaceBranchRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.RefreshRepositoryWorkspaceBranchResponse;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryAnalysisServiceGrpc;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryIdentity;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryPreparation;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositorySourceDatabaseSettingsCandidate;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositorySourceDatabaseSettingsPublicView;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositorySourceDatabaseSettingsStatus;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositorySourceDatabaseSettingsValidationResponse;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryReference;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryWorkspace;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryWorkspaceBranch;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryWorkspaceBranchSelector;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryWorkspaceBranchStatus;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryWorkspaceStatus;
import de.burger.forensics.analytics.repositoryanalysis.v1.RevisionSelector;
import de.burger.forensics.analytics.repositoryanalysis.v1.SourcePackageDescriptor;
import de.burger.forensics.analytics.repositoryanalysis.v1.SourceRoot;
import de.burger.forensics.analytics.repositoryanalysis.v1.SourceSnapshot;
import de.burger.forensics.analytics.repositoryanalysis.v1.SourceSnapshotCompleteness;
import de.burger.forensics.analytics.repositoryanalysis.v1.ValidateRepositorySourceDatabaseSettingsRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.WorkspacePolicy;
import de.burger.forensics.analytics.services.repositorysource.application.IdempotencyConflictException;
import de.burger.forensics.analytics.services.repositorysource.application.RepositorySourceApplicationService;
import de.burger.forensics.analytics.services.repositorysource.application.RepositoryPreparationNotFoundException;
import de.burger.forensics.analytics.services.repositorysource.application.RepositoryWorkspaceApplicationService;
import de.burger.forensics.analytics.services.repositorysource.application.RepositoryWorkspaceNotFoundException;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryMetadataPreviewPolicy;
import de.burger.forensics.analytics.services.repositorysource.bootstrap.RepositorySourceDatabaseSettingsValidationResult;
import de.burger.forensics.analytics.services.repositorysource.bootstrap.RepositorySourceServiceProperties;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourceSnapshotId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceBranchId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceId;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.net.URI;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

import static de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.requireText;

public final class RepositorySourceGrpcEndpoint extends RepositoryAnalysisServiceGrpc.RepositoryAnalysisServiceImplBase {
    private final RepositorySourceApplicationService applicationService;
    private final RepositoryWorkspaceApplicationService workspaceApplicationService;
    private final RepositorySourceServiceProperties properties;
    private final DatabaseSettingsConnectionValidator databaseSettingsConnectionValidator;

    public RepositorySourceGrpcEndpoint(
        RepositorySourceApplicationService applicationService,
        RepositoryWorkspaceApplicationService workspaceApplicationService
    ) {
        this(
            applicationService,
            workspaceApplicationService,
            defaultDatabaseSettingsProperties(),
            postgres -> RepositorySourceDatabaseSettingsValidationResult.unreachable()
        );
    }

    public RepositorySourceGrpcEndpoint(
        RepositorySourceApplicationService applicationService,
        RepositoryWorkspaceApplicationService workspaceApplicationService,
        RepositorySourceServiceProperties properties,
        DatabaseSettingsConnectionValidator databaseSettingsConnectionValidator
    ) {
        this.applicationService = Objects.requireNonNull(applicationService, "application service must not be null");
        this.workspaceApplicationService = Objects.requireNonNull(
            workspaceApplicationService,
            "workspace application service must not be null"
        );
        this.properties = Objects.requireNonNull(properties, "repository-source properties must not be null");
        this.databaseSettingsConnectionValidator = Objects.requireNonNull(
            databaseSettingsConnectionValidator,
            "database settings connection validator must not be null"
        );
    }

    @Override
    public void prepareRepository(
        PrepareRepositoryRequest request,
        StreamObserver<PrepareRepositoryResponse> responseObserver
    ) {
        try {
            requireText(request.getRequestId(), "request id");
            var preparation = applicationService.prepare(
                request.getIdempotencyKey(),
                request.getSchemaVersion(),
                request.getCorrelationId(),
                new AnalysisRunId(request.getAnalysisRunId()),
                repository(request.getRepository()),
                revision(request.getRevision()),
                workspacePolicy(request.getWorkspacePolicy()),
                request.getSafeAttributesMap()
            );
            responseObserver.onNext(PrepareRepositoryResponse.newBuilder()
                .setPreparation(preparation(preparation))
                .setStatus(status("PREPARED", "Repository preparation completed", request.getCorrelationId()))
                .build());
            responseObserver.onCompleted();
        } catch (RuntimeException error) {
            responseObserver.onError(status(error).asRuntimeException());
        }
    }

    @Override
    public void getRepositorySourceDatabaseSettings(
        GetRepositorySourceDatabaseSettingsRequest request,
        StreamObserver<RepositorySourceDatabaseSettingsStatus> responseObserver
    ) {
        try {
            requireText(request.getRequestId(), "request id");
            requireText(request.getCorrelationId(), "correlation id");
            responseObserver.onNext(RepositorySourceDatabaseSettingsStatus.newBuilder()
                .setSettings(databaseSettingsView(
                    properties.persistence().postgres(),
                    "REPOSITORY_SOURCE_RUNTIME",
                    isPasswordConfigured(properties.persistence().postgres().password())
                ))
                .setStatus(status("SETTINGS_AVAILABLE", "Repository-source database settings available", request.getCorrelationId()))
                .build());
            responseObserver.onCompleted();
        } catch (RuntimeException error) {
            responseObserver.onError(status(error).asRuntimeException());
        }
    }

    @Override
    public void validateRepositorySourceDatabaseSettings(
        ValidateRepositorySourceDatabaseSettingsRequest request,
        StreamObserver<RepositorySourceDatabaseSettingsValidationResponse> responseObserver
    ) {
        try {
            requireText(request.getRequestId(), "request id");
            requireText(request.getCorrelationId(), "correlation id");
            var postgres = postgres(request.getSettings());
            var validation = databaseSettingsConnectionValidator.validate(postgres);
            responseObserver.onNext(RepositorySourceDatabaseSettingsValidationResponse.newBuilder()
                .setSettings(databaseSettingsView(
                    postgres,
                    "VALIDATION_REQUEST",
                    isPasswordConfigured(request.getSettings().getPassword())
                ))
                .setValidationStatus(validationStatus(validation.status()))
                .setStatus(status(validation.code(), validation.message(), request.getCorrelationId(), validation.retryable()))
                .addDiagnostics(Diagnostic.newBuilder()
                    .setSeverity(diagnosticSeverity(validation.status()))
                    .setCode(validation.code())
                    .setMessage(validation.message())
                    .build())
                .build());
            responseObserver.onCompleted();
        } catch (RuntimeException error) {
            responseObserver.onError(status(error).asRuntimeException());
        }
    }

    @Override
    public void getRepositoryPreparation(
        GetRepositoryPreparationRequest request,
        StreamObserver<RepositoryPreparation> responseObserver
    ) {
        try {
            requireText(request.getRequestId(), "request id");
            requireText(request.getCorrelationId(), "correlation id");
            var preparation = applicationService.get(
                new AnalysisRunId(request.getAnalysisRunId()),
                new SourceSnapshotId(request.getSourceSnapshotId())
            );
            responseObserver.onNext(preparation(preparation));
            responseObserver.onCompleted();
        } catch (RuntimeException error) {
            responseObserver.onError(status(error).asRuntimeException());
        }
    }

    @Override
    public void cleanupRepositoryWorkspace(
        CleanupRepositoryWorkspaceRequest request,
        StreamObserver<CleanupRepositoryWorkspaceResponse> responseObserver
    ) {
        try {
            requireText(request.getRequestId(), "request id");
            var result = applicationService.cleanup(
                request.getIdempotencyKey(),
                request.getCorrelationId(),
                new AnalysisRunId(request.getAnalysisRunId()),
                new WorkspaceId(request.getWorkspaceId())
            );
            var builder = CleanupRepositoryWorkspaceResponse.newBuilder()
                .setWorkspaceId(result.workspaceId().value())
                .setWorkspaceStatus(workspaceStatus(result.workspaceStatus()))
                .setStatus(status("CLEANED", "Repository workspace cleaned", request.getCorrelationId()));
            result.diagnostics().forEach(diagnostic -> builder.addDiagnostics(diagnostic(diagnostic)));
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (RuntimeException error) {
            responseObserver.onError(status(error).asRuntimeException());
        }
    }

    @Override
    public void previewRepositoryWorkspaceMetadata(
        PreviewRepositoryWorkspaceMetadataRequest request,
        StreamObserver<PreviewRepositoryWorkspaceMetadataResponse> responseObserver
    ) {
        try {
            requireText(request.getRequestId(), "request id");
            var preview = workspaceApplicationService.previewRepositoryWorkspaceMetadata(
                request.getSchemaVersion(),
                request.getCorrelationId(),
                repository(request.getRepository()),
                metadataPolicy(request.getMetadataPolicy()),
                request.getSafeAttributesMap()
            );
            var builder = PreviewRepositoryWorkspaceMetadataResponse.newBuilder()
                .setRepository(repositoryIdentity(preview.repository()))
                .setWorkspaceTitle(preview.workspaceTitle().value())
                .setStatus(status("METADATA_RESOLVED", "Repository metadata resolved", request.getCorrelationId()))
                .putAllSafeAttributes(preview.safeAttributes());
            preview.diagnostics().forEach(diagnostic -> builder.addDiagnostics(diagnostic(diagnostic)));
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (RuntimeException error) {
            responseObserver.onError(status(error).asRuntimeException());
        }
    }

    @Override
    public void createRepositoryWorkspace(
        CreateRepositoryWorkspaceRequest request,
        StreamObserver<CreateRepositoryWorkspaceResponse> responseObserver
    ) {
        try {
            requireText(request.getRequestId(), "request id");
            var workspace = workspaceApplicationService.createOrReuseRepositoryWorkspaceWithCheckout(
                request.getIdempotencyKey(),
                request.getSchemaVersion(),
                request.getCorrelationId(),
                repository(request.getRepository()),
                branchSelector(request.getBranchSelector()),
                workspacePolicy(request.getWorkspacePolicy()),
                request.getSafeAttributesMap()
            );
            responseObserver.onNext(CreateRepositoryWorkspaceResponse.newBuilder()
                .setWorkspace(repositoryWorkspace(workspace))
                .setStatus(status("WORKSPACE_ACCEPTED", "Repository workspace checkout accepted", request.getCorrelationId()))
                .build());
            responseObserver.onCompleted();
        } catch (RuntimeException error) {
            responseObserver.onError(status(error).asRuntimeException());
        }
    }

    @Override
    public void getRepositoryWorkspace(
        GetRepositoryWorkspaceRequest request,
        StreamObserver<RepositoryWorkspace> responseObserver
    ) {
        try {
            requireText(request.getRequestId(), "request id");
            requireText(request.getCorrelationId(), "correlation id");
            responseObserver.onNext(repositoryWorkspace(workspaceApplicationService.getRepositoryWorkspace(
                new WorkspaceId(request.getWorkspaceId())
            )));
            responseObserver.onCompleted();
        } catch (RuntimeException error) {
            responseObserver.onError(status(error).asRuntimeException());
        }
    }

    @Override
    public void listRepositoryWorkspaces(
        ListRepositoryWorkspacesRequest request,
        StreamObserver<ListRepositoryWorkspacesResponse> responseObserver
    ) {
        try {
            requireText(request.getRequestId(), "request id");
            var workspaces = workspaceApplicationService.listRepositoryWorkspaces(
                request.getSchemaVersion(),
                request.getCorrelationId(),
                request.getIncludeCleaned()
            );
            var builder = ListRepositoryWorkspacesResponse.newBuilder()
                .setStatus(status("WORKSPACES_LISTED", "Repository workspaces listed", request.getCorrelationId()));
            workspaces.forEach(workspace -> builder.addWorkspaces(repositoryWorkspace(workspace)));
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (RuntimeException error) {
            responseObserver.onError(status(error).asRuntimeException());
        }
    }

    @Override
    public void cleanupRepositoryWorkspaceById(
        CleanupRepositoryWorkspaceByIdRequest request,
        StreamObserver<CleanupRepositoryWorkspaceByIdResponse> responseObserver
    ) {
        try {
            requireText(request.getRequestId(), "request id");
            var result = workspaceApplicationService.cleanupRepositoryWorkspaceById(
                request.getIdempotencyKey(),
                request.getSchemaVersion(),
                request.getCorrelationId(),
                new WorkspaceId(request.getWorkspaceId()),
                request.getSafeAttributesMap()
            );
            var builder = CleanupRepositoryWorkspaceByIdResponse.newBuilder()
                .setWorkspaceId(result.workspaceId().value())
                .setWorkspaceStatus(workspaceStatus(result.workspaceStatus()))
                .setStatus(status("CLEANED", "Repository workspace cleaned", request.getCorrelationId()));
            result.diagnostics().forEach(diagnostic -> builder.addDiagnostics(diagnostic(diagnostic)));
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (RuntimeException error) {
            responseObserver.onError(status(error).asRuntimeException());
        }
    }

    @Override
    public void refreshRepositoryWorkspaceBranch(
        RefreshRepositoryWorkspaceBranchRequest request,
        StreamObserver<RefreshRepositoryWorkspaceBranchResponse> responseObserver
    ) {
        try {
            requireText(request.getRequestId(), "request id");
            var result = workspaceApplicationService.refreshRepositoryWorkspaceBranch(
                request.getIdempotencyKey(),
                request.getSchemaVersion(),
                request.getCorrelationId(),
                new WorkspaceId(request.getWorkspaceId()),
                new WorkspaceBranchId(request.getWorkspaceBranchId()),
                workspacePolicy(request.getWorkspacePolicy()),
                request.getSafeAttributesMap()
            );
            var builder = RefreshRepositoryWorkspaceBranchResponse.newBuilder()
                .setBranch(repositoryWorkspaceBranch(result.branch()))
                .setChanged(result.changed())
                .setPreviousCommit(result.previousCommit())
                .setStatus(status(
                    result.changed() ? "BRANCH_UPDATED" : "BRANCH_UP_TO_DATE",
                    result.changed() ? "Repository branch updated" : "Repository branch is up to date",
                    request.getCorrelationId()
                ))
                .putAllSafeAttributes(result.safeAttributes());
            result.diagnostics().forEach(diagnostic -> builder.addDiagnostics(diagnostic(diagnostic)));
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (RuntimeException error) {
            responseObserver.onError(status(error).asRuntimeException());
        }
    }

    private static de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryReference repository(
        RepositoryReference repository
    ) {
        return new de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryReference(
            repository.getRemoteUrl(),
            repository.getProvider(),
            repository.getSafeAttributesMap()
        );
    }

    private static de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RevisionSelector revision(
        RevisionSelector revision
    ) {
        return new de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RevisionSelector(
            revision.getBranch(),
            revision.getBranchRequired(),
            revision.getCommit(),
            revision.getCommitRequired()
        );
    }

    private static de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceBranchSelector branchSelector(
        RepositoryWorkspaceBranchSelector selector
    ) {
        return new de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceBranchSelector(
            selector.getBranch(),
            selector.getCommit()
        );
    }

    private static RepositoryMetadataPreviewPolicy metadataPolicy(MetadataPreviewPolicy policy) {
        return new RepositoryMetadataPreviewPolicy(policy.getTimeoutSeconds());
    }

    private static de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspacePolicy workspacePolicy(
        WorkspacePolicy policy
    ) {
        return new de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspacePolicy(
            policy.getEphemeral(),
            policy.getAllowShallowClone(),
            policy.getAllowPartialClone(),
            policy.getAllowSparseCheckout(),
            policy.getTimeoutSeconds(),
            policy.getMaxWorkspaceBytes()
        );
    }

    private static RepositoryPreparation preparation(
        de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryPreparation preparation
    ) {
        var builder = RepositoryPreparation.newBuilder()
            .setAnalysisRunId(preparation.analysisRunId().value())
            .setSourceSnapshotId(preparation.sourceSnapshotId().value())
            .setWorkspaceId(preparation.workspaceId().value())
            .setRepository(repository(preparation.repository()))
            .setRequestedRevision(revision(preparation.requestedRevision()))
            .setCheckout(checkout(preparation.checkout()))
            .setSourceSnapshot(sourceSnapshot(preparation.sourceSnapshot()))
            .setWorkspaceStatus(workspaceStatus(preparation.workspaceStatus()))
            .setCreatedAt(preparation.createdAt().toString())
            .setUpdatedAt(preparation.updatedAt().toString())
            .putAllSafeAttributes(preparation.safeAttributes());
        preparation.diagnostics().forEach(diagnostic -> builder.addDiagnostics(diagnostic(diagnostic)));
        return builder.build();
    }

    private static RepositoryIdentity repositoryIdentity(
        de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryIdentity repository
    ) {
        return RepositoryIdentity.newBuilder()
            .setRepositoryKey(repository.repositoryKey().value())
            .setRepositoryUrl(repository.repositoryUrl())
            .setRepositoryHost(repository.repositoryHost())
            .setRepositoryOwner(repository.repositoryOwner())
            .setRepositoryName(repository.repositoryName())
            .setDefaultBranch(repository.defaultBranch())
            .build();
    }

    private static RepositoryWorkspace repositoryWorkspace(
        de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspace workspace
    ) {
        var builder = RepositoryWorkspace.newBuilder()
            .setWorkspaceId(workspace.workspaceId().value())
            .setWorkspaceTitle(workspace.workspaceTitle().value())
            .setRepository(repositoryIdentity(workspace.repository()))
            .setStatus(workspaceStatus(workspace.status()))
            .setCreatedAt(workspace.createdAt().toString())
            .setUpdatedAt(workspace.updatedAt().toString())
            .putAllSafeAttributes(workspace.safeAttributes());
        workspace.branches().forEach(branch -> builder.addBranches(repositoryWorkspaceBranch(branch)));
        workspace.diagnostics().forEach(diagnostic -> builder.addDiagnostics(diagnostic(diagnostic)));
        return builder.build();
    }

    private static RepositoryWorkspaceBranch repositoryWorkspaceBranch(
        de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceBranch branch
    ) {
        var builder = RepositoryWorkspaceBranch.newBuilder()
            .setWorkspaceBranchId(branch.workspaceBranchId().value())
            .setWorkspaceId(branch.workspaceId().value())
            .setRepositoryBranch(branch.repositoryBranch())
            .setRequestedCommit(branch.requestedCommit())
            .setResolvedCommit(branch.resolvedCommit())
            .setStatus(branchStatus(branch.status()))
            .setLastUpdatedAt(branch.lastUpdatedAt().toString());
        if (branch.sourceSnapshotId() != null) {
            builder.setSourceSnapshotId(branch.sourceSnapshotId().value());
        }
        if (branch.lastCheckedAt() != null) {
            builder.setLastCheckedAt(branch.lastCheckedAt().toString());
        }
        branch.sourceRoots().forEach(sourceRoot -> builder.addSourceRoots(sourceRoot(sourceRoot)));
        branch.diagnostics().forEach(diagnostic -> builder.addDiagnostics(diagnostic(diagnostic)));
        return builder.build();
    }

    private static RepositoryReference repository(
        de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryReference repository
    ) {
        return RepositoryReference.newBuilder()
            .setRemoteUrl(repository.remoteUrl())
            .setProvider(repository.provider())
            .putAllSafeAttributes(repository.safeAttributes())
            .build();
    }

    private static RevisionSelector revision(
        de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RevisionSelector revision
    ) {
        return RevisionSelector.newBuilder()
            .setBranch(revision.branch())
            .setBranchRequired(revision.branchRequired())
            .setCommit(revision.commit())
            .setCommitRequired(revision.commitRequired())
            .build();
    }

    private static CheckoutResult checkout(
        de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.CheckoutResult checkout
    ) {
        var builder = CheckoutResult.newBuilder()
            .setStatus(checkoutStatus(checkout.status()))
            .setResolvedRemoteUrl(checkout.resolvedRemoteUrl())
            .setResolvedCommit(checkout.resolvedCommit())
            .setRequestedBranch(checkout.requestedBranch())
            .setRequestedCommit(checkout.requestedCommit())
            .setShallowClone(checkout.shallowClone())
            .setElapsedMillis(checkout.elapsedMillis())
            .setPartialClone(checkout.partialClone())
            .setSparseCheckout(checkout.sparseCheckout());
        checkout.diagnostics().forEach(diagnostic -> builder.addDiagnostics(diagnostic(diagnostic)));
        return builder.build();
    }

    private static SourceSnapshot sourceSnapshot(
        de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourceSnapshot sourceSnapshot
    ) {
        var builder = SourceSnapshot.newBuilder()
            .setSourceSnapshotId(sourceSnapshot.sourceSnapshotId().value())
            .setCompleteness(completeness(sourceSnapshot.completeness()))
            .setManifestArtifact(artifact(sourceSnapshot.manifestArtifact()))
            .addAllLimitations(sourceSnapshot.limitations())
            .setSourcePackage(sourcePackage(sourceSnapshot.sourcePackage()))
            .setBuildOutputPackage(buildOutputPackage(sourceSnapshot.buildOutputPackage()));
        sourceSnapshot.sourceRoots().forEach(sourceRoot -> builder.addSourceRoots(sourceRoot(sourceRoot)));
        return builder.build();
    }

    private static ArtifactReference artifact(
        de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.ArtifactReference artifact
    ) {
        return ArtifactReference.newBuilder()
            .setReference(artifact.reference())
            .setType(artifact.type())
            .setSha256(artifact.sha256())
            .setSizeBytes(artifact.sizeBytes())
            .build();
    }

    private static SourcePackageDescriptor sourcePackage(
        de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourcePackageDescriptor descriptor
    ) {
        var builder = SourcePackageDescriptor.newBuilder()
            .setAvailability(packageAvailability(descriptor.availability()))
            .setManifestArtifact(artifact(descriptor.manifestArtifact()))
            .setSchemaVersion(descriptor.schemaVersion())
            .setProducerService(descriptor.producerService())
            .setByteAccess(byteAccess(descriptor.byteAccess()))
            .setCompleteness(completeness(descriptor.completeness()));
        if (descriptor.packageArtifact() != null) {
            builder.setPackageArtifact(artifact(descriptor.packageArtifact()));
        }
        return builder.build();
    }

    private static BuildOutputPackageDescriptor buildOutputPackage(
        de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.BuildOutputPackageDescriptor descriptor
    ) {
        var builder = BuildOutputPackageDescriptor.newBuilder()
            .setAvailability(packageAvailability(descriptor.availability()))
            .setSchemaVersion(descriptor.schemaVersion())
            .setProducerService(descriptor.producerService())
            .setByteAccess(byteAccess(descriptor.byteAccess()))
            .setCompleteness(completeness(descriptor.completeness()))
            .setResolution(buildOutputResolution(descriptor.resolution()))
            .setBuildSystem(descriptor.buildSystem());
        if (descriptor.manifestArtifact() != null) {
            builder.setManifestArtifact(artifact(descriptor.manifestArtifact()));
        }
        if (descriptor.packageArtifact() != null) {
            builder.setPackageArtifact(artifact(descriptor.packageArtifact()));
        }
        return builder.build();
    }

    private static BuildOutputResolution buildOutputResolution(
        de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.BuildOutputResolution resolution
    ) {
        var builder = BuildOutputResolution.newBuilder()
            .setSelectedProducer(buildOutputProducer(resolution.selectedProducer()))
            .setTerminalIntegrityFailure(resolution.terminalIntegrityFailure());
        resolution.candidates().forEach(candidate -> builder.addCandidates(buildOutputCandidate(candidate)));
        resolution.diagnostics().forEach(diagnostic -> builder.addDiagnostics(diagnostic(diagnostic)));
        return builder.build();
    }

    private static BuildOutputProducerCandidate buildOutputCandidate(
        de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.BuildOutputProducerCandidate candidate
    ) {
        var builder = BuildOutputProducerCandidate.newBuilder()
            .setProducer(buildOutputProducer(candidate.producer()))
            .setStatus(buildOutputProducerStatus(candidate.status()))
            .setReference(candidate.reference());
        candidate.diagnostics().forEach(diagnostic -> builder.addDiagnostics(diagnostic(diagnostic)));
        return builder.build();
    }

    private static de.burger.forensics.analytics.analysisjob.v1.ArtifactByteAccess byteAccess(
        de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.ArtifactByteAccess byteAccess
    ) {
        return de.burger.forensics.analytics.analysisjob.v1.ArtifactByteAccess.newBuilder()
            .setOwnerService(byteAccess.ownerService())
            .setRetrievalContract(byteAccess.retrievalContract())
            .setRetrievalReference(byteAccess.retrievalReference())
            .setByteCustody(byteCustody(byteAccess.byteCustody()))
            .build();
    }

    private static SourceRoot sourceRoot(
        de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourceRoot sourceRoot
    ) {
        return SourceRoot.newBuilder()
            .setRelativePath(sourceRoot.relativePath())
            .setLanguage(sourceRoot.language())
            .build();
    }

    private static Diagnostic diagnostic(
        de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.Diagnostic diagnostic
    ) {
        return Diagnostic.newBuilder()
            .setCode(diagnostic.code())
            .setMessage(diagnostic.message())
            .setSeverity(severity(diagnostic.severity()))
            .build();
    }

    private static OperationStatus status(String code, String message, String correlationId) {
        return status(code, message, correlationId, false);
    }

    private static OperationStatus status(String code, String message, String correlationId, boolean retryable) {
        return OperationStatus.newBuilder()
            .setCode(code)
            .setMessage(message)
            .setRetryable(retryable)
            .setCorrelationId(correlationId)
            .build();
    }

    private static RepositorySourceDatabaseSettingsPublicView databaseSettingsView(
        RepositorySourceServiceProperties.Postgres postgres,
        String configurationSource,
        boolean authenticationConfigured
    ) {
        var location = postgresLocation(postgres.jdbcUrl());
        return RepositorySourceDatabaseSettingsPublicView.newBuilder()
            .setEngine("POSTGRESQL")
            .setHost(location.host())
            .setPort(location.port())
            .setDatabaseName(location.databaseName())
            .setUsername(postgres.username())
            .setAuthenticationConfigured(authenticationConfigured)
            .setSchema(postgres.schema())
            .setSslMode(location.sslMode())
            .setConfigurationSource(configurationSource)
            .setApplyMode("RESTART_REQUIRED")
            .setHotApplySupported(false)
            .build();
    }

    private static RepositorySourceServiceProperties.Postgres postgres(
        RepositorySourceDatabaseSettingsCandidate candidate
    ) {
        var host = requireDatabaseHost(candidate.getHost());
        var port = requireDatabasePort(candidate.getPort());
        var databaseName = requireSqlIdentifier(candidate.getDatabaseName(), "database name");
        var schema = requireSqlIdentifier(candidate.getSchema(), "PostgreSQL schema");
        var sslMode = sslMode(candidate.getSslMode());
        return new RepositorySourceServiceProperties.Postgres(
            postgresJdbcUrl(host, port, databaseName, sslMode),
            requireText(candidate.getUsername(), "PostgreSQL username"),
            candidate.getPassword(),
            schema,
            propertiesChangeLog()
        );
    }

    private static String postgresJdbcUrl(String host, int port, String databaseName, String sslMode) {
        var base = "jdbc:postgresql://" + host + ":" + port + "/" + databaseName;
        return "UNSPECIFIED".equals(sslMode) ? base : base + "?sslmode=" + sslMode;
    }

    private static DatabaseSettingsLocation postgresLocation(String jdbcUrl) {
        var uri = URI.create(jdbcUrl.substring("jdbc:".length()));
        var databaseName = uri.getPath() == null || uri.getPath().length() <= 1 ? "" : uri.getPath().substring(1);
        var sslMode = "UNSPECIFIED";
        if (uri.getQuery() != null) {
            for (var parameter : uri.getQuery().split("&")) {
                var parts = parameter.split("=", 2);
                if (parts.length == 2 && "sslmode".equals(parts[0])) {
                    sslMode = sslMode(parts[1]);
                }
            }
        }
        return new DatabaseSettingsLocation(
            uri.getHost(),
            uri.getPort() < 1 ? 5432 : uri.getPort(),
            databaseName,
            sslMode
        );
    }

    private static String requireDatabaseHost(String host) {
        var text = requireText(host, "PostgreSQL host");
        if (!text.matches("[A-Za-z0-9.-]{1,253}") || text.startsWith(".") || text.endsWith(".")) {
            throw new IllegalArgumentException("PostgreSQL host must be a DNS name or address label");
        }
        return text;
    }

    private static int requireDatabasePort(int port) {
        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("PostgreSQL port must be between 1 and 65535");
        }
        return port;
    }

    private static String requireSqlIdentifier(String value, String name) {
        var text = requireText(value, name);
        if (!text.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException(name + " must be a simple SQL identifier");
        }
        return text;
    }

    private static String sslMode(String value) {
        if (value == null || value.isBlank() || "UNSPECIFIED".equalsIgnoreCase(value)) {
            return "UNSPECIFIED";
        }
        var normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "disable", "allow", "prefer", "require", "verify-ca", "verify-full" -> normalized;
            default -> throw new IllegalArgumentException("PostgreSQL ssl mode is not supported");
        };
    }

    private static DatabaseSettingsValidationStatus validationStatus(
        RepositorySourceDatabaseSettingsValidationResult.Status status
    ) {
        return switch (status) {
            case VALID -> DatabaseSettingsValidationStatus.DATABASE_SETTINGS_VALIDATION_STATUS_VALID;
            case UNREACHABLE -> DatabaseSettingsValidationStatus.DATABASE_SETTINGS_VALIDATION_STATUS_UNREACHABLE;
            case AUTHENTICATION_FAILED ->
                DatabaseSettingsValidationStatus.DATABASE_SETTINGS_VALIDATION_STATUS_AUTHENTICATION_FAILED;
        };
    }

    private static DiagnosticSeverity diagnosticSeverity(RepositorySourceDatabaseSettingsValidationResult.Status status) {
        return switch (status) {
            case VALID -> DiagnosticSeverity.DIAGNOSTIC_SEVERITY_INFO;
            case UNREACHABLE, AUTHENTICATION_FAILED -> DiagnosticSeverity.DIAGNOSTIC_SEVERITY_ERROR;
        };
    }

    private static boolean isPasswordConfigured(String password) {
        return password != null && !password.isBlank();
    }

    private static String propertiesChangeLog() {
        return "classpath:db/changelog/repository-source-workspace.postgresql.yaml";
    }

    private static RepositorySourceServiceProperties defaultDatabaseSettingsProperties() {
        return new RepositorySourceServiceProperties(
            new RepositorySourceServiceProperties.Grpc(false, "127.0.0.1", 0),
            new RepositorySourceServiceProperties.Health(false, "127.0.0.1", 0),
            new RepositorySourceServiceProperties.Workspace(Path.of("build/repository-source-workspaces")),
            new RepositorySourceServiceProperties.Persistence(
                "postgres",
                new RepositorySourceServiceProperties.Postgres(
                    "jdbc:postgresql://127.0.0.1:5432/forensic_analytics",
                    "forensic",
                    "",
                    "repository_source",
                    propertiesChangeLog()
                )
            )
        );
    }

    static Status status(RuntimeException error) {
        if (error instanceof RepositoryPreparationNotFoundException || error instanceof RepositoryWorkspaceNotFoundException) {
            return Status.NOT_FOUND.withDescription(error.getMessage());
        }
        if (error instanceof IdempotencyConflictException) {
            return Status.ALREADY_EXISTS.withDescription(error.getMessage());
        }
        if (error instanceof IllegalArgumentException) {
            return Status.INVALID_ARGUMENT.withDescription("Invalid repository source request");
        }
        if (error instanceof IllegalStateException) {
            return stateStatus(error);
        }
        return Status.INTERNAL.withDescription("Repository source service failed");
    }

    private static Status stateStatus(RuntimeException error) {
        var message = Objects.toString(error.getMessage(), "").toLowerCase(java.util.Locale.ROOT);
        if (message.startsWith("failed to save") || message.startsWith("failed to load")
            || message.startsWith("failed to initialize") || message.contains("h2")
            || message.contains("idempotency")) {
            return Status.INTERNAL.withDescription("Repository source persistence failed");
        }
        if (message.contains("workspace branch checkout")) {
            return Status.FAILED_PRECONDITION.withDescription("Repository workspace checkout failed");
        }
        if (message.contains("repository workspace") || message.contains("branch workspace")
            || message.contains("workspace path") || message.contains("workspace byte quota")) {
            return Status.FAILED_PRECONDITION.withDescription("Repository workspace operation failed");
        }
        return Status.FAILED_PRECONDITION.withDescription("Repository preparation failed");
    }

    static CheckoutStatus checkoutStatus(
        de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.CheckoutStatus status
    ) {
        return switch (status) {
            case ACCEPTED -> CheckoutStatus.CHECKOUT_STATUS_ACCEPTED;
            case WORKSPACE_PREPARED -> CheckoutStatus.CHECKOUT_STATUS_WORKSPACE_PREPARED;
            case CHECKED_OUT -> CheckoutStatus.CHECKOUT_STATUS_CHECKED_OUT;
            case FAILED -> CheckoutStatus.CHECKOUT_STATUS_FAILED;
        };
    }

    static RepositoryWorkspaceStatus workspaceStatus(
        de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceStatus status
    ) {
        return switch (status) {
            case READY -> RepositoryWorkspaceStatus.REPOSITORY_WORKSPACE_STATUS_READY;
            case CHECKED_OUT -> RepositoryWorkspaceStatus.REPOSITORY_WORKSPACE_STATUS_CHECKED_OUT;
            case CLEANED -> RepositoryWorkspaceStatus.REPOSITORY_WORKSPACE_STATUS_CLEANED;
            case FAILED -> RepositoryWorkspaceStatus.REPOSITORY_WORKSPACE_STATUS_FAILED;
        };
    }

    static RepositoryWorkspaceBranchStatus branchStatus(
        de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceBranchStatus status
    ) {
        return switch (status) {
            case CHECKING_OUT -> RepositoryWorkspaceBranchStatus.REPOSITORY_WORKSPACE_BRANCH_STATUS_CHECKING_OUT;
            case CHECKED_OUT -> RepositoryWorkspaceBranchStatus.REPOSITORY_WORKSPACE_BRANCH_STATUS_CHECKED_OUT;
            case UP_TO_DATE -> RepositoryWorkspaceBranchStatus.REPOSITORY_WORKSPACE_BRANCH_STATUS_UP_TO_DATE;
            case UPDATING -> RepositoryWorkspaceBranchStatus.REPOSITORY_WORKSPACE_BRANCH_STATUS_UPDATING;
            case UPDATED -> RepositoryWorkspaceBranchStatus.REPOSITORY_WORKSPACE_BRANCH_STATUS_UPDATED;
            case FAILED -> RepositoryWorkspaceBranchStatus.REPOSITORY_WORKSPACE_BRANCH_STATUS_FAILED;
        };
    }

    static SourceSnapshotCompleteness completeness(
        de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourceSnapshotCompleteness completeness
    ) {
        return switch (completeness) {
            case COMPLETE -> SourceSnapshotCompleteness.SOURCE_SNAPSHOT_COMPLETENESS_COMPLETE;
            case INCOMPLETE -> SourceSnapshotCompleteness.SOURCE_SNAPSHOT_COMPLETENESS_INCOMPLETE;
            case UNKNOWN -> SourceSnapshotCompleteness.SOURCE_SNAPSHOT_COMPLETENESS_UNKNOWN;
        };
    }

    static PackageAvailability packageAvailability(
        de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.PackageAvailability availability
    ) {
        return switch (availability) {
            case AVAILABLE -> PackageAvailability.PACKAGE_AVAILABILITY_AVAILABLE;
            case PENDING -> PackageAvailability.PACKAGE_AVAILABILITY_PENDING;
            case UNAVAILABLE -> PackageAvailability.PACKAGE_AVAILABILITY_UNAVAILABLE;
            case FAILED_INTEGRITY -> PackageAvailability.PACKAGE_AVAILABILITY_FAILED_INTEGRITY;
        };
    }

    static BuildOutputProducer buildOutputProducer(
        de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.BuildOutputProducer producer
    ) {
        return switch (producer) {
            case UNSPECIFIED -> BuildOutputProducer.BUILD_OUTPUT_PRODUCER_UNSPECIFIED;
            case ARTIFACT_STORE -> BuildOutputProducer.BUILD_OUTPUT_PRODUCER_ARTIFACT_STORE;
            case ARTIFACTORY -> BuildOutputProducer.BUILD_OUTPUT_PRODUCER_ARTIFACTORY;
            case JENKINS -> BuildOutputProducer.BUILD_OUTPUT_PRODUCER_JENKINS;
            case BUILD_ARTIFACT_WORKER -> BuildOutputProducer.BUILD_OUTPUT_PRODUCER_BUILD_ARTIFACT_WORKER;
        };
    }

    static BuildOutputProducerStatus buildOutputProducerStatus(
        de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.BuildOutputProducerStatus status
    ) {
        return switch (status) {
            case AVAILABLE -> BuildOutputProducerStatus.BUILD_OUTPUT_PRODUCER_STATUS_AVAILABLE;
            case NOT_CONFIGURED -> BuildOutputProducerStatus.BUILD_OUTPUT_PRODUCER_STATUS_NOT_CONFIGURED;
            case MISSING -> BuildOutputProducerStatus.BUILD_OUTPUT_PRODUCER_STATUS_MISSING;
            case FALLBACK_PLANNED -> BuildOutputProducerStatus.BUILD_OUTPUT_PRODUCER_STATUS_FALLBACK_PLANNED;
            case TERMINAL_INTEGRITY_FAILURE -> BuildOutputProducerStatus.BUILD_OUTPUT_PRODUCER_STATUS_TERMINAL_INTEGRITY_FAILURE;
        };
    }

    static de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody byteCustody(
        de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.ArtifactByteCustody custody
    ) {
        return switch (custody) {
            case PRODUCER_RETAINED ->
                de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_PRODUCER_RETAINED;
            case SCOPED_OBJECT_ACCESS ->
                de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_SCOPED_OBJECT_ACCESS;
            case EXPLICIT_HANDOFF ->
                de.burger.forensics.analytics.analysisjob.v1.ArtifactByteCustody.ARTIFACT_BYTE_CUSTODY_EXPLICIT_HANDOFF;
        };
    }

    static DiagnosticSeverity severity(
        de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.DiagnosticSeverity severity
    ) {
        return switch (severity) {
            case INFO -> DiagnosticSeverity.DIAGNOSTIC_SEVERITY_INFO;
            case WARNING -> DiagnosticSeverity.DIAGNOSTIC_SEVERITY_WARNING;
            case ERROR -> DiagnosticSeverity.DIAGNOSTIC_SEVERITY_ERROR;
        };
    }

    @FunctionalInterface
    public interface DatabaseSettingsConnectionValidator {
        RepositorySourceDatabaseSettingsValidationResult validate(RepositorySourceServiceProperties.Postgres postgres);
    }

    private record DatabaseSettingsLocation(
        String host,
        int port,
        String databaseName,
        String sslMode
    ) {
    }
}
