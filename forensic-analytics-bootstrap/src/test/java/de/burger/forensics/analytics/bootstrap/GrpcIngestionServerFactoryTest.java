package de.burger.forensics.analytics.bootstrap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class GrpcIngestionServerFactoryTest {
    @Test
    void createsGrpcServerWithoutStartingIt() {
        var server = new GrpcIngestionServerFactory().create(new GrpcIngestionServerSettings(true, 9090));

        assertNotNull(server);
        server.shutdownNow();
    }
}
