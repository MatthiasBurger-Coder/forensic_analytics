package de.burger.forensics.analytics.engine;

import de.burger.forensics.analytics.application.analysis.RunRepositoryAnalysisUseCase;
import de.burger.forensics.analytics.application.analysis.command.RunRepositoryAnalysisCommand;
import de.burger.forensics.analytics.application.analysis.result.RuleGenerationResult;
import de.burger.forensics.analytics.application.analysis.result.RunRepositoryAnalysisResult;
import de.burger.forensics.analytics.application.analysis.result.SemanticAnalysisResult;
import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.repository.RepositoryMetadata;
import de.burger.forensics.analytics.domain.semantic.SemanticGraph;
import de.burger.forensics.analytics.domain.source.SourceFact;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RepositoryAnalysisEngineTest {
    @Test
    void delegatesRepositoryAnalysisToApplicationUseCase() {
        var useCase = new RecordingUseCase();
        var engine = new RepositoryAnalysisEngine(useCase);
        var command = command();

        var result = engine.run(command);

        assertEquals(command, useCase.recordedCommand);
        assertEquals(command.analysisRunId(), result.analysisRunId());
        assertEquals(command.repositoryMetadata(), result.repositoryMetadata());
    }

    @Test
    void useCaseIsRequired() {
        assertThrows(NullPointerException.class, () -> new RepositoryAnalysisEngine(null));
    }

    @Test
    void commandIsRequired() {
        var engine = new RepositoryAnalysisEngine(new RecordingUseCase());

        assertThrows(NullPointerException.class, () -> engine.run(null));
    }

    private static RunRepositoryAnalysisCommand command() {
        return new RunRepositoryAnalysisCommand(
            new AnalysisRunId("analysis-1"),
            new RepositoryMetadata("project-a", "file:///workspace/project", "main", "abcdef"),
            "baseline"
        );
    }

    private static final class RecordingUseCase implements RunRepositoryAnalysisUseCase {
        private RunRepositoryAnalysisCommand recordedCommand;

        @Override
        public RunRepositoryAnalysisResult run(RunRepositoryAnalysisCommand command) {
            recordedCommand = command;
            return RunRepositoryAnalysisResult.completed(
                command.analysisRunId(),
                command.repositoryMetadata(),
                command.analysisProfile(),
                List.<SourceFact>of(),
                new SemanticAnalysisResult("fake-semantic", "sha256:semantic", List.of(), SemanticGraph.empty()),
                new RuleGenerationResult(List.of())
            );
        }
    }
}
