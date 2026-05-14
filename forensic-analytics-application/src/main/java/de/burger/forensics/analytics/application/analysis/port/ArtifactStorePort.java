package de.burger.forensics.analytics.application.analysis.port;

import de.burger.forensics.analytics.domain.analysis.AnalysisArtifactRecord;
import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;

import java.util.List;
import java.util.Optional;

public interface ArtifactStorePort {
    AnalysisArtifactRecord storeArtifact(AnalysisArtifactRecord artifact);

    Optional<AnalysisArtifactRecord> findArtifact(AnalysisRunId analysisRunId, String artifactPath);

    List<AnalysisArtifactRecord> findArtifacts(AnalysisRunId analysisRunId);
}
