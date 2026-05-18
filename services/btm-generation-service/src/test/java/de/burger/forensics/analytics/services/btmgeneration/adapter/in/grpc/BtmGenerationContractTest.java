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
import de.burger.forensics.analytics.btmgeneration.v1.DeliveredAnalysisFacts;
import de.burger.forensics.analytics.btmgeneration.v1.DownloadBtmArtifactsRequest;
import de.burger.forensics.analytics.btmgeneration.v1.GenerateBtmRulesRequest;
import de.burger.forensics.analytics.btmgeneration.v1.GenerateBtmRulesResponse;
import de.burger.forensics.analytics.btmgeneration.v1.InstrumentationTarget;
import de.burger.forensics.analytics.btmgeneration.v1.InstrumentationTargetSelection;
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
        assertEquals(12, GenerateBtmRulesResponse.getDescriptor().findFieldByName("target_selection").getNumber());
        assertEquals(4, AnalysisWorkerKind.ANALYSIS_WORKER_KIND_BTM_GENERATION.getNumber());
        assertEquals(4, InstrumentationTarget.getDescriptor().findFieldByName("relative_path").getNumber());
        assertEquals(9, InstrumentationTarget.getDescriptor().findFieldByName("probe_kind").getNumber());
        assertEquals(10, InstrumentationTarget.getDescriptor().findFieldByName("source_fact_artifact_reference").getNumber());
        assertEquals(11, InstrumentationTarget.getDescriptor().findFieldByName("semantic_artifact_reference").getNumber());
        assertEquals(12, InstrumentationTarget.getDescriptor().findFieldByName("order_index").getNumber());
        assertEquals(13, InstrumentationTarget.getDescriptor().findFieldByName("completeness").getNumber());
        assertEquals(14, InstrumentationTarget.getDescriptor().findFieldByName("sensitivity").getNumber());
        assertEquals(1, ProbeKind.PROBE_KIND_METHOD_ENTRY.getNumber());
        assertEquals(1, BtmRuleSummary.getDescriptor().findFieldByName("rule_id").getNumber());
        assertEquals(8, BtmDiagnostic.getDescriptor().findFieldByName("affects_completeness").getNumber());
        assertEquals(5, DeliveredAnalysisFacts.getDescriptor().findFieldByName("target_selection").getNumber());
        assertEquals(1, InstrumentationTargetSelection.getDescriptor().findFieldByName("selection_id").getNumber());
        assertEquals(2, InstrumentationTargetSelection.getDescriptor().findFieldByName("owner_service").getNumber());
        assertEquals(3, InstrumentationTargetSelection.getDescriptor().findFieldByName("policy_version").getNumber());
        assertEquals(4, InstrumentationTargetSelection.getDescriptor().findFieldByName("selection_fingerprint").getNumber());
        assertEquals(5, InstrumentationTargetSelection.getDescriptor().findFieldByName("completeness").getNumber());
        assertEquals(6, InstrumentationTargetSelection.getDescriptor().findFieldByName("deterministic_order").getNumber());
        assertEquals(7, InstrumentationTargetSelection.getDescriptor().findFieldByName("correlation_id").getNumber());
        assertEquals(8, InstrumentationTargetSelection.getDescriptor().findFieldByName("target_count").getNumber());
    }

    @Test
    void generatedContractPreservesPublicBtmArtifactDeliverySurfaceAndKeyFields() {
        assertNotNull(BtmArtifactDeliveryServiceGrpc.getDownloadBtmArtifactsMethod());

        assertEquals(1, DownloadBtmArtifactsRequest.getDescriptor().findFieldByName("request_id").getNumber());
        assertEquals(2, DownloadBtmArtifactsRequest.getDescriptor().findFieldByName("idempotency_key").getNumber());
        assertEquals(4, DownloadBtmArtifactsRequest.getDescriptor().findFieldByName("correlation_id").getNumber());
        assertEquals(7, DownloadBtmArtifactsRequest.getDescriptor().findFieldByName("max_chunk_bytes").getNumber());
        assertEquals(8, DownloadBtmArtifactsRequest.getDescriptor().findFieldByName("max_total_bytes").getNumber());
        assertEquals(11, DownloadBtmArtifactsRequest.getDescriptor().findFieldByName("accepted_generated_artifacts").getNumber());
        assertEquals(1, BtmArtifactDeliveryMessage.getDescriptor().findFieldByName("manifest").getNumber());
        assertEquals(2, BtmArtifactDeliveryMessage.getDescriptor().findFieldByName("chunk").getNumber());
        assertEquals(3, BtmArtifactDeliveryMessage.getDescriptor().findFieldByName("status").getNumber());
        assertEquals(6, BtmArtifactManifest.getDescriptor().findFieldByName("artifacts").getNumber());
        assertEquals(8, BtmArtifactManifest.getDescriptor().findFieldByName("manifest_sha256").getNumber());
        assertEquals(10, BtmArtifactManifest.getDescriptor().findFieldByName("reproducibility").getNumber());
        assertEquals(11, BtmArtifactManifest.getDescriptor().findFieldByName("target_selection").getNumber());
        assertEquals(3, BtmArtifactDescriptor.getDescriptor().findFieldByName("relative_path").getNumber());
        assertEquals(4, BtmArtifactChunk.getDescriptor().findFieldByName("chunk_index").getNumber());
        assertEquals(6, BtmArtifactChunk.getDescriptor().findFieldByName("data").getNumber());
        assertEquals(1, BtmArtifactKind.BTM_ARTIFACT_KIND_RULE_FILE.getNumber());
        assertEquals(2, BtmArtifactKind.BTM_ARTIFACT_KIND_MANIFEST.getNumber());
    }
}
