package de.burger.forensics.analytics.services.joernanalysis.bootstrap;

import java.net.URI;
import java.util.Arrays;

final class HealthProbe {
    private HealthProbe() {
    }

    static boolean isHealthCheck(String[] args) {
        return Arrays.asList(args).contains("--healthcheck");
    }

    static int run(String[] args) {
        var host = property(args, "--forensics.joern-analysis.service.health.host=", "127.0.0.1");
        var port = property(args, "--forensics.joern-analysis.service.health.port=", "8087");
        try {
            var connection = URI.create("http://" + host + ":" + port + "/health").toURL().openConnection();
            return ((java.net.HttpURLConnection) connection).getResponseCode() == 200 ? 0 : 1;
        } catch (Exception error) {
            return 1;
        }
    }

    private static String property(String[] args, String prefix, String fallback) {
        return Arrays.stream(args)
            .filter(arg -> arg.startsWith(prefix))
            .map(arg -> arg.substring(prefix.length()))
            .findFirst()
            .orElse(fallback);
    }
}
