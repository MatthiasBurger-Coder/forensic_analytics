package de.burger.forensics.analytics.application.ingestion.result;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class UploadAnalysisDataResultTest {
    @Test
    void rejectsNegativeReceivedItems() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new UploadAnalysisDataResult("session-1", IngestionStatus.ACCEPTED, -1, "invalid")
        );
    }
}
