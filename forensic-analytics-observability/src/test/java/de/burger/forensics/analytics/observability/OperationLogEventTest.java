package de.burger.forensics.analytics.observability;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationLogEventTest {
    @AfterEach
    void clearContext() {
        CorrelationContext.clear();
    }

    @Test
    void sanitizesOperationNameTokens() {
        var event = OperationLogEvent.started("rest /secret path");

        assertTrue(event.message().contains("operation=rest__secret_path"));
        assertFalse(event.message().contains("/secret"));
    }

    @Test
    void messageContainsOnlyStructuredFailureCategory() {
        try (var scope = CorrelationContext.open("corr-1")) {
            var event = OperationLogEvent.failed(
                "grpc.UploadAnalysisData",
                7,
                new IllegalStateException("token=secret at example.Secret(File.java:1)")
            );

            assertEquals("corr-1", scope.correlationId().value());
            assertEquals(Optional.of(new CorrelationId("corr-1")), event.correlationId());
            assertTrue(event.message().contains("phase=FAILED"));
            assertTrue(event.message().contains("durationMs=7"));
            assertTrue(event.message().contains("errorType=IllegalStateException"));
            assertFalse(event.message().contains("token=secret"));
            assertFalse(event.message().contains("example.Secret"));
        }
    }

    @Test
    void validatesRequiredEventFields() {
        assertThrows(IllegalArgumentException.class, () -> OperationLogEvent.started(" "));
        assertThrows(NullPointerException.class, () -> new OperationLogEvent(
            "operation",
            null,
            OperationLogLevel.INFO,
            Optional.empty(),
            -1,
            ""
        ));
        assertThrows(NullPointerException.class, () -> new OperationLogEvent(
            "operation",
            OperationLogPhase.STARTED,
            null,
            Optional.empty(),
            -1,
            ""
        ));
        assertThrows(NullPointerException.class, () -> new OperationLogEvent(
            "operation",
            OperationLogPhase.STARTED,
            OperationLogLevel.INFO,
            null,
            -1,
            ""
        ));
        assertThrows(IllegalArgumentException.class, () -> OperationLogEvent.succeeded("operation", -2));
    }
}
