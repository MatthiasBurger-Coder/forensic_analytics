package de.burger.forensics.analytics.services.ingestion.adapter.in.grpc;

import de.burger.forensics.analytics.ingestion.v1.AbortAnalysisSessionRequest;
import de.burger.forensics.analytics.ingestion.v1.AbortAnalysisSessionResponse;
import de.burger.forensics.analytics.ingestion.v1.AnalysisDataEnvelope;
import de.burger.forensics.analytics.ingestion.v1.AnalysisPayloadKind;
import de.burger.forensics.analytics.ingestion.v1.AnalyzeRepositoryRequest;
import de.burger.forensics.analytics.ingestion.v1.AnalyzeRepositoryResponse;
import de.burger.forensics.analytics.ingestion.v1.CompleteAnalysisSessionRequest;
import de.burger.forensics.analytics.ingestion.v1.CompleteAnalysisSessionResponse;
import de.burger.forensics.analytics.ingestion.v1.ForensicIngestionServiceGrpc;
import de.burger.forensics.analytics.ingestion.v1.IngestionStatus;
import de.burger.forensics.analytics.ingestion.v1.StartAnalysisSessionRequest;
import de.burger.forensics.analytics.ingestion.v1.StartAnalysisSessionResponse;
import de.burger.forensics.analytics.ingestion.v1.UploadAnalysisDataResponse;
import de.burger.forensics.analytics.services.ingestion.application.IngestionApplicationService;
import de.burger.forensics.analytics.services.ingestion.application.IngestionSessionNotFoundException;
import de.burger.forensics.analytics.services.ingestion.application.command.StartAnalysisSessionCommand;
import de.burger.forensics.analytics.services.ingestion.application.command.UploadAnalysisDataCommand;
import de.burger.forensics.analytics.services.ingestion.domain.BuildIdentity;
import de.burger.forensics.analytics.services.ingestion.domain.ModuleIdentity;
import de.burger.forensics.analytics.services.ingestion.domain.PayloadDescriptor;
import de.burger.forensics.analytics.services.ingestion.domain.PluginIdentity;
import de.burger.forensics.analytics.services.ingestion.domain.RawIngestionPayload;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.Map;
import java.util.Objects;

public final class ForensicIngestionGrpcEndpoint extends ForensicIngestionServiceGrpc.ForensicIngestionServiceImplBase {
    private static final String ANALYZE_REPOSITORY_NON_SCOPE =
        "AnalyzeRepository is not implemented by forensic-ingestion-service; repository checkout is owned by repository-analysis-service";

    private final IngestionApplicationService applicationService;
    private final ForensicIngestionRequestValidator validator;

    public ForensicIngestionGrpcEndpoint(IngestionApplicationService applicationService) {
        this(applicationService, new ForensicIngestionRequestValidator());
    }

    ForensicIngestionGrpcEndpoint(
        IngestionApplicationService applicationService,
        ForensicIngestionRequestValidator validator
    ) {
        this.applicationService = Objects.requireNonNull(applicationService, "applicationService must not be null");
        this.validator = Objects.requireNonNull(validator, "validator must not be null");
    }

    @Override
    public void analyzeRepository(
        AnalyzeRepositoryRequest request,
        StreamObserver<AnalyzeRepositoryResponse> responseObserver
    ) {
        responseObserver.onError(Status.UNIMPLEMENTED.withDescription(ANALYZE_REPOSITORY_NON_SCOPE)
            .asRuntimeException());
    }

    @Override
    public void startAnalysisSession(
        StartAnalysisSessionRequest request,
        StreamObserver<StartAnalysisSessionResponse> responseObserver
    ) {
        try {
            validator.validate(request);
            var result = applicationService.start(new StartAnalysisSessionCommand(
                buildIdentity(request.getBuildIdentity()),
                pluginIdentity(request.getPluginIdentity()),
                request.getSchemaVersion()
            ));
            responseObserver.onNext(StartAnalysisSessionResponse.newBuilder()
                .setSessionId(result.sessionId())
                .setStatus(toProto(result.status()))
                .setMessage(result.message())
                .build());
            responseObserver.onCompleted();
        } catch (RuntimeException error) {
            responseObserver.onError(toStatus(error).asRuntimeException());
        }
    }

