package de.burger.forensics.analytics.services.repositorysource.adapter.in.grpc;

import de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputPackageDescriptor;
import de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputProducerCandidate;
import de.burger.forensics.analytics.repositoryanalysis.v1.BuildOutputResolution;
import de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryAnalysisServiceGrpc;
import de.burger.forensics.analytics.repositoryanalysis.v1.SourcePackageDescriptor;
import de.burger.forensics.analytics.repositoryanalysis.v1.SourceSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositorySourceContractTest {
    @Test
    void exposesSourceAndBuildPackageDescriptorFields() {
        var service = RepositoryAnalysisServiceGrpc.getServiceDescriptor();

        assertEquals("de.burger.forensics.analytics.repositoryanalysis.v1.RepositoryAnalysisService", service.getName());
        assertTrue(service.getMethods().stream().anyMatch(method -> "PrepareRepository".equals(method.getBareMethodName())));
        assertEquals(6, SourceSnapshot.getDescriptor().findFieldByName("source_package").getNumber());
        assertEquals(7, SourceSnapshot.getDescriptor().findFieldByName("build_output_package").getNumber());
        assertNotNull(SourcePackageDescriptor.getDescriptor().findFieldByName("byte_access"));
        assertNotNull(BuildOutputPackageDescriptor.getDescriptor().findFieldByName("byte_access"));
        assertNotNull(BuildOutputPackageDescriptor.getDescriptor().findFieldByName("build_system"));
        assertNotNull(BuildOutputResolution.getDescriptor().findFieldByName("terminal_integrity_failure"));
        assertNotNull(BuildOutputProducerCandidate.getDescriptor().findFieldByName("producer"));
    }
}
