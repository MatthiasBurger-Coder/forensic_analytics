package de.burger.forensics.analytics.services.repositorysource.application.port;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RepositorySourcePortValueObjectTest {
    @Test
    void validatesMetadataPreviewPolicyBounds() {
        assertEquals(60, new RepositoryMetadataPreviewPolicy(60).timeoutSeconds());
        assertThrows(IllegalArgumentException.class, () -> new RepositoryMetadataPreviewPolicy(0));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryMetadataPreviewPolicy(3_601));
    }

    @Test
    void normalizesIdempotencyPayloadAndRequiresStableRecordFields() {
        var createdAt = Instant.parse("2026-05-24T09:00:00Z");
        var expiresAt = Instant.parse("2026-05-24T10:00:00Z");
        var record = new RepositorySourceIdempotencyRecord(
            "idem-1",
            "CREATE_WORKSPACE",
            "fingerprint",
            "REPOSITORY_WORKSPACE",
            "workspace-0001",
            null,
            "COMPLETED",
            createdAt,
            expiresAt
        );

        assertEquals("", record.resultPayload());
        assertEquals(expiresAt, record.expiresAt());
        assertThrows(IllegalArgumentException.class, () -> new RepositorySourceIdempotencyRecord(
            " ",
            "CREATE_WORKSPACE",
            "fingerprint",
            "REPOSITORY_WORKSPACE",
            "workspace-0001",
            "",
            "COMPLETED",
            createdAt,
            null
        ));
        assertThrows(IllegalArgumentException.class, () -> new RepositorySourceIdempotencyRecord(
            "idem-1",
            " ",
            "fingerprint",
            "REPOSITORY_WORKSPACE",
            "workspace-0001",
            "",
            "COMPLETED",
            createdAt,
            null
        ));
        assertThrows(IllegalArgumentException.class, () -> new RepositorySourceIdempotencyRecord(
            "idem-1",
            "CREATE_WORKSPACE",
            " ",
            "REPOSITORY_WORKSPACE",
            "workspace-0001",
            "",
            "COMPLETED",
            createdAt,
            null
        ));
        assertThrows(IllegalArgumentException.class, () -> new RepositorySourceIdempotencyRecord(
            "idem-1",
            "CREATE_WORKSPACE",
            "fingerprint",
            " ",
            "workspace-0001",
            "",
            "COMPLETED",
            createdAt,
            null
        ));
        assertThrows(IllegalArgumentException.class, () -> new RepositorySourceIdempotencyRecord(
            "idem-1",
            "CREATE_WORKSPACE",
            "fingerprint",
            "REPOSITORY_WORKSPACE",
            " ",
            "",
            "COMPLETED",
            createdAt,
            null
        ));
        assertThrows(IllegalArgumentException.class, () -> new RepositorySourceIdempotencyRecord(
            "idem-1",
            "CREATE_WORKSPACE",
            "fingerprint",
            "REPOSITORY_WORKSPACE",
            "workspace-0001",
            "",
            " ",
            createdAt,
            null
        ));
        assertThrows(NullPointerException.class, () -> new RepositorySourceIdempotencyRecord(
            "idem-1",
            "CREATE_WORKSPACE",
            "fingerprint",
            "REPOSITORY_WORKSPACE",
            "workspace-0001",
            "",
            "COMPLETED",
            null,
            null
        ));
    }
}
