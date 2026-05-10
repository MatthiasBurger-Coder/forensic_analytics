package de.burger.forensics.analytics.cli;

import de.burger.forensics.analytics.application.analysis.command.RunRepositoryAnalysisCommand;
import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.repository.RepositoryMetadata;

import java.net.URI;
import java.nio.file.Path;
import java.util.Objects;

record AnalyzeCommand(
    String repositoryLocation,
    String profile,
    Path outputDirectory,
    JoernMode joernMode
) implements CliCommand {
    private static final String UNKNOWN_REVISION = "UNKNOWN";

    AnalyzeCommand {
        requireText(repositoryLocation, "repository location");
        requireText(profile, "profile");
        outputDirectory = Objects.requireNonNull(outputDirectory, "outputDirectory must not be null")
            .toAbsolutePath()
            .normalize();
        Objects.requireNonNull(joernMode, "joernMode must not be null");
    }

    RunRepositoryAnalysisCommand toRunRepositoryAnalysisCommand() {
        return new RunRepositoryAnalysisCommand(
            AnalysisRunId.deterministic(repositoryLocation + "|" + profile + "|" + outputDirectory + "|" + joernMode.cliValue()),
            new RepositoryMetadata(projectId(), repositoryLocation, UNKNOWN_REVISION, UNKNOWN_REVISION),
            profile
        );
    }

    private String projectId() {
        try {
            var repositoryPath = repositoryLocation.startsWith("file:")
                ? Path.of(URI.create(repositoryLocation))
                : Path.of(repositoryLocation);
            var fileName = repositoryPath.getFileName();
            if (fileName != null && !fileName.toString().isBlank()) {
                return fileName.toString();
            }
        } catch (IllegalArgumentException ignored) {
            return "repository";
        }
        return "repository";
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
