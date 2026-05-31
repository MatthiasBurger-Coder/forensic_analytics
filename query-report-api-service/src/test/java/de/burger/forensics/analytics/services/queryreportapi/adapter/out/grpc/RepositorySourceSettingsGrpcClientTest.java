package de.burger.forensics.analytics.services.queryreportapi.adapter.out.grpc;

import de.burger.forensics.analytics.repositoryanalysis.v1.DatabaseSettingsValidationStatus;
import de.burger.forensics.analytics.repositoryanalysis.v1.GetRepositorySourceDatabaseSettingsRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryAnalysisServiceGrpc;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositorySourceDatabaseSettingsPublicView;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositorySourceDatabaseSettingsStatus;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositorySourceDatabaseSettingsValidationResponse;
import de.burger.forensics.analytics.repositoryanalysis.v1.ValidateRepositorySourceDatabaseSettingsRequest;
import de.burger.forensics.analytics.services.queryreportapi.application.QueryReportApiSettingsException;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiSettings.DatabaseSettingsRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiSettings.DatabaseSettingsValidationRequest;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RepositorySourceSettingsGrpcClientTest {
    @Test
    void mapsDatabaseSettingsOwnerRequestsAndSanitizedResponses() throws Exception {
        var service = new RecordingService();
        withClient(service, client -> {
            var current = client.current(new DatabaseSettingsRequest("settings-current", "correlation-settings"));
            var validation = client.validate(new DatabaseSettingsValidationRequest(
                "settings-validation",
                "correlation-settings",
                "postgres.example.test",
                5432,
                "forensic_analytics",
                "forensic_user",
                "candidate-secret",
                "repository_source",
                "require"
            ));

            assertEquals("settings-current", service.currentRequest.getRequestId());
            assertEquals("correlation-settings", service.currentRequest.getCorrelationId());
            assertEquals("settings-validation", service.validationRequest.getRequestId());
            assertEquals("postgres.example.test", service.validationRequest.getSettings().getHost());
            assertEquals("candidate-secret", service.validationRequest.getSettings().getPassword());
            assertEquals("POSTGRESQL", current.settings().engine());
            assertEquals("AVAILABLE", current.status());
            assertEquals("UNREACHABLE", validation.validationStatus());
            assertEquals("RESTART_REQUIRED", validation.applyMode());
            assertFalse(validation.hotApplySupported());
            assertFalse(validation.toString().contains("candidate-secret"));
        });
    }

    @Test
    void mapsRepositorySourceSettingsGrpcErrorsToPublicErrors() throws Exception {
        var cases = List.of(
            new ErrorCase(Status.INVALID_ARGUMENT, 400, "VALIDATION_ERROR"),
            new ErrorCase(Status.DEADLINE_EXCEEDED, 504, "TIMEOUT"),
            new ErrorCase(Status.UNAVAILABLE, 503, "BACKEND_UNAVAILABLE"),
            new ErrorCase(Status.FAILED_PRECONDITION, 502, "BACKEND_UNAVAILABLE"),
            new ErrorCase(Status.INTERNAL, 502, "BACKEND_UNAVAILABLE")
        );

        for (var current : cases) {
            withClient(new FailingService(current.status()), client -> {
                var error = assertThrows(
                    QueryReportApiSettingsException.class,
                    () -> client.current(new DatabaseSettingsRequest("settings-current", "correlation-settings"))
                );

                assertEquals(current.statusCode(), error.statusCode());
                assertEquals(current.errorCode(), error.errorCode());
            });
        }
    }

    private static void withClient(
        RepositoryAnalysisServiceGrpc.RepositoryAnalysisServiceImplBase service,
        ClientAssertion assertion
    ) throws Exception {
        var serverName = InProcessServerBuilder.generateName();
        var server = InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(service)
            .build()
            .start();
        var channel = InProcessChannelBuilder.forName(serverName)
            .directExecutor()
            .build();
        try {
            assertion.verify(new RepositorySourceSettingsGrpcClient(
                RepositoryAnalysisServiceGrpc.newBlockingStub(channel),
                10
            ));
        } finally {
            channel.shutdownNow();
            server.shutdownNow();
        }
    }

    private static RepositorySourceDatabaseSettingsPublicView settings(String configurationSource) {
        return RepositorySourceDatabaseSettingsPublicView.newBuilder()
            .setEngine("POSTGRESQL")
            .setHost("postgres.example.test")
            .setPort(5432)
            .setDatabaseName("forensic_analytics")
            .setUsername("forensic_user")
            .setAuthenticationConfigured(true)
            .setSchema("repository_source")
            .setSslMode("require")
            .setConfigurationSource(configurationSource)
            .setApplyMode("RESTART_REQUIRED")
            .setHotApplySupported(false)
            .build();
    }

    @FunctionalInterface
    private interface ClientAssertion {
        void verify(RepositorySourceSettingsGrpcClient client);
    }

    private record ErrorCase(Status status, int statusCode, String errorCode) {
    }

    private static final class RecordingService extends RepositoryAnalysisServiceGrpc.RepositoryAnalysisServiceImplBase {
        private GetRepositorySourceDatabaseSettingsRequest currentRequest;
        private ValidateRepositorySourceDatabaseSettingsRequest validationRequest;

        @Override
        public void getRepositorySourceDatabaseSettings(
            GetRepositorySourceDatabaseSettingsRequest request,
            StreamObserver<RepositorySourceDatabaseSettingsStatus> responseObserver
        ) {
            currentRequest = request;
            responseObserver.onNext(RepositorySourceDatabaseSettingsStatus.newBuilder()
                .setSettings(settings("REPOSITORY_SOURCE_RUNTIME"))
                .build());
            responseObserver.onCompleted();
        }

        @Override
        public void validateRepositorySourceDatabaseSettings(
            ValidateRepositorySourceDatabaseSettingsRequest request,
            StreamObserver<RepositorySourceDatabaseSettingsValidationResponse> responseObserver
        ) {
            validationRequest = request;
            responseObserver.onNext(RepositorySourceDatabaseSettingsValidationResponse.newBuilder()
                .setSettings(settings("VALIDATION_REQUEST"))
                .setValidationStatus(DatabaseSettingsValidationStatus.DATABASE_SETTINGS_VALIDATION_STATUS_UNREACHABLE)
                .build());
            responseObserver.onCompleted();
        }
    }

    private static final class FailingService extends RepositoryAnalysisServiceGrpc.RepositoryAnalysisServiceImplBase {
        private final Status status;

        private FailingService(Status status) {
            this.status = status;
        }

        @Override
        public void getRepositorySourceDatabaseSettings(
            GetRepositorySourceDatabaseSettingsRequest request,
            StreamObserver<RepositorySourceDatabaseSettingsStatus> responseObserver
        ) {
            responseObserver.onError(status.asRuntimeException());
        }
    }
}
