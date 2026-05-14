package de.burger.forensics.analytics.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EngineRequestImportCommandTest {
    @TempDir
    Path tempDir;

    @Test
    void normalizesPaths() {
        var command = new EngineRequestImportCommand(
            tempDir.resolve("requests").resolve("..").resolve("engine-request.json"),
            tempDir.resolve("out").resolve(".")
        );

        assertEquals(tempDir.resolve("engine-request.json").toAbsolutePath().normalize(), command.requestFile());
        assertEquals(tempDir.resolve("out").toAbsolutePath().normalize(), command.outputDirectory());
    }

    @Test
    void requiresPaths() {
        assertThrows(NullPointerException.class, () -> new EngineRequestImportCommand(null, tempDir));
        assertThrows(NullPointerException.class, () -> new EngineRequestImportCommand(tempDir.resolve("request.json"), null));
    }
}
