package de.burger.forensics.analytics.cli;

import de.burger.forensics.analytics.ingestion.request.EngineIngestionImportResult;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

final class EngineRequestImportOutput {
    private static final String SUMMARY_FILE_NAME = "engine-request-import-summary.txt";

    Path write(EngineRequestImportCommand command, EngineIngestionImportResult result) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(result, "result must not be null");
        try {
            Files.createDirectories(command.outputDirectory());
            var summaryPath = command.outputDirectory().resolve(SUMMARY_FILE_NAME);
            Files.writeString(summaryPath, format(command, result), StandardCharsets.UTF_8);
            return summaryPath;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write engine request import output.", e);
        }
    }

    String format(EngineRequestImportCommand command, EngineIngestionImportResult result) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(result, "result must not be null");
        return String.join(System.lineSeparator(),
            "requestFile=" + command.requestFile(),
            "sessionId=" + result.sessionId(),
            "status=" + result.completionStatus(),
            "uploadedPayloads=" + result.uploadedPayloads(),
            ""
        );
    }
}
