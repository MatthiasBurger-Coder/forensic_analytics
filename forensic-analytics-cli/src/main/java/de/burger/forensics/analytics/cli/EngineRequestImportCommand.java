package de.burger.forensics.analytics.cli;

import java.nio.file.Path;
import java.util.Objects;

record EngineRequestImportCommand(
    Path requestFile,
    Path outputDirectory
) implements CliCommand {
    EngineRequestImportCommand {
        requestFile = Objects.requireNonNull(requestFile, "requestFile must not be null")
            .toAbsolutePath()
            .normalize();
        outputDirectory = Objects.requireNonNull(outputDirectory, "outputDirectory must not be null")
            .toAbsolutePath()
            .normalize();
    }
}
