package de.burger.forensics.analytics.services.joerncpganalysis.adapter.in.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind;
import de.burger.forensics.analytics.joerncpganalysis.v1.AnalyzeJoernCpgRequest;
import de.burger.forensics.analytics.joerncpganalysis.v1.AnalyzeJoernCpgResponse;
import de.burger.forensics.analytics.joerncpganalysis.v1.JoernCpgAnalysisServiceGrpc;
import de.burger.forensics.analytics.joerncpganalysis.v1.JoernCpgPolicy;
import de.burger.forensics.analytics.joerncpganalysis.v1.SourceWorkspace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JoernCpgAnalysisContractTest {
    @Test
    void exposesProvisionalJoernCpgAnalysisContractShape() {
        var service = JoernCpgAnalysisServiceGrpc.getServiceDescriptor();

        assertEquals("de.burger.forensics.analytics.joerncpganalysis.v1.JoernCpgAnalysisService", service.getName());
        assertTrue(service.getMethods().stream().anyMatch(method -> "AnalyzeSourceSnapshot".equals(method.getBareMethodName())));
        assertNotNull(AnalyzeJoernCpgRequest.getDescriptor().findFieldByName("source_snapshot_id"));
        assertNotNull(AnalyzeJoernCpgRequest.getDescriptor().findFieldByName("safe_attributes"));
        assertNotNull(AnalyzeJoernCpgResponse.getDescriptor().findFieldByName("semantic_artifacts"));
        assertNotNull(SourceWorkspace.getDescriptor().findFieldByName("workspace_id"));
        assertNotNull(JoernCpgPolicy.getDescriptor().findFieldByName("joern_image_reference"));
        assertEquals(3, AnalysisWorkerKind.ANALYSIS_WORKER_KIND_JOERN_ANALYSIS.getNumber());
    }
}
