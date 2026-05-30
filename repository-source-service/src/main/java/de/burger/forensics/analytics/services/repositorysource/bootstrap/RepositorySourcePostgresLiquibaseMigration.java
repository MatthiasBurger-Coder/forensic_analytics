package de.burger.forensics.analytics.services.repositorysource.bootstrap;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

final class RepositorySourcePostgresLiquibaseMigration {
    private static final String CLASSPATH_PREFIX = "classpath:";

    private final RepositorySourceServiceProperties.Postgres properties;
    private final ConnectionFactory connectionFactory;

    RepositorySourcePostgresLiquibaseMigration(RepositorySourceServiceProperties.Postgres properties) {
        this(properties, driverConnectionFactory(properties));
    }

    RepositorySourcePostgresLiquibaseMigration(
        RepositorySourceServiceProperties.Postgres properties,
        ConnectionFactory connectionFactory
    ) {
        this.properties = Objects.requireNonNull(properties, "PostgreSQL properties must not be null");
        this.connectionFactory = Objects.requireNonNull(connectionFactory, "connection factory must not be null");
    }

    void migrate() {
        try (
            var connection = connectionFactory.open();
            var accessor = new ClassLoaderResourceAccessor(RepositorySourcePostgresLiquibaseMigration.class.getClassLoader())
        ) {
            var database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            try (var liquibase = new Liquibase(changeLogPath(), accessor, database)) {
                liquibase.setChangeLogParameter("repositorySourceSchema", properties.schema());
                liquibase.update(new Contexts(), new LabelExpression());
            }
        } catch (Exception error) {
            throw new IllegalStateException("Repository-source PostgreSQL storage is not ready");
        }
    }

    private String changeLogPath() {
        var changeLog = properties.changeLog();
        return changeLog.startsWith(CLASSPATH_PREFIX) ? changeLog.substring(CLASSPATH_PREFIX.length()) : changeLog;
    }

    private static ConnectionFactory driverConnectionFactory(RepositorySourceServiceProperties.Postgres properties) {
        return () -> DriverManager.getConnection(properties.jdbcUrl(), connectionProperties(properties));
    }

    private static Properties connectionProperties(RepositorySourceServiceProperties.Postgres properties) {
        var connectionProperties = new Properties();
        connectionProperties.setProperty("user", properties.username());
        connectionProperties.setProperty("password", properties.password());
        connectionProperties.setProperty("loginTimeout", "10");
        connectionProperties.setProperty("connectTimeout", "10");
        return connectionProperties;
    }

    @FunctionalInterface
    interface ConnectionFactory {
        Connection open() throws SQLException;
    }
}
