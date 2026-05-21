package de.burger.forensics.analytics.services.ingestion.adapter.in.grpc;

import de.burger.forensics.analytics.ingestion.v1.AnalysisDataEnvelope;
import de.burger.forensics.analytics.ingestion.v1.ForensicIngestionServiceGrpc;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForensicIngestionContractTest {
    @Test
    void generatedContractPreservesRpcSurfaceAndDeprecatedPayloadTypeField() {
        assertNotNull(ForensicIngestionServiceGrpc.getAnalyzeRepositoryMethod());
        assertNotNull(ForensicIngestionServiceGrpc.getStartAnalysisSessionMethod());
        assertNotNull(ForensicIngestionServiceGrpc.getUploadAnalysisDataMethod());
        assertNotNull(ForensicIngestionServiceGrpc.getCompleteAnalysisSessionMethod());
        assertNotNull(ForensicIngestionServiceGrpc.getAbortAnalysisSessionMethod());

        var payloadType = AnalysisDataEnvelope.getDescriptor().findFieldByName("payload_type");

        assertNotNull(payloadType);
        assertEquals(6, payloadType.getNumber());
        assertTrue(payloadType.getOptions().getDeprecated());
    }
}
