package de.burger.forensics.analytics.bootstrap;

import io.grpc.Server;
import de.burger.forensics.analytics.rest.RestApiServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

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

        var result = ForensicAnalyticsServerApplication.run(
            new GrpcIngestionServerSettings(true, 9090),
            () -> grpcServer,
            new RestApiServerSettings(true, "127.0.0.1", 8080),
            () -> restServer
        );

        assertTrue(result);
        assertTrue(grpcServer.started);
        assertTrue(grpcServer.awaited);
        assertTrue(restServer.started);
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

        assertThrows(
            IllegalStateException.class,
            () -> ForensicAnalyticsServerApplication.run(
                new GrpcIngestionServerSettings(true, 9090),
                () -> grpcServer,
                new RestApiServerSettings(true, "127.0.0.1", 8080),
                () -> {
                    throw new IllegalStateException("REST failed");
                }
            )
        );

        assertTrue(grpcServer.started);
        assertTrue(grpcServer.shutdown);
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
}
