package de.burger.forensics.analytics.domain.artifact;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArtifactReferenceTest {
    @Test
    void storesArtifactMetadata() {
        var reference = new ArtifactReference("forensics.btm", "byteman-rules", "abc123", 42L);

        assertEquals("forensics.btm", reference.path());
        assertEquals("byteman-rules", reference.type());
        assertEquals("abc123", reference.sha256());
        assertEquals(42L, reference.sizeBytes());
    }

    @Test
    void rejectsInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> new ArtifactReference(null, "type", "abc", 1L));
        assertThrows(IllegalArgumentException.class, () -> new ArtifactReference("", "type", "abc", 1L));
        assertThrows(IllegalArgumentException.class, () -> new ArtifactReference("path", null, "abc", 1L));
        assertThrows(IllegalArgumentException.class, () -> new ArtifactReference("path", "", "abc", 1L));
        assertThrows(IllegalArgumentException.class, () -> new ArtifactReference("path", "type", null, 1L));
        assertThrows(IllegalArgumentException.class, () -> new ArtifactReference("path", "type", "", 1L));
        assertThrows(IllegalArgumentException.class, () -> new ArtifactReference("path", "type", "abc", -1L));
    }
}
