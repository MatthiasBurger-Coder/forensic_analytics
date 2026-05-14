package de.burger.forensics.analytics.persistence;

import de.burger.forensics.analytics.application.analysis.port.ArtifactStoreConflictException;
import de.burger.forensics.analytics.domain.analysis.AnalysisArtifactCategory;
import de.burger.forensics.analytics.domain.analysis.AnalysisArtifactPurpose;
import de.burger.forensics.analytics.domain.analysis.AnalysisArtifactRecord;
import de.burger.forensics.analytics.domain.analysis.AnalysisArtifactReference;
import de.burger.forensics.analytics.domain.analysis.AnalysisArtifactSensitivity;
import de.burger.forensics.analytics.domain.analysis.AnalysisJobId;
import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.analysis.AnalysisWorkerKind;
import de.burger.forensics.analytics.domain.artifact.ArtifactReference;
import de.burger.forensics.analytics.domain.workspace.ProjectStorageArea;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryArtifactStoreTest {
    private final InMemoryArtifactStore store = new InMemoryArtifactStore();

    @Test
    void storesArtifactRecordsWithRunJobWorkerAttemptAreaPurposeAndSensitivity() {
        var artifact = artifact("analysis/semantic.json", "sha256:semantic", AnalysisArtifactPurpose.SEMANTIC_FACTS);

        assertEquals(artifact, store.storeArtifact(artifact));
        assertEquals(artifact, store.findArtifact(analysisRunId(), "analysis/semantic.json").orElseThrow());
        assertEquals(List.of(artifact), store.findArtifacts(analysisRunId()));
    }

    @Test
    void storingTheSameCanonicalArtifactIsIdempotent() {
        var artifact = artifact("analysis/semantic.json", "sha256:semantic", AnalysisArtifactPurpose.SEMANTIC_FACTS);

        store.storeArtifact(artifact);
        store.storeArtifact(artifact);

        assertEquals(List.of(artifact), store.findArtifacts(analysisRunId()));
    }

    @Test
    void conflictingArtifactMetadataFailsExplicitly() {
        store.storeArtifact(artifact("analysis/semantic.json", "sha256:semantic", AnalysisArtifactPurpose.SEMANTIC_FACTS));

        assertThrows(
            ArtifactStoreConflictException.class,
            () -> store.storeArtifact(artifact("analysis/semantic.json", "sha256:other", AnalysisArtifactPurpose.STATIC_FACTS))
        );
        assertTrue(store.findArtifact(analysisRunId(), "missing.json").isEmpty());
    }

    @Test
    void identicalArtifactPathsAreScopedByAnalysisRun() {
        var firstRun = artifact("analysis/semantic.json", "sha256:semantic", AnalysisArtifactPurpose.SEMANTIC_FACTS);
        var secondRun = new AnalysisArtifactRecord(
            new AnalysisRunId("analysis-2"),
            new AnalysisJobId("job-1"),
            AnalysisWorkerKind.JOERN_ANALYSIS,
            1,
            ProjectStorageArea.ANALYSIS_RESULTS,
            AnalysisArtifactPurpose.SEMANTIC_FACTS,
            AnalysisArtifactSensitivity.INTERNAL,
            new AnalysisArtifactReference(
                new ArtifactReference("analysis/semantic.json", "semantic-report", "sha256:other", 256L),
                AnalysisArtifactCategory.STATIC
            )
        );

        store.storeArtifact(firstRun);
        store.storeArtifact(secondRun);

        assertEquals(firstRun, store.findArtifact(analysisRunId(), "analysis/semantic.json").orElseThrow());
        assertEquals(secondRun, store.findArtifact(new AnalysisRunId("analysis-2"), "analysis/semantic.json").orElseThrow());
    }

    @Test
    void rejectsMissingLookupInputs() {
        assertThrows(NullPointerException.class, () -> store.storeArtifact(null));
        assertThrows(NullPointerException.class, () -> store.findArtifact(null, "analysis/semantic.json"));
        assertThrows(IllegalArgumentException.class, () -> store.findArtifact(analysisRunId(), null));
        assertThrows(IllegalArgumentException.class, () -> store.findArtifact(analysisRunId(), " "));
        assertThrows(NullPointerException.class, () -> store.findArtifacts(null));
    }

    private static AnalysisArtifactRecord artifact(
        String path,
        String checksum,
        AnalysisArtifactPurpose purpose
    ) {
        return new AnalysisArtifactRecord(
            analysisRunId(),
            new AnalysisJobId("job-1"),
            AnalysisWorkerKind.JOERN_ANALYSIS,
            1,
            ProjectStorageArea.ANALYSIS_RESULTS,
            purpose,
            AnalysisArtifactSensitivity.INTERNAL,
            new AnalysisArtifactReference(
                new ArtifactReference(path, "semantic-report", checksum, 256L),
                AnalysisArtifactCategory.STATIC
            )
        );
    }

    private static AnalysisRunId analysisRunId() {
        return new AnalysisRunId("analysis-1");
    }
}
