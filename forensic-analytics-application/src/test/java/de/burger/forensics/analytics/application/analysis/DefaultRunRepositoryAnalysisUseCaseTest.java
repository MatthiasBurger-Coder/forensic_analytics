package de.burger.forensics.analytics.application.analysis;

import de.burger.forensics.analytics.application.analysis.command.RunRepositoryAnalysisCommand;
import de.burger.forensics.analytics.application.analysis.command.RuleGenerationRequest;
import de.burger.forensics.analytics.application.analysis.command.SemanticAnalysisRequest;
import de.burger.forensics.analytics.application.analysis.port.RepositoryAnalysisResultStore;
import de.burger.forensics.analytics.application.analysis.port.RepositorySourcePort;
import de.burger.forensics.analytics.application.analysis.port.RuleGenerationPort;
import de.burger.forensics.analytics.application.analysis.port.SemanticAnalysisPort;
import de.burger.forensics.analytics.application.analysis.port.SourceScannerPort;
import de.burger.forensics.analytics.application.analysis.result.RepositoryAnalysisStatus;
import de.burger.forensics.analytics.application.analysis.result.RuleGenerationResult;
import de.burger.forensics.analytics.application.analysis.result.RunRepositoryAnalysisResult;
import de.burger.forensics.analytics.application.analysis.result.SemanticAnalysisResult;
import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.artifact.ArtifactReference;
import de.burger.forensics.analytics.domain.repository.RepositoryMetadata;
import de.burger.forensics.analytics.domain.repository.RepositorySource;
import de.burger.forensics.analytics.domain.semantic.SemanticGraph;
import de.burger.forensics.analytics.domain.source.SourceFact;
import de.burger.forensics.analytics.domain.source.SourceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultRunRepositoryAnalysisUseCaseTest {
    private final RecordingRepositorySourcePort repositorySourcePort = new RecordingRepositorySourcePort();
    private final RecordingSourceScannerPort sourceScannerPort = new RecordingSourceScannerPort();
    private final RecordingSemanticAnalysisPort semanticAnalysisPort = new RecordingSemanticAnalysisPort();
    private final RecordingRuleGenerationPort ruleGenerationPort = new RecordingRuleGenerationPort();
    private final RecordingResultStore resultStore = new RecordingResultStore();
    private final DefaultRunRepositoryAnalysisUseCase useCase = new DefaultRunRepositoryAnalysisUseCase(
        repositorySourcePort,
        sourceScannerPort,
        semanticAnalysisPort,
        ruleGenerationPort,
        resultStore
    );

    @Test
    void runCoordinatesRepositoryAnalysisThroughApplicationPorts() {
        var command = command();

        var result = useCase.run(command);

        assertEquals(repositoryMetadata(), repositorySourcePort.resolvedMetadata);
        assertEquals(repositorySource(), sourceScannerPort.scannedSource);
        assertEquals(command.analysisRunId(), semanticAnalysisPort.request.analysisRunId());
        assertEquals(repositorySource(), semanticAnalysisPort.request.repositorySource());
        assertEquals(List.of(sourceFact()), semanticAnalysisPort.request.sourceFacts());
        assertEquals(semanticAnalysisPort.result, ruleGenerationPort.request.semanticAnalysis());
        assertEquals(List.of(sourceFact()), ruleGenerationPort.request.sourceFacts());
        assertEquals(RepositoryAnalysisStatus.COMPLETED, result.status());
        assertEquals(command.analysisRunId(), result.analysisRunId());
        assertEquals(command.repositoryMetadata(), result.repositoryMetadata());
        assertEquals("baseline", result.analysisProfile());
        assertEquals(List.of(sourceFact()), result.sourceFacts());
        assertEquals(semanticAnalysisPort.result, result.semanticAnalysis());
        assertEquals(ruleGenerationPort.result, result.ruleGeneration());
        assertEquals(result, resultStore.storedResult);
    }

    @Test
    void dependenciesAreRequired() {
        assertThrows(
            NullPointerException.class,
            () -> new DefaultRunRepositoryAnalysisUseCase(
                null,
                sourceScannerPort,
                semanticAnalysisPort,
                ruleGenerationPort,
                resultStore
            )
        );
        assertThrows(
            NullPointerException.class,
            () -> new DefaultRunRepositoryAnalysisUseCase(
                repositorySourcePort,
                null,
                semanticAnalysisPort,
                ruleGenerationPort,
                resultStore
            )
        );
        assertThrows(
            NullPointerException.class,
            () -> new DefaultRunRepositoryAnalysisUseCase(
                repositorySourcePort,
                sourceScannerPort,
                null,
                ruleGenerationPort,
                resultStore
            )
        );
        assertThrows(
            NullPointerException.class,
            () -> new DefaultRunRepositoryAnalysisUseCase(
                repositorySourcePort,
                sourceScannerPort,
                semanticAnalysisPort,
                null,
                resultStore
            )
        );
        assertThrows(
            NullPointerException.class,
            () -> new DefaultRunRepositoryAnalysisUseCase(
                repositorySourcePort,
                sourceScannerPort,
                semanticAnalysisPort,
                ruleGenerationPort,
                null
            )
        );
    }

    @Test
    void commandIsRequired() {
        assertThrows(NullPointerException.class, () -> useCase.run(null));
    }

    @Test
    void requestAndResultModelsCopyMutableInputs() {
        var facts = new ArrayList<SourceFact>();
        facts.add(sourceFact());
        var semantic = semanticResult();
        var semanticRequest = new SemanticAnalysisRequest(analysisRunId(), repositorySource(), facts);
        var ruleRequest = new RuleGenerationRequest(analysisRunId(), repositorySource(), facts, semantic);
        var result = RunRepositoryAnalysisResult.completed(
            analysisRunId(),
            repositoryMetadata(),
            "baseline",
            facts,
            semantic,
            ruleResult()
        );
        facts.add(new SourceFact("method", sourceLocation(), "main()", "main"));

        assertEquals(List.of(sourceFact()), semanticRequest.sourceFacts());
        assertEquals(List.of(sourceFact()), ruleRequest.sourceFacts());
        assertEquals(List.of(sourceFact()), result.sourceFacts());
    }

    @Test
    void analysisResultsCopyMutableArtifacts() {
        var artifacts = new ArrayList<ArtifactReference>();
        artifacts.add(new ArtifactReference("semantic.json", "semantic-report", "abc123", 12L));

        var semantic = new SemanticAnalysisResult("fake-semantic", "sha256:semantic", artifacts, SemanticGraph.empty());
        var rules = new RuleGenerationResult(artifacts);
        artifacts.add(new ArtifactReference("rules.btm", "byteman-rules", "def456", 34L));

        assertEquals(List.of(new ArtifactReference("semantic.json", "semantic-report", "abc123", 12L)), semantic.artifacts());
        assertEquals(List.of(new ArtifactReference("semantic.json", "semantic-report", "abc123", 12L)), rules.artifacts());
    }

    @Test
    void commandFieldsAreRequired() {
        assertThrows(NullPointerException.class, () -> new RunRepositoryAnalysisCommand(null, repositoryMetadata(), "baseline"));
        assertThrows(NullPointerException.class, () -> new RunRepositoryAnalysisCommand(analysisRunId(), null, "baseline"));
        assertThrows(NullPointerException.class, () -> new RunRepositoryAnalysisCommand(analysisRunId(), repositoryMetadata(), null));
    }

    @Test
    void resultFieldsAreRequired() {
        var facts = List.of(sourceFact());
        var semantic = semanticResult();
        var rules = ruleResult();

        assertThrows(
            NullPointerException.class,
            () -> new RunRepositoryAnalysisResult(null, repositoryMetadata(), "baseline", RepositoryAnalysisStatus.COMPLETED, facts, semantic, rules)
        );
        assertThrows(
            NullPointerException.class,
            () -> new RunRepositoryAnalysisResult(analysisRunId(), null, "baseline", RepositoryAnalysisStatus.COMPLETED, facts, semantic, rules)
        );
        assertThrows(
            NullPointerException.class,
            () -> new RunRepositoryAnalysisResult(analysisRunId(), repositoryMetadata(), null, RepositoryAnalysisStatus.COMPLETED, facts, semantic, rules)
        );
        assertThrows(
            NullPointerException.class,
            () -> new RunRepositoryAnalysisResult(analysisRunId(), repositoryMetadata(), "baseline", null, facts, semantic, rules)
        );
        assertThrows(
            NullPointerException.class,
            () -> new RunRepositoryAnalysisResult(analysisRunId(), repositoryMetadata(), "baseline", RepositoryAnalysisStatus.COMPLETED, null, semantic, rules)
        );
        assertThrows(
            NullPointerException.class,
            () -> new RunRepositoryAnalysisResult(analysisRunId(), repositoryMetadata(), "baseline", RepositoryAnalysisStatus.COMPLETED, facts, null, rules)
        );
        assertThrows(
            NullPointerException.class,
            () -> new RunRepositoryAnalysisResult(analysisRunId(), repositoryMetadata(), "baseline", RepositoryAnalysisStatus.COMPLETED, facts, semantic, null)
        );
    }

    @Test
    void portResultFieldsAreRequired() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new SemanticAnalysisResult(null, "sha256:semantic", List.of(), SemanticGraph.empty())
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new SemanticAnalysisResult("", "sha256:semantic", List.of(), SemanticGraph.empty())
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new SemanticAnalysisResult("fake-semantic", null, List.of(), SemanticGraph.empty())
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new SemanticAnalysisResult("fake-semantic", "", List.of(), SemanticGraph.empty())
        );
        assertThrows(
            NullPointerException.class,
            () -> new SemanticAnalysisResult("fake-semantic", "sha256:semantic", null, SemanticGraph.empty())
        );
        assertThrows(
            NullPointerException.class,
            () -> new SemanticAnalysisResult("fake-semantic", "sha256:semantic", List.of(), null)
        );
        assertThrows(NullPointerException.class, () -> new RuleGenerationResult(null));
        assertThrows(NullPointerException.class, () -> new SemanticAnalysisRequest(null, repositorySource(), List.of()));
        assertThrows(NullPointerException.class, () -> new SemanticAnalysisRequest(analysisRunId(), null, List.of()));
        assertThrows(NullPointerException.class, () -> new SemanticAnalysisRequest(analysisRunId(), repositorySource(), null));
        assertThrows(NullPointerException.class, () -> new RuleGenerationRequest(null, repositorySource(), List.of(), semanticResult()));
        assertThrows(NullPointerException.class, () -> new RuleGenerationRequest(analysisRunId(), null, List.of(), semanticResult()));
        assertThrows(NullPointerException.class, () -> new RuleGenerationRequest(analysisRunId(), repositorySource(), null, semanticResult()));
        assertThrows(NullPointerException.class, () -> new RuleGenerationRequest(analysisRunId(), repositorySource(), List.of(), null));
    }

    private static RunRepositoryAnalysisCommand command() {
        return new RunRepositoryAnalysisCommand(analysisRunId(), repositoryMetadata(), "baseline");
    }

    private static AnalysisRunId analysisRunId() {
        return new AnalysisRunId("analysis-1");
    }

    private static RepositoryMetadata repositoryMetadata() {
        return new RepositoryMetadata("project-a", "file:///workspace/project", "main", "abcdef");
    }

    private static RepositorySource repositorySource() {
        return new RepositorySource(repositoryMetadata(), List.of("src/main/java"));
    }

    private static SourceFact sourceFact() {
        return new SourceFact("type", sourceLocation(), "App", "class App");
    }

    private static SourceLocation sourceLocation() {
        return new SourceLocation("src/main/java/App.java", "com.example.App", "main", 1);
    }

    private static SemanticAnalysisResult semanticResult() {
        return new SemanticAnalysisResult(
            "fake-semantic",
            "sha256:semantic",
            List.of(new ArtifactReference("semantic.json", "semantic-report", "abc123", 12L)),
            SemanticGraph.empty()
        );
    }

    private static RuleGenerationResult ruleResult() {
        return new RuleGenerationResult(List.of(new ArtifactReference("rules.btm", "byteman-rules", "def456", 34L)));
    }

    private static final class RecordingRepositorySourcePort implements RepositorySourcePort {
        private RepositoryMetadata resolvedMetadata;

        @Override
        public RepositorySource resolve(RepositoryMetadata repositoryMetadata) {
            resolvedMetadata = repositoryMetadata;
            return repositorySource();
        }
    }

    private static final class RecordingSourceScannerPort implements SourceScannerPort {
        private RepositorySource scannedSource;

        @Override
        public List<SourceFact> scan(RepositorySource source) {
            scannedSource = source;
            return List.of(sourceFact());
        }
    }

    private static final class RecordingSemanticAnalysisPort implements SemanticAnalysisPort {
        private final SemanticAnalysisResult result = semanticResult();
        private SemanticAnalysisRequest request;

        @Override
        public SemanticAnalysisResult analyze(SemanticAnalysisRequest request) {
            this.request = request;
            return result;
        }
    }

    private static final class RecordingRuleGenerationPort implements RuleGenerationPort {
        private final RuleGenerationResult result = ruleResult();
        private RuleGenerationRequest request;

        @Override
        public RuleGenerationResult generate(RuleGenerationRequest request) {
            this.request = request;
            return result;
        }
    }

    private static final class RecordingResultStore implements RepositoryAnalysisResultStore {
        private RunRepositoryAnalysisResult storedResult;

        @Override
        public void store(RunRepositoryAnalysisResult result) {
            storedResult = result;
        }
    }
}
