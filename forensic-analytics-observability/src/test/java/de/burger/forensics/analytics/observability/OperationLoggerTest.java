package de.burger.forensics.analytics.observability;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OperationLoggerTest {
    @Test
    void loggedSupplierRecordsSuccessfulOperationLifecycle() {
        var logger = new RecordingOperationLogger();

        var result = logger.logged("adapter.sample", () -> "completed");

        assertEquals("completed", result);
        assertEquals(List.of("started:adapter.sample", "succeeded:adapter.sample"), logger.events());
    }

    @Test
    void loggedSupplierRecordsFailedOperationAndRethrowsOriginalError() {
        var logger = new RecordingOperationLogger();
        var failure = new IllegalStateException("hidden details");

        var thrown = assertThrows(IllegalStateException.class, () -> logger.logged("adapter.sample", () -> {
            throw failure;
        }));

        assertSame(failure, thrown);
        assertEquals(List.of("started:adapter.sample", "failed:adapter.sample:IllegalStateException"), logger.events());
    }

    @Test
    void loggedSupplierCanRepresentVoidOperations() {
        var logger = new RecordingOperationLogger();

        logger.logged("adapter.runnable", () -> {
            return null;
        });

        assertEquals(List.of("started:adapter.runnable", "succeeded:adapter.runnable"), logger.events());
    }

    private static final class RecordingOperationLogger implements OperationLogger {
        private final List<String> events = new ArrayList<>();

        @Override
        public void started(String operation) {
            events.add("started:" + operation);
        }

        @Override
        public void succeeded(String operation, long durationMillis) {
            events.add("succeeded:" + operation);
        }

        @Override
        public void failed(String operation, long durationMillis, Throwable error) {
            events.add("failed:" + operation + ":" + error.getClass().getSimpleName());
        }

        private List<String> events() {
            return List.copyOf(events);
        }
    }
}
