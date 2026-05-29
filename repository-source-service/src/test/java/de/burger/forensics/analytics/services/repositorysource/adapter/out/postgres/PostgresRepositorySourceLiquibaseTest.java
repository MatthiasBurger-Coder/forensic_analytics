package de.burger.forensics.analytics.services.repositorysource.adapter.out.postgres;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.change.Change;
import liquibase.change.ColumnConfig;
import liquibase.change.ConstraintsConfig;
import liquibase.change.core.AddForeignKeyConstraintChange;
import liquibase.change.core.AddPrimaryKeyChange;
import liquibase.change.core.AddUniqueConstraintChange;
import liquibase.change.core.CreateIndexChange;
import liquibase.change.core.CreateTableChange;
import liquibase.database.OfflineConnection;
import liquibase.database.core.PostgresDatabase;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostgresRepositorySourceLiquibaseTest {
    private static final String CHANGELOG = "db/changelog/repository-source-workspace.postgresql.yaml";
    private static final String SCHEMA = "repository_source";

    @TempDir
    Path tempDir;

    @Test
    void definesRepositorySourceWorkspaceMetadataSchemaWithoutExternalDatabase() throws Exception {
        var changes = changes(tempDir);
        var tables = changes.stream()
            .filter(CreateTableChange.class::isInstance)
            .map(CreateTableChange.class::cast)
            .collect(Collectors.toMap(
                CreateTableChange::getTableName,
                Function.identity(),
                (left, right) -> left,
                LinkedHashMap::new
            ));

        assertEquals(
            List.of("workspace", "workspace_branch", "repository_preparation", "idempotency_record"),
            List.copyOf(tables.keySet())
        );
        assertColumns(tables.get("workspace"), Map.ofEntries(
            column("workspace_id", "varchar(128)", false),
            column("workspace_title", "varchar(255)", false),
            column("repository_key", "varchar(512)", false),
            column("repository_url", "varchar(2048)", false),
            column("repository_host", "varchar(255)", false),
            column("repository_owner", "varchar(255)", true),
            column("repository_name", "varchar(255)", false),
            column("default_branch", "varchar(512)", true),
            column("status", "varchar(64)", false),
            column("diagnostics_json", "text", false),
            column("safe_attributes_json", "text", false),
            column("created_at", "varchar(64)", false),
            column("updated_at", "varchar(64)", false)
        ));
        assertColumns(tables.get("workspace_branch"), Map.ofEntries(
            column("workspace_branch_id", "varchar(128)", false),
            column("workspace_id", "varchar(128)", false),
            column("repository_branch", "varchar(512)", false),
            column("requested_commit", "varchar(128)", true),
            column("resolved_commit", "varchar(128)", true),
            column("source_snapshot_id", "varchar(128)", true),
            column("status", "varchar(64)", false),
            column("source_roots_json", "text", false),
            column("last_checked_at", "varchar(64)", true),
            column("last_updated_at", "varchar(64)", true),
            column("diagnostics_json", "text", false)
        ));
        assertColumns(tables.get("repository_preparation"), Map.ofEntries(
            column("analysis_run_id", "varchar(128)", false),
            column("source_snapshot_id", "varchar(128)", false),
            column("workspace_id", "varchar(128)", false),
            column("workspace_branch_id", "varchar(128)", true),
            column("repository_url", "varchar(2048)", false),
            column("repository_provider", "varchar(128)", true),
            column("repository_attributes_json", "text", false),
            column("requested_branch", "varchar(512)", true),
            column("branch_required", "boolean", false),
            column("requested_commit", "varchar(128)", true),
            column("commit_required", "boolean", false),
            column("resolved_commit", "varchar(128)", false),
            column("checkout_status", "varchar(64)", false),
            column("checkout_diagnostics_json", "text", false),
            column("shallow_clone", "boolean", false),
            column("checkout_duration_millis", "bigint", false),
            column("partial_clone", "boolean", false),
            column("sparse_checkout", "boolean", false),
            column("workspace_status", "varchar(64)", false),
            column("source_snapshot_completeness", "varchar(64)", false),
            column("source_snapshot_json", "text", false),
            column("source_roots_json", "text", false),
            column("diagnostics_json", "text", false),
            column("safe_attributes_json", "text", false),
            column("created_at", "varchar(64)", false),
            column("updated_at", "varchar(64)", false)
        ));
        assertColumns(tables.get("idempotency_record"), Map.ofEntries(
            column("idempotency_key", "varchar(256)", false),
            column("operation", "varchar(128)", false),
            column("fingerprint", "varchar(2048)", false),
            column("result_type", "varchar(128)", false),
            column("result_reference", "varchar(512)", false),
            column("result_payload", "text", false),
            column("status", "varchar(64)", false),
            column("created_at", "varchar(64)", false),
            column("expires_at", "varchar(64)", true)
        ));
        assertPrimaryKeys(changes);
        assertUniqueConstraints(changes);
        assertForeignKeys(changes);
        assertIndexes(changes);
        assertNoByteStorage(tables);
    }

    private static List<Change> changes(Path tempDir) throws Exception {
        var accessor = new ClassLoaderResourceAccessor(PostgresRepositorySourceLiquibaseTest.class.getClassLoader());
        try {
            var database = new PostgresDatabase();
            var historyFile = tempDir.resolve("databasechangelog.csv").toAbsolutePath().normalize();
            database.setConnection(new OfflineConnection("offline:postgresql?schema=" + SCHEMA + "&changeLogFile=" + historyFile, accessor));
            try (var liquibase = new Liquibase(CHANGELOG, accessor, database)) {
                liquibase.setChangeLogParameter("repositorySourceSchema", SCHEMA);
                liquibase.validate();
                return liquibase.getDatabaseChangeLog().getChangeSets().stream()
                    .flatMap(changeSet -> changeSet.getChanges().stream())
                    .toList();
            }
        } finally {
            accessor.close();
        }
    }

    private static Map.Entry<String, ColumnExpectation> column(String name, String type, boolean nullable) {
        return Map.entry(name, new ColumnExpectation(type, nullable));
    }

    private static void assertColumns(CreateTableChange table, Map<String, ColumnExpectation> expected) {
        assertNotNull(table);
        assertEquals(SCHEMA, table.getSchemaName());
        var columns = table.getColumns().stream()
            .collect(Collectors.toMap(
                ColumnConfig::getName,
                column -> new ColumnExpectation(column.getType(), nullable(column.getConstraints())),
                (left, right) -> left,
                LinkedHashMap::new
            ));
        assertEquals(expected, columns);
    }

    private static boolean nullable(ConstraintsConfig constraints) {
        return constraints == null || constraints.isNullable() == null || constraints.isNullable();
    }

    private static void assertPrimaryKeys(List<Change> changes) {
        var primaryKeys = changes.stream()
            .filter(AddPrimaryKeyChange.class::isInstance)
            .map(AddPrimaryKeyChange.class::cast)
            .collect(Collectors.toMap(
                AddPrimaryKeyChange::getConstraintName,
                change -> new KeyExpectation(change.getSchemaName(), change.getTableName(), change.getColumnNames())
            ));

        assertEquals(Map.of(
            "pk_repository_source_workspace", new KeyExpectation(SCHEMA, "workspace", "workspace_id"),
            "pk_repository_source_workspace_branch", new KeyExpectation(SCHEMA, "workspace_branch", "workspace_branch_id"),
            "pk_repository_source_preparation", new KeyExpectation(SCHEMA, "repository_preparation", "analysis_run_id, source_snapshot_id"),
            "pk_repository_source_idempotency_record", new KeyExpectation(SCHEMA, "idempotency_record", "idempotency_key, operation")
        ), primaryKeys);
    }

    private static void assertUniqueConstraints(List<Change> changes) {
        var uniqueConstraints = changes.stream()
            .filter(AddUniqueConstraintChange.class::isInstance)
            .map(AddUniqueConstraintChange.class::cast)
            .collect(Collectors.toMap(
                AddUniqueConstraintChange::getConstraintName,
                change -> new KeyExpectation(change.getSchemaName(), change.getTableName(), change.getColumnNames())
            ));

        assertEquals(Map.of(
            "uq_repository_source_workspace_repository_key", new KeyExpectation(SCHEMA, "workspace", "repository_key"),
            "uq_repository_source_workspace_branch_name", new KeyExpectation(SCHEMA, "workspace_branch", "workspace_id, repository_branch")
        ), uniqueConstraints);
    }

    private static void assertForeignKeys(List<Change> changes) {
        var foreignKeys = changes.stream()
            .filter(AddForeignKeyConstraintChange.class::isInstance)
            .map(AddForeignKeyConstraintChange.class::cast)
            .collect(Collectors.toMap(
                AddForeignKeyConstraintChange::getConstraintName,
                change -> new ForeignKeyExpectation(
                    change.getBaseTableSchemaName(),
                    change.getBaseTableName(),
                    change.getBaseColumnNames(),
                    change.getReferencedTableSchemaName(),
                    change.getReferencedTableName(),
                    change.getReferencedColumnNames()
                )
            ));

        assertEquals(Map.of(
            "fk_repository_source_workspace_branch_workspace",
            new ForeignKeyExpectation(SCHEMA, "workspace_branch", "workspace_id", SCHEMA, "workspace", "workspace_id")
        ), foreignKeys);
    }

    private static void assertIndexes(List<Change> changes) {
        var indexes = changes.stream()
            .filter(CreateIndexChange.class::isInstance)
            .map(CreateIndexChange.class::cast)
            .collect(Collectors.toMap(
                CreateIndexChange::getIndexName,
                change -> new IndexExpectation(
                    change.getSchemaName(),
                    change.getTableName(),
                    change.getColumns().stream().map(ColumnConfig::getName).toList()
                )
            ));

        assertEquals(Map.of(
            "ix_repository_source_workspace_branch_workspace",
            new IndexExpectation(SCHEMA, "workspace_branch", List.of("workspace_id")),
            "ix_repository_source_preparation_run_workspace",
            new IndexExpectation(SCHEMA, "repository_preparation", List.of("analysis_run_id", "workspace_id"))
        ), indexes);
    }

    private static void assertNoByteStorage(Map<String, CreateTableChange> tables) {
        var forbiddenFragments = List.of("byte", "bytes", "blob", "large_object", "file_content", "checkout_content", "source_package_bytes");
        var columnNamesAndTypes = tables.values().stream()
            .flatMap(table -> table.getColumns().stream())
            .flatMap(column -> Arrays.stream(new String[] { column.getName(), column.getType() }))
            .map(value -> value.toLowerCase(Locale.ROOT))
            .toList();

        assertFalse(columnNamesAndTypes.stream().anyMatch(value -> forbiddenFragments.stream().anyMatch(value::contains)));
        assertTrue(tables.get("repository_preparation").getColumns().stream()
            .map(ColumnConfig::getName)
            .toList()
            .contains("source_snapshot_json"));
    }

    private record ColumnExpectation(String type, boolean nullable) {
    }

    private record KeyExpectation(String schema, String tableName, String columnNames) {
    }

    private record ForeignKeyExpectation(
        String baseSchema,
        String baseTable,
        String baseColumns,
        String referencedSchema,
        String referencedTable,
        String referencedColumns
    ) {
    }

    private record IndexExpectation(String schema, String tableName, List<String> columns) {
    }
}
