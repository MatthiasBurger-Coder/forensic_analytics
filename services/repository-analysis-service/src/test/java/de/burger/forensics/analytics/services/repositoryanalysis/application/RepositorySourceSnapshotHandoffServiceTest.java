package de.burger.forensics.analytics.services.repositoryanalysis.application;

import de.burger.forensics.analytics.services.repositoryanalysis.adapter.out.memory.InMemoryRepositoryPreparationRepository;
import de.burger.forensics.analytics.services.repositoryanalysis.application.port.JavaAstAnalysisPort;
import de.burger.forensics.analytics.services.repositoryanalysis.application.port.SourceSnapshotFileCollectorPort;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.AnalysisJobId;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.ArtifactReference;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.CheckoutResult;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.CheckoutStatus;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.Diagnostic;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.JavaAstAnalysisHandoffCommand;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.JavaAstAnalysisHandoffResult;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.JavaAstScanSummary;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RepositoryPreparation;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RepositoryReference;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RepositoryWorkspaceStatus;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RevisionSelector;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceRoot;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceSnapshot;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceSnapshotCompleteness;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceSnapshotHandoffPolicy;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceSnapshotId;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceSnapshotSourceFile;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.WorkspaceId;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.sha256Hex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositorySourceSnapshotHandoffServiceTest {
    private final InMemoryRepositoryPreparationRepository repository = new InMemoryRepositoryPreparationRepository();
    private final CapturingSourceFileCollector collector = new CapturingSourceFileCollector();
    private final CapturingJavaAstPort javaAst = new CapturingJavaAstPort();
    private final RepositorySourceSnapshotHandoffService service = new RepositorySourceSnapshotHandoffService(
        repository,
        collector,
        javaAst
    );

    @Test
    void pushesBoundedInlineSourceFilesToJavaAstWithoutWorkspaceLeakage() {
        repository.save(preparation(RepositoryWorkspaceStatus.CHECKED_OUT));

        var result = service.analyzeWithJavaAst(
            "request-ast",
            "idempotency-ast",
            "schema-v1",
            "correlation-1",
            runId(),
            new AnalysisJobId("job-ast-1"),
            snapshotId(),
            new SourceSnapshotHandoffPolicy(10, 10_000, 30),
            Map.of("tenant", "demo")
        );

        assertEquals("run-1", result.analysisRunId().value());
        assertEquals("job-ast-1", javaAst.command.analysisJobId().value());
        assertEquals("snapshot-1", javaAst.command.sourceSnapshotId().value());
        assertEquals(List.of("src/main/java"), javaAst.command.sourceRoots().stream().map(SourceRoot::relativePath).toList());
        assertEquals(List.of("a/A.java"), javaAst.command.sourceFiles().stream().map(SourceSnapshotSourceFile::relativePath).toList());
        assertEquals("correlation-1", javaAst.command.correlationId());
        assertEquals("idempotency-ast", javaAst.command.idempotencyKey());
        assertTrue(javaAst.command.workerVersion().contains("java-ast-analysis-service"));
        assertFalse(javaAst.command.sourceFiles().getFirst().sourcePath().contains("workspace-1"));
    }

    @Test
    void rejectsMissingOrCleanedPreparationsBeforeHandoff() {
        assertThrows(RepositoryPreparationNotFoundException.class, () -> service.analyzeWithJavaAst(
            "request-ast",
            "idempotency-ast",
            "schema-v1",
            "correlation-1",
            runId(),
            new AnalysisJobId("job-ast-1"),
            snapshotId(),
            new SourceSnapshotHandoffPolicy(10, 10_000, 30),
            Map.of()
        ));

        repository.save(preparation(RepositoryWorkspaceStatus.CLEANED));
        assertThrows(IllegalStateException.class, () -> service.analyzeWithJavaAst(
            "request-ast",
            "idempotency-ast",
            "schema-v1",
            "correlation-1",
            runId(),
            new AnalysisJobId("job-ast-1"),
            snapshotId(),
            new SourceSnapshotHandoffPolicy(10, 10_000, 30),
            Map.of()
        ));
    }

    private static AnalysisRunId runId() {
        return new AnalysisRunId("run-1");
    }

    private static SourceSnapshotId snapshotId() {
        return new SourceSnapshotId("snapshot-1");
    }

    private static RepositoryPreparation preparation(RepositoryWorkspaceStatus workspaceStatus) {
        var roots = List.of(new SourceRoot("src/main/java", "java"));
        return new RepositoryPreparation(
            runId(),
            snapshotId(),
            new WorkspaceId("workspace-1"),
            new RepositoryReference("https://example.com/acme/demo.git", "github", Map.of()),
            new RevisionSelector("main", true, "", false),
            new CheckoutResult(
                CheckoutStatus.CHECKED_OUT,
                "https://example.com/acme/demo.git",
                "b".repeat(40),
                "main",
                "",
                true,
                5,
                List.of(),
                false,
                false,
                roots
            ),
            new SourceSnapshot(
                snapshotId(),
                SourceSnapshotCompleteness.COMPLETE,
                roots,
                new ArtifactReference("snapshots/snapshot-1/manifest.json", "application/json", "a".repeat(64), 10),
                List.of()
            ),
            workspaceStatus,
            Instant.parse("2026-05-18T10:00:00Z"),
            Instant.parse("2026-05-18T10:00:00Z"),
            List.of(),
            Map.of()
        );
    }

    private static final class CapturingSourceFileCollector implements SourceSnapshotFileCollectorPort {
        @Override
        public List<SourceSnapshotSourceFile> collect(
            WorkspaceId workspaceId,
            List<SourceRoot> sourceRoots,
            SourceSnapshotHandoffPolicy policy
        ) {
            return List.of(new SourceSnapshotSourceFile(
                "src/main/java",
                "a/A.java",
                "package a; class A {}",
                sha256Hex("package a; class A {}"),
                "package a; class A {}".getBytes(StandardCharsets.UTF_8).length
            ));
        }
    }

    private static final class CapturingJavaAstPort implements JavaAstAnalysisPort {
        private JavaAstAnalysisHandoffCommand command;

        @Override
        public JavaAstAnalysisHandoffResult analyze(JavaAstAnalysisHandoffCommand command) {
            this.command = command;
            return new JavaAstAnalysisHandoffResult(
                command.analysisRunId(),
                command.analysisJobId(),
                command.sourceSnapshotId(),
                SourceSnapshotCompleteness.INCOMPLETE,
                new ArtifactReference(
                    "java-ast/snapshot-1-job-ast-1-source-facts.json",
                    "application/vnd.forensic-analytics.java-ast-source-facts.v1+json",
                    "c".repeat(64),
                    100
                ),
                new JavaAstScanSummary(1, 1, 0, 0, 1, "JavaParser", "3.27.1"),
                List.of(Diagnostic.info("SYMBOL_RESOLUTION_NOT_CONFIGURED", "unresolved symbols remain explicit")),
                command.safeAttributes()
            );
        }
    }
}
