package de.burger.forensics.analytics.ingestion.grpc;

import de.burger.forensics.analytics.application.ingestion.ForensicIngestionUseCase;
import de.burger.forensics.analytics.application.ingestion.IngestionSessionException;
import de.burger.forensics.analytics.application.ingestion.RepositoryAnalysisIngestionException;
import de.burger.forensics.analytics.application.ingestion.RepositoryAnalysisIngestionUseCase;
import de.burger.forensics.analytics.application.ingestion.RepositoryCheckoutException;
import de.burger.forensics.analytics.application.ingestion.command.AbortAnalysisSessionCommand;
import de.burger.forensics.analytics.application.ingestion.command.CompleteAnalysisSessionCommand;
import de.burger.forensics.analytics.application.ingestion.command.StartAnalysisSessionCommand;
import de.burger.forensics.analytics.ingestion.grpc.mapper.AnalysisDataEnvelopeMapper;
import de.burger.forensics.analytics.ingestion.grpc.mapper.AnalyzeRepositoryMapper;
import de.burger.forensics.analytics.ingestion.grpc.mapper.BuildIdentityMapper;
import de.burger.forensics.analytics.ingestion.grpc.mapper.IngestionStatusMapper;
import de.burger.forensics.analytics.ingestion.grpc.mapper.ModuleIdentityMapper;
import de.burger.forensics.analytics.ingestion.grpc.mapper.PluginIdentityMapper;
import de.burger.forensics.analytics.ingestion.grpc.validator.AbortAnalysisSessionRequestValidator;
import de.burger.forensics.analytics.ingestion.grpc.validator.AnalysisDataEnvelopeValidator;
import de.burger.forensics.analytics.ingestion.grpc.validator.AnalyzeRepositoryRequestValidator;
import de.burger.forensics.analytics.ingestion.grpc.validator.CompleteAnalysisSessionRequestValidator;
import de.burger.forensics.analytics.ingestion.grpc.validator.StartAnalysisSessionRequestValidator;
import de.burger.forensics.analytics.ingestion.grpc.validator.ValidationException;
import de.burger.forensics.analytics.ingestion.v1.AbortAnalysisSessionRequest;
import de.burger.forensics.analytics.ingestion.v1.AbortAnalysisSessionResponse;
import de.burger.forensics.analytics.ingestion.v1.AnalysisDataEnvelope;
import de.burger.forensics.analytics.ingestion.v1.AnalyzeRepositoryRequest;
import de.burger.forensics.analytics.ingestion.v1.AnalyzeRepositoryResponse;
import de.burger.forensics.analytics.ingestion.v1.CompleteAnalysisSessionRequest;
import de.burger.forensics.analytics.ingestion.v1.CompleteAnalysisSessionResponse;
import de.burger.forensics.analytics.ingestion.v1.ForensicIngestionServiceGrpc;
import de.burger.forensics.analytics.ingestion.v1.StartAnalysisSessionRequest;
import de.burger.forensics.analytics.ingestion.v1.StartAnalysisSessionResponse;
import de.burger.forensics.analytics.ingestion.v1.UploadAnalysisDataResponse;
import de.burger.forensics.analytics.observability.CorrelationContext;
import de.burger.forensics.analytics.observability.CorrelationScope;
import de.burger.forensics.analytics.observability.OperationLogger;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class ForensicIngestionGrpcService
    extends ForensicIngestionServiceGrpc.ForensicIngestionServiceImplBase {

    private final ForensicIngestionUseCase useCase;
    private final RepositoryAnalysisIngestionUseCase repositoryAnalysisUseCase;
    private final AnalyzeRepositoryRequestValidator analyzeRepositoryValidator;
    private final StartAnalysisSessionRequestValidator startValidator;
    private final AnalysisDataEnvelopeValidator envelopeValidator;
    private final CompleteAnalysisSessionRequestValidator completeValidator;
    private final AbortAnalysisSessionRequestValidator abortValidator;
    private final BuildIdentityMapper buildIdentityMapper;
    private final PluginIdentityMapper pluginIdentityMapper;
    private final AnalysisDataEnvelopeMapper envelopeMapper;
    private final AnalyzeRepositoryMapper analyzeRepositoryMapper;
    private final IngestionStatusMapper statusMapper;
    private final OperationLogger operationLogger;

    public ForensicIngestionGrpcService(
        ForensicIngestionUseCase useCase,
        RepositoryAnalysisIngestionUseCase repositoryAnalysisUseCase
    ) {
        this(
            useCase,
            repositoryAnalysisUseCase,
            new AnalyzeRepositoryRequestValidator(),
            new StartAnalysisSessionRequestValidator(),
            new AnalysisDataEnvelopeValidator(),
            new CompleteAnalysisSessionRequestValidator(),
            new AbortAnalysisSessionRequestValidator(),
            new BuildIdentityMapper(),
            new PluginIdentityMapper(),
            new AnalysisDataEnvelopeMapper(
                new BuildIdentityMapper(),
                new ModuleIdentityMapper(),
                new PluginIdentityMapper()
            ),
            new AnalyzeRepositoryMapper(),
            new IngestionStatusMapper(),
            OperationLogger.system(ForensicIngestionGrpcService.class)
        );
    }

    ForensicIngestionGrpcService(
        ForensicIngestionUseCase useCase,
        RepositoryAnalysisIngestionUseCase repositoryAnalysisUseCase,
        OperationLogger operationLogger
    ) {
        this(
            useCase,
            repositoryAnalysisUseCase,
            new AnalyzeRepositoryRequestValidator(),
            new StartAnalysisSessionRequestValidator(),
            new AnalysisDataEnvelopeValidator(),
            new CompleteAnalysisSessionRequestValidator(),
            new AbortAnalysisSessionRequestValidator(),
            new BuildIdentityMapper(),
            new PluginIdentityMapper(),
            new AnalysisDataEnvelopeMapper(
                new BuildIdentityMapper(),
                new ModuleIdentityMapper(),
                new PluginIdentityMapper()
            ),
            new AnalyzeRepositoryMapper(),
            new IngestionStatusMapper(),
            operationLogger
        );
    }

    ForensicIngestionGrpcService(
        ForensicIngestionUseCase useCase,
        RepositoryAnalysisIngestionUseCase repositoryAnalysisUseCase,
        AnalyzeRepositoryRequestValidator analyzeRepositoryValidator,
        StartAnalysisSessionRequestValidator startValidator,
        AnalysisDataEnvelopeValidator envelopeValidator,
        CompleteAnalysisSessionRequestValidator completeValidator,
        AbortAnalysisSessionRequestValidator abortValidator,
        BuildIdentityMapper buildIdentityMapper,
        PluginIdentityMapper pluginIdentityMapper,
        AnalysisDataEnvelopeMapper envelopeMapper,
        AnalyzeRepositoryMapper analyzeRepositoryMapper,
        IngestionStatusMapper statusMapper,
        OperationLogger operationLogger
    ) {
        this.useCase = Objects.requireNonNull(useCase, "useCase must not be null");
        this.repositoryAnalysisUseCase = Objects.requireNonNull(
            repositoryAnalysisUseCase,
            "repositoryAnalysisUseCase must not be null"
        );
        this.analyzeRepositoryValidator = Objects.requireNonNull(
            analyzeRepositoryValidator,
            "analyzeRepositoryValidator must not be null"
        );
        this.startValidator = Objects.requireNonNull(startValidator, "startValidator must not be null");
        this.envelopeValidator = Objects.requireNonNull(envelopeValidator, "envelopeValidator must not be null");
        this.completeValidator = Objects.requireNonNull(completeValidator, "completeValidator must not be null");
        this.abortValidator = Objects.requireNonNull(abortValidator, "abortValidator must not be null");
        this.buildIdentityMapper = Objects.requireNonNull(buildIdentityMapper, "buildIdentityMapper must not be null");
        this.pluginIdentityMapper = Objects.requireNonNull(pluginIdentityMapper, "pluginIdentityMapper must not be null");
        this.envelopeMapper = Objects.requireNonNull(envelopeMapper, "envelopeMapper must not be null");
        this.analyzeRepositoryMapper = Objects.requireNonNull(
            analyzeRepositoryMapper,
            "analyzeRepositoryMapper must not be null"
        );
        this.statusMapper = Objects.requireNonNull(statusMapper, "statusMapper must not be null");
        this.operationLogger = Objects.requireNonNull(operationLogger, "operationLogger must not be null");
    }

    @Override
    public void analyzeRepository(
        AnalyzeRepositoryRequest request,
        StreamObserver<AnalyzeRepositoryResponse> responseObserver
    ) {
        try (var correlationScope = CorrelationContext.openGenerated()) {
            var operation = start("grpc.AnalyzeRepository", correlationScope);
            try {
                analyzeRepositoryValidator.validate(request);
                var result = repositoryAnalysisUseCase.analyze(analyzeRepositoryMapper.toCommand(request));
                responseObserver.onNext(analyzeRepositoryMapper.toProto(result));
                responseObserver.onCompleted();
                succeeded(operation);
            } catch (RuntimeException error) {
                failed(operation, error);
                responseObserver.onError(toStatus(error).asRuntimeException());
            }
        }
    }

    @Override
    public void startAnalysisSession(
        StartAnalysisSessionRequest request,
        StreamObserver<StartAnalysisSessionResponse> responseObserver
    ) {
        try (var correlationScope = CorrelationContext.openGenerated()) {
            var operation = start("grpc.StartAnalysisSession", correlationScope);
            try {
                startValidator.validate(request);
                var result = useCase.start(new StartAnalysisSessionCommand(
                    buildIdentityMapper.toCommand(request.getBuildIdentity()),
                    pluginIdentityMapper.toCommand(request.getPluginIdentity()),
                    request.getSchemaVersion()
                ));
                responseObserver.onNext(StartAnalysisSessionResponse.newBuilder()
                    .setSessionId(result.sessionId())
                    .setStatus(statusMapper.toProto(result.status()))
                    .setMessage(result.message())
                    .build());
                responseObserver.onCompleted();
                succeeded(operation);
            } catch (RuntimeException error) {
                failed(operation, error);
                responseObserver.onError(toStatus(error).asRuntimeException());
            }
        }
    }

    @Override
    public StreamObserver<AnalysisDataEnvelope> uploadAnalysisData(
        StreamObserver<UploadAnalysisDataResponse> responseObserver
    ) {
        var correlationScope = CorrelationContext.openGenerated();
        var operation = start("grpc.UploadAnalysisData");
        return new StreamObserver<>() {
            private long receivedItems;
            private String sessionId = "";
            private boolean failed;
            private boolean completed;

            @Override
            public void onNext(AnalysisDataEnvelope envelope) {
                if (failed) {
                    return;
                }
                try {
                    envelopeValidator.validate(envelope);
                    var result = useCase.upload(envelopeMapper.toCommand(envelope));
                    sessionId = result.sessionId();
                    receivedItems++;
                } catch (RuntimeException error) {
                    failed = true;
                    completed = true;
                    ForensicIngestionGrpcService.this.failed(operation, error);
                    correlationScope.close();
                    responseObserver.onError(toStatus(error).asRuntimeException());
                }
            }

            @Override
            public void onError(Throwable throwable) {
                failed = true;
                if (!completed) {
                    completed = true;
                    ForensicIngestionGrpcService.this.failed(operation, throwable);
                    correlationScope.close();
                }
            }

            @Override
            public void onCompleted() {
                if (failed) {
                    return;
                }
                responseObserver.onNext(UploadAnalysisDataResponse.newBuilder()
                    .setSessionId(sessionId)
                    .setStatus(de.burger.forensics.analytics.ingestion.v1.IngestionStatus.INGESTION_STATUS_ACCEPTED)
                    .setReceivedItems(receivedItems)
                    .setMessage("Analysis data stream accepted")
                    .build());
                responseObserver.onCompleted();
                completed = true;
                ForensicIngestionGrpcService.this.succeeded(operation);
                correlationScope.close();
            }
        };
    }

    @Override
    public void completeAnalysisSession(
        CompleteAnalysisSessionRequest request,
        StreamObserver<CompleteAnalysisSessionResponse> responseObserver
    ) {
        try (var correlationScope = CorrelationContext.openGenerated()) {
            var operation = start("grpc.CompleteAnalysisSession", correlationScope);
            try {
                completeValidator.validate(request);
                var result = useCase.complete(new CompleteAnalysisSessionCommand(request.getSessionId()));
                responseObserver.onNext(CompleteAnalysisSessionResponse.newBuilder()
                    .setSessionId(result.sessionId())
                    .setStatus(statusMapper.toProto(result.status()))
                    .setMessage(result.message())
                    .build());
                responseObserver.onCompleted();
                succeeded(operation);
            } catch (RuntimeException error) {
                failed(operation, error);
                responseObserver.onError(toStatus(error).asRuntimeException());
            }
        }
    }

    @Override
    public void abortAnalysisSession(
        AbortAnalysisSessionRequest request,
        StreamObserver<AbortAnalysisSessionResponse> responseObserver
    ) {
        try (var correlationScope = CorrelationContext.openGenerated()) {
            var operation = start("grpc.AbortAnalysisSession", correlationScope);
            try {
                abortValidator.validate(request);
                var result = useCase.abort(new AbortAnalysisSessionCommand(request.getSessionId(), request.getReason()));
                responseObserver.onNext(AbortAnalysisSessionResponse.newBuilder()
                    .setSessionId(result.sessionId())
                    .setStatus(statusMapper.toProto(result.status()))
                    .setMessage(result.message())
                    .build());
                responseObserver.onCompleted();
                succeeded(operation);
            } catch (RuntimeException error) {
                failed(operation, error);
                responseObserver.onError(toStatus(error).asRuntimeException());
            }
        }
    }

    private OperationCall start(String operation) {
        operationLogger.started(operation);
        return new OperationCall(operation, System.nanoTime());
    }

    private OperationCall start(String operation, CorrelationScope correlationScope) {
        Objects.requireNonNull(correlationScope, "correlationScope must not be null").correlationId();
        return start(operation);
    }

    private void succeeded(OperationCall operation) {
        operationLogger.succeeded(operation.name(), elapsedMillis(operation.startedAt()));
    }

    private void failed(OperationCall operation, Throwable error) {
        operationLogger.failed(operation.name(), elapsedMillis(operation.startedAt()), error);
    }

    private static long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    private Status toStatus(RuntimeException error) {
        if (error instanceof ValidationException) {
            return Status.INVALID_ARGUMENT.withDescription(error.getMessage()).withCause(error);
        }
        if (error instanceof IngestionSessionException
            || error instanceof RepositoryAnalysisIngestionException
            || error instanceof RepositoryCheckoutException) {
            return Status.FAILED_PRECONDITION.withDescription(error.getMessage()).withCause(error);
        }
        return Status.INTERNAL.withDescription("Unexpected ingestion failure").withCause(error);
    }

    private record OperationCall(String name, long startedAt) {
    }
}
