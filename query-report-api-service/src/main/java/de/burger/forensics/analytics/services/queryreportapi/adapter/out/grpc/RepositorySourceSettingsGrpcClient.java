package de.burger.forensics.analytics.services.queryreportapi.adapter.out.grpc;

import de.burger.forensics.analytics.repositoryanalysis.v1.DatabaseSettingsValidationStatus;
import de.burger.forensics.analytics.repositoryanalysis.v1.GetRepositorySourceDatabaseSettingsRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryAnalysisServiceGrpc;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositorySourceDatabaseSettingsCandidate;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositorySourceDatabaseSettingsPublicView;
import de.burger.forensics.analytics.repositoryanalysis.v1.ValidateRepositorySourceDatabaseSettingsRequest;
import de.burger.forensics.analytics.services.queryreportapi.application.QueryReportApiSettingsException;
import de.burger.forensics.analytics.services.queryreportapi.application.port.RepositorySourceSettingsOwnerPort;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiRepositoryAnalysis.Diagnostic;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiSettings.DatabaseSettingsRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiSettings.DatabaseSettingsStatus;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiSettings.DatabaseSettingsValidationRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiSettings.DatabaseSettingsValidationResponse;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiSettings.DatabaseSettingsView;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.util.List;
import java.util.concurrent.TimeUnit;

public final class RepositorySourceSettingsGrpcClient implements RepositorySourceSettingsOwnerPort, AutoCloseable {
    private final ManagedChannel channel;
    private final RepositoryAnalysisServiceGrpc.RepositoryAnalysisServiceBlockingStub stub;
    private final long deadlineSeconds;

    public RepositorySourceSettingsGrpcClient(String host, int port, long deadlineSeconds) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext().build(), deadlineSeconds);
    }

    RepositorySourceSettingsGrpcClient(
        RepositoryAnalysisServiceGrpc.RepositoryAnalysisServiceBlockingStub stub,
        long deadlineSeconds
    ) {
        this.channel = null;
        this.stub = stub;
        this.deadlineSeconds = deadlineSeconds;
    }

    private RepositorySourceSettingsGrpcClient(ManagedChannel channel, long deadlineSeconds) {
        this.channel = channel;
        this.stub = RepositoryAnalysisServiceGrpc.newBlockingStub(channel);
        this.deadlineSeconds = deadlineSeconds;
    }

    @Override
    public DatabaseSettingsStatus current(DatabaseSettingsRequest request) {
        try {
            var response = stub.withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS)
                .getRepositorySourceDatabaseSettings(GetRepositorySourceDatabaseSettingsRequest.newBuilder()
                    .setRequestId(request.requestId())
                    .setCorrelationId(request.correlationId())
                    .build());
            return new DatabaseSettingsStatus(
                settings(response.getSettings()),
                "AVAILABLE",
                diagnostics(response.getDiagnosticsList())
            );
        } catch (StatusRuntimeException error) {
            throw map(error);
        }
    }

    @Override
    public DatabaseSettingsValidationResponse validate(DatabaseSettingsValidationRequest request) {
        try {
            var response = stub.withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS)
                .validateRepositorySourceDatabaseSettings(ValidateRepositorySourceDatabaseSettingsRequest.newBuilder()
                    .setRequestId(request.requestId())
                    .setCorrelationId(request.correlationId())
                    .setSettings(RepositorySourceDatabaseSettingsCandidate.newBuilder()
                        .setHost(request.host())
                        .setPort(request.port())
                        .setDatabaseName(request.databaseName())
                        .setUsername(request.username())
                        .setPassword(request.password())
                        .setSchema(request.schema())
                        .setSslMode(request.sslMode()))
                    .build());
            return new DatabaseSettingsValidationResponse(
                settings(response.getSettings()),
                validationStatus(response.getValidationStatus()),
                "RESTART_REQUIRED",
                false,
                diagnostics(response.getDiagnosticsList())
            );
        } catch (StatusRuntimeException error) {
            throw map(error);
        }
    }

    private static DatabaseSettingsView settings(RepositorySourceDatabaseSettingsPublicView settings) {
        return new DatabaseSettingsView(
            settings.getEngine(),
            settings.getHost(),
            settings.getPort(),
            settings.getDatabaseName(),
            settings.getUsername(),
            settings.getAuthenticationConfigured(),
            settings.getSchema(),
            settings.getSslMode(),
            settings.getConfigurationSource(),
            settings.getApplyMode(),
            settings.getHotApplySupported()
        );
    }

    private static String validationStatus(DatabaseSettingsValidationStatus status) {
        return switch (status) {
            case DATABASE_SETTINGS_VALIDATION_STATUS_VALID -> "VALID";
            case DATABASE_SETTINGS_VALIDATION_STATUS_INVALID -> "INVALID";
            case DATABASE_SETTINGS_VALIDATION_STATUS_UNREACHABLE -> "UNREACHABLE";
            case DATABASE_SETTINGS_VALIDATION_STATUS_AUTHENTICATION_FAILED -> "AUTHENTICATION_FAILED";
            case DATABASE_SETTINGS_VALIDATION_STATUS_UNSUPPORTED -> "UNSUPPORTED";
            case DATABASE_SETTINGS_VALIDATION_STATUS_UNSPECIFIED, UNRECOGNIZED -> throw unsupportedStatus();
        };
    }

    private static List<Diagnostic> diagnostics(List<de.burger.forensics.analytics.repositoryanalysis.v1.Diagnostic> diagnostics) {
        return diagnostics.stream()
            .map(diagnostic -> new Diagnostic(
                diagnostic.getSeverity().name().replace("DIAGNOSTIC_SEVERITY_", ""),
                diagnostic.getCode(),
                diagnostic.getMessage()
            ))
            .toList();
    }

    private static RuntimeException map(StatusRuntimeException error) {
        var code = error.getStatus().getCode();
        if (code == Status.Code.INVALID_ARGUMENT) {
            return new QueryReportApiSettingsException(400, "VALIDATION_ERROR", false, "Invalid database settings request");
        }
        if (code == Status.Code.DEADLINE_EXCEEDED) {
            return new QueryReportApiSettingsException(504, "TIMEOUT", true, "Database settings request timed out");
        }
        if (code == Status.Code.UNAVAILABLE) {
            return new QueryReportApiSettingsException(503, "BACKEND_UNAVAILABLE", true, "Repository Source service is unavailable");
        }
        return new QueryReportApiSettingsException(502, "BACKEND_UNAVAILABLE", false, "Repository Source database settings are unavailable");
    }

    private static QueryReportApiSettingsException unsupportedStatus() {
        return new QueryReportApiSettingsException(
            502,
            "BACKEND_UNAVAILABLE",
            false,
            "Repository Source returned unsupported database settings state"
        );
    }

    @Override
    public void close() {
        if (channel != null) {
            channel.shutdownNow();
        }
    }
}
