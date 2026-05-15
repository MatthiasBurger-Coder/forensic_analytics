package de.burger.forensics.analytics.rest;

import com.sun.net.httpserver.HttpServer;
import de.burger.forensics.analytics.application.ingestion.RepositoryAnalysisIngestionUseCase;
import de.burger.forensics.analytics.application.ingestion.RepositoryAnalysisQueryUseCase;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class RestApiServerFactory {
    private static final int BACKLOG = 32;
    private static final int WORKER_THREADS = 4;

    public RestApiServer create(
        InetSocketAddress address,
        RepositoryAnalysisIngestionUseCase ingestionUseCase,
        RepositoryAnalysisQueryUseCase queryUseCase
    ) throws IOException {
        Objects.requireNonNull(address, "address must not be null");
        var server = HttpServer.create(address, BACKLOG);
        server.createContext("/api", new RepositoryAnalysisHttpHandler(ingestionUseCase, queryUseCase));
        var executor = restExecutor();
        server.setExecutor(executor);
        return new JdkRestApiServer(server, executor);
    }

    private static ExecutorService restExecutor() {
        return Executors.newFixedThreadPool(WORKER_THREADS, new RestThreadFactory());
    }

    private static final class RestThreadFactory implements ThreadFactory {
        private final AtomicInteger nextThreadId = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            var thread = new Thread(runnable, "forensic-rest-" + nextThreadId.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
