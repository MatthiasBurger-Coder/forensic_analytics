package de.burger.forensics.analytics.services.repositorysource.bootstrap;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

final class RepositorySourceDatabaseSettingsConnectionValidator {
    private RepositorySourceDatabaseSettingsConnectionValidator() {
    }

    static RepositorySourceDatabaseSettingsValidationResult validate(RepositorySourceServiceProperties.Postgres postgres) {
        var properties = new Properties();
        properties.setProperty("user", postgres.username());
        properties.setProperty("password", postgres.password());
        try (var connection = DriverManager.getConnection(postgres.jdbcUrl(), properties)) {
            if (connection.isValid(5)) {
                return RepositorySourceDatabaseSettingsValidationResult.valid();
            }
            return RepositorySourceDatabaseSettingsValidationResult.unreachable();
        } catch (SQLException error) {
            return failure(error);
        }
    }

    static RepositorySourceDatabaseSettingsValidationResult failure(SQLException error) {
        var sqlState = Objects.toString(error.getSQLState(), "");
        if (sqlState.startsWith("28")) {
            return RepositorySourceDatabaseSettingsValidationResult.authenticationFailed();
        }
        return RepositorySourceDatabaseSettingsValidationResult.unreachable();
    }
}
