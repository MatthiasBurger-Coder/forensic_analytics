package de.burger.forensics.analytics.services.joernanalysis.adapter.in.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind;
import de.burger.forensics.analytics.joerncpganalysis.v1.AnalyzeJoernCpgRequest;
import de.burger.forensics.analytics.joerncpganalysis.v1.AnalyzeJoernCpgResponse;
import de.burger.forensics.analytics.joerncpganalysis.v1.GetSemanticArtifactBytesRequest;
import de.burger.forensics.analytics.joerncpganalysis.v1.GetSemanticArtifactBytesResponse;
import de.burger.forensics.analytics.joerncpganalysis.v1.JoernCpgAnalysisServiceGrpc;
import de.burger.forensics.analytics.joerncpganalysis.v1.JoernMaterializationPolicy;
import de.burger.forensics.analytics.joerncpganalysis.v1.JoernCpgPolicy;
import de.burger.forensics.analytics.joerncpganalysis.v1.MaterializeJoernWorkspaceRequest;
import de.burger.forensics.analytics.joerncpganalysis.v1.MaterializeJoernWorkspaceResponse;
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
        assertTrue(service.getMethods().stream().anyMatch(method -> "MaterializeSourceSnapshot".equals(method.getBareMethodName())));
        assertTrue(service.getMethods().stream().anyMatch(method -> "AnalyzeSourceSnapshot".equals(method.getBareMethodName())));
        assertNotNull(JoernCpgAnalysisServiceGrpc.getGetSemanticArtifactBytesMethod());
        assertNotNull(MaterializeJoernWorkspaceRequest.getDescriptor().findFieldByName("source_package"));
        assertNotNull(MaterializeJoernWorkspaceRequest.getDescriptor().findFieldByName("build_output_package"));
        assertNotNull(MaterializeJoernWorkspaceResponse.getDescriptor().findFieldByName("workspace"));
        assertNotNull(AnalyzeJoernCpgRequest.getDescriptor().findFieldByName("source_snapshot_id"));
        assertNotNull(AnalyzeJoernCpgRequest.getDescriptor().findFieldByName("safe_attributes"));
        assertNotNull(AnalyzeJoernCpgResponse.getDescriptor().findFieldByName("semantic_artifacts"));
        assertEquals(6, GetSemanticArtifactBytesRequest.getDescriptor().findFieldByName("retrieval_reference").getNumber());
        assertEquals(7, GetSemanticArtifactBytesRequest.getDescriptor().findFieldByName("expected_sha256").getNumber());
        assertEquals(8, GetSemanticArtifactBytesRequest.getDescriptor().findFieldByName("expected_size_bytes").getNumber());
        assertEquals(10, GetSemanticArtifactBytesRequest.getDescriptor().findFieldByName("schema_version").getNumber());
        assertEquals(5, GetSemanticArtifactBytesResponse.getDescriptor().findFieldByName("semantic_artifact").getNumber());
        assertEquals(6, GetSemanticArtifactBytesResponse.getDescriptor().findFieldByName("content").getNumber());
        assertEquals(7, GetSemanticArtifactBytesResponse.getDescriptor().findFieldByName("sha256").getNumber());
        assertNotNull(SourceWorkspace.getDescriptor().findFieldByName("workspace_id"));
        assertNotNull(JoernCpgPolicy.getDescriptor().findFieldByName("joern_image_reference"));
        assertNotNull(JoernMaterializationPolicy.getDescriptor().findFieldByName("reject_symlinks"));
        assertEquals(3, AnalysisWorkerKind.ANALYSIS_WORKER_KIND_JOERN_ANALYSIS.getNumber());
    }
}
