package de.burger.forensics.analytics.domain.analysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnalysisRunIdTest {
    @Test
    void createsRandomAndDeterministicIds() {
        assertNotNull(AnalysisRunId.random().value());
        assertEquals(AnalysisRunId.deterministic("same"), AnalysisRunId.deterministic("same"));
        assertNotEquals(AnalysisRunId.deterministic("same"), AnalysisRunId.deterministic("other"));
    }

    @Test
    void rejectsInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> new AnalysisRunId(null));
        assertThrows(IllegalArgumentException.class, () -> new AnalysisRunId(" "));
        assertThrows(NullPointerException.class, () -> AnalysisRunId.deterministic(null));
    }
}
