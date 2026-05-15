package de.burger.forensics.analytics.bootstrap;

import io.grpc.Server;
import de.burger.forensics.analytics.rest.RestApiServer;
import de.burger.forensics.analytics.observability.CorrelationContext;
import de.burger.forensics.analytics.observability.OperationLogger;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForensicAnalyticsServerApplicationTest {
    @Test
    void runReturnsFalseWhenGrpcServerIsDisabled() throws Exception {
        var result = ForensicAnalyticsServerApplication.run(
            new GrpcIngestionServerSettings(false, 9090),
            FakeServer::new
        );

        assertFalse(result);
    }

    @Test
    void runStartsServerWhenGrpcServerIsEnabled() throws Exception {
        var server = new FakeServer();

        var result = ForensicAnalyticsServerApplication.run(
            new GrpcIngestionServerSettings(true, 9090),
            () -> server
        );

        assertTrue(result);
        assertTrue(server.started);
        assertTrue(server.awaited);
    }

    @Test
    void runStartsGrpcAndRestServersWhenBothAreEnabled() throws Exception {
        var grpcServer = new FakeServer();
        var restServer = new FakeRestApiServer();
        var operationLogger = new RecordingOperationLogger();

        var result = ForensicAnalyticsServerApplication.run(
            new GrpcIngestionServerSettings(true, 9090),
            () -> grpcServer,
            new RestApiServerSettings(true, "127.0.0.1", 8080),
            () -> restServer,
            operationLogger
        );

        assertTrue(result);
        assertTrue(grpcServer.started);
        assertTrue(grpcServer.awaited);
        assertTrue(restServer.started);
        assertEquals(List.of("STARTED", "SUCCEEDED"), operationLogger.phases());
        assertEquals("bootstrap.server-application", operationLogger.events.get(0).operation());
        assertFalse(operationLogger.events.get(0).correlationId().isBlank());
    }

    @Test
    void runAwaitsRestServerWhenGrpcIsDisabled() throws Exception {
        var restServer = new FakeRestApiServer();

        var result = ForensicAnalyticsServerApplication.run(
            new GrpcIngestionServerSettings(false, 9090),
            FakeServer::new,
            new RestApiServerSettings(true, "127.0.0.1", 8080),
            () -> restServer
        );

        assertTrue(result);
        assertTrue(restServer.started);
        assertTrue(restServer.awaited);
    }

    @Test
    void runAwaitsGrpcServerWhenRestIsDisabled() throws Exception {
        var grpcServer = new FakeServer();

        var result = ForensicAnalyticsServerApplication.run(
            new GrpcIngestionServerSettings(true, 9090),
            () -> grpcServer,
            new RestApiServerSettings(false, "127.0.0.1", 8080),
            () -> {
                throw new AssertionError("REST server supplier must not be called");
            }
        );

        assertTrue(result);
        assertTrue(grpcServer.started);
        assertTrue(grpcServer.awaited);
    }

    @Test
    void runReturnsFalseWhenGrpcAndRestServersAreDisabled() throws Exception {
        var result = ForensicAnalyticsServerApplication.run(
            new GrpcIngestionServerSettings(false, 9090),
            FakeServer::new,
            new RestApiServerSettings(false, "127.0.0.1", 8080),
            FakeRestApiServer::new
        );

        assertFalse(result);
    }

    @Test
    void runShutsDownStartedGrpcServerWhenRestStartupFails() {
        var grpcServer = new FakeServer();
        var operationLogger = new RecordingOperationLogger();

        assertThrows(
            IllegalStateException.class,
            () -> ForensicAnalyticsServerApplication.run(
                new GrpcIngestionServerSettings(true, 9090),
                () -> grpcServer,
                new RestApiServerSettings(true, "127.0.0.1", 8080),
                () -> {
                    throw new IllegalStateException("REST failed");
                },
                operationLogger
            )
        );

        assertTrue(grpcServer.started);
        assertTrue(grpcServer.shutdown);
        assertEquals(List.of("STARTED", "FAILED"), operationLogger.phases());
        assertEquals("IllegalStateException", operationLogger.events.get(1).errorType());
    }

    @Test
    void runLogsSingleGrpcStartupFailure() {
        var operationLogger = new RecordingOperationLogger();

        assertThrows(
            IllegalStateException.class,
            () -> ForensicAnalyticsServerApplication.run(
                new GrpcIngestionServerSettings(true, 9090),
                () -> {
                    throw new IllegalStateException("gRPC failed");
                },
                operationLogger
            )
        );

        assertEquals(List.of("STARTED", "FAILED"), operationLogger.phases());
        assertEquals("bootstrap.grpc-server", operationLogger.events.get(0).operation());
        assertEquals("IllegalStateException", operationLogger.events.get(1).errorType());
    }

    private static final class FakeServer extends Server {
        private boolean started;
        private boolean awaited;
        private boolean shutdown;

        @Override
        public Server start() throws IOException {
            started = true;
            return this;
        }

        @Override
        public Server shutdown() {
            shutdown = true;
            return this;
        }

        @Override
        public Server shutdownNow() {
            shutdown = true;
            return this;
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            awaited = true;
            return true;
        }

        @Override
        public void awaitTermination() {
            awaited = true;
        }
    }

    private static final class FakeRestApiServer implements RestApiServer {
        private boolean started;
        private boolean stopped;
        private boolean awaited;

        @Override
        public void start() {
            started = true;
        }

        @Override
        public void stop() {
            stopped = true;
        }

        @Override
        public void awaitTermination() {
            awaited = true;
        }

        @Override
        public int port() {
            return 8080;
        }
    }

    private static final class RecordingOperationLogger implements OperationLogger {
        private final List<Event> events = new CopyOnWriteArrayList<>();

        @Override
        public void started(String operation) {
            events.add(new Event(
                operation,
                "STARTED",
                CorrelationContext.current().map(correlationId -> correlationId.value()).orElse(""),
                -1L,
                ""
            ));
        }

        @Override
        public void succeeded(String operation, long durationMillis) {
            events.add(new Event(
                operation,
                "SUCCEEDED",
                CorrelationContext.current().map(correlationId -> correlationId.value()).orElse(""),
                durationMillis,
                ""
            ));
        }

        @Override
        public void failed(String operation, long durationMillis, Throwable error) {
            events.add(new Event(
                operation,
                "FAILED",
                CorrelationContext.current().map(correlationId -> correlationId.value()).orElse(""),
                durationMillis,
                error.getClass().getSimpleName()
            ));
        }

        private List<String> phases() {
            return events.stream().map(Event::phase).toList();
        }
    }

    private record Event(
        String operation,
        String phase,
        String correlationId,
        long durationMillis,
        String errorType
    ) {
    }
}
