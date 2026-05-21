package de.burger.forensics.analytics.services.ingestion.application.command;

import de.burger.forensics.analytics.services.ingestion.domain.AnalysisPayloadKind;
import de.burger.forensics.analytics.services.ingestion.domain.BuildIdentity;
import de.burger.forensics.analytics.services.ingestion.domain.ModuleIdentity;
import de.burger.forensics.analytics.services.ingestion.domain.PayloadDescriptor;
import de.burger.forensics.analytics.services.ingestion.domain.PluginIdentity;
import de.burger.forensics.analytics.services.ingestion.domain.RawIngestionPayload;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IngestionCommandTest {
    @Test
    void trimsValidTextFields() {
        var start = new StartAnalysisSessionCommand(buildIdentity(), pluginIdentity(), " schema-v1 ");
        var upload = new UploadAnalysisDataCommand(" session-1 ", payload());

        assertEquals("schema-v1", start.schemaVersion());
        assertEquals("session-1", upload.sessionId());
    }

    @Test
    void rejectsInvalidStartCommandFields() {
        assertThrows(NullPointerException.class, () -> new StartAnalysisSessionCommand(null, pluginIdentity(), "schema-v1"));
        assertThrows(NullPointerException.class, () -> new StartAnalysisSessionCommand(buildIdentity(), null, "schema-v1"));
        assertThrows(IllegalArgumentException.class, () -> new StartAnalysisSessionCommand(
            buildIdentity(),
            pluginIdentity(),
            null
        ));
        assertThrows(IllegalArgumentException.class, () -> new StartAnalysisSessionCommand(
            buildIdentity(),
            pluginIdentity(),
            " "
        ));
    }

    @Test
    void rejectsInvalidUploadCommandFields() {
        assertThrows(IllegalArgumentException.class, () -> new UploadAnalysisDataCommand(null, payload()));
        assertThrows(IllegalArgumentException.class, () -> new UploadAnalysisDataCommand(" ", payload()));
        assertThrows(NullPointerException.class, () -> new UploadAnalysisDataCommand("session-1", null));
    }

    private static RawIngestionPayload payload() {
        return new RawIngestionPayload(
            buildIdentity(),
            new ModuleIdentity("module-a", ":module-a"),
            pluginIdentity(),
            "schema-v1",
            new PayloadDescriptor("payload-a", AnalysisPayloadKind.SOURCE_FACTS, "application/json", Map.of()),
            "{}".getBytes(StandardCharsets.UTF_8)
        );
    }

    private static BuildIdentity buildIdentity() {
        return new BuildIdentity(
            "project-a",
            "https://example.invalid/repo.git",
            "main",
            "abcdef",
            "build-1",
            "2026-05-16T00:00:00Z"
        );
    }

    private static PluginIdentity pluginIdentity() {
        return new PluginIdentity("forensic-plugin", "0.1.0");
    }
}
