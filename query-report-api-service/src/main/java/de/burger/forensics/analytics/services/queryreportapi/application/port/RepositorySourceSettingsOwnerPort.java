package de.burger.forensics.analytics.services.queryreportapi.application.port;

import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiSettings.DatabaseSettingsRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiSettings.DatabaseSettingsStatus;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiSettings.DatabaseSettingsValidationRequest;
import de.burger.forensics.analytics.services.queryreportapi.domain.QueryReportApiSettings.DatabaseSettingsValidationResponse;

public interface RepositorySourceSettingsOwnerPort {
    DatabaseSettingsStatus current(DatabaseSettingsRequest request);

    DatabaseSettingsValidationResponse validate(DatabaseSettingsValidationRequest request);
}
