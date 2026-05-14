package de.burger.forensics.analytics.ingestion.grpc.mapper;

import de.burger.forensics.analytics.application.ingestion.result.IngestionStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IngestionStatusMapperTest {
    private final IngestionStatusMapper mapper = new IngestionStatusMapper();

    @Test
    void mapsAllApplicationStatusesToProtoStatuses() {
        assertEquals(
            de.burger.forensics.analytics.ingestion.v1.IngestionStatus.INGESTION_STATUS_ACCEPTED,
            mapper.toProto(IngestionStatus.ACCEPTED)
        );
        assertEquals(
            de.burger.forensics.analytics.ingestion.v1.IngestionStatus.INGESTION_STATUS_COMPLETED,
            mapper.toProto(IngestionStatus.COMPLETED)
        );
        assertEquals(
            de.burger.forensics.analytics.ingestion.v1.IngestionStatus.INGESTION_STATUS_ABORTED,
            mapper.toProto(IngestionStatus.ABORTED)
        );
        assertEquals(
            de.burger.forensics.analytics.ingestion.v1.IngestionStatus.INGESTION_STATUS_REJECTED,
            mapper.toProto(IngestionStatus.REJECTED)
        );
    }
}
