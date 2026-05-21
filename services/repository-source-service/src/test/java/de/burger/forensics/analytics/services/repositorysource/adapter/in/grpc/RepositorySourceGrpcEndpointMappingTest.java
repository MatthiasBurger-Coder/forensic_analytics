package de.burger.forensics.analytics.services.repositorysource.adapter.in.grpc;

import de.burger.forensics.analytics.repositoryanalysis.v1.CheckoutStatus;
import de.burger.forensics.analytics.repositoryanalysis.v1.DiagnosticSeverity;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryWorkspaceStatus;
import de.burger.forensics.analytics.repositoryanalysis.v1.SourceSnapshotCompleteness;
import de.burger.forensics.analytics.services.repositorysource.application.IdempotencyConflictException;
import de.burger.forensics.analytics.services.repositorysource.application.RepositoryPreparationNotFoundException;
import io.grpc.Status;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RepositorySourceGrpcEndpointMappingTest {
    @Test
    void mapsAllDomainEnumsToGrpcEnums() {
        assertEquals(CheckoutStatus.CHECKOUT_STATUS_ACCEPTED, RepositorySourceGrpcEndpoint.checkoutStatus(
            de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.CheckoutStatus.ACCEPTED
        ));
        assertEquals(CheckoutStatus.CHECKOUT_STATUS_WORKSPACE_PREPARED, RepositorySourceGrpcEndpoint.checkoutStatus(
            de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.CheckoutStatus.WORKSPACE_PREPARED
        ));
        assertEquals(CheckoutStatus.CHECKOUT_STATUS_CHECKED_OUT, RepositorySourceGrpcEndpoint.checkoutStatus(
            de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.CheckoutStatus.CHECKED_OUT
        ));
        assertEquals(CheckoutStatus.CHECKOUT_STATUS_FAILED, RepositorySourceGrpcEndpoint.checkoutStatus(
            de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.CheckoutStatus.FAILED
        ));

        assertEquals(RepositoryWorkspaceStatus.REPOSITORY_WORKSPACE_STATUS_READY, RepositorySourceGrpcEndpoint.workspaceStatus(
            de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceStatus.READY
        ));
        assertEquals(RepositoryWorkspaceStatus.REPOSITORY_WORKSPACE_STATUS_CHECKED_OUT, RepositorySourceGrpcEndpoint.workspaceStatus(
            de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceStatus.CHECKED_OUT
        ));
        assertEquals(RepositoryWorkspaceStatus.REPOSITORY_WORKSPACE_STATUS_CLEANED, RepositorySourceGrpcEndpoint.workspaceStatus(
            de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceStatus.CLEANED
        ));
        assertEquals(RepositoryWorkspaceStatus.REPOSITORY_WORKSPACE_STATUS_FAILED, RepositorySourceGrpcEndpoint.workspaceStatus(
            de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.RepositoryWorkspaceStatus.FAILED
        ));

        assertEquals(SourceSnapshotCompleteness.SOURCE_SNAPSHOT_COMPLETENESS_COMPLETE, RepositorySourceGrpcEndpoint.completeness(
            de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourceSnapshotCompleteness.COMPLETE
        ));
        assertEquals(SourceSnapshotCompleteness.SOURCE_SNAPSHOT_COMPLETENESS_INCOMPLETE, RepositorySourceGrpcEndpoint.completeness(
            de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourceSnapshotCompleteness.INCOMPLETE
        ));
        assertEquals(SourceSnapshotCompleteness.SOURCE_SNAPSHOT_COMPLETENESS_UNKNOWN, RepositorySourceGrpcEndpoint.completeness(
            de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.SourceSnapshotCompleteness.UNKNOWN
        ));

        assertEquals(DiagnosticSeverity.DIAGNOSTIC_SEVERITY_INFO, RepositorySourceGrpcEndpoint.severity(
            de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.DiagnosticSeverity.INFO
        ));
        assertEquals(DiagnosticSeverity.DIAGNOSTIC_SEVERITY_WARNING, RepositorySourceGrpcEndpoint.severity(
            de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.DiagnosticSeverity.WARNING
        ));
        assertEquals(DiagnosticSeverity.DIAGNOSTIC_SEVERITY_ERROR, RepositorySourceGrpcEndpoint.severity(
            de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.DiagnosticSeverity.ERROR
        ));
    }

    @Test
    void mapsApplicationExceptionsToGrpcStatuses() {
        assertEquals(Status.Code.NOT_FOUND, RepositorySourceGrpcEndpoint
            .status(new RepositoryPreparationNotFoundException("missing")).getCode());
        assertEquals(Status.Code.ALREADY_EXISTS, RepositorySourceGrpcEndpoint
            .status(new IdempotencyConflictException("conflict")).getCode());
        assertEquals(Status.Code.INVALID_ARGUMENT, RepositorySourceGrpcEndpoint
            .status(new IllegalArgumentException("private /tmp/input")).getCode());
        assertEquals("Invalid repository source request", RepositorySourceGrpcEndpoint
            .status(new IllegalArgumentException("private /tmp/input")).getDescription());
        assertEquals(Status.Code.FAILED_PRECONDITION, RepositorySourceGrpcEndpoint
            .status(new IllegalStateException("private /tmp/workspace")).getCode());
        assertEquals(Status.Code.INTERNAL, RepositorySourceGrpcEndpoint
            .status(new RuntimeException("private /tmp/internal")).getCode());
        assertEquals("Repository source service failed", RepositorySourceGrpcEndpoint
            .status(new RuntimeException("private /tmp/internal")).getDescription());
    }
}
