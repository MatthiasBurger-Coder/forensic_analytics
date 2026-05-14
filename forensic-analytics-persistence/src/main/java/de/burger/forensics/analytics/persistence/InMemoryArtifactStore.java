package de.burger.forensics.analytics.persistence;

import de.burger.forensics.analytics.application.analysis.port.ArtifactStoreConflictException;
import de.burger.forensics.analytics.application.analysis.port.ArtifactStorePort;
import de.burger.forensics.analytics.domain.analysis.AnalysisArtifactRecord;
import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryArtifactStore implements ArtifactStorePort {
    private final Map<ArtifactKey, AnalysisArtifactRecord> artifacts = new ConcurrentHashMap<>();

    @Override
    public AnalysisArtifactRecord storeArtifact(AnalysisArtifactRecord artifact) {
        Objects.requireNonNull(artifact, "artifact must not be null");
        var key = ArtifactKey.from(artifact);
        return artifacts.compute(key, (artifactKey, existing) -> merge(artifactKey, existing, artifact));
    }

    @Override
    public Optional<AnalysisArtifactRecord> findArtifact(AnalysisRunId analysisRunId, String artifactPath) {
        Objects.requireNonNull(analysisRunId, "analysisRunId must not be null");
        requireText(artifactPath, "artifactPath");
        return Optional.ofNullable(artifacts.get(new ArtifactKey(analysisRunId, artifactPath)));
    }

    @Override
    public List<AnalysisArtifactRecord> findArtifacts(AnalysisRunId analysisRunId) {
        Objects.requireNonNull(analysisRunId, "analysisRunId must not be null");
        return artifacts.values().stream()
            .filter(artifact -> artifact.analysisRunId().equals(analysisRunId))
            .sorted(Comparator.comparing(artifact -> artifact.artifact().artifact().path()))
            .toList();
    }

    private static AnalysisArtifactRecord merge(
        ArtifactKey key,
        AnalysisArtifactRecord existing,
        AnalysisArtifactRecord artifact
    ) {
        if (existing == null || existing.equals(artifact)) {
            return artifact;
        }
        throw new ArtifactStoreConflictException("conflicting artifact metadata for path " + key.path());
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private record ArtifactKey(AnalysisRunId analysisRunId, String path) {
        private ArtifactKey {
            Objects.requireNonNull(analysisRunId, "analysisRunId must not be null");
            requireText(path, "artifact path");
        }

        private static ArtifactKey from(AnalysisArtifactRecord artifact) {
            return new ArtifactKey(artifact.analysisRunId(), artifact.artifact().artifact().path());
        }
    }
}
