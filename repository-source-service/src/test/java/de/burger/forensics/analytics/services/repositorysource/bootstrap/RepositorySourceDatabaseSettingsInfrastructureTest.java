package de.burger.forensics.analytics.services.repositorysource.bootstrap;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RepositorySourceDatabaseSettingsInfrastructureTest {
    @Test
    void mapsDatabaseSettingsConnectionValidationWithoutLeakingDriverDetails() throws Exception {
        var driver = new SettingsValidationDriver();
        DriverManager.registerDriver(driver);
        try {
            assertEquals(
                RepositorySourceDatabaseSettingsValidationResult.Status.VALID,
                RepositorySourceDatabaseSettingsConnectionValidator.validate(postgres("valid")).status()
            );
            assertEquals(
                RepositorySourceDatabaseSettingsValidationResult.Status.UNREACHABLE,
                RepositorySourceDatabaseSettingsConnectionValidator.validate(postgres("invalid")).status()
            );
            assertEquals(
                RepositorySourceDatabaseSettingsValidationResult.Status.AUTHENTICATION_FAILED,
                RepositorySourceDatabaseSettingsConnectionValidator.failure(new SQLException("authentication failed", "28000")).status()
            );
            assertEquals(
                RepositorySourceDatabaseSettingsValidationResult.Status.UNREACHABLE,
                RepositorySourceDatabaseSettingsConnectionValidator.failure(new SQLException("connection failed", "08001")).status()
            );
        } finally {
            DriverManager.deregisterDriver(driver);
        }
    }

    @Test
    void sanitizesLiquibaseMigrationConnectionFailures() {
        var migration = new RepositorySourcePostgresLiquibaseMigration(postgres("valid"), () -> {
            throw new SQLException("jdbc:postgresql://private.example/db password=secret");
        });

        var error = assertThrows(IllegalStateException.class, migration::migrate);

        assertEquals("Repository-source PostgreSQL storage is not ready", error.getMessage());
    }

    private static RepositorySourceServiceProperties.Postgres postgres(String databaseName) {
        return new RepositorySourceServiceProperties.Postgres(
            "jdbc:postgresql://validator.test/" + databaseName,
            "forensic",
            "secret",
            "repository_source",
            "classpath:db/changelog/repository-source-workspace.postgresql.yaml"
        );
    }

    private static Connection connection(boolean valid) {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "isValid" -> valid;
            case "close" -> null;
            case "isClosed" -> false;
            case "toString" -> "settings validation connection";
            default -> defaultValue(method.getReturnType());
        };
        return (Connection) Proxy.newProxyInstance(
            RepositorySourceDatabaseSettingsInfrastructureTest.class.getClassLoader(),
            new Class<?>[] { Connection.class },
            handler
        );
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == void.class) {
            return null;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        return 0.0d;
    }

    private static final class SettingsValidationDriver implements Driver {
        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            if (!acceptsURL(url)) {
                return null;
            }
            if (url.endsWith("/auth")) {
                throw new SQLException("authentication failed", "28000");
            }
            if (url.endsWith("/down")) {
                throw new SQLException("connection failed", "08001");
            }
            return connection(!url.endsWith("/invalid"));
        }

        @Override
        public boolean acceptsURL(String url) {
            return url != null && url.startsWith("jdbc:postgresql://validator.test/");
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
            return new DriverPropertyInfo[0];
        }

        @Override
        public int getMajorVersion() {
            return 1;
        }

        @Override
        public int getMinorVersion() {
            return 0;
        }

        @Override
        public boolean jdbcCompliant() {
            return false;
        }

        @Override
        public Logger getParentLogger() {
            return Logger.getGlobal();
        }
    }
}
