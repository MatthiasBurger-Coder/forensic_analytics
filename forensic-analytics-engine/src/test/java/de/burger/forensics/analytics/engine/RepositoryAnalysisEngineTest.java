package de.burger.forensics.analytics.engine;

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
        var request = new RepositoryAnalysisRequest("analysis-1", "file:///workspace/project", "baseline");

        var result = engine.run(request);

        assertEquals("file:///workspace/project", sourceProvider.resolvedLocation);
        assertEquals("file:///workspace/project", sourceFactScanner.scannedSource.repositoryLocation());
        assertEquals(RepositoryAnalysisStatus.COMPLETED, result.status());
        assertEquals("analysis-1", result.analysisId());
        assertEquals("baseline", result.analysisProfile());
        assertEquals(List.of(new SourceFact("type", "src/main/java/App.java", "class App")), result.sourceFacts());
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
            () -> new RepositoryAnalysisRequest(null, "file:///workspace/project", "baseline")
        );
        assertThrows(
            NullPointerException.class,
            () -> new RepositoryAnalysisRequest("analysis-1", null, "baseline")
        );
        assertThrows(
            NullPointerException.class,
            () -> new RepositoryAnalysisRequest("analysis-1", "file:///workspace/project", null)
        );
    }

    @Test
    void sourceFactsAreCopiedIntoResult() {
        var facts = new ArrayList<SourceFact>();
        facts.add(new SourceFact("type", "src/main/java/App.java", "class App"));

        var result = RepositoryAnalysisResult.completed(
            "analysis-1",
            "file:///workspace/project",
            "baseline",
            facts
        );
        facts.add(new SourceFact("method", "src/main/java/App.java", "main"));

        assertEquals(List.of(new SourceFact("type", "src/main/java/App.java", "class App")), result.sourceFacts());
    }

    @Test
    void sourceRootsAreCopiedIntoRepositorySource() {
        var sourceRoots = new ArrayList<String>();
        sourceRoots.add("src/main/java");

        var source = new RepositorySource("file:///workspace/project", sourceRoots);
        sourceRoots.add("generated");

        assertEquals(List.of("src/main/java"), source.sourceRoots());
    }

    @Test
    void sourceFactFieldsAreRequired() {
        assertThrows(NullPointerException.class, () -> new SourceFact(null, "src/main/java/App.java", "class App"));
        assertThrows(NullPointerException.class, () -> new SourceFact("type", null, "class App"));
        assertThrows(NullPointerException.class, () -> new SourceFact("type", "src/main/java/App.java", null));
    }

    @Test
    void resultFieldsAreRequired() {
        var facts = List.of(new SourceFact("type", "src/main/java/App.java", "class App"));

        assertThrows(
            NullPointerException.class,
            () -> new RepositoryAnalysisResult(null, "file:///workspace/project", "baseline", RepositoryAnalysisStatus.COMPLETED, facts)
        );
        assertThrows(
            NullPointerException.class,
            () -> new RepositoryAnalysisResult("analysis-1", null, "baseline", RepositoryAnalysisStatus.COMPLETED, facts)
        );
        assertThrows(
            NullPointerException.class,
            () -> new RepositoryAnalysisResult("analysis-1", "file:///workspace/project", null, RepositoryAnalysisStatus.COMPLETED, facts)
        );
        assertThrows(
            NullPointerException.class,
            () -> new RepositoryAnalysisResult("analysis-1", "file:///workspace/project", "baseline", null, facts)
        );
        assertThrows(
            NullPointerException.class,
            () -> new RepositoryAnalysisResult("analysis-1", "file:///workspace/project", "baseline", RepositoryAnalysisStatus.COMPLETED, null)
        );
    }

    private static final class RecordingSourceProvider implements RepositorySourceProvider {
        private String resolvedLocation;

        @Override
        public RepositorySource resolve(String repositoryLocation) {
            resolvedLocation = repositoryLocation;
            return new RepositorySource(repositoryLocation, List.of("src/main/java"));
        }
    }

    private static final class RecordingSourceFactScanner implements SourceFactScanner {
        private RepositorySource scannedSource;

        @Override
        public List<SourceFact> scan(RepositorySource source) {
            scannedSource = source;
            return List.of(new SourceFact("type", "src/main/java/App.java", "class App"));
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
