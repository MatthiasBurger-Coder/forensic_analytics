package de.burger.forensics.analytics.bootstrap;

import io.grpc.Server;

import java.util.function.Supplier;

public final class ForensicAnalyticsServerApplication {
    private ForensicAnalyticsServerApplication() {
    }

    public static void main(String[] args) throws Exception {
        var settings = GrpcIngestionServerSettings.fromEnvironment();
        run(settings, () -> new GrpcIngestionServerFactory().create(settings));
    }

    static boolean run(GrpcIngestionServerSettings settings, Supplier<Server> serverSupplier) throws Exception {
        if (!settings.enabled()) {
            return false;
        }

        var server = serverSupplier.get();
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
        server.awaitTermination();
        return true;
    }
}
