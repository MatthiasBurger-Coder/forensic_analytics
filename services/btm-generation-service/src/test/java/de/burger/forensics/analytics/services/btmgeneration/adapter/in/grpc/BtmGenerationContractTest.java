package de.burger.forensics.analytics.services.btmgeneration.adapter.in.grpc;

import de.burger.forensics.analytics.analysisjob.v1.AnalysisWorkerKind;
import de.burger.forensics.analytics.btmgeneration.v1.BtmArtifactChunk;
import de.burger.forensics.analytics.btmgeneration.v1.BtmArtifactDeliveryMessage;
import de.burger.forensics.analytics.btmgeneration.v1.BtmArtifactDeliveryServiceGrpc;
import de.burger.forensics.analytics.btmgeneration.v1.BtmArtifactDescriptor;
import de.burger.forensics.analytics.btmgeneration.v1.BtmArtifactKind;
import de.burger.forensics.analytics.btmgeneration.v1.BtmArtifactManifest;
import de.burger.forensics.analytics.btmgeneration.v1.BtmDiagnostic;
import de.burger.forensics.analytics.btmgeneration.v1.BtmGenerationServiceGrpc;
import de.burger.forensics.analytics.btmgeneration.v1.BtmRuleSummary;
import de.burger.forensics.analytics.btmgeneration.v1.DownloadBtmArtifactsRequest;
import de.burger.forensics.analytics.btmgeneration.v1.GenerateBtmRulesRequest;
import de.burger.forensics.analytics.btmgeneration.v1.GenerateBtmRulesResponse;
import de.burger.forensics.analytics.btmgeneration.v1.InstrumentationTarget;
import de.burger.forensics.analytics.btmgeneration.v1.ProbeKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BtmGenerationContractTest {
    @Test
    void generatedContractPreservesProvisionalRpcSurfaceAndKeyFields() {
        assertNotNull(BtmGenerationServiceGrpc.getGenerateBtmRulesMethod());

        assertEquals(5, GenerateBtmRulesRequest.getDescriptor().findFieldByName("worker_kind").getNumber());
        assertEquals(10, GenerateBtmRulesRequest.getDescriptor().findFieldByName("policy").getNumber());
        assertEquals(11, GenerateBtmRulesRequest.getDescriptor().findFieldByName("facts").getNumber());
        assertEquals(6, GenerateBtmRulesResponse.getDescriptor().findFieldByName("generated_artifacts").getNumber());
        assertEquals(9, GenerateBtmRulesResponse.getDescriptor().findFieldByName("reproducibility").getNumber());
        assertEquals(4, AnalysisWorkerKind.ANALYSIS_WORKER_KIND_BTM_GENERATION.getNumber());
        assertEquals(4, InstrumentationTarget.getDescriptor().findFieldByName("relative_path").getNumber());
        assertEquals(9, InstrumentationTarget.getDescriptor().findFieldByName("probe_kind").getNumber());
        assertEquals(1, ProbeKind.PROBE_KIND_METHOD_ENTRY.getNumber());
        assertEquals(1, BtmRuleSummary.getDescriptor().findFieldByName("rule_id").getNumber());
        assertEquals(8, BtmDiagnostic.getDescriptor().findFieldByName("affects_completeness").getNumber());
    }

    @Test
    void generatedContractPreservesPublicBtmArtifactDeliverySurfaceAndKeyFields() {
        assertNotNull(BtmArtifactDeliveryServiceGrpc.getDownloadBtmArtifactsMethod());

        assertEquals(1, DownloadBtmArtifactsRequest.getDescriptor().findFieldByName("request_id").getNumber());
        assertEquals(2, DownloadBtmArtifactsRequest.getDescriptor().findFieldByName("idempotency_key").getNumber());
        assertEquals(4, DownloadBtmArtifactsRequest.getDescriptor().findFieldByName("correlation_id").getNumber());
        assertEquals(7, DownloadBtmArtifactsRequest.getDescriptor().findFieldByName("max_chunk_bytes").getNumber());
        assertEquals(8, DownloadBtmArtifactsRequest.getDescriptor().findFieldByName("max_total_bytes").getNumber());
        assertEquals(1, BtmArtifactDeliveryMessage.getDescriptor().findFieldByName("manifest").getNumber());
        assertEquals(2, BtmArtifactDeliveryMessage.getDescriptor().findFieldByName("chunk").getNumber());
        assertEquals(3, BtmArtifactDeliveryMessage.getDescriptor().findFieldByName("status").getNumber());
        assertEquals(6, BtmArtifactManifest.getDescriptor().findFieldByName("artifacts").getNumber());
        assertEquals(8, BtmArtifactManifest.getDescriptor().findFieldByName("manifest_sha256").getNumber());
        assertEquals(3, BtmArtifactDescriptor.getDescriptor().findFieldByName("relative_path").getNumber());
        assertEquals(4, BtmArtifactChunk.getDescriptor().findFieldByName("chunk_index").getNumber());
        assertEquals(6, BtmArtifactChunk.getDescriptor().findFieldByName("data").getNumber());
        assertEquals(1, BtmArtifactKind.BTM_ARTIFACT_KIND_RULE_FILE.getNumber());
        assertEquals(2, BtmArtifactKind.BTM_ARTIFACT_KIND_MANIFEST.getNumber());
    }
}
