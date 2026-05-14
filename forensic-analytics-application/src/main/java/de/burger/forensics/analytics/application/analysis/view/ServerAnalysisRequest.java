package de.burger.forensics.analytics.application.analysis.view;

import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.repository.RepositoryMetadata;

import java.util.Objects;

public record ServerAnalysisRequest(
    AnalysisRunId analysisRunId,
    RepositoryMetadata repositoryMetadata,
    String analysisProfile
) {
    public ServerAnalysisRequest {
        Objects.requireNonNull(analysisRunId, "analysisRunId must not be null");
        Objects.requireNonNull(repositoryMetadata, "repositoryMetadata must not be null");
        requireText(analysisProfile, "analysisProfile");
    }

    static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
