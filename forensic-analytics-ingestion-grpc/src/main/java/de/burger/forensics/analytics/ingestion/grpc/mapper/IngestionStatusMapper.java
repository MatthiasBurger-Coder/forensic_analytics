package de.burger.forensics.analytics.ingestion.grpc.mapper;

public final class IngestionStatusMapper {
    public de.burger.forensics.analytics.ingestion.v1.IngestionStatus toProto(
        de.burger.forensics.analytics.application.ingestion.result.IngestionStatus status
    ) {
        return switch (status) {
            case ACCEPTED -> de.burger.forensics.analytics.ingestion.v1.IngestionStatus.INGESTION_STATUS_ACCEPTED;
            case COMPLETED -> de.burger.forensics.analytics.ingestion.v1.IngestionStatus.INGESTION_STATUS_COMPLETED;
            case ABORTED -> de.burger.forensics.analytics.ingestion.v1.IngestionStatus.INGESTION_STATUS_ABORTED;
            case REJECTED -> de.burger.forensics.analytics.ingestion.v1.IngestionStatus.INGESTION_STATUS_REJECTED;
        };
    }
}
