package de.burger.forensics.analytics.application.analysis.port;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnalysisPortContractTest {
    @Test
    void artifactStoreConflictExceptionPreservesMessage() {
        var exception = new ArtifactStoreConflictException("conflicting artifact metadata");

        assertEquals("conflicting artifact metadata", exception.getMessage());
    }
}
