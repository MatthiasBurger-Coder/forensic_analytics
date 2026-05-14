package de.burger.forensics.analytics.domain.source;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SourceDomainModelTest {
    @Test
    void storesSourceLocation() {
        var location = location();

        assertEquals("src/main/java/App.java", location.sourcePath());
        assertEquals("com.example.App", location.fullyQualifiedClassName());
        assertEquals("main", location.methodName());
        assertEquals(1, location.lineNumber());
    }

    @Test
    void rejectsInvalidSourceLocation() {
        assertThrows(IllegalArgumentException.class, () -> new SourceLocation(null, "Class", "method", 1));
        assertThrows(IllegalArgumentException.class, () -> new SourceLocation("", "Class", "method", 1));
        assertThrows(IllegalArgumentException.class, () -> new SourceLocation("App.java", null, "method", 1));
        assertThrows(IllegalArgumentException.class, () -> new SourceLocation("App.java", "", "method", 1));
        assertThrows(IllegalArgumentException.class, () -> new SourceLocation("App.java", "Class", null, 1));
        assertThrows(IllegalArgumentException.class, () -> new SourceLocation("App.java", "Class", "", 1));
        assertThrows(IllegalArgumentException.class, () -> new SourceLocation("App.java", "Class", "method", 0));
    }

    @Test
    void storesSourceFact() {
        var fact = sourceFact();

        assertEquals("method", fact.factType());
        assertEquals(location(), fact.location());
        assertEquals("void main()", fact.signature());
        assertEquals("main method", fact.summary());
    }

    @Test
    void rejectsInvalidSourceFact() {
        assertThrows(IllegalArgumentException.class, () -> new SourceFact(null, location(), "signature", "summary"));
        assertThrows(IllegalArgumentException.class, () -> new SourceFact("", location(), "signature", "summary"));
        assertThrows(NullPointerException.class, () -> new SourceFact("method", null, "signature", "summary"));
        assertThrows(IllegalArgumentException.class, () -> new SourceFact("method", location(), null, "summary"));
        assertThrows(IllegalArgumentException.class, () -> new SourceFact("method", location(), "", "summary"));
        assertThrows(IllegalArgumentException.class, () -> new SourceFact("method", location(), "signature", null));
        assertThrows(IllegalArgumentException.class, () -> new SourceFact("method", location(), "signature", ""));
    }

    private static SourceFact sourceFact() {
        return new SourceFact("method", location(), "void main()", "main method");
    }

    private static SourceLocation location() {
        return new SourceLocation("src/main/java/App.java", "com.example.App", "main", 1);
    }
}
