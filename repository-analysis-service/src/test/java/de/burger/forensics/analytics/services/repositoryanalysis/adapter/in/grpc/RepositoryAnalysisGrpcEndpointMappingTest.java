package de.burger.forensics.analytics.services.repositoryanalysis.adapter.in.grpc;

import de.burger.forensics.analytics.repositoryanalysis.v1.CheckoutStatus;
import de.burger.forensics.analytics.repositoryanalysis.v1.DiagnosticSeverity;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryWorkspaceStatus;
import de.burger.forensics.analytics.repositoryanalysis.v1.SourceSnapshotCompleteness;
import de.burger.forensics.analytics.services.repositoryanalysis.application.IdempotencyConflictException;
import de.burger.forensics.analytics.services.repositoryanalysis.application.RepositoryPreparationNotFoundException;
import io.grpc.Status;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RepositoryAnalysisGrpcEndpointMappingTest {
    @Test
    void mapsAllDomainEnumsToGrpcEnums() {
        assertEquals(CheckoutStatus.CHECKOUT_STATUS_ACCEPTED, RepositoryAnalysisGrpcEndpoint.checkoutStatus(
            de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.CheckoutStatus.ACCEPTED
        ));
        assertEquals(CheckoutStatus.CHECKOUT_STATUS_WORKSPACE_PREPARED, RepositoryAnalysisGrpcEndpoint.checkoutStatus(
            de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.CheckoutStatus.WORKSPACE_PREPARED
        ));
        assertEquals(CheckoutStatus.CHECKOUT_STATUS_CHECKED_OUT, RepositoryAnalysisGrpcEndpoint.checkoutStatus(
            de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.CheckoutStatus.CHECKED_OUT
        ));
        assertEquals(CheckoutStatus.CHECKOUT_STATUS_FAILED, RepositoryAnalysisGrpcEndpoint.checkoutStatus(
            de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.CheckoutStatus.FAILED
        ));

        assertEquals(RepositoryWorkspaceStatus.REPOSITORY_WORKSPACE_STATUS_READY, RepositoryAnalysisGrpcEndpoint.workspaceStatus(
            de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RepositoryWorkspaceStatus.READY
        ));
        assertEquals(RepositoryWorkspaceStatus.REPOSITORY_WORKSPACE_STATUS_CHECKED_OUT, RepositoryAnalysisGrpcEndpoint.workspaceStatus(
            de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RepositoryWorkspaceStatus.CHECKED_OUT
        ));
        assertEquals(RepositoryWorkspaceStatus.REPOSITORY_WORKSPACE_STATUS_CLEANED, RepositoryAnalysisGrpcEndpoint.workspaceStatus(
            de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RepositoryWorkspaceStatus.CLEANED
        ));
        assertEquals(RepositoryWorkspaceStatus.REPOSITORY_WORKSPACE_STATUS_FAILED, RepositoryAnalysisGrpcEndpoint.workspaceStatus(
            de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RepositoryWorkspaceStatus.FAILED
        ));

        assertEquals(SourceSnapshotCompleteness.SOURCE_SNAPSHOT_COMPLETENESS_COMPLETE, RepositoryAnalysisGrpcEndpoint.completeness(
            de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceSnapshotCompleteness.COMPLETE
        ));
        assertEquals(SourceSnapshotCompleteness.SOURCE_SNAPSHOT_COMPLETENESS_INCOMPLETE, RepositoryAnalysisGrpcEndpoint.completeness(
            de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceSnapshotCompleteness.INCOMPLETE
        ));
        assertEquals(SourceSnapshotCompleteness.SOURCE_SNAPSHOT_COMPLETENESS_UNKNOWN, RepositoryAnalysisGrpcEndpoint.completeness(
            de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceSnapshotCompleteness.UNKNOWN
        ));

        assertEquals(DiagnosticSeverity.DIAGNOSTIC_SEVERITY_INFO, RepositoryAnalysisGrpcEndpoint.severity(
            de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.DiagnosticSeverity.INFO
        ));
        assertEquals(DiagnosticSeverity.DIAGNOSTIC_SEVERITY_WARNING, RepositoryAnalysisGrpcEndpoint.severity(
            de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.DiagnosticSeverity.WARNING
        ));
        assertEquals(DiagnosticSeverity.DIAGNOSTIC_SEVERITY_ERROR, RepositoryAnalysisGrpcEndpoint.severity(
            de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.DiagnosticSeverity.ERROR
        ));
    }

    @Test
    void mapsApplicationExceptionsToGrpcStatuses() {
        assertEquals(Status.Code.NOT_FOUND, RepositoryAnalysisGrpcEndpoint
            .status(new RepositoryPreparationNotFoundException("missing")).getCode());
        assertEquals(Status.Code.ALREADY_EXISTS, RepositoryAnalysisGrpcEndpoint
            .status(new IdempotencyConflictException("conflict")).getCode());
        assertEquals(Status.Code.INVALID_ARGUMENT, RepositoryAnalysisGrpcEndpoint
            .status(new IllegalArgumentException("private /tmp/input")).getCode());
        assertEquals("Invalid repository analysis request", RepositoryAnalysisGrpcEndpoint
            .status(new IllegalArgumentException("private /tmp/input")).getDescription());
        assertEquals(Status.Code.FAILED_PRECONDITION, RepositoryAnalysisGrpcEndpoint
            .status(new IllegalStateException("private /tmp/workspace")).getCode());
        assertEquals(Status.Code.INTERNAL, RepositoryAnalysisGrpcEndpoint
            .status(new RuntimeException("private /tmp/internal")).getCode());
        assertEquals("Repository analysis service failed", RepositoryAnalysisGrpcEndpoint
            .status(new RuntimeException("private /tmp/internal")).getDescription());
    }
}
