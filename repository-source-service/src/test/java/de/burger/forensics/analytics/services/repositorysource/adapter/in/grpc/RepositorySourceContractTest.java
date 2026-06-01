package de.burger.forensics.analytics.services.repositorysource.adapter.in.grpc;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import de.burger.forensics.analytics.repositoryanalysis.v1.AnalyzeSourceSnapshotWithJavaAstRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputPackageDescriptor;
import de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducerCandidate;
import de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputResolution;
import de.burger.forensics.analytics.repositoryanalysis.v1.CleanupRepositoryWorkspaceByIdRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.CleanupRepositoryWorkspaceByIdResponse;
import de.burger.forensics.analytics.repositoryanalysis.v1.CleanupRepositoryWorkspaceRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.CleanupRepositoryWorkspaceResponse;
import de.burger.forensics.analytics.repositoryanalysis.v1.CreateRepositoryWorkspaceResponse;
import de.burger.forensics.analytics.repositoryanalysis.v1.CreateRepositoryWorkspaceRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.DatabaseSettingsValidationStatus;
import de.burger.forensics.analytics.repositoryanalysis.v1.GetRepositoryPreparationRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.GetRepositorySourceDatabaseSettingsRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.GetRepositoryWorkspaceRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.JavaAstHandoffResponse;
import de.burger.forensics.analytics.repositoryanalysis.v1.ListRepositoryWorkspacesRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.ListRepositoryWorkspacesResponse;
import de.burger.forensics.analytics.repositoryanalysis.v1.MetadataPreviewPolicy;
import de.burger.forensics.analytics.repositoryanalysis.v1.PrepareRepositoryRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.PrepareRepositoryResponse;
import de.burger.forensics.analytics.repositoryanalysis.v1.PreviewRepositoryWorkspaceMetadataResponse;
import de.burger.forensics.analytics.repositoryanalysis.v1.PreviewRepositoryWorkspaceMetadataRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryAnalysisServiceGrpc;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryIdentity;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryPreparation;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositorySourceDatabaseSettingsCandidate;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositorySourceDatabaseSettingsPublicView;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositorySourceDatabaseSettingsStatus;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositorySourceDatabaseSettingsValidationResponse;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryWorkspace;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryWorkspaceBranch;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryWorkspaceBranchSelector;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryWorkspaceBranchStatus;
import de.burger.forensics.analytics.repositoryanalysis.v1.RefreshRepositoryWorkspaceBranchRequest;
import de.burger.forensics.analytics.repositoryanalysis.v1.RefreshRepositoryWorkspaceBranchResponse;
import de.burger.forensics.analytics.repositoryanalysis.v1.SourcePackageDescriptor;
import de.burger.forensics.analytics.repositoryanalysis.v1.SourceRoot;
import de.burger.forensics.analytics.repositoryanalysis.v1.SourceSnapshot;
import de.burger.forensics.analytics.repositoryanalysis.v1.SourceSnapshotHandoffPolicy;
import de.burger.forensics.analytics.repositoryanalysis.v1.ValidateRepositorySourceDatabaseSettingsRequest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositorySourceContractTest {
    private static final Set<String> FORBIDDEN_PRIVATE_TRANSPORT_FIELD_NAMES = Set.of(
        "path",
        "workspace_path",
        "local_path",
        "filesystem_path",
        "h2_path",
        "directory",
        "stdout",
        "stderr",
        "raw_stdout",
        "raw_stderr"
    );

    @Test
    void exposesSourceAndBuildPackageDescriptorFields() {
        var service = RepositoryAnalysisServiceGrpc.getServiceDescriptor();

        assertEquals("de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryAnalysisService", service.getName());
        assertServiceMethod(service, "PrepareRepository");
        assertServiceMethod(service, "GetRepositoryPreparation");
        assertServiceMethod(service, "AnalyzeSourceSnapshotWithJavaAst");
        assertServiceMethod(service, "CleanupRepositoryWorkspace");

        assertFieldNumber(PrepareRepositoryRequest.getDescriptor(), "request_id", 1);
        assertFieldNumber(PrepareRepositoryRequest.getDescriptor(), "idempotency_key", 2);
        assertFieldNumber(PrepareRepositoryRequest.getDescriptor(), "schema_version", 3);
        assertFieldNumber(PrepareRepositoryRequest.getDescriptor(), "correlation_id", 4);
        assertFieldNumber(PrepareRepositoryRequest.getDescriptor(), "analysis_run_id", 5);
        assertFieldNumber(PrepareRepositoryRequest.getDescriptor(), "repository", 6);
        assertFieldNumber(PrepareRepositoryRequest.getDescriptor(), "revision", 7);
        assertFieldNumber(PrepareRepositoryRequest.getDescriptor(), "workspace_policy", 8);
        assertFieldNumber(PrepareRepositoryRequest.getDescriptor(), "safe_attributes", 9);
        assertFieldNumber(PrepareRepositoryResponse.getDescriptor(), "preparation", 1);
        assertFieldNumber(PrepareRepositoryResponse.getDescriptor(), "status", 2);

        assertFieldNumber(GetRepositoryPreparationRequest.getDescriptor(), "request_id", 1);
        assertFieldNumber(GetRepositoryPreparationRequest.getDescriptor(), "correlation_id", 2);
        assertFieldNumber(GetRepositoryPreparationRequest.getDescriptor(), "analysis_run_id", 3);
        assertFieldNumber(GetRepositoryPreparationRequest.getDescriptor(), "source_snapshot_id", 4);

        assertFieldNumber(AnalyzeSourceSnapshotWithJavaAstRequest.getDescriptor(), "request_id", 1);
        assertFieldNumber(AnalyzeSourceSnapshotWithJavaAstRequest.getDescriptor(), "idempotency_key", 2);
        assertFieldNumber(AnalyzeSourceSnapshotWithJavaAstRequest.getDescriptor(), "schema_version", 3);
        assertFieldNumber(AnalyzeSourceSnapshotWithJavaAstRequest.getDescriptor(), "correlation_id", 4);
        assertFieldNumber(AnalyzeSourceSnapshotWithJavaAstRequest.getDescriptor(), "analysis_run_id", 5);
        assertFieldNumber(AnalyzeSourceSnapshotWithJavaAstRequest.getDescriptor(), "analysis_job_id", 6);
        assertFieldNumber(AnalyzeSourceSnapshotWithJavaAstRequest.getDescriptor(), "source_snapshot_id", 7);
        assertFieldNumber(AnalyzeSourceSnapshotWithJavaAstRequest.getDescriptor(), "handoff_policy", 8);
        assertFieldNumber(AnalyzeSourceSnapshotWithJavaAstRequest.getDescriptor(), "safe_attributes", 9);
        assertFieldNumber(SourceSnapshotHandoffPolicy.getDescriptor(), "max_files", 1);
        assertFieldNumber(SourceSnapshotHandoffPolicy.getDescriptor(), "max_source_bytes", 2);
        assertFieldNumber(SourceSnapshotHandoffPolicy.getDescriptor(), "timeout_seconds", 3);
        assertFieldNumber(JavaAstHandoffResponse.getDescriptor(), "status", 1);
        assertFieldNumber(JavaAstHandoffResponse.getDescriptor(), "analysis_run_id", 2);
        assertFieldNumber(JavaAstHandoffResponse.getDescriptor(), "analysis_job_id", 3);
        assertFieldNumber(JavaAstHandoffResponse.getDescriptor(), "source_snapshot_id", 4);
        assertFieldNumber(JavaAstHandoffResponse.getDescriptor(), "completeness", 5);
        assertFieldNumber(JavaAstHandoffResponse.getDescriptor(), "source_fact_artifact", 6);
        assertFieldNumber(JavaAstHandoffResponse.getDescriptor(), "summary", 7);
        assertFieldNumber(JavaAstHandoffResponse.getDescriptor(), "diagnostics", 8);
        assertFieldNumber(JavaAstHandoffResponse.getDescriptor(), "safe_attributes", 9);

        assertFieldNumber(CleanupRepositoryWorkspaceRequest.getDescriptor(), "request_id", 1);
        assertFieldNumber(CleanupRepositoryWorkspaceRequest.getDescriptor(), "idempotency_key", 2);
        assertFieldNumber(CleanupRepositoryWorkspaceRequest.getDescriptor(), "correlation_id", 3);
        assertFieldNumber(CleanupRepositoryWorkspaceRequest.getDescriptor(), "analysis_run_id", 4);
        assertFieldNumber(CleanupRepositoryWorkspaceRequest.getDescriptor(), "workspace_id", 5);
        assertFieldNumber(CleanupRepositoryWorkspaceResponse.getDescriptor(), "workspace_id", 1);
        assertFieldNumber(CleanupRepositoryWorkspaceResponse.getDescriptor(), "workspace_status", 2);
        assertFieldNumber(CleanupRepositoryWorkspaceResponse.getDescriptor(), "status", 3);
        assertFieldNumber(CleanupRepositoryWorkspaceResponse.getDescriptor(), "diagnostics", 4);

        assertFieldNumber(RepositoryPreparation.getDescriptor(), "analysis_run_id", 1);
        assertFieldNumber(RepositoryPreparation.getDescriptor(), "source_snapshot_id", 2);
        assertFieldNumber(RepositoryPreparation.getDescriptor(), "workspace_id", 3);
        assertFieldNumber(RepositoryPreparation.getDescriptor(), "repository", 4);
        assertFieldNumber(RepositoryPreparation.getDescriptor(), "requested_revision", 5);
        assertFieldNumber(RepositoryPreparation.getDescriptor(), "checkout", 6);
        assertFieldNumber(RepositoryPreparation.getDescriptor(), "source_snapshot", 7);
        assertFieldNumber(RepositoryPreparation.getDescriptor(), "workspace_status", 8);
        assertFieldNumber(RepositoryPreparation.getDescriptor(), "created_at", 9);
        assertFieldNumber(RepositoryPreparation.getDescriptor(), "updated_at", 10);
        assertFieldNumber(RepositoryPreparation.getDescriptor(), "diagnostics", 11);
        assertFieldNumber(RepositoryPreparation.getDescriptor(), "safe_attributes", 12);
        assertFieldNumber(SourceSnapshot.getDescriptor(), "source_package", 6);
        assertFieldNumber(SourceSnapshot.getDescriptor(), "build_output_package", 7);
        assertFieldNumber(SourceRoot.getDescriptor(), "relative_path", 1);
        assertNotNull(SourcePackageDescriptor.getDescriptor().findFieldByName("byte_access"));
        assertNotNull(BuildOutputPackageDescriptor.getDescriptor().findFieldByName("byte_access"));
        assertNotNull(BuildOutputPackageDescriptor.getDescriptor().findFieldByName("build_system"));
        assertNotNull(BuildOutputResolution.getDescriptor().findFieldByName("terminal_integrity_failure"));
        assertNotNull(BuildOutputProducerCandidate.getDescriptor().findFieldByName("producer"));
    }

    @Test
    void exposesRepositoryCheckoutWorkspaceOwnerApiContract() {
        var service = RepositoryAnalysisServiceGrpc.getServiceDescriptor();

        assertServiceMethod(service, "PreviewRepositoryWorkspaceMetadata");
        assertServiceMethod(service, "CreateRepositoryWorkspace");
        assertServiceMethod(service, "GetRepositoryWorkspace");
        assertServiceMethod(service, "ListRepositoryWorkspaces");
        assertServiceMethod(service, "CleanupRepositoryWorkspaceById");
        assertServiceMethod(service, "RefreshRepositoryWorkspaceBranch");

        assertFieldNumber(PreviewRepositoryWorkspaceMetadataRequest.getDescriptor(), "request_id", 1);
        assertFieldNumber(PreviewRepositoryWorkspaceMetadataRequest.getDescriptor(), "schema_version", 2);
        assertFieldNumber(PreviewRepositoryWorkspaceMetadataRequest.getDescriptor(), "correlation_id", 3);
        assertFieldNumber(PreviewRepositoryWorkspaceMetadataRequest.getDescriptor(), "repository", 4);
        assertFieldNumber(PreviewRepositoryWorkspaceMetadataRequest.getDescriptor(), "metadata_policy", 5);
        assertFieldNumber(PreviewRepositoryWorkspaceMetadataRequest.getDescriptor(), "safe_attributes", 6);
        assertFieldNumber(MetadataPreviewPolicy.getDescriptor(), "timeout_seconds", 1);
        assertFieldNumber(PreviewRepositoryWorkspaceMetadataResponse.getDescriptor(), "repository", 1);
        assertFieldNumber(PreviewRepositoryWorkspaceMetadataResponse.getDescriptor(), "workspace_title", 2);
        assertFieldNumber(PreviewRepositoryWorkspaceMetadataResponse.getDescriptor(), "status", 3);
        assertFieldNumber(PreviewRepositoryWorkspaceMetadataResponse.getDescriptor(), "diagnostics", 4);
        assertFieldNumber(PreviewRepositoryWorkspaceMetadataResponse.getDescriptor(), "safe_attributes", 5);

        assertFieldNumber(CreateRepositoryWorkspaceRequest.getDescriptor(), "request_id", 1);
        assertFieldNumber(CreateRepositoryWorkspaceRequest.getDescriptor(), "idempotency_key", 2);
        assertFieldNumber(CreateRepositoryWorkspaceRequest.getDescriptor(), "schema_version", 3);
        assertFieldNumber(CreateRepositoryWorkspaceRequest.getDescriptor(), "correlation_id", 4);
        assertFieldNumber(CreateRepositoryWorkspaceRequest.getDescriptor(), "repository", 5);
        assertFieldNumber(CreateRepositoryWorkspaceRequest.getDescriptor(), "branch_selector", 6);
        assertFieldNumber(CreateRepositoryWorkspaceRequest.getDescriptor(), "workspace_policy", 7);
        assertFieldNumber(CreateRepositoryWorkspaceRequest.getDescriptor(), "safe_attributes", 8);
        assertFieldNumber(CreateRepositoryWorkspaceResponse.getDescriptor(), "workspace", 1);
        assertFieldNumber(CreateRepositoryWorkspaceResponse.getDescriptor(), "status", 2);

        assertFieldNumber(GetRepositoryWorkspaceRequest.getDescriptor(), "request_id", 1);
        assertFieldNumber(GetRepositoryWorkspaceRequest.getDescriptor(), "correlation_id", 2);
        assertFieldNumber(GetRepositoryWorkspaceRequest.getDescriptor(), "workspace_id", 3);

        assertFieldNumber(ListRepositoryWorkspacesRequest.getDescriptor(), "request_id", 1);
        assertFieldNumber(ListRepositoryWorkspacesRequest.getDescriptor(), "schema_version", 2);
        assertFieldNumber(ListRepositoryWorkspacesRequest.getDescriptor(), "correlation_id", 3);
        assertFieldNumber(ListRepositoryWorkspacesRequest.getDescriptor(), "include_cleaned", 4);
        assertFieldNumber(ListRepositoryWorkspacesResponse.getDescriptor(), "workspaces", 1);
        assertFieldNumber(ListRepositoryWorkspacesResponse.getDescriptor(), "status", 2);
        assertFieldNumber(ListRepositoryWorkspacesResponse.getDescriptor(), "diagnostics", 3);

        assertFieldNumber(CleanupRepositoryWorkspaceByIdRequest.getDescriptor(), "request_id", 1);
        assertFieldNumber(CleanupRepositoryWorkspaceByIdRequest.getDescriptor(), "idempotency_key", 2);
        assertFieldNumber(CleanupRepositoryWorkspaceByIdRequest.getDescriptor(), "schema_version", 3);
        assertFieldNumber(CleanupRepositoryWorkspaceByIdRequest.getDescriptor(), "correlation_id", 4);
        assertFieldNumber(CleanupRepositoryWorkspaceByIdRequest.getDescriptor(), "workspace_id", 5);
        assertFieldNumber(CleanupRepositoryWorkspaceByIdRequest.getDescriptor(), "safe_attributes", 6);
        assertFieldNumber(CleanupRepositoryWorkspaceByIdResponse.getDescriptor(), "workspace_id", 1);
        assertFieldNumber(CleanupRepositoryWorkspaceByIdResponse.getDescriptor(), "workspace_status", 2);
        assertFieldNumber(CleanupRepositoryWorkspaceByIdResponse.getDescriptor(), "status", 3);
        assertFieldNumber(CleanupRepositoryWorkspaceByIdResponse.getDescriptor(), "diagnostics", 4);
        assertFieldNumber(CleanupRepositoryWorkspaceByIdResponse.getDescriptor(), "safe_attributes", 5);

        assertFieldNumber(RefreshRepositoryWorkspaceBranchRequest.getDescriptor(), "request_id", 1);
        assertFieldNumber(RefreshRepositoryWorkspaceBranchRequest.getDescriptor(), "idempotency_key", 2);
        assertFieldNumber(RefreshRepositoryWorkspaceBranchRequest.getDescriptor(), "schema_version", 3);
        assertFieldNumber(RefreshRepositoryWorkspaceBranchRequest.getDescriptor(), "correlation_id", 4);
        assertFieldNumber(RefreshRepositoryWorkspaceBranchRequest.getDescriptor(), "workspace_id", 5);
        assertFieldNumber(RefreshRepositoryWorkspaceBranchRequest.getDescriptor(), "workspace_branch_id", 6);
        assertFieldNumber(RefreshRepositoryWorkspaceBranchRequest.getDescriptor(), "workspace_policy", 7);
        assertFieldNumber(RefreshRepositoryWorkspaceBranchRequest.getDescriptor(), "safe_attributes", 8);
        assertFieldNumber(RefreshRepositoryWorkspaceBranchResponse.getDescriptor(), "branch", 1);
        assertFieldNumber(RefreshRepositoryWorkspaceBranchResponse.getDescriptor(), "changed", 2);
        assertFieldNumber(RefreshRepositoryWorkspaceBranchResponse.getDescriptor(), "previous_commit", 3);
        assertFieldNumber(RefreshRepositoryWorkspaceBranchResponse.getDescriptor(), "status", 4);
        assertFieldNumber(RefreshRepositoryWorkspaceBranchResponse.getDescriptor(), "diagnostics", 5);
        assertFieldNumber(RefreshRepositoryWorkspaceBranchResponse.getDescriptor(), "safe_attributes", 6);

        assertFieldNumber(RepositoryIdentity.getDescriptor(), "repository_key", 1);
        assertFieldNumber(RepositoryIdentity.getDescriptor(), "repository_url", 2);
        assertFieldNumber(RepositoryIdentity.getDescriptor(), "repository_host", 3);
        assertFieldNumber(RepositoryIdentity.getDescriptor(), "repository_owner", 4);
        assertFieldNumber(RepositoryIdentity.getDescriptor(), "repository_name", 5);
        assertFieldNumber(RepositoryIdentity.getDescriptor(), "default_branch", 6);
        assertFieldNumber(RepositoryWorkspace.getDescriptor(), "workspace_id", 1);
        assertFieldNumber(RepositoryWorkspace.getDescriptor(), "workspace_title", 2);
        assertFieldNumber(RepositoryWorkspace.getDescriptor(), "repository", 3);
        assertFieldNumber(RepositoryWorkspace.getDescriptor(), "status", 4);
        assertFieldNumber(RepositoryWorkspace.getDescriptor(), "created_at", 5);
        assertFieldNumber(RepositoryWorkspace.getDescriptor(), "updated_at", 6);
        assertFieldNumber(RepositoryWorkspace.getDescriptor(), "branches", 7);
        assertFieldNumber(RepositoryWorkspace.getDescriptor(), "diagnostics", 8);
        assertFieldNumber(RepositoryWorkspace.getDescriptor(), "safe_attributes", 9);
        assertFieldNumber(RepositoryWorkspaceBranch.getDescriptor(), "workspace_branch_id", 1);
        assertFieldNumber(RepositoryWorkspaceBranch.getDescriptor(), "workspace_id", 2);
        assertFieldNumber(RepositoryWorkspaceBranch.getDescriptor(), "repository_branch", 3);
        assertFieldNumber(RepositoryWorkspaceBranch.getDescriptor(), "requested_commit", 4);
        assertFieldNumber(RepositoryWorkspaceBranch.getDescriptor(), "resolved_commit", 5);
        assertFieldNumber(RepositoryWorkspaceBranch.getDescriptor(), "source_snapshot_id", 6);
        assertFieldNumber(RepositoryWorkspaceBranch.getDescriptor(), "status", 7);
        assertFieldNumber(RepositoryWorkspaceBranch.getDescriptor(), "source_roots", 8);
        assertFieldNumber(RepositoryWorkspaceBranch.getDescriptor(), "last_checked_at", 9);
        assertFieldNumber(RepositoryWorkspaceBranch.getDescriptor(), "last_updated_at", 10);
        assertFieldNumber(RepositoryWorkspaceBranch.getDescriptor(), "diagnostics", 11);
        assertFieldNumber(RepositoryWorkspaceBranchSelector.getDescriptor(), "branch", 1);
        assertFieldNumber(RepositoryWorkspaceBranchSelector.getDescriptor(), "commit", 2);

        assertEnumNumber(
            RepositoryWorkspaceBranchStatus.getDescriptor(),
            "REPOSITORY_WORKSPACE_BRANCH_STATUS_UNSPECIFIED",
            0
        );
        assertEnumNumber(
            RepositoryWorkspaceBranchStatus.getDescriptor(),
            "REPOSITORY_WORKSPACE_BRANCH_STATUS_CHECKING_OUT",
            1
        );
        assertEnumNumber(
            RepositoryWorkspaceBranchStatus.getDescriptor(),
            "REPOSITORY_WORKSPACE_BRANCH_STATUS_CHECKED_OUT",
            2
        );
        assertEnumNumber(
            RepositoryWorkspaceBranchStatus.getDescriptor(),
            "REPOSITORY_WORKSPACE_BRANCH_STATUS_UP_TO_DATE",
            3
        );
        assertEnumNumber(
            RepositoryWorkspaceBranchStatus.getDescriptor(),
            "REPOSITORY_WORKSPACE_BRANCH_STATUS_UPDATING",
            4
        );
        assertEnumNumber(
            RepositoryWorkspaceBranchStatus.getDescriptor(),
            "REPOSITORY_WORKSPACE_BRANCH_STATUS_UPDATED",
            5
        );
        assertEnumNumber(
            RepositoryWorkspaceBranchStatus.getDescriptor(),
            "REPOSITORY_WORKSPACE_BRANCH_STATUS_FAILED",
            6
        );

        assertNoPrivateTransportLeakageFields(PreviewRepositoryWorkspaceMetadataRequest.getDescriptor());
        assertNoPrivateTransportLeakageFields(PreviewRepositoryWorkspaceMetadataResponse.getDescriptor());
        assertNoPrivateTransportLeakageFields(CreateRepositoryWorkspaceRequest.getDescriptor());
        assertNoPrivateTransportLeakageFields(CreateRepositoryWorkspaceResponse.getDescriptor());
        assertNoPrivateTransportLeakageFields(GetRepositoryWorkspaceRequest.getDescriptor());
        assertNoPrivateTransportLeakageFields(ListRepositoryWorkspacesRequest.getDescriptor());
        assertNoPrivateTransportLeakageFields(ListRepositoryWorkspacesResponse.getDescriptor());
        assertNoPrivateTransportLeakageFields(CleanupRepositoryWorkspaceByIdRequest.getDescriptor());
        assertNoPrivateTransportLeakageFields(CleanupRepositoryWorkspaceByIdResponse.getDescriptor());
        assertNoPrivateTransportLeakageFields(RefreshRepositoryWorkspaceBranchRequest.getDescriptor());
        assertNoPrivateTransportLeakageFields(RefreshRepositoryWorkspaceBranchResponse.getDescriptor());
        assertNoPrivateTransportLeakageFields(RepositoryIdentity.getDescriptor());
        assertNoPrivateTransportLeakageFields(RepositoryWorkspace.getDescriptor());
        assertNoPrivateTransportLeakageFields(RepositoryWorkspaceBranch.getDescriptor());
        assertNoPrivateTransportLeakageFields(RepositoryWorkspaceBranchSelector.getDescriptor());
    }

    @Test
    void repositoryWorkspaceOwnerApiDocumentsCleanRepositoryUrlAndIdempotencyConflictRules() throws IOException {
        var contract = Files.readString(findRepositoryRoot().resolve("contracts/grpc/repository-analysis.proto"));

        assertTrue(contract.contains("query strings or fragments"));
        assertFalse(contract.contains("query secrets"));
        assertTrue(contract.contains("clean HTTPS"));
        assertTrue(contract.contains("same key plus different fingerprint is a conflict"));
        assertTrue(contract.contains("the same key plus a different fingerprint is a conflict"));
        assertTrue(contract.contains("private, loopback, link-local and special-use networks"));
        assertTrue(contract.contains("public lists hide CLEANED workspaces"));
        assertTrue(contract.contains("deterministic order"));
        assertTrue(contract.contains("Cleanup retains metadata"));
    }

    @Test
    void exposesRepositorySourceDatabaseSettingsOwnerApiContract() {
        var service = RepositoryAnalysisServiceGrpc.getServiceDescriptor();

        assertServiceMethod(service, "GetRepositorySourceDatabaseSettings");
        assertServiceMethod(service, "ValidateRepositorySourceDatabaseSettings");

        assertFieldNumber(GetRepositorySourceDatabaseSettingsRequest.getDescriptor(), "request_id", 1);
        assertFieldNumber(GetRepositorySourceDatabaseSettingsRequest.getDescriptor(), "correlation_id", 2);
        assertFieldNumber(ValidateRepositorySourceDatabaseSettingsRequest.getDescriptor(), "request_id", 1);
        assertFieldNumber(ValidateRepositorySourceDatabaseSettingsRequest.getDescriptor(), "correlation_id", 2);
        assertFieldNumber(ValidateRepositorySourceDatabaseSettingsRequest.getDescriptor(), "settings", 3);
        assertFieldNumber(RepositorySourceDatabaseSettingsCandidate.getDescriptor(), "host", 1);
        assertFieldNumber(RepositorySourceDatabaseSettingsCandidate.getDescriptor(), "port", 2);
        assertFieldNumber(RepositorySourceDatabaseSettingsCandidate.getDescriptor(), "database_name", 3);
        assertFieldNumber(RepositorySourceDatabaseSettingsCandidate.getDescriptor(), "username", 4);
        assertFieldNumber(RepositorySourceDatabaseSettingsCandidate.getDescriptor(), "password", 5);
        assertFieldNumber(RepositorySourceDatabaseSettingsCandidate.getDescriptor(), "schema", 6);
        assertFieldNumber(RepositorySourceDatabaseSettingsCandidate.getDescriptor(), "ssl_mode", 7);
        assertFieldNumber(RepositorySourceDatabaseSettingsPublicView.getDescriptor(), "engine", 1);
        assertFieldNumber(RepositorySourceDatabaseSettingsPublicView.getDescriptor(), "host", 2);
        assertFieldNumber(RepositorySourceDatabaseSettingsPublicView.getDescriptor(), "port", 3);
        assertFieldNumber(RepositorySourceDatabaseSettingsPublicView.getDescriptor(), "database_name", 4);
        assertFieldNumber(RepositorySourceDatabaseSettingsPublicView.getDescriptor(), "username", 5);
        assertFieldNumber(RepositorySourceDatabaseSettingsPublicView.getDescriptor(), "authentication_configured", 6);
        assertFieldNumber(RepositorySourceDatabaseSettingsPublicView.getDescriptor(), "schema", 7);
        assertFieldNumber(RepositorySourceDatabaseSettingsPublicView.getDescriptor(), "ssl_mode", 8);
        assertFieldNumber(RepositorySourceDatabaseSettingsPublicView.getDescriptor(), "configuration_source", 9);
        assertFieldNumber(RepositorySourceDatabaseSettingsPublicView.getDescriptor(), "apply_mode", 10);
        assertFieldNumber(RepositorySourceDatabaseSettingsPublicView.getDescriptor(), "hot_apply_supported", 11);
        assertFieldNumber(RepositorySourceDatabaseSettingsStatus.getDescriptor(), "settings", 1);
        assertFieldNumber(RepositorySourceDatabaseSettingsStatus.getDescriptor(), "status", 2);
        assertFieldNumber(RepositorySourceDatabaseSettingsStatus.getDescriptor(), "diagnostics", 3);
        assertFieldNumber(RepositorySourceDatabaseSettingsValidationResponse.getDescriptor(), "settings", 1);
        assertFieldNumber(RepositorySourceDatabaseSettingsValidationResponse.getDescriptor(), "validation_status", 2);
        assertFieldNumber(RepositorySourceDatabaseSettingsValidationResponse.getDescriptor(), "status", 3);
        assertFieldNumber(RepositorySourceDatabaseSettingsValidationResponse.getDescriptor(), "diagnostics", 4);

        assertEnumNumber(
            DatabaseSettingsValidationStatus.getDescriptor(),
            "DATABASE_SETTINGS_VALIDATION_STATUS_UNSPECIFIED",
            0
        );
        assertEnumNumber(DatabaseSettingsValidationStatus.getDescriptor(), "DATABASE_SETTINGS_VALIDATION_STATUS_VALID", 1);
        assertEnumNumber(DatabaseSettingsValidationStatus.getDescriptor(), "DATABASE_SETTINGS_VALIDATION_STATUS_INVALID", 2);
        assertEnumNumber(DatabaseSettingsValidationStatus.getDescriptor(), "DATABASE_SETTINGS_VALIDATION_STATUS_UNREACHABLE", 3);
        assertEnumNumber(
            DatabaseSettingsValidationStatus.getDescriptor(),
            "DATABASE_SETTINGS_VALIDATION_STATUS_AUTHENTICATION_FAILED",
            4
        );
        assertEnumNumber(DatabaseSettingsValidationStatus.getDescriptor(), "DATABASE_SETTINGS_VALIDATION_STATUS_UNSUPPORTED", 5);

        assertNoPrivateTransportLeakageFields(RepositorySourceDatabaseSettingsCandidate.getDescriptor());
        assertNoPrivateTransportLeakageFields(RepositorySourceDatabaseSettingsPublicView.getDescriptor());
        assertNoPrivateTransportLeakageFields(RepositorySourceDatabaseSettingsStatus.getDescriptor());
        assertNoPrivateTransportLeakageFields(RepositorySourceDatabaseSettingsValidationResponse.getDescriptor());
    }

    private static void assertServiceMethod(
        io.grpc.ServiceDescriptor service,
        String methodName
    ) {
        assertTrue(service.getMethods().stream().anyMatch(method -> methodName.equals(method.getBareMethodName())));
    }

    private static void assertFieldNumber(Descriptor descriptor, String fieldName, int expectedNumber) {
        var field = descriptor.findFieldByName(fieldName);
        assertNotNull(field, () -> "Missing field " + descriptor.getFullName() + "." + fieldName);
        assertEquals(expectedNumber, field.getNumber(), () -> "Unexpected number for " + descriptor.getFullName()
            + "." + fieldName);
    }

    private static void assertEnumNumber(EnumDescriptor descriptor, String valueName, int expectedNumber) {
        var value = descriptor.findValueByName(valueName);
        assertNotNull(value, () -> "Missing enum value " + descriptor.getFullName() + "." + valueName);
        assertEquals(expectedNumber, value.getNumber(), () -> "Unexpected number for " + descriptor.getFullName()
            + "." + valueName);
    }

    private static void assertNoPrivateTransportLeakageFields(Descriptor descriptor) {
        descriptor.getFields().forEach(field -> {
            var fieldName = field.getName();
            assertFalse(
                FORBIDDEN_PRIVATE_TRANSPORT_FIELD_NAMES.contains(fieldName) || fieldName.endsWith("_path")
                    || fieldName.endsWith("_directory") || fieldName.endsWith("_stdout")
                    || fieldName.endsWith("_stderr"),
                () -> "Private transport field leaked through " + descriptor.getFullName() + "." + fieldName
            );
        });
    }

    private static Path findRepositoryRoot() {
        var current = Path.of("").toAbsolutePath();
        while (current != null) {
            var candidate = current.resolve("settings.gradle.kts");
            if (Files.isRegularFile(candidate)) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("repository root not found from test working directory");
    }
}
