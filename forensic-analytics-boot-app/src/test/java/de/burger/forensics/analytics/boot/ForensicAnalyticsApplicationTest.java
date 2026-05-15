package de.burger.forensics.analytics.boot;

import de.burger.forensics.analytics.application.ingestion.ForensicIngestionUseCase;
import de.burger.forensics.analytics.application.ingestion.RepositoryAnalysisIngestionUseCase;
import de.burger.forensics.analytics.application.ingestion.RepositoryAnalysisQueryUseCase;
import de.burger.forensics.analytics.adapter.joern.docker.JoernDockerSettings;
import de.burger.forensics.analytics.boot.config.ForensicAnalyticsProperties;
import de.burger.forensics.analytics.boot.grpc.GrpcServerLifecycle;
import de.burger.forensics.analytics.boot.rest.RestApiServerLifecycle;
import de.burger.forensics.analytics.ingestion.v1.BuildIdentity;
import de.burger.forensics.analytics.ingestion.v1.ForensicIngestionServiceGrpc;
import de.burger.forensics.analytics.ingestion.v1.IngestionStatus;
import de.burger.forensics.analytics.ingestion.v1.PluginIdentity;
import de.burger.forensics.analytics.ingestion.v1.StartAnalysisSessionRequest;
import de.burger.forensics.analytics.logging.ForensicLoggerFactory;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForensicAnalyticsApplicationTest {
    @Test
    void startsMinimalContextWithTestProfileAndServersDisabled() {
        try (var context = new SpringApplicationBuilder(ForensicAnalyticsApplication.class)
            .web(WebApplicationType.NONE)
            .profiles("test")
            .run("--spring.main.banner-mode=off")) {

            assertNotNull(context.getBean(ForensicAnalyticsProperties.class));
            assertNotNull(context.getBean(ForensicLoggerFactory.class));
            assertNotNull(context.getBean(JoernDockerSettings.class));
            assertNotNull(context.getBean(ForensicIngestionUseCase.class));
            assertNotNull(context.getBean(RepositoryAnalysisIngestionUseCase.class));
            assertNotNull(context.getBean(RepositoryAnalysisQueryUseCase.class));
            var grpcLifecycle = context.getBean(GrpcServerLifecycle.class);
            var restLifecycle = context.getBean(RestApiServerLifecycle.class);

            grpcLifecycle.start();
            restLifecycle.start();
            grpcLifecycle.stop();
            restLifecycle.stop();

            assertFalse(grpcLifecycle.isRunning());
            assertFalse(restLifecycle.isRunning());
        }
    }

    @Test
    void mainEntrypointStartsWithTestProfile() {
        ForensicAnalyticsApplication.main(new String[] {
            "--spring.main.banner-mode=off",
            "--spring.main.web-application-type=none",
            "--spring.main.register-shutdown-hook=false",
            "--spring.profiles.active=test"
        });

        try (var context = ForensicAnalyticsApplication.run(new String[] {
            "--spring.main.banner-mode=off",
            "--spring.main.web-application-type=none",
            "--spring.profiles.active=test"
        })) {
            assertNotNull(context.getBean(ForensicAnalyticsProperties.class));
        }
    }

    @Test
    void dockerProfileDoesNotExposeServersByDefault() {
        try (var context = new SpringApplicationBuilder(ForensicAnalyticsApplication.class)
            .web(WebApplicationType.NONE)
            .profiles("docker")
            .run("--spring.main.banner-mode=off")) {

            var properties = context.getBean(ForensicAnalyticsProperties.class);
            assertFalse(properties.grpc().enabled());
            assertFalse(properties.rest().enabled());
            assertFalse(context.getBean(GrpcServerLifecycle.class).isRunning());
            assertFalse(context.getBean(RestApiServerLifecycle.class).isRunning());
        }
    }

    @Test
    void startsGrpcLifecycleWhenEnabled() throws Exception {
        try (var context = new SpringApplicationBuilder(ForensicAnalyticsApplication.class)
            .web(WebApplicationType.NONE)
            .run(
                "--spring.main.banner-mode=off",
                "--forensics.analytics.ingestion.grpc.enabled=true",
                "--forensics.analytics.ingestion.grpc.port=0",
                "--forensics.analytics.rest.enabled=false"
            )) {

            var lifecycle = context.getBean(GrpcServerLifecycle.class);
            assertTrue(lifecycle.isRunning());
            assertTrue(lifecycle.port() > 0);
            lifecycle.start();
            assertGrpcServerAcceptsSessionStart(lifecycle.port());
        }
    }

    @Test
    void startsRestLifecycleWhenEnabled() {
        try (var context = new SpringApplicationBuilder(ForensicAnalyticsApplication.class)
            .web(WebApplicationType.NONE)
            .run(
                "--spring.main.banner-mode=off",
                "--forensics.analytics.ingestion.grpc.enabled=false",
                "--forensics.analytics.rest.enabled=true",
                "--forensics.analytics.rest.port=0"
            )) {

            var lifecycle = context.getBean(RestApiServerLifecycle.class);
            assertTrue(lifecycle.isRunning());
            assertTrue(lifecycle.port() > 0);
            lifecycle.start();
        }
    }

    private static void assertGrpcServerAcceptsSessionStart(int port) throws Exception {
        var channel = ManagedChannelBuilder.forAddress("127.0.0.1", port)
            .usePlaintext()
            .build();
        try {
            var response = ForensicIngestionServiceGrpc.newBlockingStub(channel)
                .startAnalysisSession(startRequest());

            assertFalse(response.getSessionId().isBlank());
            assertEquals(IngestionStatus.INGESTION_STATUS_ACCEPTED, response.getStatus());
        } finally {
            channel.shutdownNow();
            assertTrue(channel.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private static StartAnalysisSessionRequest startRequest() {
        return StartAnalysisSessionRequest.newBuilder()
            .setBuildIdentity(BuildIdentity.newBuilder()
                .setProjectId("project-a")
                .setRepositoryUrl("https://example.invalid/repo.git")
                .setBranchName("main")
                .setCommitHash("abcdef")
                .setBuildId("build-1")
                .setScanTimestamp("2026-05-09T12:00:00Z"))
            .setPluginIdentity(PluginIdentity.newBuilder()
                .setPluginName("forensic-plugin")
                .setPluginVersion("0.1.0"))
            .setSchemaVersion("schema-v1")
            .build();
    }

}
