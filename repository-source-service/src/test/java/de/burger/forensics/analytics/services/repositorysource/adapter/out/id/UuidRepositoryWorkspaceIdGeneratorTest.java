package de.burger.forensics.analytics.services.repositorysource.adapter.out.id;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UuidRepositoryWorkspaceIdGeneratorTest {
    @Test
    void generatesOpaqueWorkspaceAndBranchIds() {
        var generator = new UuidRepositoryWorkspaceIdGenerator();

        var workspaceId = generator.newWorkspaceId();
        var branchId = generator.newWorkspaceBranchId();

        assertTrue(workspaceId.value().startsWith("workspace-"));
        assertTrue(branchId.value().startsWith("workspace-branch-"));
        assertTrue(workspaceId.value().length() > "workspace-".length());
        assertTrue(branchId.value().length() > "workspace-branch-".length());
    }
}
