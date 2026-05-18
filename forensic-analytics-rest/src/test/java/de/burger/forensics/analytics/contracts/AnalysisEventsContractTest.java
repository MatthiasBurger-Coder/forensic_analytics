package de.burger.forensics.analytics.contracts;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisEventsContractTest {
    @Test
    void artifactRegisteredEventKeepsByteCustodyExplicitAfterMetadataAcceptance() throws IOException {
        var contract = Files.readString(findAnalysisEventsContract());
        var event = section(contract, "### `analysis.artifact.registered`", "### `analysis.report.requested`");

        assertContains(event, "`producerService`");
        assertContains(event, "`byteOwnerService`");
        assertContains(event, "`metadataOwnerService`");
        assertContains(event, "`byteCustody`");
        assertContains(event, "`PRODUCER_RETAINED`, `SCOPED_OBJECT_ACCESS` or `EXPLICIT_HANDOFF`");
        assertContains(event, "Analysis Store acceptance registers canonical artifact metadata only");
        assertContains(event, "does not transfer byte custody");
    }

    private static Path findAnalysisEventsContract() {
        var current = Path.of("").toAbsolutePath();
        while (current != null) {
            var candidate = current.resolve("contracts/events/analysis-events.md");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("contracts/events/analysis-events.md not found from test working directory");
    }

    private static String section(String content, String startMarker, String endMarker) {
        var start = content.indexOf(startMarker);
        var end = content.indexOf(endMarker, start + startMarker.length());
        if (start < 0 || end < 0) {
            throw new AssertionError("Cannot find section from " + startMarker + " to " + endMarker);
        }
        return content.substring(start, end);
    }

    private static void assertContains(String content, String expected) {
        assertTrue(normalize(content).contains(normalize(expected)),
            () -> "Expected contract content to contain: " + expected);
    }

    private static String normalize(String value) {
        return value.replaceAll("\\s+", " ").trim();
    }
}
