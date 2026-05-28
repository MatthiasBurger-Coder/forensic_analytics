package de.burger.forensics.analytics.services.repositoryanalysis.bootstrap;

import java.net.URI;

public final class HealthProbe {
    private HealthProbe() {
    }

    public static boolean isHealthCheck(String[] args) {
        for (String arg : args) {
            if ("--healthcheck".equals(arg)) {
                return true;
            }
        }
        return false;
    }

    public static int run(String[] args) {
        var host = "127.0.0.1";
        var port = 8083;
        for (String arg : args) {
            if (arg.startsWith("--forensics.repository-analysis.service.health.host=")) {
                host = arg.substring(arg.indexOf('=') + 1);
            }
            if (arg.startsWith("--forensics.repository-analysis.service.health.port=")) {
                port = Integer.parseInt(arg.substring(arg.indexOf('=') + 1));
            }
        }
        try {
            var connection = URI.create("http://" + host + ":" + port + "/health").toURL().openConnection();
            return ((java.net.HttpURLConnection) connection).getResponseCode() == 200 ? 0 : 1;
        } catch (Exception error) {
            return 1;
        }
    }
}