    @Override
    public StreamObserver<AnalysisDataEnvelope> uploadAnalysisData(
        StreamObserver<UploadAnalysisDataResponse> responseObserver
    ) {
        return new StreamObserver<>() {
            private String sessionId = "";
            private long acceptedItems;
            private boolean failed;

            @Override
            public void onNext(AnalysisDataEnvelope envelope) {
                if (failed) {
                    return;
                }
                try {
                    validator.validate(envelope);
                    var result = applicationService.upload(new UploadAnalysisDataCommand(
                        envelope.getSessionId(),
                        rawPayload(envelope)
                    ));
                    sessionId = result.sessionId();
                    if (result.acceptedNewPayload()) {
                        acceptedItems++;
                    }
                } catch (RuntimeException error) {
                    failed = true;
                    responseObserver.onError(toStatus(error).asRuntimeException());
                }
            }

            @Override
            public void onError(Throwable throwable) {
                failed = true;
            }

            @Override
            public void onCompleted() {
                if (failed) {
                    return;
                }
                if (sessionId.isBlank()) {
                    responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("analysis data stream must contain at least one envelope")
                        .asRuntimeException());
                    return;
                }
                responseObserver.onNext(UploadAnalysisDataResponse.newBuilder()
                    .setSessionId(sessionId)
                    .setStatus(IngestionStatus.INGESTION_STATUS_ACCEPTED)
                    .setReceivedItems(acceptedItems)
                    .setMessage("Analysis data stream accepted")
                    .build());
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public void completeAnalysisSession(
        CompleteAnalysisSessionRequest request,
        StreamObserver<CompleteAnalysisSessionResponse> responseObserver
    ) {
        try {
            validator.validateComplete(request.getSessionId());
            var result = applicationService.complete(request.getSessionId());
            responseObserver.onNext(CompleteAnalysisSessionResponse.newBuilder()
                .setSessionId(result.sessionId())
                .setStatus(toProto(result.status()))
                .setMessage(result.message())
                .build());
            responseObserver.onCompleted();
        } catch (RuntimeException error) {
            responseObserver.onError(toStatus(error).asRuntimeException());
        }
    }

    @Override
    public void abortAnalysisSession(
        AbortAnalysisSessionRequest request,
        StreamObserver<AbortAnalysisSessionResponse> responseObserver
    ) {
        try {
            validator.validateAbort(request.getSessionId(), request.getReason());
            var result = applicationService.abort(request.getSessionId(), request.getReason());
            responseObserver.onNext(AbortAnalysisSessionResponse.newBuilder()
                .setSessionId(result.sessionId())
                .setStatus(toProto(result.status()))
                .setMessage(result.message())
                .build());
            responseObserver.onCompleted();
        } catch (RuntimeException error) {
            responseObserver.onError(toStatus(error).asRuntimeException());
        }
    }

    private static Status toStatus(RuntimeException error) {
        if (error instanceof ValidationException || error instanceof IllegalArgumentException) {
            return Status.INVALID_ARGUMENT.withDescription(error.getMessage());
        }
        if (error instanceof IngestionSessionNotFoundException) {
            return Status.NOT_FOUND.withDescription(error.getMessage());
        }
        if (error instanceof IllegalStateException) {
            return Status.FAILED_PRECONDITION.withDescription(error.getMessage());
        }
        return Status.INTERNAL.withDescription("Unexpected ingestion service failure");
    }

    private static BuildIdentity buildIdentity(de.burger.forensics.analytics.ingestion.v1.BuildIdentity identity) {
        return new BuildIdentity(
            identity.getProjectId(),
            identity.getRepositoryUrl(),
            identity.getBranchName(),
            identity.getCommitHash(),
            identity.getBuildId(),
            identity.getScanTimestamp()
        );
    }

    private static PluginIdentity pluginIdentity(de.burger.forensics.analytics.ingestion.v1.PluginIdentity identity) {
        return new PluginIdentity(identity.getPluginName(), identity.getPluginVersion());
    }

    private static ModuleIdentity moduleIdentity(de.burger.forensics.analytics.ingestion.v1.ModuleIdentity identity) {
        return new ModuleIdentity(identity.getModuleName(), identity.getModulePath());
    }

    private static RawIngestionPayload rawPayload(AnalysisDataEnvelope envelope) {
        return new RawIngestionPayload(
            buildIdentity(envelope.getBuildIdentity()),
            moduleIdentity(envelope.getModuleIdentity()),
            pluginIdentity(envelope.getPluginIdentity()),
            envelope.getSchemaVersion(),
            payloadDescriptor(envelope.getPayloadDescriptor()),
            envelope.getPayload().toByteArray()
        );
    }

    private static PayloadDescriptor payloadDescriptor(
        de.burger.forensics.analytics.ingestion.v1.AnalysisPayloadDescriptor descriptor
    ) {
        return new PayloadDescriptor(
            descriptor.getPayloadId(),
            payloadKind(descriptor.getKind()),
            descriptor.getContentType(),
            Map.copyOf(descriptor.getAttributesMap())
        );
    }

    private static de.burger.forensics.analytics.services.ingestion.domain.AnalysisPayloadKind payloadKind(
        AnalysisPayloadKind kind
    ) {
        return switch (kind) {
            case ANALYSIS_PAYLOAD_KIND_SOURCE_FACTS ->
                de.burger.forensics.analytics.services.ingestion.domain.AnalysisPayloadKind.SOURCE_FACTS;
            case ANALYSIS_PAYLOAD_KIND_SEMANTIC_ARTIFACTS ->
                de.burger.forensics.analytics.services.ingestion.domain.AnalysisPayloadKind.SEMANTIC_ARTIFACTS;
            case ANALYSIS_PAYLOAD_KIND_RULE_ARTIFACTS ->
                de.burger.forensics.analytics.services.ingestion.domain.AnalysisPayloadKind.RULE_ARTIFACTS;
            case ANALYSIS_PAYLOAD_KIND_RUNTIME_TRACE ->
                de.burger.forensics.analytics.services.ingestion.domain.AnalysisPayloadKind.RUNTIME_TRACE;
            case ANALYSIS_PAYLOAD_KIND_DIAGNOSTIC_REPORT ->
                de.burger.forensics.analytics.services.ingestion.domain.AnalysisPayloadKind.DIAGNOSTIC_REPORT;
            default -> throw new ValidationException("payloadDescriptor.kind must be specified");
        };
    }

    private static IngestionStatus toProto(
        de.burger.forensics.analytics.services.ingestion.domain.IngestionStatus status
    ) {
        return switch (status) {
            case ACCEPTED -> IngestionStatus.INGESTION_STATUS_ACCEPTED;
            case COMPLETED -> IngestionStatus.INGESTION_STATUS_COMPLETED;
            case ABORTED -> IngestionStatus.INGESTION_STATUS_ABORTED;
            case REJECTED -> IngestionStatus.INGESTION_STATUS_REJECTED;
        };
    }
}
