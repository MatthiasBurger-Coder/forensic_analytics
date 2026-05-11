package de.burger.forensics.analytics.ingestion.request;

import de.burger.forensics.analytics.application.ingestion.ForensicIngestionUseCase;
import de.burger.forensics.analytics.application.ingestion.command.CompleteAnalysisSessionCommand;
import de.burger.forensics.analytics.application.ingestion.command.StartAnalysisSessionCommand;
import de.burger.forensics.analytics.application.ingestion.command.UploadAnalysisDataCommand;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class EngineIngestionRequestImporter {
    private final ForensicIngestionUseCase ingestionUseCase;
    private final EngineIngestionRequestReader requestReader;

    public EngineIngestionRequestImporter(ForensicIngestionUseCase ingestionUseCase) {
        this(ingestionUseCase, new EngineIngestionRequestReader());
    }

    EngineIngestionRequestImporter(
        ForensicIngestionUseCase ingestionUseCase,
        EngineIngestionRequestReader requestReader
    ) {
        this.ingestionUseCase = Objects.requireNonNull(ingestionUseCase, "ingestionUseCase must not be null");
        this.requestReader = Objects.requireNonNull(requestReader, "requestReader must not be null");
    }

    public EngineIngestionImportResult importRequest(Path requestFile) {
        var request = requestReader.read(requestFile);
        var payloads = payloads(request);
        var session = ingestionUseCase.start(new StartAnalysisSessionCommand(
            request.buildIdentity(),
            request.pluginIdentity(),
            request.schemaVersion()
        ));

        var uploadedPayloads = 0;
        for (var payload : payloads) {
            ingestionUseCase.upload(new UploadAnalysisDataCommand(
                session.sessionId(),
                request.buildIdentity(),
                request.moduleIdentity(),
                request.pluginIdentity(),
                request.schemaVersion(),
                payload.reference().descriptor(),
                payload.content()
            ));
            uploadedPayloads++;
        }

        var completion = ingestionUseCase.complete(new CompleteAnalysisSessionCommand(session.sessionId()));
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
