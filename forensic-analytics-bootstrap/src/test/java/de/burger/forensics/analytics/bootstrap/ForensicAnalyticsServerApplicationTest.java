package de.burger.forensics.analytics.bootstrap;

import io.grpc.Server;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
