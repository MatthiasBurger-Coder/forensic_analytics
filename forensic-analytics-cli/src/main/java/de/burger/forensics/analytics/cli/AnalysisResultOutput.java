package de.burger.forensics.analytics.cli;

import de.burger.forensics.analytics.application.analysis.result.RunRepositoryAnalysisResult;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

final class AnalysisResultOutput {
    private static final String SUMMARY_FILE_NAME = "analysis-summary.txt";

    Path write(AnalyzeCommand command, RunRepositoryAnalysisResult result) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(result, "result must not be null");
        try {
            Files.createDirectories(command.outputDirectory());
            var summaryPath = command.outputDirectory().resolve(SUMMARY_FILE_NAME);
            Files.writeString(summaryPath, format(command, result), StandardCharsets.UTF_8);
            return summaryPath;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write CLI analysis output.", e);
        }
    }

    String format(AnalyzeCommand command, RunRepositoryAnalysisResult result) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(result, "result must not be null");
        return String.join(System.lineSeparator(),
            "analysisRunId=" + result.analysisRunId().value(),
            "projectId=" + result.repositoryMetadata().projectId(),
            "repositoryLocation=" + result.repositoryMetadata().repositoryLocation(),
            "profile=" + result.analysisProfile(),
            "joernMode=" + command.joernMode().cliValue(),
            "status=" + result.status(),
            "sourceFacts=" + result.sourceFacts().size(),
            "semanticProvider=" + result.semanticAnalysis().providerName(),
            "semanticArtifacts=" + result.semanticAnalysis().artifacts().size(),
            "ruleArtifacts=" + result.ruleGeneration().artifacts().size(),
            ""
        );
    }
}
