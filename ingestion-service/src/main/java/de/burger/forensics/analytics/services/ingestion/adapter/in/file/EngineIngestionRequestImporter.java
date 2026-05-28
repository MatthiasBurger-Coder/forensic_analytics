package de.burger.forensics.analytics.services.ingestion.adapter.in.file;

import de.burger.forensics.analytics.services.ingestion.application.IngestionApplicationService;
import de.burger.forensics.analytics.services.ingestion.application.command.StartAnalysisSessionCommand;
import de.burger.forensics.analytics.services.ingestion.application.command.UploadAnalysisDataCommand;
import de.burger.forensics.analytics.services.ingestion.domain.RawIngestionPayload;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class EngineIngestionRequestImporter {
    private final IngestionApplicationService ingestionService;
    private final EngineIngestionRequestReader requestReader;

    public EngineIngestionRequestImporter(IngestionApplicationService ingestionService) {
        this(ingestionService, new EngineIngestionRequestReader());
    }

    EngineIngestionRequestImporter(
        IngestionApplicationService ingestionService,
        EngineIngestionRequestReader requestReader
    ) {
        this.ingestionService = Objects.requireNonNull(ingestionService, "ingestionService must not be null");
        this.requestReader = Objects.requireNonNull(requestReader, "requestReader must not be null");
    }

    public EngineIngestionImportResult importRequest(Path requestFile) {
        return importVerifiedRequest(requestFile);
    }

    private EngineIngestionImportResult importVerifiedRequest(Path requestFile) {
        var request = requestReader.read(requestFile);
        var payloads = payloads(request);
        var session = ingestionService.start(new StartAnalysisSessionCommand(
            request.buildIdentity(),
            request.pluginIdentity(),
            request.schemaVersion()
        ));

        var uploadedPayloads = 0;
        for (var payload : payloads) {
            ingestionService.upload(new UploadAnalysisDataCommand(
                session.sessionId(),
                new RawIngestionPayload(
                    request.buildIdentity(),
                    request.moduleIdentity(),
                    request.pluginIdentity(),
                    request.schemaVersion(),
                    payload.reference().descriptor(),
                    payload.content()
                )
            ));
            uploadedPayloads++;
        }

        var completion = ingestionService.complete(session.sessionId());
        return new EngineIngestionImportResult(session.sessionId(), completion.status(), uploadedPayloads);
    }

    private static List<PayloadUpload> payloads(EngineIngestionRequest request) {
        return request.payloads().stream()
            .map(reference -> new PayloadUpload(reference, readPayload(reference.file())))
            .toList();
    }

    private static byte[] readPayload(Path file) {
        try {
            if (!Files.isRegularFile(file)) {
                throw new EngineIngestionRequestException("Engine payload file does not exist: " + file);
            }
            return Files.readAllBytes(file);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read engine payload file " + file + ".", exception);
        }
    }

    private record PayloadUpload(EngineIngestionPayloadReference reference, byte[] content) {
        private PayloadUpload {
            Objects.requireNonNull(reference, "reference must not be null");
            Objects.requireNonNull(content, "content must not be null");
        }
    }
}
