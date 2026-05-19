package de.burger.forensics.analytics.services.btmgeneration.adapter.out.filesystem;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.burger.forensics.analytics.services.btmgeneration.application.BtmArtifactDeliveryException;
import de.burger.forensics.analytics.services.btmgeneration.application.port.BtmArtifactReaderPort;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisJobId;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ArtifactReference;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ReadableBtmArtifact;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ReproducibilityMetadata;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.SourceSnapshotId;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.StoredBtmArtifactManifest;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.TargetSelection;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FileSystemBtmArtifactReader implements BtmArtifactReaderPort {
    private final Path artifactRoot;

    public FileSystemBtmArtifactReader(Path artifactRoot) {
        this.artifactRoot = Objects.requireNonNull(artifactRoot, "artifact root must not be null")
            .toAbsolutePath()
            .normalize();
    }

    @Override
    public StoredBtmArtifactManifest readManifest(AnalysisArtifactReference manifestReference) {
        return storedManifest(manifestReference);
    }

    @Override
    public void verify(AnalysisArtifactReference artifact) {
        verifyReadableArtifact(artifact);
    }

    @Override
    public InputStream open(ReadableBtmArtifact artifact) {
        try {
            return Files.newInputStream(resolve(artifact.reference().artifact().path()));
        } catch (IOException error) {
            throw new BtmArtifactDeliveryException(
                BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION,
                "Failed to open accepted BTM artifact.",
                error
            );
        }
    }

    private void verifyReadableArtifact(AnalysisArtifactReference reference) {
        var target = resolve(reference.artifact().path());
        try {
            if (Files.size(target) != reference.artifact().sizeBytes()) {
                throw new BtmArtifactDeliveryException(
                    BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION,
                    "BTM artifact size does not match accepted metadata."
                );
            }
        } catch (IOException error) {
            throw new BtmArtifactDeliveryException(
                BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION,
                "Failed to verify accepted BTM artifact.",
                error
            );
        }
    }

    private Path resolve(String artifactPath) {
        var relativePath = Path.of(artifactPath).normalize();
        var target = artifactRoot.resolve(relativePath).normalize();
        if (!target.startsWith(artifactRoot)) {
            throw new BtmArtifactDeliveryException(
                BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION,
                "Accepted BTM artifact path escapes the artifact root."
            );
        }
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            throw new BtmArtifactDeliveryException(
                BtmArtifactDeliveryException.Reason.NOT_FOUND,
                "Accepted BTM artifact is not available."
            );
        }
        rejectSymlinkSegments(relativePath);
        if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
            throw new BtmArtifactDeliveryException(
                BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION,
                "Accepted BTM artifact is not a regular file."
            );
        }
        return target;
    }

    private void rejectSymlinkSegments(Path relativePath) {
        var probe = artifactRoot;
        for (var segment : relativePath) {
            probe = probe.resolve(segment);
            if (Files.isSymbolicLink(probe)) {
                throw new BtmArtifactDeliveryException(
                    BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION,
                    "Accepted BTM artifact path must not contain symbolic links."
                );
            }
        }
    }

    private StoredBtmArtifactManifest storedManifest(AnalysisArtifactReference manifestReference) {
        try {
            try (var reader = Files.newBufferedReader(resolve(manifestReference.artifact().path()))) {
                var document = JsonParser.parseReader(reader).getAsJsonObject();
                return new StoredBtmArtifactManifest(
                    new AnalysisRunId(requiredString(document, "analysisRunId")),
                    new AnalysisJobId(requiredString(document, "analysisJobId")),
                    new SourceSnapshotId(requiredString(document, "sourceSnapshotId")),
                    AnalysisCompleteness.valueOf(requiredString(document, "completeness")),
                    generatedArtifacts(document),
                    reproducibility(document.getAsJsonObject("reproducibility")),
                    targetSelection(document.getAsJsonObject("targetSelection"))
                );
            }
        } catch (RuntimeException error) {
            throw new BtmArtifactDeliveryException(
                BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION,
                "Stored BTM manifest does not preserve required delivery metadata.",
                error
            );
        } catch (IOException error) {
            throw new BtmArtifactDeliveryException(
                BtmArtifactDeliveryException.Reason.FAILED_PRECONDITION,
                "Failed to parse stored BTM manifest.",
                error
            );
        }
    }

    private static List<ArtifactReference> generatedArtifacts(JsonObject document) {
        if (document == null || !document.has("generatedArtifacts") || !document.get("generatedArtifacts").isJsonArray()) {
            throw new IllegalArgumentException("generatedArtifacts is required");
        }
        var artifacts = new ArrayList<ArtifactReference>();
        for (var element : document.getAsJsonArray("generatedArtifacts")) {
            var artifact = element.getAsJsonObject();
            artifacts.add(new ArtifactReference(
                requiredString(artifact, "path"),
                requiredString(artifact, "type"),
                requiredString(artifact, "sha256"),
                artifact.get("sizeBytes").getAsLong()
            ));
        }
        if (artifacts.isEmpty()) {
            throw new IllegalArgumentException("generatedArtifacts must not be empty");
        }
        return List.copyOf(artifacts);
    }

    private static ReproducibilityMetadata reproducibility(JsonObject object) {
        return new ReproducibilityMetadata(
            requiredString(object, "factsFingerprint"),
            requiredString(object, "policyFingerprint"),
            requiredString(object, "generationFingerprint"),
            requiredString(object, "generatorVersion"),
            requiredString(object, "deterministicSort")
        );
    }

    private static TargetSelection targetSelection(JsonObject object) {
        return new TargetSelection(
            requiredString(object, "selectionId"),
            requiredString(object, "ownerService"),
            requiredString(object, "policyVersion"),
            requiredString(object, "selectionFingerprint"),
            AnalysisCompleteness.valueOf(requiredString(object, "completeness")),
            requiredString(object, "deterministicOrder"),
            requiredString(object, "correlationId"),
            object.get("targetCount").getAsInt()
        );
    }

    private static String requiredString(JsonObject object, String fieldName) {
        if (object == null || !object.has(fieldName) || object.get(fieldName).getAsString().isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return object.get(fieldName).getAsString();
    }

}
