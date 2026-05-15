package de.burger.forensics.analytics.observability;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorrelationContextTest {
    @AfterEach
    void clearContext() {
        CorrelationContext.clear();
    }

    @Test
    void generatedScopeExposesCorrelationIdUntilClosed() {
        assertTrue(CorrelationContext.current().isEmpty());

        CorrelationId generated;
        try (var scope = CorrelationContext.openGenerated()) {
            generated = scope.correlationId();
            assertEquals(generated, CorrelationContext.current().orElseThrow());
            assertFalse(generated.value().isBlank());
        }

        assertTrue(CorrelationContext.current().isEmpty());
    }

    @Test
    void externalCorrelationIdIsSanitizedBeforeItEntersContext() {
        try (var scope = CorrelationContext.open(" request / secret ")) {
            assertEquals("request___secret", scope.correlationId().value());
            assertEquals("request___secret", CorrelationContext.current().orElseThrow().value());
        }
    }

    @Test
    void nullAndBlankExternalCorrelationIdsGenerateNewIds() {
        try (var nullScope = CorrelationContext.open((String) null)) {
            assertFalse(nullScope.correlationId().value().isBlank());
        }

        try (var blankScope = CorrelationContext.open(" ")) {
            assertFalse(blankScope.correlationId().value().isBlank());
        }
    }

    @Test
    void nestedScopesRestoreOuterCorrelationId() {
        try (var outer = CorrelationContext.open(new CorrelationId("outer"))) {
            try (var inner = CorrelationContext.open(new CorrelationId("inner"))) {
                assertEquals("inner", CorrelationContext.current().orElseThrow().value());
                assertEquals("inner", inner.correlationId().value());
            }

            assertEquals("outer", CorrelationContext.current().orElseThrow().value());
            assertEquals("outer", outer.correlationId().value());
        }

        assertTrue(CorrelationContext.current().isEmpty());
    }

    @Test
    void closingScopeTwiceDoesNotRemoveReplacementScope() {
        var first = CorrelationContext.open(new CorrelationId("first"));
        first.close();

        try (var second = CorrelationContext.open(new CorrelationId("second"))) {
            first.close();

            assertEquals("second", second.correlationId().value());
            assertEquals("second", CorrelationContext.current().orElseThrow().value());
        }
    }

    @Test
    void closingNestedScopeOutOfOrderKeepsCurrentTopScope() {
        try (var outer = CorrelationContext.open(new CorrelationId("outer"))) {
            var middle = CorrelationContext.open(new CorrelationId("middle"));
            try (var inner = CorrelationContext.open(new CorrelationId("inner"))) {
                middle.close();

                assertEquals("inner", inner.correlationId().value());
                assertEquals("inner", CorrelationContext.current().orElseThrow().value());
            }

            assertEquals("outer", CorrelationContext.current().orElseThrow().value());
            assertEquals("outer", outer.correlationId().value());
        }
    }

    @Test
    void closingAfterClearIsHarmless() {
        var scope = CorrelationContext.open(new CorrelationId("first"));

        CorrelationContext.clear();
        scope.close();

        assertTrue(CorrelationContext.current().isEmpty());
    }

    @Test
    void rejectsBlankCorrelationIds() {
        assertThrows(IllegalArgumentException.class, () -> new CorrelationId(" "));
        assertThrows(NullPointerException.class, () -> CorrelationContext.open((CorrelationId) null));
    }
}
