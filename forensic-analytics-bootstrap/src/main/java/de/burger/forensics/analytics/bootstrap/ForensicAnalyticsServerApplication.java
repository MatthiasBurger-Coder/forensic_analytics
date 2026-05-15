package de.burger.forensics.analytics.bootstrap;

import io.grpc.Server;
import de.burger.forensics.analytics.observability.CorrelationContext;
import de.burger.forensics.analytics.observability.OperationLogger;
import de.burger.forensics.analytics.rest.RestApiServer;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class ForensicAnalyticsServerApplication {
    private ForensicAnalyticsServerApplication() {
    }

    public static void main(String[] args) throws Exception {
        var grpcSettings = GrpcIngestionServerSettings.fromEnvironment();
        var restSettings = RestApiServerSettings.fromEnvironment();
        var components = ForensicAnalyticsBackendComponents.createDefault();
        run(
            grpcSettings,
            () -> new GrpcIngestionServerFactory().create(grpcSettings, components),
            restSettings,
            () -> {
                try {
                    return new RestApiServerBootstrapFactory().create(restSettings, components);
                } catch (java.io.IOException exception) {
                    throw new RestApiStartupException("Failed to create REST API server", exception);
                }
            }
        );
    }

    static boolean run(GrpcIngestionServerSettings settings, Supplier<Server> serverSupplier) throws Exception {
        return run(settings, serverSupplier, OperationLogger.system(ForensicAnalyticsServerApplication.class));
    }

    static boolean run(
        GrpcIngestionServerSettings settings,
        Supplier<Server> serverSupplier,
        OperationLogger operationLogger
    ) throws Exception {
        Objects.requireNonNull(operationLogger, "operationLogger must not be null");
        if (!settings.enabled()) {
            return false;
        }

        try (var correlationScope = CorrelationContext.openGenerated()) {
            correlationScope.correlationId();
            var operation = "bootstrap.grpc-server";
            var startedAt = System.nanoTime();
            operationLogger.started(operation);
            try {
                var server = serverSupplier.get();
                server.start();
                Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
                server.awaitTermination();
                operationLogger.succeeded(operation, elapsedMillis(startedAt));
                return true;
            } catch (Exception error) {
                operationLogger.failed(operation, elapsedMillis(startedAt), error);
                throw error;
            }
        }
    }

    static boolean run(
        GrpcIngestionServerSettings grpcSettings,
        Supplier<Server> grpcServerSupplier,
        RestApiServerSettings restSettings,
        Supplier<RestApiServer> restServerSupplier
    ) throws Exception {
        return run(
            grpcSettings,
            grpcServerSupplier,
            restSettings,
            restServerSupplier,
            OperationLogger.system(ForensicAnalyticsServerApplication.class)
        );
    }

    static boolean run(
        GrpcIngestionServerSettings grpcSettings,
        Supplier<Server> grpcServerSupplier,
        RestApiServerSettings restSettings,
        Supplier<RestApiServer> restServerSupplier,
        OperationLogger operationLogger
    ) throws Exception {
        Objects.requireNonNull(operationLogger, "operationLogger must not be null");
        if (!grpcSettings.enabled() && !restSettings.enabled()) {
            return false;
        }
        try (var correlationScope = CorrelationContext.openGenerated()) {
            correlationScope.correlationId();
            var operation = "bootstrap.server-application";
            var startedAt = System.nanoTime();
            operationLogger.started(operation);
            try {
                var result = runWithServers(grpcSettings, grpcServerSupplier, restSettings, restServerSupplier);
                operationLogger.succeeded(operation, elapsedMillis(startedAt));
                return result;
            } catch (Exception error) {
                operationLogger.failed(operation, elapsedMillis(startedAt), error);
                throw error;
            }
        }
    }

    private static boolean runWithServers(
        GrpcIngestionServerSettings grpcSettings,
        Supplier<Server> grpcServerSupplier,
        RestApiServerSettings restSettings,
        Supplier<RestApiServer> restServerSupplier
    ) throws Exception {
        Server grpcServer = null;
        RestApiServer restServer = null;
        try {
            grpcServer = startGrpc(grpcSettings, grpcServerSupplier);
            restServer = startRest(restSettings, restServerSupplier);
            var startedGrpcServer = grpcServer;
            var startedRestServer = restServer;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (startedRestServer != null) {
                    startedRestServer.stop();
                }
                if (startedGrpcServer != null) {
                    startedGrpcServer.shutdown();
                }
            }));
            if (grpcServer != null) {
                grpcServer.awaitTermination();
            } else {
                restServer.awaitTermination();
            }
            return true;
        } catch (Exception error) {
            if (restServer != null) {
                restServer.stop();
            }
            if (grpcServer != null) {
                grpcServer.shutdown();
            }
            throw error;
        }
    }

    private static long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    private static Server startGrpc(GrpcIngestionServerSettings settings, Supplier<Server> serverSupplier)
        throws java.io.IOException {
        if (!settings.enabled()) {
            return null;
        }
        var server = serverSupplier.get();
        server.start();
        return server;
    }

    private static RestApiServer startRest(RestApiServerSettings settings, Supplier<RestApiServer> serverSupplier) {
        if (!settings.enabled()) {
            return null;
        }
        var server = serverSupplier.get();
        server.start();
        return server;
    }

    private static final class RestApiStartupException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        RestApiStartupException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
