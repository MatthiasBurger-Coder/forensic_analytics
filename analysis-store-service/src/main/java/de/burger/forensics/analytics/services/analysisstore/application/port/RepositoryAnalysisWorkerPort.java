package de.burger.forensics.analytics.services.analysisstore.application.port;

import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJobId;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisRunId;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactByteAccess;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactReference;
import de.burger.forensics.analytics.services.analysisstore.domain.RepositoryToBtmOrchestrationDomain.StartRepositoryToBtmCommand;
import de.burger.forensics.analytics.services.analysisstore.domain.SourceSnapshotId;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public interface RepositoryAnalysisWorkerPort {
    RepositoryAnalysisResult prepareAndAnalyzeJavaAst(
        StartRepositoryToBtmCommand command,
        AnalysisJobId astAnalysisJobId
    );

    static RepositoryAnalysisWorkerPort unavailable() {
        return (command, astAnalysisJobId) -> {
            throw new WorkerOwnerApiUnavailableException("Repository Analysis");
        };
    }

    record RepositoryAnalysisResult(
        AnalysisRunId analysisRunId,
        AnalysisJobId astAnalysisJobId,
        SourceSnapshotId sourceSnapshotId,
        List<SourceRoot> sourceRoots,
        PackageDescriptor sourcePackage,
        PackageDescriptor buildOutputPackage,
        AnalysisArtifactReference sourceFactArtifact,
        AnalysisCompleteness completeness,
        List<WorkerDiagnostic> diagnostics,
        Map<String, String> attributes
    ) {
        public RepositoryAnalysisResult {
            analysisRunId = Objects.requireNonNull(analysisRunId, "analysisRunId must not be null");
            astAnalysisJobId = Objects.requireNonNull(astAnalysisJobId, "astAnalysisJobId must not be null");
            sourceSnapshotId = Objects.requireNonNull(sourceSnapshotId, "sourceSnapshotId must not be null");
            sourceRoots = List.copyOf(Objects.requireNonNull(sourceRoots, "sourceRoots must not be null"));
            sourcePackage = Objects.requireNonNull(sourcePackage, "sourcePackage must not be null");
            buildOutputPackage = Objects.requireNonNull(buildOutputPackage, "buildOutputPackage must not be null");
            sourceFactArtifact = Objects.requireNonNull(sourceFactArtifact, "sourceFactArtifact must not be null");
            completeness = Objects.requireNonNull(completeness, "completeness must not be null");
            diagnostics = List.copyOf(Objects.requireNonNullElse(diagnostics, List.of()));
            attributes = Map.copyOf(Objects.requireNonNullElse(attributes, Map.of()));
        }

        public boolean hasJoernReadyPackages() {
            return sourcePackage.isAvailableCompletePackage() && buildOutputPackage.isAvailableCompletePackage();
        }
    }

    record SourceRoot(String relativePath, String language) {
        public SourceRoot {
            relativePath = ArtifactByteAccess.requirePublicReference(relativePath, "sourceRoot.relativePath");
            language = requireText(language, "sourceRoot.language");
        }
    }

    record PackageDescriptor(
        PackageAvailability availability,
        ArtifactReference manifestArtifact,
        ArtifactReference packageArtifact,
        String schemaVersion,
        String producerService,
        ArtifactByteAccess byteAccess,
        AnalysisCompleteness completeness,
        BuildOutputResolution buildOutputResolution,
        String buildSystem
    ) {
        public PackageDescriptor {
            availability = Objects.requireNonNull(availability, "availability must not be null");
            schemaVersion = requireText(schemaVersion, "schemaVersion");
            producerService = requireText(producerService, "producerService");
            byteAccess = Objects.requireNonNull(byteAccess, "byteAccess must not be null");
            completeness = Objects.requireNonNull(completeness, "completeness must not be null");
            buildOutputResolution = Objects.requireNonNullElse(buildOutputResolution, BuildOutputResolution.empty());
            buildSystem = buildSystem == null ? "" : buildSystem.strip();
        }

        public boolean isAvailableCompletePackage() {
            return availability == PackageAvailability.AVAILABLE
                && completeness == AnalysisCompleteness.COMPLETE
                && manifestArtifact != null
                && packageArtifact != null;
        }
    }

    record BuildOutputResolution(
        List<BuildOutputProducerCandidate> candidates,
        BuildOutputProducer selectedProducer,
        boolean terminalIntegrityFailure,
        List<WorkerDiagnostic> diagnostics
    ) {
        public BuildOutputResolution {
            candidates = List.copyOf(Objects.requireNonNullElse(candidates, List.of()));
            selectedProducer = Objects.requireNonNull(selectedProducer, "selectedProducer must not be null");
            diagnostics = List.copyOf(Objects.requireNonNullElse(diagnostics, List.of()));
        }

        public static BuildOutputResolution empty() {
            return new BuildOutputResolution(List.of(), BuildOutputProducer.UNSPECIFIED, false, List.of());
        }
    }

    record BuildOutputProducerCandidate(
        BuildOutputProducer producer,
        BuildOutputProducerStatus status,
        String reference,
        List<WorkerDiagnostic> diagnostics
    ) {
        public BuildOutputProducerCandidate {
            producer = Objects.requireNonNull(producer, "producer must not be null");
            status = Objects.requireNonNull(status, "status must not be null");
            reference = reference == null || reference.isBlank()
                ? ""
                : ArtifactByteAccess.requirePublicReference(reference, "buildOutputProducer.reference");
            diagnostics = List.copyOf(Objects.requireNonNullElse(diagnostics, List.of()));
        }
    }

    record WorkerDiagnostic(
        String code,
        String message,
        WorkerDiagnosticSeverity severity,
        boolean retryable,
        boolean affectsCompleteness
    ) {
        public WorkerDiagnostic {
            code = requireText(code, "diagnostic.code");
            message = requireText(message, "diagnostic.message");
            severity = Objects.requireNonNull(severity, "severity must not be null");
        }
    }

    enum PackageAvailability {
        AVAILABLE,
        PENDING,
        UNAVAILABLE,
        FAILED_INTEGRITY
    }

    enum BuildOutputProducer {
        UNSPECIFIED,
        ARTIFACT_STORE,
        ARTIFACTORY,
        JENKINS,
        BUILD_ARTIFACT_WORKER
    }

    enum BuildOutputProducerStatus {
        AVAILABLE,
        NOT_CONFIGURED,
        MISSING,
        FALLBACK_PLANNED,
        TERMINAL_INTEGRITY_FAILURE
    }

    enum WorkerDiagnosticSeverity {
        INFO,
        WARNING,
        ERROR
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.strip();
    }
}
