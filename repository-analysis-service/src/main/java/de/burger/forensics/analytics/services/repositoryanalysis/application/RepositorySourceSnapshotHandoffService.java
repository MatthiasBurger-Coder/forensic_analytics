package de.burger.forensics.analytics.services.repositoryanalysis.application;

import de.burger.forensics.analytics.services.repositoryanalysis.application.port.JavaAstAnalysisPort;
import de.burger.forensics.analytics.services.repositoryanalysis.application.port.RepositoryPreparationRepository;
import de.burger.forensics.analytics.services.repositoryanalysis.application.port.SourceSnapshotFileCollectorPort;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.AnalysisJobId;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.JavaAstAnalysisHandoffCommand;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.JavaAstAnalysisHandoffResult;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.RepositoryWorkspaceStatus;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceSnapshotHandoffPolicy;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.SourceSnapshotId;

import java.util.Map;
import java.util.Objects;

import static de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.requireText;
import static de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.safeAttributes;

public final class RepositorySourceSnapshotHandoffService {
    private static final String JAVA_AST_WORKER_VERSION = "java-ast-analysis-service-v1";

    private final RepositoryPreparationRepository repository;
    private final SourceSnapshotFileCollectorPort sourceFileCollector;
    private final JavaAstAnalysisPort javaAstAnalysisPort;

    public RepositorySourceSnapshotHandoffService(
        RepositoryPreparationRepository repository,
        SourceSnapshotFileCollectorPort sourceFileCollector,
        JavaAstAnalysisPort javaAstAnalysisPort
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.sourceFileCollector = Objects.requireNonNull(sourceFileCollector, "source file collector must not be null");
        this.javaAstAnalysisPort = Objects.requireNonNull(javaAstAnalysisPort, "java ast analysis port must not be null");
    }

    public JavaAstAnalysisHandoffResult analyzeWithJavaAst(
        String requestId,
        String idempotencyKey,
        String schemaVersion,
        String correlationId,
        AnalysisRunId analysisRunId,
        AnalysisJobId analysisJobId,
        SourceSnapshotId sourceSnapshotId,
        SourceSnapshotHandoffPolicy policy,
        Map<String, String> attributes
    ) {
        var safeAttributes = safeAttributes(attributes);
        var preparation = repository.findByRunAndSnapshot(
                Objects.requireNonNull(analysisRunId, "analysis run id must not be null"),
                Objects.requireNonNull(sourceSnapshotId, "source snapshot id must not be null")
            )
            .orElseThrow(() -> new RepositoryPreparationNotFoundException("repository preparation was not found"));
        if (preparation.workspaceStatus() == RepositoryWorkspaceStatus.CLEANED) {
            throw new IllegalStateException("Repository workspace is not available for source snapshot handoff");
        }
        var sourceFiles = sourceFileCollector.collect(
            preparation.workspaceId(),
            preparation.sourceSnapshot().sourceRoots(),
            Objects.requireNonNull(policy, "handoff policy must not be null")
        );
        return javaAstAnalysisPort.analyze(new JavaAstAnalysisHandoffCommand(
            requireText(requestId, "request id"),
            requireText(idempotencyKey, "idempotency key"),
            requireText(schemaVersion, "schema version"),
            requireText(correlationId, "correlation id"),
            analysisRunId,
            Objects.requireNonNull(analysisJobId, "analysis job id must not be null"),
            sourceSnapshotId,
            JAVA_AST_WORKER_VERSION,
            policy,
            preparation.sourceSnapshot().sourceRoots(),
            sourceFiles,
            safeAttributes
        ));
    }
}
