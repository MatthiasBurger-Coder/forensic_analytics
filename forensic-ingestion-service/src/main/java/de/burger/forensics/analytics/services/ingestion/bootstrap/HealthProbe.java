package de.burger.forensics.analytics.services.ingestion.bootstrap;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;

public final class HealthProbe {
    private HealthProbe() {
    }

    public static boolean isHealthCheck(String[] args) {
        return Arrays.asList(args).contains("--healthcheck");
    }

    public static int run(String[] args) {
        var host = option(args, "--forensics.ingestion.service.health.host=", "127.0.0.1");
        var port = Integer.parseInt(option(args, "--forensics.ingestion.service.health.port=", "8081"));
        try {
            var connection = (HttpURLConnection) URI.create("http://" + host + ":" + port + "/health")
                .toURL()
                .openConnection();
            connection.setConnectTimeout((int) Duration.ofSeconds(2).toMillis());
            connection.setReadTimeout((int) Duration.ofSeconds(2).toMillis());
            return connection.getResponseCode() == 200 ? 0 : 1;
        } catch (IOException error) {
            return 1;
        }
    }

    private static String option(String[] args, String prefix, String defaultValue) {
        return Arrays.stream(args)
            .filter(arg -> arg.startsWith(prefix))
            .map(arg -> arg.substring(prefix.length()))
            .findFirst()
            .orElse(defaultValue);
    }
}
