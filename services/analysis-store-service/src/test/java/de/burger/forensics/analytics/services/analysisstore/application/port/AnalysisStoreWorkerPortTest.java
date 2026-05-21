package de.burger.forensics.analytics.services.analysisstore.application.port;

import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactCategory;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJobId;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisRunId;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactByteAccess;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactByteCustody;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactReference;
import de.burger.forensics.analytics.services.analysisstore.domain.RepositoryToBtmOrchestrationDomain;
import de.burger.forensics.analytics.services.analysisstore.domain.SourceSnapshotId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisStoreWorkerPortTest {
    @Test
    void classifiesJoernReadinessOnlyForAvailableCompletePackages() {
        var ready = result(packageDescriptor(RepositoryAnalysisWorkerPort.PackageAvailability.AVAILABLE, AnalysisCompleteness.COMPLETE));
        var pending = result(packageDescriptor(RepositoryAnalysisWorkerPort.PackageAvailability.PENDING, AnalysisCompleteness.COMPLETE));
        var incomplete = result(packageDescriptor(RepositoryAnalysisWorkerPort.PackageAvailability.AVAILABLE, AnalysisCompleteness.INCOMPLETE));

        assertTrue(ready.hasJoernReadyPackages());
        assertFalse(pending.hasJoernReadyPackages());
        assertFalse(incomplete.hasJoernReadyPackages());
    }

    @Test
    void keepsOptionalPackageDescriptorAndCandidateFieldsDeterministic() {
        var descriptor = new RepositoryAnalysisWorkerPort.PackageDescriptor(
            RepositoryAnalysisWorkerPort.PackageAvailability.PENDING,
            null,
            null,
            "schema-v1",
            "repository-analysis-service",
            byteAccess("repository-analysis-service", "source-package"),
            AnalysisCompleteness.UNKNOWN,
            null,
            null
        );
        var blankReference = new RepositoryAnalysisWorkerPort.BuildOutputProducerCandidate(
            RepositoryAnalysisWorkerPort.BuildOutputProducer.JENKINS,
            RepositoryAnalysisWorkerPort.BuildOutputProducerStatus.NOT_CONFIGURED,
            " ",
            null
        );

        assertFalse(descriptor.isAvailableCompletePackage());
        assertEquals(RepositoryAnalysisWorkerPort.BuildOutputProducer.UNSPECIFIED, descriptor.buildOutputResolution().selectedProducer());
        assertEquals("", descriptor.buildSystem());
        assertEquals("", blankReference.reference());
        assertEquals(List.of(), blankReference.diagnostics());
    }

    @Test
    void rejectsInvalidWorkerDiagnosticsAndIntegrityCodes() {
        assertThrows(IllegalArgumentException.class, () -> new SourceFactArtifactReaderPort.SourceFactDiagnostic("", "message", true));
        assertThrows(IllegalArgumentException.class, () -> new SourceFactArtifactReaderPort.SourceFactDiagnostic(null, "message", true));
        assertThrows(IllegalArgumentException.class, () -> new SourceFactArtifactReaderPort.SourceFactDiagnostic("CODE", "", true));
        assertThrows(IllegalArgumentException.class, () -> new SourceFactArtifactReaderPort.SourceFactDiagnostic("CODE", null, true));
        assertThrows(IllegalArgumentException.class, () -> new EvidenceArtifactIntegrityException("", "message"));
        assertThrows(IllegalArgumentException.class, () -> new EvidenceArtifactIntegrityException(null, "message"));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryAnalysisWorkerPort.SourceRoot("src/main/java", " "));
        assertThrows(IllegalArgumentException.class, () -> new RepositoryAnalysisWorkerPort.WorkerDiagnostic(
            "",
            "message",
            RepositoryAnalysisWorkerPort.WorkerDiagnosticSeverity.ERROR,
            false,
            true
        ));

        var diagnostic = new SourceFactArtifactReaderPort.SourceFactDiagnostic("CODE", "message", true);
        var integrity = new EvidenceArtifactIntegrityException("INTEGRITY_FAILURE", "sanitized integrity failure");

        assertEquals("CODE", diagnostic.code());
        assertTrue(diagnostic.affectsCompleteness());
        assertEquals("INTEGRITY_FAILURE", integrity.diagnosticCode());
    }

    @Test
    void unavailablePortsReportExplicitOwnerApiFailures() {
        var command = command();
        var verifier = SourceFactArtifactByteVerifierPort.unavailable();
        assertTrue(verifier.supports(sourceFactArtifact()));
        assertFalse(verifier.supports(new AnalysisArtifactReference(
            sourceFactArtifact().artifact(),
            AnalysisArtifactCategory.RUNTIME,
            "java-ast-analysis-service",
            "source-facts-v1",
            AnalysisCompleteness.COMPLETE,
            byteAccess("java-ast-analysis-service", "source-facts.json")
        )));
        assertThrows(IllegalStateException.class, () -> verifier.verify(
            new AnalysisRunId("run-1"),
            new AnalysisJobId("job-ast"),
            new SourceSnapshotId("snapshot-1"),
            "request-1",
            "correlation-1",
            sourceFactArtifact(),
            Map.of()
        ));
        assertThrows(WorkerOwnerApiUnavailableException.class, () -> RepositoryAnalysisWorkerPort.unavailable()
            .prepareAndAnalyzeJavaAst(command, new AnalysisJobId("job-ast")));
        assertThrows(WorkerOwnerApiUnavailableException.class, () -> SourceFactArtifactReaderPort.unavailable()
            .readFacts(
                new AnalysisRunId("run-1"),
                new AnalysisJobId("job-ast"),
                new SourceSnapshotId("snapshot-1"),
                "request-1",
                "correlation-1",
                sourceFactArtifact(),
                Map.of()
            ));
        assertThrows(WorkerOwnerApiUnavailableException.class, () -> JoernSemanticAnalysisPort.unavailable()
            .analyze(command, new AnalysisJobId("job-joern"), result(packageDescriptor(
                RepositoryAnalysisWorkerPort.PackageAvailability.AVAILABLE,
                AnalysisCompleteness.COMPLETE
            ))));
        assertThrows(WorkerOwnerApiUnavailableException.class, () -> BtmGenerationWorkerPort.unavailable()
            .generate(
                command,
                new AnalysisJobId("job-btm"),
                new SourceSnapshotId("snapshot-1"),
                List.of(sourceFactArtifact()),
                List.of(),
                AnalysisCompleteness.COMPLETE,
                null,
                List.of()
            ));
    }

    private static RepositoryAnalysisWorkerPort.RepositoryAnalysisResult result(
        RepositoryAnalysisWorkerPort.PackageDescriptor packageDescriptor
    ) {
        return new RepositoryAnalysisWorkerPort.RepositoryAnalysisResult(
            new AnalysisRunId("run-1"),
            new AnalysisJobId("job-ast"),
            new SourceSnapshotId("snapshot-1"),
            List.of(new RepositoryAnalysisWorkerPort.SourceRoot("src/main/java", "java")),
            packageDescriptor,
            packageDescriptor,
            sourceFactArtifact(),
            AnalysisCompleteness.COMPLETE,
            null,
            null
        );
    }

    private static RepositoryAnalysisWorkerPort.PackageDescriptor packageDescriptor(
        RepositoryAnalysisWorkerPort.PackageAvailability availability,
        AnalysisCompleteness completeness
    ) {
        return new RepositoryAnalysisWorkerPort.PackageDescriptor(
            availability,
            new ArtifactReference("package-manifest.json", "application/json", "a".repeat(64), 128),
            new ArtifactReference("package.zip", "application/zip", "b".repeat(64), 512),
            "schema-v1",
            "repository-analysis-service",
            byteAccess("repository-analysis-service", "package.zip"),
            completeness,
            RepositoryAnalysisWorkerPort.BuildOutputResolution.empty(),
            "gradle"
        );
    }

    private static AnalysisArtifactReference sourceFactArtifact() {
        return new AnalysisArtifactReference(
            new ArtifactReference("source-facts.json", "application/json", "c".repeat(64), 512),
            AnalysisArtifactCategory.STATIC,
            "java-ast-analysis-service",
            "source-facts-v1",
            AnalysisCompleteness.COMPLETE,
            new ArtifactByteAccess(
                "java-ast-analysis-service",
                SourceFactArtifactByteVerifierPort.JAVA_AST_SOURCE_FACT_RETRIEVAL_CONTRACT,
                "source-facts.json",
                ArtifactByteCustody.PRODUCER_RETAINED
            )
        );
    }

    private static ArtifactByteAccess byteAccess(String owner, String reference) {
        return new ArtifactByteAccess(
            owner,
            owner + ".v1.DownloadArtifact",
            reference,
            ArtifactByteCustody.PRODUCER_RETAINED
        );
    }

    private static RepositoryToBtmOrchestrationDomain.StartRepositoryToBtmCommand command() {
        return new RepositoryToBtmOrchestrationDomain.StartRepositoryToBtmCommand(
            new RepositoryToBtmOrchestrationDomain.OrchestrationMetadata(
                "request-1",
                "schema-v1",
                "correlation-1",
                new AnalysisRunId("run-1")
            ),
            new RepositoryToBtmOrchestrationDomain.RepositoryReference("https://example.org/repository.git", "github"),
            new RepositoryToBtmOrchestrationDomain.RevisionSelector("main", ""),
            new RepositoryToBtmOrchestrationDomain.WorkspacePolicy(false, true, false, false, 30, 104_857_600),
            new RepositoryToBtmOrchestrationDomain.BuildContext("gradle", "build-1", "demo", List.of("app"), Map.of()),
            List.of(RepositoryToBtmOrchestrationDomain.RequestedOutput.BTM_RULES),
            Map.of()
        );
    }
}
