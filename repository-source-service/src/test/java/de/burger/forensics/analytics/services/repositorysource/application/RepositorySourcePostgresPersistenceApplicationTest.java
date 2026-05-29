package de.burger.forensics.analytics.services.repositorysource.application;

import de.burger.forensics.analytics.services.repositorysource.adapter.out.postgres.PostgresRepositorySourcePersistenceAdapter;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositorySourceIdempotencyRecord;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.ArtifactByteAccess;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.ArtifactByteCustody;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.ArtifactReference;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.BuildOutputPackageDescriptor;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.BuildOutputProducer;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.BuildOutputProducerCandidate;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.BuildOutputProducerStatus;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.BuildOutputResolution;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.CheckoutResult;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.CheckoutStatus;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.Diagnostic;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.PackageAvailability;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryIdentity;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryPreparation;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryReference;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspace;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceBranch;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceBranchStatus;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceStatus;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RevisionSelector;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourcePackageDescriptor;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourceRoot;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourceSnapshot;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourceSnapshotCompleteness;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourceSnapshotId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceBranchId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceId;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.WorkspaceTitle;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositorySourcePostgresPersistenceApplicationTest {
    private static final String SHA = "a".repeat(64);
    private static final Instant NOW = Instant.parse("2026-05-24T09:30:00Z");

    @Test
    void savesPreparationWithCompositeConflictTargetAndLoadsSnapshotMetadata() {
        var recording = new RecordingConnection("");
        var adapter = new PostgresRepositorySourcePersistenceAdapter("repository_source", recording::connection);
        var preparation = preparation();

        adapter.save(preparation);
        var loadedBySnapshot = adapter.findByRunAndSnapshot(preparation.analysisRunId(), preparation.sourceSnapshotId()).orElseThrow();
        var loadedByWorkspace = adapter.findByRunAndWorkspace(preparation.analysisRunId(), preparation.workspaceId()).orElseThrow();

        assertSql(recording, "INSERT INTO repository_source.repository_preparation");
        assertSql(recording, "ON CONFLICT (analysis_run_id, source_snapshot_id) DO UPDATE SET");
        assertEquals(preparation.sourceSnapshot(), loadedBySnapshot.sourceSnapshot());
        assertEquals(preparation.sourceSnapshot(), loadedByWorkspace.sourceSnapshot());
        assertEquals("repository-source-service", loadedBySnapshot.sourceSnapshot().sourcePackage().producerService());
        assertEquals("build-artifact-worker-service", loadedBySnapshot.sourceSnapshot().buildOutputPackage().producerService());
    }

    @Test
    void failsWhenPostgresPreparationSnapshotMetadataIsMissing() {
        var recording = new RecordingConnection("");
        var adapter = new PostgresRepositorySourcePersistenceAdapter("repository_source", recording::connection);
        var preparation = preparation();
        adapter.save(preparation);
        recording.repositoryPreparations.getFirst().put("source_snapshot_json", "");

        assertThrows(IllegalStateException.class, () -> adapter.findByRunAndSnapshot(
            preparation.analysisRunId(),
            preparation.sourceSnapshotId()
        ));
    }

    @Test
    void savesWorkspaceWithSchemaQualifiedPostgresUpsertsInOneTransaction() {
        var recording = new RecordingConnection("");
        var adapter = new PostgresRepositorySourcePersistenceAdapter("repository_source", recording::connection);

        adapter.save(workspace());

        assertEquals(List.of(false), recording.autoCommitValues);
        assertTrue(recording.committed);
        assertFalse(recording.rolledBack);
        assertSql(recording, "INSERT INTO repository_source.workspace");
        assertSql(recording, "ON CONFLICT (workspace_id) DO UPDATE SET");
        assertSql(recording, "DELETE FROM repository_source.workspace_branch WHERE workspace_id = ?");
        assertSql(recording, "INSERT INTO repository_source.workspace_branch");
        assertSql(recording, "ON CONFLICT (workspace_branch_id) DO UPDATE SET");
    }

    @Test
    void loadsWorkspaceRowsWithDeterministicOrderingFilteringAndBranchMapping() {
        var recording = new RecordingConnection("");
        var adapter = new PostgresRepositorySourcePersistenceAdapter("repository_source", recording::connection);
        var beta = workspace("workspace-0002", "beta", RepositoryWorkspaceStatus.CLEANED);
        var alphaWorkspaceId = new WorkspaceId("workspace-0001");
        var alpha = workspace(alphaWorkspaceId, "alpha", RepositoryWorkspaceStatus.READY, List.of(
            branch(alphaWorkspaceId, "0001-zeta", "zeta"),
            branch(alphaWorkspaceId, "0001-alpha", "alpha")
        ));
        var alphaWithSortedBranches = workspace(alphaWorkspaceId, "alpha", RepositoryWorkspaceStatus.READY, List.of(
            branch(alphaWorkspaceId, "0001-alpha", "alpha"),
            branch(alphaWorkspaceId, "0001-zeta", "zeta")
        ));
        adapter.save(beta);
        adapter.save(alpha);

        var visible = adapter.findAll(false);
        var all = adapter.findAll(true);
        var loadedById = adapter.findById(alpha.workspaceId()).orElseThrow();
        var loadedByRepositoryKey = adapter.findByRepositoryKey(alpha.repository().repositoryKey()).orElseThrow();
        var loadedBranch = adapter.findBranch(alpha.workspaceId(), alpha.branches().getFirst().workspaceBranchId()).orElseThrow();
        var updated = adapter.updateWorkspaceStatus(
            alpha.workspaceId(),
            RepositoryWorkspaceStatus.CLEANED,
            NOW.plusSeconds(60),
            List.of(Diagnostic.info("CLEANED", "Repository workspace cleaned"))
        );

        assertEquals(List.of("workspace-0001"), workspaceIds(visible));
        assertEquals(List.of("workspace-0001", "workspace-0002"), workspaceIds(all));
        assertEquals(alphaWithSortedBranches, loadedById);
        assertEquals(alphaWithSortedBranches, loadedByRepositoryKey);
        assertEquals(alpha.branches().getFirst(), loadedBranch);
        assertEquals(RepositoryWorkspaceStatus.CLEANED, updated.status());
        assertEquals(NOW.plusSeconds(60), updated.updatedAt());
        assertSql(recording, "WHERE status <> ?");
        assertSql(recording, "ORDER BY workspace_id");
        assertSql(recording, "ORDER BY repository_branch, workspace_branch_id");
    }

    @Test
    void rollsBackWorkspaceSaveWhenBranchPersistenceFails() {
        var recording = new RecordingConnection("INSERT INTO repository_source.workspace_branch");
        var adapter = new PostgresRepositorySourcePersistenceAdapter("repository_source", recording::connection);

        assertThrows(IllegalStateException.class, () -> adapter.save(workspace()));

        assertTrue(recording.rolledBack);
        assertFalse(recording.committed);
    }

    @Test
    void savesAndLoadsIdempotencyRecordWithCompositeConflictTarget() {
        var recording = new RecordingConnection("");
        var adapter = new PostgresRepositorySourcePersistenceAdapter("repository_source", recording::connection);

        adapter.save(new RepositorySourceIdempotencyRecord(
            "key-1",
            "CREATE_WORKSPACE",
            "fingerprint-1",
            "REPOSITORY_WORKSPACE",
            "workspace-0001",
            "payload",
            "COMPLETED",
            NOW,
            null
        ));
        var loaded = adapter.find("CREATE_WORKSPACE", "key-1").orElseThrow();

        assertEquals("payload", loaded.resultPayload());
        assertSql(recording, "INSERT INTO repository_source.idempotency_record");
        assertSql(recording, "ON CONFLICT (idempotency_key, operation) DO UPDATE SET");
        assertSql(recording, "SELECT * FROM repository_source.idempotency_record");
    }

    @Test
    void rejectsUnsafeSchemaAndSurfacesSqlFailures() {
        assertThrows(IllegalArgumentException.class, () -> new PostgresRepositorySourcePersistenceAdapter(
            "repository-source",
            () -> {
                throw new AssertionError("schema validation should fail before opening a connection");
            }
        ));

        var recording = new RecordingConnection("SELECT * FROM repository_source.idempotency_record");
        var adapter = new PostgresRepositorySourcePersistenceAdapter("repository_source", recording::connection);

        assertThrows(IllegalStateException.class, () -> adapter.find("CREATE_WORKSPACE", "key-1"));
    }

    private static void assertSql(RecordingConnection recording, String expected) {
        assertTrue(
            recording.sql.stream().anyMatch(sql -> sql.contains(expected)),
            () -> "Expected SQL fragment was not recorded: " + expected + "\nRecorded SQL:\n" + String.join("\n---\n", recording.sql)
        );
    }

    private static List<String> workspaceIds(List<RepositoryWorkspace> workspaces) {
        return workspaces.stream().map(workspace -> workspace.workspaceId().value()).toList();
    }

    private static RepositoryPreparation preparation() {
        var sourceSnapshotId = new SourceSnapshotId("source-snapshot-postgres");
        return new RepositoryPreparation(
            new AnalysisRunId("run-postgres"),
            sourceSnapshotId,
            new WorkspaceId("workspace-preparation"),
            repository(),
            revision(),
            checkoutResult(),
            sourceSnapshot(sourceSnapshotId),
            RepositoryWorkspaceStatus.CHECKED_OUT,
            NOW,
            NOW,
            List.of(Diagnostic.info("CHECKED_OUT", "Repository checkout completed")),
            Map.of("tenant", "demo")
        );
    }

    private static CheckoutResult checkoutResult() {
        return new CheckoutResult(
            CheckoutStatus.CHECKED_OUT,
            repository().remoteUrl(),
            "b".repeat(40),
            "main",
            "",
            true,
            123,
            List.of(Diagnostic.info("CHECKED_OUT", "Repository checkout completed")),
            false,
            false,
            sourceRoots()
        );
    }

    private static RevisionSelector revision() {
        return new RevisionSelector("main", true, "", false);
    }

    private static SourceSnapshot sourceSnapshot(SourceSnapshotId sourceSnapshotId) {
        var manifest = manifest(sourceSnapshotId);
        return new SourceSnapshot(
            sourceSnapshotId,
            SourceSnapshotCompleteness.COMPLETE,
            sourceRoots(),
            manifest,
            List.of(),
            sourcePackage(sourceSnapshotId, manifest),
            buildOutputPackage(sourceSnapshotId)
        );
    }

    private static ArtifactReference manifest(SourceSnapshotId sourceSnapshotId) {
        return new ArtifactReference(
            "snapshots/" + sourceSnapshotId.value() + "/manifest.json",
            "application/json",
            SHA,
            100
        );
    }

    private static SourcePackageDescriptor sourcePackage(SourceSnapshotId sourceSnapshotId, ArtifactReference manifest) {
        return new SourcePackageDescriptor(
            PackageAvailability.PENDING,
            manifest,
            null,
            "source-package-descriptor-v1",
            "repository-source-service",
            new ArtifactByteAccess(
                "repository-source-service",
                "repository-source.v1.SourcePackage",
                "source-snapshot/" + sourceSnapshotId.value(),
                ArtifactByteCustody.PRODUCER_RETAINED
            ),
            SourceSnapshotCompleteness.COMPLETE
        );
    }

    private static BuildOutputPackageDescriptor buildOutputPackage(SourceSnapshotId sourceSnapshotId) {
        return new BuildOutputPackageDescriptor(
            PackageAvailability.PENDING,
            null,
            null,
            "build-output-package-descriptor-v1",
            "build-artifact-worker-service",
            new ArtifactByteAccess(
                "build-artifact-worker-service",
                "build-artifact-worker.v1.BuildOutputPackage",
                "source-snapshot/" + sourceSnapshotId.value(),
                ArtifactByteCustody.PRODUCER_RETAINED
            ),
            SourceSnapshotCompleteness.UNKNOWN,
            new BuildOutputResolution(
                List.of(
                    new BuildOutputProducerCandidate(BuildOutputProducer.ARTIFACT_STORE, BuildOutputProducerStatus.NOT_CONFIGURED, "", List.of()),
                    new BuildOutputProducerCandidate(BuildOutputProducer.ARTIFACTORY, BuildOutputProducerStatus.NOT_CONFIGURED, "", List.of()),
                    new BuildOutputProducerCandidate(BuildOutputProducer.JENKINS, BuildOutputProducerStatus.NOT_CONFIGURED, "", List.of()),
                    new BuildOutputProducerCandidate(
                        BuildOutputProducer.BUILD_ARTIFACT_WORKER,
                        BuildOutputProducerStatus.FALLBACK_PLANNED,
                        "source-snapshot/" + sourceSnapshotId.value(),
                        List.of(Diagnostic.info("BUILD_ARTIFACT_WORKER_FALLBACK_PLANNED", "Build artifact worker fallback is planned"))
                    )
                ),
                BuildOutputProducer.UNSPECIFIED,
                false,
                List.of()
            ),
            "auto-detect"
        );
    }

    private static RepositoryWorkspace workspace() {
        return workspace("workspace-0001", "demo", RepositoryWorkspaceStatus.READY);
    }

    private static RepositoryWorkspace workspace(String workspaceIdValue, String repositoryName, RepositoryWorkspaceStatus status) {
        var workspaceId = new WorkspaceId(workspaceIdValue);
        return workspace(workspaceId, repositoryName, status, List.of(branch(workspaceId)));
    }

    private static RepositoryWorkspace workspace(
        WorkspaceId workspaceId,
        String repositoryName,
        RepositoryWorkspaceStatus status,
        List<RepositoryWorkspaceBranch> branches
    ) {
        return new RepositoryWorkspace(
            workspaceId,
            new WorkspaceTitle(repositoryName),
            RepositoryIdentity.from(repository(repositoryName), "main"),
            status,
            NOW,
            NOW,
            branches,
            List.of(Diagnostic.info(status.name(), "Repository workspace " + status.name().toLowerCase())),
            Map.of("tenant", "demo")
        );
    }

    private static RepositoryWorkspaceBranch branch(WorkspaceId workspaceId) {
        var suffix = workspaceId.value().substring("workspace-".length());
        return branch(workspaceId, suffix, "main");
    }

    private static RepositoryWorkspaceBranch branch(WorkspaceId workspaceId, String suffix, String repositoryBranch) {
        return new RepositoryWorkspaceBranch(
            new WorkspaceBranchId("workspace-branch-" + suffix),
            workspaceId,
            repositoryBranch,
            "",
            "b".repeat(40),
            new SourceSnapshotId("source-snapshot-postgres-" + suffix),
            RepositoryWorkspaceBranchStatus.CHECKED_OUT,
            sourceRoots(),
            NOW,
            NOW,
            List.of(Diagnostic.info("CHECKED_OUT", "Repository checkout completed"))
        );
    }

    private static List<SourceRoot> sourceRoots() {
        return List.of(new SourceRoot("src/main/java", "java"));
    }

    private static RepositoryReference repository() {
        return repository("demo");
    }

    private static RepositoryReference repository(String repositoryName) {
        return new RepositoryReference("https://example.com/acme/" + repositoryName + ".git", "github", Map.of());
    }

    private static final class RecordingConnection implements InvocationHandler {
        private final String failingSqlFragment;
        private final List<String> sql = new ArrayList<>();
        private final List<Boolean> autoCommitValues = new ArrayList<>();
        private final List<Map<String, Object>> repositoryPreparations = new ArrayList<>();
        private final List<Map<String, Object>> workspaces = new ArrayList<>();
        private final List<Map<String, Object>> branches = new ArrayList<>();
        private final List<Map<String, Object>> idempotencyRecords = new ArrayList<>();
        private boolean committed;
        private boolean rolledBack;

        private RecordingConnection(String failingSqlFragment) {
            this.failingSqlFragment = failingSqlFragment;
        }

        private Connection connection() {
            return (Connection) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { Connection.class },
                this
            );
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return switch (method.getName()) {
                case "prepareStatement" -> prepareStatement((String) args[0]);
                case "setAutoCommit" -> {
                    autoCommitValues.add((Boolean) args[0]);
                    yield null;
                }
                case "commit" -> {
                    committed = true;
                    yield null;
                }
                case "rollback" -> {
                    rolledBack = true;
                    yield null;
                }
                case "close" -> null;
                case "toString" -> "recording connection";
                default -> defaultValue(method.getReturnType());
            };
        }

        private PreparedStatement prepareStatement(String statementSql) throws SQLException {
            sql.add(statementSql);
            if (!failingSqlFragment.isBlank() && statementSql.contains(failingSqlFragment)) {
                throw new SQLException("synthetic SQL failure");
            }
            return (PreparedStatement) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { PreparedStatement.class },
                new RecordingStatement(statementSql)
            );
        }

        private int executeUpdate(String statementSql, Map<Integer, Object> params) throws SQLException {
            if (statementSql.contains("INSERT INTO repository_source.repository_preparation")) {
                var row = row(params, List.of(
                    "analysis_run_id",
                    "source_snapshot_id",
                    "workspace_id",
                    "workspace_branch_id",
                    "repository_url",
                    "repository_provider",
                    "repository_attributes_json",
                    "requested_branch",
                    "branch_required",
                    "requested_commit",
                    "commit_required",
                    "resolved_commit",
                    "checkout_status",
                    "checkout_diagnostics_json",
                    "shallow_clone",
                    "checkout_duration_millis",
                    "partial_clone",
                    "sparse_checkout",
                    "workspace_status",
                    "source_snapshot_completeness",
                    "source_snapshot_json",
                    "source_roots_json",
                    "diagnostics_json",
                    "safe_attributes_json",
                    "created_at",
                    "updated_at"
                ));
                replace(repositoryPreparations, existing -> same(existing, row, "analysis_run_id", "source_snapshot_id"), row);
                return 1;
            }
            if (statementSql.contains("DELETE FROM repository_source.workspace_branch")) {
                branches.removeIf(branch -> value(branch, "workspace_id").equals(params.get(1)));
                return 1;
            }
            if (statementSql.contains("INSERT INTO repository_source.workspace_branch")) {
                var row = row(params, List.of(
                    "workspace_branch_id",
                    "workspace_id",
                    "repository_branch",
                    "requested_commit",
                    "resolved_commit",
                    "source_snapshot_id",
                    "status",
                    "source_roots_json",
                    "last_checked_at",
                    "last_updated_at",
                    "diagnostics_json"
                ));
                replace(branches, existing -> same(existing, row, "workspace_branch_id"), row);
                return 1;
            }
            if (statementSql.contains("INSERT INTO repository_source.workspace")) {
                var row = row(params, List.of(
                    "workspace_id",
                    "workspace_title",
                    "repository_key",
                    "repository_url",
                    "repository_host",
                    "repository_owner",
                    "repository_name",
                    "default_branch",
                    "status",
                    "diagnostics_json",
                    "safe_attributes_json",
                    "created_at",
                    "updated_at"
                ));
                replace(workspaces, existing -> same(existing, row, "workspace_id"), row);
                return 1;
            }
            if (statementSql.contains("UPDATE repository_source.workspace")) {
                return workspaces.stream()
                    .filter(row -> value(row, "workspace_id").equals(params.get(4)))
                    .findFirst()
                    .map(row -> {
                        row.put("status", params.get(1));
                        row.put("diagnostics_json", params.get(2));
                        row.put("updated_at", params.get(3));
                        return 1;
                    })
                    .orElse(0);
            }
            if (statementSql.contains("INSERT INTO repository_source.idempotency_record")) {
                var row = row(params, List.of(
                    "idempotency_key",
                    "operation",
                    "fingerprint",
                    "result_type",
                    "result_reference",
                    "result_payload",
                    "status",
                    "created_at",
                    "expires_at"
                ));
                replace(idempotencyRecords, existing -> same(existing, row, "idempotency_key", "operation"), row);
                return 1;
            }
            throw new SQLException("Unhandled SQL update: " + statementSql);
        }

        private List<Map<String, Object>> executeQuery(String statementSql, Map<Integer, Object> params) throws SQLException {
            if (statementSql.contains("FROM repository_source.repository_preparation")
                && statementSql.contains("source_snapshot_id")) {
                return repositoryPreparations.stream()
                    .filter(row -> value(row, "analysis_run_id").equals(params.get(1)))
                    .filter(row -> value(row, "source_snapshot_id").equals(params.get(2)))
                    .map(RepositorySourcePostgresPersistenceApplicationTest::copy)
                    .toList();
            }
            if (statementSql.contains("FROM repository_source.repository_preparation")
                && statementSql.contains("workspace_id")) {
                return repositoryPreparations.stream()
                    .filter(row -> value(row, "analysis_run_id").equals(params.get(1)))
                    .filter(row -> value(row, "workspace_id").equals(params.get(2)))
                    .map(RepositorySourcePostgresPersistenceApplicationTest::copy)
                    .toList();
            }
            if (statementSql.contains("SELECT workspace_id FROM repository_source.workspace")) {
                return workspaces.stream()
                    .filter(row -> value(row, "repository_key").equals(params.get(1)))
                    .map(row -> Map.of("workspace_id", row.get("workspace_id")))
                    .toList();
            }
            if (statementSql.contains("FROM repository_source.workspace_branch")
                && statementSql.contains("AND workspace_branch_id = ?")) {
                return branches.stream()
                    .filter(row -> value(row, "workspace_id").equals(params.get(1)))
                    .filter(row -> value(row, "workspace_branch_id").equals(params.get(2)))
                    .map(RepositorySourcePostgresPersistenceApplicationTest::copy)
                    .toList();
            }
            if (statementSql.contains("FROM repository_source.workspace_branch")) {
                if (!statementSql.contains("ORDER BY repository_branch, workspace_branch_id")) {
                    throw new SQLException("Branch query must preserve deterministic ordering");
                }
                return branches.stream()
                    .filter(row -> value(row, "workspace_id").equals(params.get(1)))
                    .sorted(Comparator.comparing((Map<String, Object> row) -> value(row, "repository_branch"))
                        .thenComparing(row -> value(row, "workspace_branch_id")))
                    .map(RepositorySourcePostgresPersistenceApplicationTest::copy)
                    .toList();
            }
            if (statementSql.contains("FROM repository_source.workspace")
                && statementSql.contains("WHERE workspace_id = ?")) {
                return workspaces.stream()
                    .filter(row -> value(row, "workspace_id").equals(params.get(1)))
                    .map(RepositorySourcePostgresPersistenceApplicationTest::copy)
                    .toList();
            }
            if (statementSql.contains("FROM repository_source.workspace")) {
                if (!statementSql.contains("ORDER BY workspace_id")) {
                    throw new SQLException("Workspace query must preserve deterministic ordering");
                }
                if (params.containsKey(1) && !statementSql.contains("WHERE status <> ?")) {
                    throw new SQLException("Visible workspace query must exclude cleaned workspaces");
                }
                var cleanedStatus = params.get(1);
                return workspaces.stream()
                    .filter(row -> cleanedStatus == null || !value(row, "status").equals(cleanedStatus))
                    .sorted(Comparator.comparing(row -> value(row, "workspace_id")))
                    .map(RepositorySourcePostgresPersistenceApplicationTest::copy)
                    .toList();
            }
            if (statementSql.contains("FROM repository_source.idempotency_record")) {
                return idempotencyRecords.stream()
                    .filter(row -> value(row, "operation").equals(params.get(1)))
                    .filter(row -> value(row, "idempotency_key").equals(params.get(2)))
                    .map(RepositorySourcePostgresPersistenceApplicationTest::copy)
                    .toList();
            }
            throw new SQLException("Unhandled SQL query: " + statementSql);
        }

        private final class RecordingStatement implements InvocationHandler {
            private final String statementSql;
            private final Map<Integer, Object> params = new LinkedHashMap<>();

            private RecordingStatement(String statementSql) {
                this.statementSql = statementSql;
            }

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                return switch (method.getName()) {
                    case "setString", "setBoolean", "setLong" -> {
                        params.put((Integer) args[0], args[1]);
                        yield null;
                    }
                    case "executeUpdate" -> executeUpdate(statementSql, params);
                    case "executeQuery" -> resultSet(executeQuery(statementSql, params));
                    case "close" -> null;
                    case "toString" -> statementSql;
                    default -> defaultValue(method.getReturnType());
                };
            }
        }
    }

    private static ResultSet resultSet(List<Map<String, Object>> rows) {
        InvocationHandler resultSet = new InvocationHandler() {
            private int index = -1;

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                return switch (method.getName()) {
                    case "next" -> ++index < rows.size();
                    case "getString" -> string(rows.get(index), (String) args[0]);
                    case "getBoolean" -> Boolean.parseBoolean(string(rows.get(index), (String) args[0]));
                    case "getLong" -> Long.parseLong(string(rows.get(index), (String) args[0]));
                    case "close" -> null;
                    case "toString" -> rows.toString();
                    default -> defaultValue(method.getReturnType());
                };
            }
        };
        return (ResultSet) Proxy.newProxyInstance(
            RepositorySourcePostgresPersistenceApplicationTest.class.getClassLoader(),
            new Class<?>[] { ResultSet.class },
            resultSet
        );
    }

    private static Map<String, Object> row(Map<Integer, Object> params, List<String> columns) {
        var row = new LinkedHashMap<String, Object>();
        for (var index = 0; index < columns.size(); index++) {
            row.put(columns.get(index), params.get(index + 1));
        }
        return row;
    }

    private static void replace(
        List<Map<String, Object>> rows,
        Predicate<Map<String, Object>> existingRowMatches,
        Map<String, Object> row
    ) {
        rows.removeIf(existingRowMatches);
        rows.add(row);
    }

    private static boolean same(Map<String, Object> existing, Map<String, Object> row, String... columns) {
        for (String column : columns) {
            if (!value(existing, column).equals(value(row, column))) {
                return false;
            }
        }
        return true;
    }

    private static Map<String, Object> copy(Map<String, Object> row) {
        return new LinkedHashMap<>(row);
    }

    private static String value(Map<String, Object> row, String column) {
        return string(row, column);
    }

    private static String string(Map<String, Object> row, String column) {
        var value = row.get(column);
        return value == null ? null : value.toString();
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
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == double.class) {
            return 0.0d;
        }
        if (returnType == float.class) {
            return 0.0f;
        }
        return 0;
    }
}
