package de.burger.forensics.analytics.observability;

import org.junit.jupiter.api.Test;

class NoopOperationLoggerTest {
    @Test
    void ignoresInvalidInputsBecauseItIsADeactivatedLogger() {
        var logger = OperationLogger.noop();

        logger.started(" ");
        logger.succeeded(null, -100);
        logger.failed("", -100, null);
    }
}
