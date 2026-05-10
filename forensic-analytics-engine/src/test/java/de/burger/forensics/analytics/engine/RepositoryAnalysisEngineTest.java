package de.burger.forensics.analytics.engine;

import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.repository.RepositoryMetadata;
import de.burger.forensics.analytics.domain.repository.RepositorySource;
import de.burger.forensics.analytics.domain.source.SourceFact;
import de.burger.forensics.analytics.domain.source.SourceLocation;
import de.burger.forensics.analytics.engine.port.AnalysisResultStore;
import de.burger.forensics.analytics.engine.port.RepositorySourceProvider;
import de.burger.forensics.analytics.engine.port.SourceFactScanner;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RepositoryAnalysisEngineTest {
    private final RecordingSourceProvider sourceProvider = new RecordingSourceProvider();
    private final RecordingSourceFactScanner sourceFactScanner = new RecordingSourceFactScanner();
    private final RecordingAnalysisResultStore resultStore = new RecordingAnalysisResultStore();
    private final RepositoryAnalysisEngine engine = new RepositoryAnalysisEngine(
        sourceProvider,
        sourceFactScanner,
        resultStore
    );

    @Test
    void runCoordinatesRepositoryAnalysisThroughPorts() {
        var request = new RepositoryAnalysisRequest(
            new AnalysisRunId("analysis-1"),
            repositoryMetadata(),
            "baseline"
        );

        var result = engine.run(request);

        assertEquals(repositoryMetadata(), sourceProvider.resolvedMetadata);
        assertEquals(repositoryMetadata(), sourceFactScanner.scannedSource.metadata());
        assertEquals(RepositoryAnalysisStatus.COMPLETED, result.status());
        assertEquals(new AnalysisRunId("analysis-1"), result.analysisRunId());
        assertEquals(repositoryMetadata(), result.repositoryMetadata());
        assertEquals("baseline", result.analysisProfile());
        assertEquals(List.of(sourceFact()), result.sourceFacts());
        assertEquals(result, resultStore.storedResult);
    }

    @Test
    void requestIsRequired() {
        assertThrows(NullPointerException.class, () -> engine.run(null));
    }

    @Test
    void dependenciesAreRequired() {
        assertThrows(
            NullPointerException.class,
            () -> new RepositoryAnalysisEngine(null, sourceFactScanner, resultStore)
        );
        assertThrows(
            NullPointerException.class,
            () -> new RepositoryAnalysisEngine(sourceProvider, null, resultStore)
        );
        assertThrows(
            NullPointerException.class,
            () -> new RepositoryAnalysisEngine(sourceProvider, sourceFactScanner, null)
        );
    }

    @Test
    void requestFieldsAreRequired() {
        assertThrows(
            NullPointerException.class,
            () -> new RepositoryAnalysisRequest(null, repositoryMetadata(), "baseline")
        );
        assertThrows(
            NullPointerException.class,
            () -> new RepositoryAnalysisRequest(new AnalysisRunId("analysis-1"), null, "baseline")
        );
        assertThrows(
            NullPointerException.class,
            () -> new RepositoryAnalysisRequest(new AnalysisRunId("analysis-1"), repositoryMetadata(), null)
        );
    }

    @Test
    void sourceFactsAreCopiedIntoResult() {
        var facts = new ArrayList<SourceFact>();
        facts.add(sourceFact());

        var result = RepositoryAnalysisResult.completed(
            new AnalysisRunId("analysis-1"),
            repositoryMetadata(),
            "baseline",
            facts
        );
        facts.add(new SourceFact("method", sourceLocation(), "main()", "main"));

        assertEquals(List.of(sourceFact()), result.sourceFacts());
    }

    @Test
    void sourceRootsAreCopiedIntoRepositorySource() {
        var sourceRoots = new ArrayList<String>();
        sourceRoots.add("src/main/java");

        var source = new RepositorySource(repositoryMetadata(), sourceRoots);
        sourceRoots.add("generated");

        assertEquals(List.of("src/main/java"), source.sourceRoots());
    }

    @Test
    void resultFieldsAreRequired() {
        var facts = List.of(sourceFact());

        assertThrows(
            NullPointerException.class,
            () -> new RepositoryAnalysisResult(null, repositoryMetadata(), "baseline", RepositoryAnalysisStatus.COMPLETED, facts)
        );
        assertThrows(
            NullPointerException.class,
            () -> new RepositoryAnalysisResult(new AnalysisRunId("analysis-1"), null, "baseline", RepositoryAnalysisStatus.COMPLETED, facts)
        );
        assertThrows(
            NullPointerException.class,
            () -> new RepositoryAnalysisResult(new AnalysisRunId("analysis-1"), repositoryMetadata(), null, RepositoryAnalysisStatus.COMPLETED, facts)
        );
        assertThrows(
            NullPointerException.class,
            () -> new RepositoryAnalysisResult(new AnalysisRunId("analysis-1"), repositoryMetadata(), "baseline", null, facts)
        );
        assertThrows(
            NullPointerException.class,
            () -> new RepositoryAnalysisResult(new AnalysisRunId("analysis-1"), repositoryMetadata(), "baseline", RepositoryAnalysisStatus.COMPLETED, null)
        );
    }

    private static RepositoryMetadata repositoryMetadata() {
        return new RepositoryMetadata("project-a", "file:///workspace/project", "main", "abcdef");
    }

    private static SourceFact sourceFact() {
        return new SourceFact("type", sourceLocation(), "App", "class App");
    }

    private static SourceLocation sourceLocation() {
        return new SourceLocation("src/main/java/App.java", "com.example.App", "main", 1);
    }

    private static final class RecordingSourceProvider implements RepositorySourceProvider {
        private RepositoryMetadata resolvedMetadata;

        @Override
        public RepositorySource resolve(RepositoryMetadata repositoryMetadata) {
            resolvedMetadata = repositoryMetadata;
            return new RepositorySource(repositoryMetadata, List.of("src/main/java"));
        }
    }

    private static final class RecordingSourceFactScanner implements SourceFactScanner {
        private RepositorySource scannedSource;

        @Override
        public List<SourceFact> scan(RepositorySource source) {
            scannedSource = source;
            return List.of(sourceFact());
        }
    }

    private static final class RecordingAnalysisResultStore implements AnalysisResultStore {
        private RepositoryAnalysisResult storedResult;

        @Override
        public void store(RepositoryAnalysisResult result) {
            storedResult = result;
        }
    }
}
