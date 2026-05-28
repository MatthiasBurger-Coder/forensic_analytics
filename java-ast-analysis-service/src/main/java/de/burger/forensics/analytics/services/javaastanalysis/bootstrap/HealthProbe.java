package de.burger.forensics.analytics.services.javaastanalysis.bootstrap;

import java.net.URI;
import java.util.Arrays;

final class HealthProbe {
    private HealthProbe() {
    }

    static boolean isHealthCheck(String[] args) {
        return Arrays.asList(args).contains("--healthcheck");
    }

    static int run(String[] args) {
        var host = value(args, "--forensics.java-ast-analysis.service.health.host=", "127.0.0.1");
        var port = value(args, "--forensics.java-ast-analysis.service.health.port=", "8084");
        try {
            var connection = URI.create("http://" + host + ":" + port + "/health").toURL().openConnection();
            return ((java.net.HttpURLConnection) connection).getResponseCode() == 200 ? 0 : 1;
        } catch (Exception error) {
            return 1;
        }
    }

    private static String value(String[] args, String prefix, String defaultValue) {
        return Arrays.stream(args)
            .filter(argument -> argument.startsWith(prefix))
            .map(argument -> argument.substring(prefix.length()))
            .findFirst()
            .orElse(defaultValue);
    }
}
