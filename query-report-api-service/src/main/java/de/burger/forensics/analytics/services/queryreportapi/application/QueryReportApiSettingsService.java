package de.burger.forensics.analytics.services.queryreportapi.application;

import de.burger.forensics.analytics.services.queryreportapi.application.port.RepositorySourceSettingsOwnerPort;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiSettings.DatabaseSettingsRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiSettings.DatabaseSettingsStatus;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiSettings.DatabaseSettingsValidationRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiSettings.DatabaseSettingsValidationResponse;

import java.util.Objects;

public final class QueryReportApiSettingsService {
    private final RepositorySourceSettingsOwnerPort repositorySourceSettingsOwnerPort;

    public QueryReportApiSettingsService(RepositorySourceSettingsOwnerPort repositorySourceSettingsOwnerPort) {
        this.repositorySourceSettingsOwnerPort = Objects.requireNonNull(
            repositorySourceSettingsOwnerPort,
            "repository source settings owner port must not be null"
        );
    }

    public DatabaseSettingsStatus current(DatabaseSettingsRequest request) {
        return repositorySourceSettingsOwnerPort.current(request);
    }

    public DatabaseSettingsValidationResponse validate(DatabaseSettingsValidationRequest request) {
        return repositorySourceSettingsOwnerPort.validate(request);
    }
}
