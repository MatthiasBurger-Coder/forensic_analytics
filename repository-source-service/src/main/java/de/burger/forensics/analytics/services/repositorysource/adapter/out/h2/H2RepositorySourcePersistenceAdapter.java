package de.burger.forensics.analytics.services.repositorysource.adapter.out.h2;

import com.google.gson.Gson;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryPreparationRepository;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositorySourceIdempotencyRecord;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositorySourceIdempotencyRepository;
import de.burger.forensics.analytics.services.repositorysource.application.port.RepositoryWorkspaceRepository;
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
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.DiagnosticSeverity;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.PackageAvailability;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryIdentity;
import de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryKey;
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

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.sha256Hex;

public final class H2RepositorySourcePersistenceAdapter implements
    RepositoryPreparationRepository,
    RepositoryWorkspaceRepository,
    RepositorySourceIdempotencyRepository {
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final Gson gson = new Gson();

    public H2RepositorySourcePersistenceAdapter(String jdbcUrl, String username, String password) {
        this.jdbcUrl = Objects.requireNonNull(jdbcUrl, "jdbc url must not be null");
        this.username = Objects.requireNonNullElse(username, "");
        this.password = Objects.requireNonNullElse(password, "");
        loadH2Driver();
        initializeSchema();
    }

    @Override
    public RepositoryPreparation save(RepositoryPreparation preparation) {
        Objects.requireNonNull(preparation, "preparation must not be null");
        try (var connection = connection()) {
            try (var statement = connection.prepareStatement("""
                MERGE INTO repository_preparation (
                    analysis_run_id,
                    source_snapshot_id,
                    workspace_id,
                    workspace_branch_id,
                    repository_url,
                    repository_provider,
                    repository_attributes_json,
                    requested_branch,
                    branch_required,
                    requested_commit,
                    commit_required,
                    resolved_commit,
                    checkout_status,
                    checkout_diagnostics_json,
                    shallow_clone,
                    checkout_duration_millis,
                    partial_clone,
                    sparse_checkout,
                    workspace_status,
                    source_snapshot_completeness,
                    source_snapshot_json,
                    source_roots_json,
                    diagnostics_json,
                    safe_attributes_json,
                    created_at,
                    updated_at
                ) KEY (analysis_run_id, source_snapshot_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
                var checkout = preparation.checkout();
                statement.setString(1, preparation.analysisRunId().value());
                statement.setString(2, preparation.sourceSnapshotId().value());
                statement.setString(3, preparation.workspaceId().value());
                statement.setString(4, null);
                statement.setString(5, preparation.repository().remoteUrl());
                statement.setString(6, preparation.repository().provider());
                statement.setString(7, attributesJson(preparation.repository().safeAttributes()));
                statement.setString(8, preparation.requestedRevision().branch());
                statement.setBoolean(9, preparation.requestedRevision().branchRequired());
                statement.setString(10, preparation.requestedRevision().commit());
                statement.setBoolean(11, preparation.requestedRevision().commitRequired());
                statement.setString(12, checkout.resolvedCommit());
                statement.setString(13, checkout.status().name());
                statement.setString(14, diagnosticsJson(checkout.diagnostics()));
                statement.setBoolean(15, checkout.shallowClone());
                statement.setLong(16, checkout.elapsedMillis());
                statement.setBoolean(17, checkout.partialClone());
                statement.setBoolean(18, checkout.sparseCheckout());
                statement.setString(19, preparation.workspaceStatus().name());
                statement.setString(20, preparation.sourceSnapshot().completeness().name());
                statement.setString(21, sourceSnapshotJson(preparation.sourceSnapshot()));
                statement.setString(22, sourceRootsJson(checkout.sourceRoots()));
                statement.setString(23, diagnosticsJson(preparation.diagnostics()));
                statement.setString(24, attributesJson(preparation.safeAttributes()));
                statement.setString(25, preparation.createdAt().toString());
                statement.setString(26, preparation.updatedAt().toString());
                statement.executeUpdate();
            }
            return preparation;
        } catch (SQLException error) {
            throw new IllegalStateException("Failed to save repository preparation", error);
        }
    }

    @Override
    public Optional<RepositoryPreparation> findByRunAndSnapshot(
        AnalysisRunId analysisRunId,
        SourceSnapshotId sourceSnapshotId
    ) {
        try (var connection = connection();
             var statement = connection.prepareStatement("""
                 SELECT * FROM repository_preparation
                 WHERE analysis_run_id = ? AND source_snapshot_id = ?
                 """)) {
            statement.setString(1, analysisRunId.value());
            statement.setString(2, sourceSnapshotId.value());
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(preparation(resultSet)) : Optional.empty();
            }
        } catch (SQLException error) {
            throw new IllegalStateException("Failed to load repository preparation", error);
        }
    }

    @Override
    public Optional<RepositoryPreparation> findByRunAndWorkspace(AnalysisRunId analysisRunId, WorkspaceId workspaceId) {
        try (var connection = connection();
             var statement = connection.prepareStatement("""
                 SELECT * FROM repository_preparation
                 WHERE analysis_run_id = ? AND workspace_id = ?
                 """)) {
            statement.setString(1, analysisRunId.value());
            statement.setString(2, workspaceId.value());
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(preparation(resultSet)) : Optional.empty();
            }
        } catch (SQLException error) {
            throw new IllegalStateException("Failed to load repository preparation", error);
        }
    }

    @Override
    public RepositoryWorkspace save(RepositoryWorkspace workspace) {
        Objects.requireNonNull(workspace, "workspace must not be null");
        try (var connection = connection()) {
            connection.setAutoCommit(false);
            try {
                saveWorkspace(connection, workspace);
                deleteBranches(connection, workspace.workspaceId());
                for (RepositoryWorkspaceBranch branch : workspace.branches()) {
                    saveBranch(connection, branch);
                }
                connection.commit();
                return workspace;
            } catch (SQLException error) {
                connection.rollback();
                throw error;
            }
        } catch (SQLException error) {
            throw new IllegalStateException("Failed to save repository workspace", error);
        }
    }

    @Override
    public List<RepositoryWorkspace> findAll(boolean includeCleaned) {
        var query = includeCleaned
            ? """
                 SELECT * FROM workspace
                 ORDER BY workspace_id
                 """
            : """
                 SELECT * FROM workspace
                 WHERE status <> ?
                 ORDER BY workspace_id
                 """;
        try (var connection = connection();
             var statement = connection.prepareStatement(query)) {
            if (!includeCleaned) {
                statement.setString(1, RepositoryWorkspaceStatus.CLEANED.name());
            }
            try (var resultSet = statement.executeQuery()) {
                var workspaces = new java.util.ArrayList<RepositoryWorkspace>();
                while (resultSet.next()) {
                    var workspaceId = new WorkspaceId(resultSet.getString("workspace_id"));
                    workspaces.add(workspace(resultSet, branches(connection, workspaceId)));
                }
                return List.copyOf(workspaces);
            }
        } catch (SQLException error) {
            throw new IllegalStateException("Failed to list repository workspaces", error);
        }
    }

    @Override
    public Optional<RepositoryWorkspace> findById(WorkspaceId workspaceId) {
        try (var connection = connection();
             var statement = connection.prepareStatement("""
                 SELECT * FROM workspace
                 WHERE workspace_id = ?
                 """)) {
            statement.setString(1, workspaceId.value());
            try (var resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                var workspace = workspace(resultSet, branches(connection, workspaceId));
                return Optional.of(workspace);
            }
        } catch (SQLException error) {
            throw new IllegalStateException("Failed to load repository workspace", error);
        }
    }

    @Override
    public Optional<RepositoryWorkspace> findByRepositoryKey(RepositoryKey repositoryKey) {
        try (var connection = connection();
             var statement = connection.prepareStatement("""
                 SELECT workspace_id FROM workspace
                 WHERE repository_key = ?
                 """)) {
            statement.setString(1, repositoryKey.value());
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next()
                    ? findById(new WorkspaceId(resultSet.getString("workspace_id")))
                    : Optional.empty();
            }
        } catch (SQLException error) {
            throw new IllegalStateException("Failed to load repository workspace", error);
        }
    }

    @Override
    public Optional<RepositoryWorkspaceBranch> findBranch(WorkspaceId workspaceId, WorkspaceBranchId workspaceBranchId) {
        try (var connection = connection();
             var statement = connection.prepareStatement("""
                 SELECT * FROM workspace_branch
                 WHERE workspace_id = ? AND workspace_branch_id = ?
                 """)) {
            statement.setString(1, workspaceId.value());
            statement.setString(2, workspaceBranchId.value());
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(branch(resultSet)) : Optional.empty();
            }
        } catch (SQLException error) {
            throw new IllegalStateException("Failed to load repository workspace branch", error);
        }
    }

    @Override
    public RepositoryWorkspace updateWorkspaceStatus(
        WorkspaceId workspaceId,
        RepositoryWorkspaceStatus status,
        Instant updatedAt,
        List<Diagnostic> diagnostics
    ) {
        try (var connection = connection();
             var statement = connection.prepareStatement("""
                 UPDATE workspace
                 SET status = ?, diagnostics_json = ?, updated_at = ?
                 WHERE workspace_id = ?
                 """)) {
            statement.setString(1, status.name());
            statement.setString(2, diagnosticsJson(diagnostics));
            statement.setString(3, updatedAt.toString());
            statement.setString(4, workspaceId.value());
            var updated = statement.executeUpdate();
            if (updated == 0) {
                throw new IllegalStateException("repository workspace was not found");
            }
            return findById(workspaceId)
                .orElseThrow(() -> new IllegalStateException("repository workspace was not found"));
        } catch (SQLException error) {
            throw new IllegalStateException("Failed to update repository workspace status", error);
        }
    }

    @Override
    public Optional<RepositorySourceIdempotencyRecord> find(String operation, String idempotencyKey) {
        try (var connection = connection();
             var statement = connection.prepareStatement("""
                 SELECT * FROM idempotency_record
                 WHERE operation = ? AND idempotency_key = ?
                 """)) {
            statement.setString(1, operation);
            statement.setString(2, idempotencyKey);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(idempotencyRecord(resultSet)) : Optional.empty();
            }
        } catch (SQLException error) {
            throw new IllegalStateException("Failed to load idempotency record", error);
        }
    }

    @Override
    public RepositorySourceIdempotencyRecord save(RepositorySourceIdempotencyRecord record) {
        try (var connection = connection();
             var statement = connection.prepareStatement("""
                 MERGE INTO idempotency_record (
                    idempotency_key,
                    operation,
                    fingerprint,
                    result_type,
                    result_reference,
                    result_payload,
                    status,
                    created_at,
                    expires_at
                 ) KEY (idempotency_key, operation) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                 """)) {
            statement.setString(1, record.idempotencyKey());
            statement.setString(2, record.operation());
            statement.setString(3, record.fingerprint());
            statement.setString(4, record.resultType());
            statement.setString(5, record.resultReference());
            statement.setString(6, record.resultPayload());
            statement.setString(7, record.status());
            statement.setString(8, record.createdAt().toString());
            statement.setString(9, record.expiresAt() == null ? null : record.expiresAt().toString());
            statement.executeUpdate();
            return record;
        } catch (SQLException error) {
            throw new IllegalStateException("Failed to save idempotency record", error);
        }
    }

    private void initializeSchema() {
        try (var connection = connection();
             var statement = connection.createStatement()) {
            H2RepositorySourceSchemaInitializer.initialize(statement);
        } catch (SQLException error) {
            throw new IllegalStateException("Failed to initialize repository-source H2 schema", error);
        }
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    private static void loadH2Driver() {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException error) {
            throw new IllegalStateException("H2 JDBC driver is not available", error);
        }
    }

    private void saveWorkspace(Connection connection, RepositoryWorkspace workspace) throws SQLException {
        try (var statement = connection.prepareStatement("""
            MERGE INTO workspace (
                workspace_id,
                workspace_title,
                repository_key,
                repository_url,
                repository_host,
                repository_owner,
                repository_name,
                default_branch,
                status,
                diagnostics_json,
                safe_attributes_json,
                created_at,
                updated_at
            ) KEY (workspace_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """)) {
            var repository = workspace.repository();
            statement.setString(1, workspace.workspaceId().value());
            statement.setString(2, workspace.workspaceTitle().value());
            statement.setString(3, repository.repositoryKey().value());
            statement.setString(4, repository.repositoryUrl());
            statement.setString(5, repository.repositoryHost());
            statement.setString(6, repository.repositoryOwner());
            statement.setString(7, repository.repositoryName());
            statement.setString(8, repository.defaultBranch());
            statement.setString(9, workspace.status().name());
            statement.setString(10, diagnosticsJson(workspace.diagnostics()));
            statement.setString(11, attributesJson(workspace.safeAttributes()));
            statement.setString(12, workspace.createdAt().toString());
            statement.setString(13, workspace.updatedAt().toString());
            statement.executeUpdate();
        }
    }

    private static void deleteBranches(Connection connection, WorkspaceId workspaceId) throws SQLException {
        try (var statement = connection.prepareStatement("DELETE FROM workspace_branch WHERE workspace_id = ?")) {
            statement.setString(1, workspaceId.value());
            statement.executeUpdate();
        }
    }

    private void saveBranch(Connection connection, RepositoryWorkspaceBranch branch) throws SQLException {
        try (var statement = connection.prepareStatement("""
            MERGE INTO workspace_branch (
                workspace_branch_id,
                workspace_id,
                repository_branch,
                requested_commit,
                resolved_commit,
                source_snapshot_id,
                status,
                source_roots_json,
                last_checked_at,
                last_updated_at,
                diagnostics_json
            ) KEY (workspace_branch_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """)) {
            statement.setString(1, branch.workspaceBranchId().value());
            statement.setString(2, branch.workspaceId().value());
            statement.setString(3, branch.repositoryBranch());
            statement.setString(4, branch.requestedCommit());
            statement.setString(5, branch.resolvedCommit());
            statement.setString(6, branch.sourceSnapshotId() == null ? null : branch.sourceSnapshotId().value());
            statement.setString(7, branch.status().name());
            statement.setString(8, sourceRootsJson(branch.sourceRoots()));
            statement.setString(9, branch.lastCheckedAt() == null ? null : branch.lastCheckedAt().toString());
            statement.setString(10, branch.lastUpdatedAt().toString());
            statement.setString(11, diagnosticsJson(branch.diagnostics()));
            statement.executeUpdate();
        }
    }

    private RepositoryPreparation preparation(ResultSet resultSet) throws SQLException {
        var repository = new RepositoryReference(
            resultSet.getString("repository_url"),
            resultSet.getString("repository_provider"),
            attributes(resultSet.getString("repository_attributes_json"))
        );
        var revision = new RevisionSelector(
            resultSet.getString("requested_branch"),
            resultSet.getBoolean("branch_required"),
            resultSet.getString("requested_commit"),
            resultSet.getBoolean("commit_required")
        );
        var sourceRoots = sourceRoots(resultSet.getString("source_roots_json"));
        var checkout = new CheckoutResult(
            CheckoutStatus.valueOf(resultSet.getString("checkout_status")),
            repository.remoteUrl(),
            resultSet.getString("resolved_commit"),
            revision.branch(),
            revision.commit(),
            resultSet.getBoolean("shallow_clone"),
            resultSet.getLong("checkout_duration_millis"),
            diagnostics(resultSet.getString("checkout_diagnostics_json")),
            resultSet.getBoolean("partial_clone"),
            resultSet.getBoolean("sparse_checkout"),
            sourceRoots
        );
        var sourceSnapshotId = new SourceSnapshotId(resultSet.getString("source_snapshot_id"));
        var snapshot = sourceSnapshot(resultSet.getString("source_snapshot_json"), repository, revision, checkout, sourceSnapshotId, sourceRoots);
        return new RepositoryPreparation(
            new AnalysisRunId(resultSet.getString("analysis_run_id")),
            sourceSnapshotId,
            new WorkspaceId(resultSet.getString("workspace_id")),
            repository,
            revision,
            checkout,
            snapshot,
            RepositoryWorkspaceStatus.valueOf(resultSet.getString("workspace_status")),
            Instant.parse(resultSet.getString("created_at")),
            Instant.parse(resultSet.getString("updated_at")),
            diagnostics(resultSet.getString("diagnostics_json")),
            attributes(resultSet.getString("safe_attributes_json"))
        );
    }

    private RepositoryWorkspace workspace(ResultSet resultSet, List<RepositoryWorkspaceBranch> branches) throws SQLException {
        return new RepositoryWorkspace(
            new WorkspaceId(resultSet.getString("workspace_id")),
            new WorkspaceTitle(resultSet.getString("workspace_title")),
            new RepositoryIdentity(
                new RepositoryKey(resultSet.getString("repository_key")),
                resultSet.getString("repository_url"),
                resultSet.getString("repository_host"),
                resultSet.getString("repository_owner"),
                resultSet.getString("repository_name"),
                resultSet.getString("default_branch")
            ),
            RepositoryWorkspaceStatus.valueOf(resultSet.getString("status")),
            Instant.parse(resultSet.getString("created_at")),
            Instant.parse(resultSet.getString("updated_at")),
            branches,
            diagnostics(resultSet.getString("diagnostics_json")),
            attributes(resultSet.getString("safe_attributes_json"))
        );
    }

    private List<RepositoryWorkspaceBranch> branches(Connection connection, WorkspaceId workspaceId) throws SQLException {
        try (var statement = connection.prepareStatement("""
            SELECT * FROM workspace_branch
            WHERE workspace_id = ?
            ORDER BY repository_branch, workspace_branch_id
            """)) {
            statement.setString(1, workspaceId.value());
            try (var resultSet = statement.executeQuery()) {
                var branches = new java.util.ArrayList<RepositoryWorkspaceBranch>();
                while (resultSet.next()) {
                    branches.add(branch(resultSet));
                }
                return List.copyOf(branches);
            }
        }
    }

    private RepositoryWorkspaceBranch branch(ResultSet resultSet) throws SQLException {
        var snapshotId = resultSet.getString("source_snapshot_id");
        var lastCheckedAt = resultSet.getString("last_checked_at");
        return new RepositoryWorkspaceBranch(
            new WorkspaceBranchId(resultSet.getString("workspace_branch_id")),
            new WorkspaceId(resultSet.getString("workspace_id")),
            resultSet.getString("repository_branch"),
            resultSet.getString("requested_commit"),
            resultSet.getString("resolved_commit"),
            snapshotId == null || snapshotId.isBlank() ? null : new SourceSnapshotId(snapshotId),
            RepositoryWorkspaceBranchStatus.valueOf(resultSet.getString("status")),
            sourceRoots(resultSet.getString("source_roots_json")),
            lastCheckedAt == null || lastCheckedAt.isBlank() ? null : Instant.parse(lastCheckedAt),
            Instant.parse(resultSet.getString("last_updated_at")),
            diagnostics(resultSet.getString("diagnostics_json"))
        );
    }

    private static RepositorySourceIdempotencyRecord idempotencyRecord(ResultSet resultSet) throws SQLException {
        var expiresAt = resultSet.getString("expires_at");
        return new RepositorySourceIdempotencyRecord(
            resultSet.getString("idempotency_key"),
            resultSet.getString("operation"),
            resultSet.getString("fingerprint"),
            resultSet.getString("result_type"),
            resultSet.getString("result_reference"),
            resultSet.getString("result_payload"),
            resultSet.getString("status"),
            Instant.parse(resultSet.getString("created_at")),
            expiresAt == null || expiresAt.isBlank() ? null : Instant.parse(expiresAt)
        );
    }

    private String diagnosticsJson(List<Diagnostic> diagnostics) {
        return gson.toJson(diagnostics.stream()
            .map(diagnostic -> new DiagnosticJson(diagnostic.code(), diagnostic.message(), diagnostic.severity().name()))
            .toArray(DiagnosticJson[]::new));
    }

    private List<Diagnostic> diagnostics(String json) {
        return Arrays.stream(gson.fromJson(emptyArrayWhenBlank(json), DiagnosticJson[].class))
            .map(diagnostic -> new Diagnostic(
                diagnostic.code(),
                diagnostic.message(),
                DiagnosticSeverity.valueOf(diagnostic.severity())
            ))
            .toList();
    }

    private String sourceRootsJson(List<SourceRoot> sourceRoots) {
        return gson.toJson(sourceRoots.stream()
            .map(sourceRoot -> new SourceRootJson(sourceRoot.relativePath(), sourceRoot.language()))
            .toArray(SourceRootJson[]::new));
    }

    private List<SourceRoot> sourceRoots(String json) {
        return Arrays.stream(gson.fromJson(emptyArrayWhenBlank(json), SourceRootJson[].class))
            .map(sourceRoot -> new SourceRoot(sourceRoot.relativePath(), sourceRoot.language()))
            .toList();
    }

    private String attributesJson(Map<String, String> attributes) {
        return gson.toJson(attributes.entrySet().stream()
            .map(entry -> new AttributeJson(entry.getKey(), entry.getValue()))
            .toArray(AttributeJson[]::new));
    }

    private Map<String, String> attributes(String json) {
        var attributes = new java.util.TreeMap<String, String>();
        Arrays.stream(gson.fromJson(emptyArrayWhenBlank(json), AttributeJson[].class))
            .forEach(attribute -> attributes.put(attribute.name(), attribute.value()));
        return attributes;
    }

    private String sourceSnapshotJson(SourceSnapshot sourceSnapshot) {
        return gson.toJson(SourceSnapshotJson.from(sourceSnapshot));
    }

    private SourceSnapshot sourceSnapshot(
        String json,
        RepositoryReference repository,
        RevisionSelector revision,
        CheckoutResult checkout,
        SourceSnapshotId sourceSnapshotId,
        List<SourceRoot> sourceRoots
    ) {
        if (json == null || json.isBlank()) {
            var manifest = manifest(repository, revision, checkout.resolvedCommit(), sourceRoots, sourceSnapshotId);
            return new SourceSnapshot(
                sourceSnapshotId,
                SourceSnapshotCompleteness.COMPLETE,
                sourceRoots,
                manifest,
                List.of(),
                sourcePackage(sourceSnapshotId, manifest),
                buildOutputPackage(sourceSnapshotId)
            );
        }
        return gson.fromJson(json, SourceSnapshotJson.class).toDomain();
    }

    private static String emptyArrayWhenBlank(String json) {
        return json == null || json.isBlank() ? "[]" : json;
    }

    private static ArtifactReference manifest(
        RepositoryReference repository,
        RevisionSelector revision,
        String resolvedCommit,
        List<SourceRoot> sourceRoots,
        SourceSnapshotId sourceSnapshotId
    ) {
        var payload = manifestPayload(repository, revision, resolvedCommit, sourceRoots);
        return new ArtifactReference(
            "snapshots/" + sourceSnapshotId.value() + "/manifest.json",
            "application/json",
            sha256Hex(payload),
            payload.getBytes(StandardCharsets.UTF_8).length
        );
    }

    private static String manifestPayload(
        RepositoryReference repository,
        RevisionSelector revision,
        String resolvedCommit,
        List<SourceRoot> sourceRoots
    ) {
        return String.join(
            "\n",
            repository.remoteUrl(),
            revision.branch(),
            revision.commit(),
            resolvedCommit,
            sourceRoots.toString()
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

    private record DiagnosticJson(String code, String message, String severity) {
    }

    private record SourceRootJson(String relativePath, String language) {
    }

    private record AttributeJson(String name, String value) {
    }

    private record SourceSnapshotJson(
        String sourceSnapshotId,
        String completeness,
        SourceRootJson[] sourceRoots,
        ArtifactReferenceJson manifestArtifact,
        String[] limitations,
        SourcePackageDescriptorJson sourcePackage,
        BuildOutputPackageDescriptorJson buildOutputPackage
    ) {
        static SourceSnapshotJson from(SourceSnapshot sourceSnapshot) {
            return new SourceSnapshotJson(
                sourceSnapshot.sourceSnapshotId().value(),
                sourceSnapshot.completeness().name(),
                sourceSnapshot.sourceRoots().stream()
                    .map(sourceRoot -> new SourceRootJson(sourceRoot.relativePath(), sourceRoot.language()))
                    .toArray(SourceRootJson[]::new),
                ArtifactReferenceJson.from(sourceSnapshot.manifestArtifact()),
                sourceSnapshot.limitations().toArray(String[]::new),
                SourcePackageDescriptorJson.from(sourceSnapshot.sourcePackage()),
                BuildOutputPackageDescriptorJson.from(sourceSnapshot.buildOutputPackage())
            );
        }

        SourceSnapshot toDomain() {
            return new SourceSnapshot(
                new SourceSnapshotId(sourceSnapshotId),
                SourceSnapshotCompleteness.valueOf(completeness),
                Arrays.stream(sourceRoots).map(sourceRoot -> new SourceRoot(sourceRoot.relativePath(), sourceRoot.language())).toList(),
                manifestArtifact.toDomain(),
                Arrays.asList(limitations),
                sourcePackage.toDomain(),
                buildOutputPackage.toDomain()
            );
        }
    }

    private record ArtifactReferenceJson(String reference, String type, String sha256, long sizeBytes) {
        static ArtifactReferenceJson from(ArtifactReference artifact) {
            return artifact == null
                ? null
                : new ArtifactReferenceJson(artifact.reference(), artifact.type(), artifact.sha256(), artifact.sizeBytes());
        }

        ArtifactReference toDomain() {
            return this == null ? null : new ArtifactReference(reference, type, sha256, sizeBytes);
        }
    }

    private record ArtifactByteAccessJson(
        String ownerService,
        String retrievalContract,
        String retrievalReference,
        String byteCustody
    ) {
        static ArtifactByteAccessJson from(ArtifactByteAccess byteAccess) {
            return new ArtifactByteAccessJson(
                byteAccess.ownerService(),
                byteAccess.retrievalContract(),
                byteAccess.retrievalReference(),
                byteAccess.byteCustody().name()
            );
        }

        ArtifactByteAccess toDomain() {
            return new ArtifactByteAccess(
                ownerService,
                retrievalContract,
                retrievalReference,
                ArtifactByteCustody.valueOf(byteCustody)
            );
        }
    }

    private record SourcePackageDescriptorJson(
        String availability,
        ArtifactReferenceJson manifestArtifact,
        ArtifactReferenceJson packageArtifact,
        String schemaVersion,
        String producerService,
        ArtifactByteAccessJson byteAccess,
        String completeness
    ) {
        static SourcePackageDescriptorJson from(SourcePackageDescriptor descriptor) {
            return new SourcePackageDescriptorJson(
                descriptor.availability().name(),
                ArtifactReferenceJson.from(descriptor.manifestArtifact()),
                ArtifactReferenceJson.from(descriptor.packageArtifact()),
                descriptor.schemaVersion(),
                descriptor.producerService(),
                ArtifactByteAccessJson.from(descriptor.byteAccess()),
                descriptor.completeness().name()
            );
        }

        SourcePackageDescriptor toDomain() {
            return new SourcePackageDescriptor(
                PackageAvailability.valueOf(availability),
                manifestArtifact.toDomain(),
                packageArtifact == null ? null : packageArtifact.toDomain(),
                schemaVersion,
                producerService,
                byteAccess.toDomain(),
                SourceSnapshotCompleteness.valueOf(completeness)
            );
        }
    }

    private record BuildOutputPackageDescriptorJson(
        String availability,
        ArtifactReferenceJson manifestArtifact,
        ArtifactReferenceJson packageArtifact,
        String schemaVersion,
        String producerService,
        ArtifactByteAccessJson byteAccess,
        String completeness,
        BuildOutputResolutionJson resolution,
        String buildSystem
    ) {
        static BuildOutputPackageDescriptorJson from(BuildOutputPackageDescriptor descriptor) {
            return new BuildOutputPackageDescriptorJson(
                descriptor.availability().name(),
                ArtifactReferenceJson.from(descriptor.manifestArtifact()),
                ArtifactReferenceJson.from(descriptor.packageArtifact()),
                descriptor.schemaVersion(),
                descriptor.producerService(),
                ArtifactByteAccessJson.from(descriptor.byteAccess()),
                descriptor.completeness().name(),
                BuildOutputResolutionJson.from(descriptor.resolution()),
                descriptor.buildSystem()
            );
        }

        BuildOutputPackageDescriptor toDomain() {
            return new BuildOutputPackageDescriptor(
                PackageAvailability.valueOf(availability),
                manifestArtifact == null ? null : manifestArtifact.toDomain(),
                packageArtifact == null ? null : packageArtifact.toDomain(),
                schemaVersion,
                producerService,
                byteAccess.toDomain(),
                SourceSnapshotCompleteness.valueOf(completeness),
                resolution.toDomain(),
                buildSystem
            );
        }
    }

    private record BuildOutputResolutionJson(
        BuildOutputProducerCandidateJson[] candidates,
        String selectedProducer,
        boolean terminalIntegrityFailure,
        DiagnosticJson[] diagnostics
    ) {
        static BuildOutputResolutionJson from(BuildOutputResolution resolution) {
            return new BuildOutputResolutionJson(
                resolution.candidates().stream()
                    .map(BuildOutputProducerCandidateJson::from)
                    .toArray(BuildOutputProducerCandidateJson[]::new),
                resolution.selectedProducer().name(),
                resolution.terminalIntegrityFailure(),
                resolution.diagnostics().stream()
                    .map(diagnostic -> new DiagnosticJson(diagnostic.code(), diagnostic.message(), diagnostic.severity().name()))
                    .toArray(DiagnosticJson[]::new)
            );
        }

        BuildOutputResolution toDomain() {
            return new BuildOutputResolution(
                Arrays.stream(candidates).map(BuildOutputProducerCandidateJson::toDomain).toList(),
                BuildOutputProducer.valueOf(selectedProducer),
                terminalIntegrityFailure,
                Arrays.stream(diagnostics)
                    .map(diagnostic -> new Diagnostic(
                        diagnostic.code(),
                        diagnostic.message(),
                        DiagnosticSeverity.valueOf(diagnostic.severity())
                    ))
                    .toList()
            );
        }
    }

    private record BuildOutputProducerCandidateJson(
        String producer,
        String status,
        String reference,
        DiagnosticJson[] diagnostics
    ) {
        static BuildOutputProducerCandidateJson from(BuildOutputProducerCandidate candidate) {
            return new BuildOutputProducerCandidateJson(
                candidate.producer().name(),
                candidate.status().name(),
                candidate.reference(),
                candidate.diagnostics().stream()
                    .map(diagnostic -> new DiagnosticJson(diagnostic.code(), diagnostic.message(), diagnostic.severity().name()))
                    .toArray(DiagnosticJson[]::new)
            );
        }

        BuildOutputProducerCandidate toDomain() {
            return new BuildOutputProducerCandidate(
                BuildOutputProducer.valueOf(producer),
                BuildOutputProducerStatus.valueOf(status),
                reference,
                Arrays.stream(diagnostics)
                    .map(diagnostic -> new Diagnostic(
                        diagnostic.code(),
                        diagnostic.message(),
                        DiagnosticSeverity.valueOf(diagnostic.severity())
                    ))
                    .toList()
            );
        }
    }

    static final class H2RepositorySourceSchemaInitializer {
        private H2RepositorySourceSchemaInitializer() {
        }

        static void initialize(Statement statement) throws SQLException {
            statement.execute("""
                CREATE TABLE IF NOT EXISTS workspace (
                    workspace_id VARCHAR(128) PRIMARY KEY,
                    workspace_title VARCHAR(255) NOT NULL,
                    repository_key VARCHAR(512) NOT NULL UNIQUE,
                    repository_url VARCHAR(2048) NOT NULL,
                    repository_host VARCHAR(255) NOT NULL,
                    repository_owner VARCHAR(255),
                    repository_name VARCHAR(255) NOT NULL,
                    default_branch VARCHAR(512),
                    status VARCHAR(64) NOT NULL,
                    diagnostics_json CLOB NOT NULL,
                    safe_attributes_json CLOB NOT NULL,
                    created_at VARCHAR(64) NOT NULL,
                    updated_at VARCHAR(64) NOT NULL
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS workspace_branch (
                    workspace_branch_id VARCHAR(128) PRIMARY KEY,
                    workspace_id VARCHAR(128) NOT NULL,
                    repository_branch VARCHAR(512) NOT NULL,
                    requested_commit VARCHAR(128),
                    resolved_commit VARCHAR(128),
                    source_snapshot_id VARCHAR(128),
                    status VARCHAR(64) NOT NULL,
                    source_roots_json CLOB NOT NULL,
                    last_checked_at VARCHAR(64),
                    last_updated_at VARCHAR(64),
                    diagnostics_json CLOB NOT NULL,
                    UNIQUE (workspace_id, repository_branch),
                    FOREIGN KEY (workspace_id) REFERENCES workspace(workspace_id)
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS repository_preparation (
                    analysis_run_id VARCHAR(128) NOT NULL,
                    source_snapshot_id VARCHAR(128) NOT NULL,
                    workspace_id VARCHAR(128) NOT NULL,
                    workspace_branch_id VARCHAR(128),
                    repository_url VARCHAR(2048) NOT NULL,
                    repository_provider VARCHAR(128),
                    repository_attributes_json CLOB NOT NULL,
                    requested_branch VARCHAR(512),
                    branch_required BOOLEAN NOT NULL,
                    requested_commit VARCHAR(128),
                    commit_required BOOLEAN NOT NULL,
                    resolved_commit VARCHAR(128) NOT NULL,
                    checkout_status VARCHAR(64) NOT NULL,
                    checkout_diagnostics_json CLOB NOT NULL,
                    shallow_clone BOOLEAN NOT NULL,
                    checkout_duration_millis BIGINT NOT NULL,
                    partial_clone BOOLEAN NOT NULL,
                    sparse_checkout BOOLEAN NOT NULL,
                    workspace_status VARCHAR(64) NOT NULL,
                    source_snapshot_completeness VARCHAR(64) NOT NULL,
                    source_snapshot_json CLOB NOT NULL,
                    source_roots_json CLOB NOT NULL,
                    diagnostics_json CLOB NOT NULL,
                    safe_attributes_json CLOB NOT NULL,
                    created_at VARCHAR(64) NOT NULL,
                    updated_at VARCHAR(64) NOT NULL,
                    PRIMARY KEY (analysis_run_id, source_snapshot_id)
                )
                """);
            statement.execute("ALTER TABLE repository_preparation ADD COLUMN IF NOT EXISTS branch_required BOOLEAN");
            statement.execute("ALTER TABLE repository_preparation ADD COLUMN IF NOT EXISTS commit_required BOOLEAN");
            statement.execute("ALTER TABLE repository_preparation ADD COLUMN IF NOT EXISTS checkout_diagnostics_json CLOB");
            statement.execute("ALTER TABLE repository_preparation ADD COLUMN IF NOT EXISTS source_snapshot_json CLOB");
            statement.execute("""
                CREATE TABLE IF NOT EXISTS idempotency_record (
                    idempotency_key VARCHAR(256) NOT NULL,
                    operation VARCHAR(128) NOT NULL,
                    fingerprint VARCHAR(2048) NOT NULL,
                    result_type VARCHAR(128) NOT NULL,
                    result_reference VARCHAR(512) NOT NULL,
                    result_payload CLOB NOT NULL,
                    status VARCHAR(64) NOT NULL,
                    created_at VARCHAR(64) NOT NULL,
                    expires_at VARCHAR(64),
                    PRIMARY KEY (idempotency_key, operation)
                )
                """);
            statement.execute("ALTER TABLE idempotency_record ADD COLUMN IF NOT EXISTS result_payload CLOB");
        }
    }
}
