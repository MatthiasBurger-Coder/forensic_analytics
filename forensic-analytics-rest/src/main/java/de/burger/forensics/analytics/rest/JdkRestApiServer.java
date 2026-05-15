package de.burger.forensics.analytics.rest;

import com.sun.net.httpserver.HttpServer;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

final class JdkRestApiServer implements RestApiServer {
    private final HttpServer server;
    private final ExecutorService executor;
    private final CountDownLatch termination = new CountDownLatch(1);
    private final AtomicBoolean started = new AtomicBoolean();

    JdkRestApiServer(HttpServer server, ExecutorService executor) {
        this.server = Objects.requireNonNull(server, "server must not be null");
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
    }

    @Override
    public void start() {
        if (started.compareAndSet(false, true)) {
            server.start();
        }
    }

    @Override
    public void stop() {
        server.stop(0);
        executor.shutdownNow();
        termination.countDown();
    }

    @Override
    public void awaitTermination() throws InterruptedException {
        termination.await();
    }

    @Override
    public int port() {
        return server.getAddress().getPort();
    }
}
