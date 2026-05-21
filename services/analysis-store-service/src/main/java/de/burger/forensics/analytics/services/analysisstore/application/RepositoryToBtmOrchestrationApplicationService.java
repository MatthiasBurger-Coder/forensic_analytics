package de.burger.forensics.analytics.services.analysisstore.application;

import de.burger.forensics.analytics.services.analysisstore.application.port.BtmGenerationWorkerPort;
import de.burger.forensics.analytics.services.analysisstore.application.port.EvidenceArtifactIntegrityException;
import de.burger.forensics.analytics.services.analysisstore.application.port.JoernSemanticAnalysisPort;
import de.burger.forensics.analytics.services.analysisstore.application.port.RepositoryAnalysisWorkerPort;
import de.burger.forensics.analytics.services.analysisstore.application.port.RepositoryAnalysisWorkerPort.WorkerDiagnostic;
import de.burger.forensics.analytics.services.analysisstore.application.port.SourceFactArtifactReaderPort;
import de.burger.forensics.analytics.services.analysisstore.application.port.WorkerOwnerApiUnavailableException;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJobId;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisWorkerKind;
import de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain;
import de.burger.forensics.analytics.services.analysisstore.domain.RepositoryToBtmOrchestrationDomain;
import de.burger.forensics.analytics.services.analysisstore.domain.RepositoryToBtmOrchestrationDomain.BtmDeliveryReadiness;
import de.burger.forensics.analytics.services.analysisstore.domain.RepositoryToBtmOrchestrationDomain.DiagnosticSeverity;
import de.burger.forensics.analytics.services.analysisstore.domain.RepositoryToBtmOrchestrationDomain.OrchestrationState;
import de.burger.forensics.analytics.services.analysisstore.domain.RepositoryToBtmOrchestrationDomain.RepositoryToBtmDiagnostic;
import de.burger.forensics.analytics.services.analysisstore.domain.RepositoryToBtmOrchestrationDomain.RepositoryToBtmOrchestrationStatus;
import de.burger.forensics.analytics.services.analysisstore.domain.RepositoryToBtmOrchestrationDomain.StartRepositoryToBtmCommand;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisRunId;
import de.burger.forensics.analytics.services.analysisstore.domain.SourceSnapshotId;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class RepositoryToBtmOrchestrationApplicationService {
    private static final int DEFAULT_MAX_TARGETS = 10_000;
    private static final String TARGET_POLICY_VERSION = "repository-to-btm-target-policy-v1";

    private static final List<RepositoryToBtmDiagnostic> PENDING_REPOSITORY_DIAGNOSTICS = List.of(
        RepositoryToBtmDiagnostic.warning(
            "REPOSITORY_SOURCE_PACKAGE_UNAVAILABLE",
            "Repository source package is not available yet; Repository Analysis must complete before Java AST and Joern workers can run.",
            true
        ),
        RepositoryToBtmDiagnostic.warning(
            "BUILD_OUTPUT_PACKAGE_UNAVAILABLE",
            "Build output package is not available yet; Artifactory, Jenkins or build-artifact-worker must provide it before bytecode-aware analysis can run.",
            true
        ),
        RepositoryToBtmDiagnostic.warning(
            "JOERN_SKIPPED_UNAVAILABLE_PACKAGE",
            "Joern analysis is skipped until source and build-output packages are AVAILABLE and COMPLETE.",
            true
        )
    );

    private final AnalysisJobApplicationService jobs;
    private final InstrumentationTargetPlanningApplicationService targetPlanningService;
    private final RepositoryAnalysisWorkerPort repositoryAnalysisWorker;
    private final SourceFactArtifactReaderPort sourceFactArtifactReader;
    private final JoernSemanticAnalysisPort joernSemanticAnalysis;
    private final BtmGenerationWorkerPort btmGenerationWorker;
    private final Map<String, StoredStart> starts = new ConcurrentHashMap<>();
    private final Map<AnalysisRunId, RepositoryToBtmOrchestrationStatus> statuses = new ConcurrentHashMap<>();

    public RepositoryToBtmOrchestrationApplicationService(AnalysisJobApplicationService jobs) {
        this(
            jobs,
            new InstrumentationTargetPlanningApplicationService(jobs),
            RepositoryAnalysisWorkerPort.unavailable(),
            SourceFactArtifactReaderPort.unavailable(),
            JoernSemanticAnalysisPort.unavailable(),
            BtmGenerationWorkerPort.unavailable()
        );
    }

    public RepositoryToBtmOrchestrationApplicationService(
        AnalysisJobApplicationService jobs,
        InstrumentationTargetPlanningApplicationService targetPlanningService,
        RepositoryAnalysisWorkerPort repositoryAnalysisWorker,
        SourceFactArtifactReaderPort sourceFactArtifactReader,
        JoernSemanticAnalysisPort joernSemanticAnalysis,
        BtmGenerationWorkerPort btmGenerationWorker
    ) {
        this.jobs = Objects.requireNonNull(jobs, "jobs must not be null");
        this.targetPlanningService = Objects.requireNonNull(targetPlanningService, "targetPlanningService must not be null");
        this.repositoryAnalysisWorker = Objects.requireNonNull(repositoryAnalysisWorker, "repositoryAnalysisWorker must not be null");
        this.sourceFactArtifactReader = Objects.requireNonNull(sourceFactArtifactReader, "sourceFactArtifactReader must not be null");
        this.joernSemanticAnalysis = Objects.requireNonNull(joernSemanticAnalysis, "joernSemanticAnalysis must not be null");
        this.btmGenerationWorker = Objects.requireNonNull(btmGenerationWorker, "btmGenerationWorker must not be null");
    }

    public synchronized RepositoryToBtmOrchestrationStatus start(
        String idempotencyKey,
        StartRepositoryToBtmCommand command
    ) {
        var verifiedCommand = Objects.requireNonNull(command, "command must not be null");
        return idempotent(idempotencyKey, verifiedCommand.fingerprint(), () -> createStart(verifiedCommand));
    }

    public synchronized RepositoryToBtmOrchestrationStatus status(String correlationId, AnalysisRunId analysisRunId) {
        var existing = statuses.get(analysisRunId);
        if (existing != null) {
            return withCorrelation(existing, correlationId);
        }
        var jobId = RepositoryToBtmOrchestrationDomain.repositoryAnalysisJobId(analysisRunId);
        var job = jobs.get(jobId);
        return pendingStatus(
            correlationId,
            analysisRunId,
            jobId,
            job.sourceSnapshotId(),
            Map.of("repositoryAnalysisJobState", job.state().name())
        );
    }

    private RepositoryToBtmOrchestrationStatus createStart(StartRepositoryToBtmCommand command) {
        var analysisRunId = command.metadata().analysisRunId();
        var repositoryJobId = RepositoryToBtmOrchestrationDomain.repositoryAnalysisJobId(analysisRunId);
        var astJobId = RepositoryToBtmOrchestrationDomain.astAnalysisJobId(analysisRunId);
        RepositoryAnalysisWorkerPort.RepositoryAnalysisResult repositoryAnalysis;
        try {
            repositoryAnalysis = repositoryAnalysisWorker.prepareAndAnalyzeJavaAst(command, astJobId);
        } catch (WorkerOwnerApiUnavailableException error) {
            return repositoryOwnerUnavailableStatus(command, repositoryJobId);
        }
        submitJob(
            command,
            "repository",
            repositoryJobId,
            AnalysisWorkerKind.REPOSITORY_ANALYSIS,
            repositoryAnalysis.sourceSnapshotId(),
            List.of(),
            repositoryAnalysis.completeness(),
            Map.of("owner", "analysis-store-service", "orchestration", "repository-to-btm")
        );
        submitJob(
            command,
            "java-ast",
            astJobId,
            AnalysisWorkerKind.AST_ANALYSIS,
            repositoryAnalysis.sourceSnapshotId(),
            List.of(),
            repositoryAnalysis.completeness(),
            Map.of("owner", "analysis-store-service", "orchestration", "repository-to-btm")
        );
        jobs.registerArtifacts(
            operationKey(command, "register-java-ast-artifact"),
            command.metadata().correlationId(),
            analysisRunId,
            astJobId,
            List.of(repositoryAnalysis.sourceFactArtifact())
        );

        SourceFactArtifactReaderPort.SourceFactArtifact sourceFacts;
        try {
            sourceFacts = sourceFactArtifactReader.readFacts(
                analysisRunId,
                astJobId,
                repositoryAnalysis.sourceSnapshotId(),
                command.metadata().requestId() + "-source-facts",
                command.metadata().correlationId(),
                repositoryAnalysis.sourceFactArtifact(),
                command.attributes()
            );
        } catch (EvidenceArtifactIntegrityException error) {
            return artifactIntegrityFailureStatus(command, repositoryJobId, astJobId, repositoryAnalysis.sourceSnapshotId(), error);
        } catch (WorkerOwnerApiUnavailableException error) {
            return ownerUnavailableStatus(
                command,
                repositoryJobId,
                repositoryAnalysis.sourceSnapshotId(),
                true,
                "JAVA_AST_SOURCE_FACT_OWNER_API_UNAVAILABLE",
                "Java AST source fact artifacts are not readable because the Java AST owner API is unavailable.",
                attributes(repositoryJobId, astJobId, null, null, 0, 0, 0)
            );
        }
        var diagnostics = new ArrayList<RepositoryToBtmDiagnostic>();
        repositoryAnalysis.diagnostics().stream().map(RepositoryToBtmOrchestrationApplicationService::diagnostic)
            .forEach(diagnostics::add);
        sourceFacts.diagnostics().stream()
            .map(RepositoryToBtmOrchestrationApplicationService::diagnostic)
            .forEach(diagnostics::add);

        var joern = analyzeWithJoernIfReady(command, repositoryAnalysis, sourceFacts.artifact(), diagnostics);
        diagnostics.addAll(joern.diagnostics());

        var btmJobId = RepositoryToBtmOrchestrationDomain.btmGenerationJobId(analysisRunId);
        var btmInputArtifacts = concat(List.of(sourceFacts.artifact()), joern.semanticArtifacts());
        submitJob(
            command,
            "btm-generation",
            btmJobId,
            AnalysisWorkerKind.BTM_GENERATION,
            repositoryAnalysis.sourceSnapshotId(),
            btmInputArtifacts,
            sourceFacts.completeness(),
            Map.of("owner", "analysis-store-service", "orchestration", "repository-to-btm")
        );
        var targetPlan = targetPlanningService.plan(
            operationKey(command, "plan-targets"),
            targetPlanningCommand(command, btmJobId, repositoryAnalysis.sourceSnapshotId(), sourceFacts, joern.semanticArtifacts())
        );
        targetPlan.diagnostics().stream()
            .map(RepositoryToBtmOrchestrationApplicationService::diagnostic)
            .forEach(diagnostics::add);
        if (targetPlan.targets().isEmpty()) {
            diagnostics.add(new RepositoryToBtmDiagnostic(
                "BTM_TARGETS_UNAVAILABLE",
                "BTM generation is unavailable because no accepted instrumentation targets were produced.",
                DiagnosticSeverity.ERROR,
                false,
                true
            ));
            var status = status(
                command.metadata().correlationId(),
                analysisRunId,
                repositoryJobId,
                repositoryAnalysis.sourceSnapshotId(),
                AnalysisCompleteness.INCOMPLETE,
                OrchestrationState.INCOMPLETE,
                BtmDeliveryReadiness.UNAVAILABLE,
                joern.skipped(),
                diagnostics,
                List.of(),
                attributes(repositoryJobId, astJobId, null, btmJobId, sourceFacts.facts().size(), 0, 0)
            );
            statuses.put(analysisRunId, status);
            return status;
        }

        BtmGenerationWorkerPort.BtmGenerationResult generated;
        try {
            generated = btmGenerationWorker.generate(
                command,
                btmJobId,
                repositoryAnalysis.sourceSnapshotId(),
                List.of(sourceFacts.artifact()),
                joern.semanticArtifacts(),
                targetPlan.completeness(),
                targetPlan.selection(),
                targetPlan.targets()
            );
        } catch (WorkerOwnerApiUnavailableException error) {
            diagnostics.add(new RepositoryToBtmDiagnostic(
                "BTM_GENERATION_OWNER_API_UNAVAILABLE",
                "BTM rule files are not deliverable because the BTM Generation owner API is unavailable.",
                DiagnosticSeverity.WARNING,
                true,
                true
            ));
            var status = status(
                command.metadata().correlationId(),
                analysisRunId,
                repositoryJobId,
                repositoryAnalysis.sourceSnapshotId(),
                AnalysisCompleteness.INCOMPLETE,
                OrchestrationState.INCOMPLETE,
                BtmDeliveryReadiness.NOT_READY,
                joern.skipped(),
                diagnostics,
                List.of(),
                attributes(repositoryJobId, astJobId, joern.jobId(), btmJobId, sourceFacts.facts().size(), targetPlan.targets().size(), 0)
            );
            statuses.put(analysisRunId, status);
            return status;
        }
        generated.diagnostics().stream()
            .map(RepositoryToBtmOrchestrationApplicationService::diagnostic)
            .forEach(diagnostics::add);
        jobs.registerArtifacts(
            operationKey(command, "register-btm-artifacts"),
            command.metadata().correlationId(),
            analysisRunId,
            btmJobId,
            generated.generatedArtifacts()
        );
        var status = status(
            command.metadata().correlationId(),
            analysisRunId,
            repositoryJobId,
            repositoryAnalysis.sourceSnapshotId(),
            completeness(diagnostics, generated.completeness()),
            generated.generatedArtifacts().isEmpty() ? OrchestrationState.INCOMPLETE : OrchestrationState.READY_FOR_BTM_DELIVERY,
            generated.generatedArtifacts().isEmpty() ? BtmDeliveryReadiness.UNAVAILABLE : BtmDeliveryReadiness.READY,
            joern.skipped(),
            diagnostics,
            generated.generatedArtifacts(),
            attributes(
                repositoryJobId,
                astJobId,
                joern.jobId(),
                btmJobId,
                sourceFacts.facts().size(),
                targetPlan.targets().size(),
                generated.generatedArtifacts().size()
            )
        );
        statuses.put(analysisRunId, status);
        return status;
    }

    private RepositoryToBtmOrchestrationStatus repositoryOwnerUnavailableStatus(
        StartRepositoryToBtmCommand command,
        AnalysisJobId repositoryJobId
    ) {
        submitPendingRepositoryJob(command, repositoryJobId);
        var diagnostics = new ArrayList<>(PENDING_REPOSITORY_DIAGNOSTICS);
        diagnostics.add(new RepositoryToBtmDiagnostic(
            "REPOSITORY_ANALYSIS_OWNER_API_UNAVAILABLE",
            "Repository-to-BTM orchestration is waiting because the Repository Analysis owner API is unavailable.",
            DiagnosticSeverity.WARNING,
            true,
            true
        ));
        var status = status(
            command.metadata().correlationId(),
            command.metadata().analysisRunId(),
            repositoryJobId,
            RepositoryToBtmOrchestrationDomain.pendingSourceSnapshotId(command.metadata().analysisRunId()),
            AnalysisCompleteness.INCOMPLETE,
            OrchestrationState.WAITING_FOR_REPOSITORY,
            BtmDeliveryReadiness.NOT_READY,
            true,
            diagnostics,
            List.of(),
            Map.of("repositoryAnalysisJobState", "DISPATCHABLE")
        );
        statuses.put(command.metadata().analysisRunId(), status);
        return status;
    }

    private RepositoryToBtmOrchestrationStatus artifactIntegrityFailureStatus(
        StartRepositoryToBtmCommand command,
        AnalysisJobId repositoryJobId,
        AnalysisJobId astJobId,
        SourceSnapshotId sourceSnapshotId,
        EvidenceArtifactIntegrityException error
    ) {
        var diagnostics = List.of(new RepositoryToBtmDiagnostic(
            error.diagnosticCode(),
            error.getMessage(),
            DiagnosticSeverity.ERROR,
            false,
            true
        ));
        var status = status(
            command.metadata().correlationId(),
            command.metadata().analysisRunId(),
            repositoryJobId,
            sourceSnapshotId,
            AnalysisCompleteness.INCOMPLETE,
            OrchestrationState.FAILED,
            BtmDeliveryReadiness.UNAVAILABLE,
            true,
            diagnostics,
            List.of(),
            attributes(repositoryJobId, astJobId, null, null, 0, 0, 0)
        );
        statuses.put(command.metadata().analysisRunId(), status);
        return status;
    }

    private RepositoryToBtmOrchestrationStatus ownerUnavailableStatus(
        StartRepositoryToBtmCommand command,
        AnalysisJobId repositoryJobId,
        SourceSnapshotId sourceSnapshotId,
        boolean joernSkipped,
        String diagnosticCode,
        String diagnosticMessage,
        Map<String, String> attributes
    ) {
        var diagnostics = List.of(new RepositoryToBtmDiagnostic(
            diagnosticCode,
            diagnosticMessage,
            DiagnosticSeverity.WARNING,
            true,
            true
        ));
        var status = status(
            command.metadata().correlationId(),
            command.metadata().analysisRunId(),
            repositoryJobId,
            sourceSnapshotId,
            AnalysisCompleteness.INCOMPLETE,
            OrchestrationState.INCOMPLETE,
            BtmDeliveryReadiness.NOT_READY,
            joernSkipped,
            diagnostics,
            List.of(),
            attributes
        );
        statuses.put(command.metadata().analysisRunId(), status);
        return status;
    }

    private void submitPendingRepositoryJob(StartRepositoryToBtmCommand command, AnalysisJobId repositoryJobId) {
        submitJob(
            command,
            "repository-pending",
            repositoryJobId,
            AnalysisWorkerKind.REPOSITORY_ANALYSIS,
            RepositoryToBtmOrchestrationDomain.pendingSourceSnapshotId(command.metadata().analysisRunId()),
            List.of(),
            AnalysisCompleteness.UNKNOWN,
            Map.of(
                "owner", "analysis-store-service",
                "orchestration", "repository-to-btm",
                "requestedOutput", "BTM_RULES"
            )
        );
    }

    private void submitJob(
        StartRepositoryToBtmCommand command,
        String keySuffix,
        AnalysisJobId jobId,
        AnalysisWorkerKind workerKind,
        SourceSnapshotId sourceSnapshotId,
        List<AnalysisArtifactReference> inputArtifacts,
        AnalysisCompleteness inputCompleteness,
        Map<String, String> attributes
    ) {
        jobs.submit(
            operationKey(command, "submit-" + keySuffix),
            command.metadata().correlationId(),
            command.metadata().analysisRunId(),
            jobId,
            command.metadata().schemaVersion(),
            workerKind,
            sourceSnapshotId,
            inputArtifacts,
            inputCompleteness,
            attributes
        );
    }

    private JoernOutcome analyzeWithJoernIfReady(
        StartRepositoryToBtmCommand command,
        RepositoryAnalysisWorkerPort.RepositoryAnalysisResult repositoryAnalysis,
        AnalysisArtifactReference sourceFactArtifact,
        List<RepositoryToBtmDiagnostic> diagnostics
    ) {
        if (!repositoryAnalysis.hasJoernReadyPackages()) {
            return new JoernOutcome(null, true, List.of(), List.of(RepositoryToBtmDiagnostic.warning(
                "JOERN_SKIPPED_UNAVAILABLE_PACKAGE",
                "Joern analysis is skipped until source and build-output packages are AVAILABLE and COMPLETE.",
                true
            )));
        }
        var joernJobId = RepositoryToBtmOrchestrationDomain.joernAnalysisJobId(command.metadata().analysisRunId());
        submitJob(
            command,
            "joern",
            joernJobId,
            AnalysisWorkerKind.JOERN_ANALYSIS,
            repositoryAnalysis.sourceSnapshotId(),
            List.of(sourceFactArtifact),
            repositoryAnalysis.completeness(),
            Map.of("owner", "analysis-store-service", "orchestration", "repository-to-btm")
        );
        try {
            var result = joernSemanticAnalysis.analyze(command, joernJobId, repositoryAnalysis);
            jobs.registerArtifacts(
                operationKey(command, "register-joern-artifacts"),
                command.metadata().correlationId(),
                command.metadata().analysisRunId(),
                joernJobId,
                result.semanticArtifacts()
            );
            return new JoernOutcome(joernJobId, false, result.semanticArtifacts(), result.diagnostics().stream()
                .map(RepositoryToBtmOrchestrationApplicationService::diagnostic)
                .toList());
        } catch (WorkerOwnerApiUnavailableException error) {
            diagnostics.add(new RepositoryToBtmDiagnostic(
                "JOERN_OWNER_API_UNAVAILABLE",
                "Joern analysis is unavailable; BTM generation continues with accepted static source facts.",
                DiagnosticSeverity.WARNING,
                true,
                true
            ));
            return new JoernOutcome(joernJobId, true, List.of(), List.of());
        }
    }

    private static InstrumentationTargetPlanningDomain.PlanInstrumentationTargetsCommand targetPlanningCommand(
        StartRepositoryToBtmCommand command,
        AnalysisJobId btmJobId,
        SourceSnapshotId sourceSnapshotId,
        SourceFactArtifactReaderPort.SourceFactArtifact sourceFacts,
        List<AnalysisArtifactReference> semanticArtifacts
    ) {
        return new InstrumentationTargetPlanningDomain.PlanInstrumentationTargetsCommand(
            new InstrumentationTargetPlanningDomain.TargetPlanningMetadata(
                command.metadata().requestId() + "-target-plan",
                command.metadata().schemaVersion(),
                command.metadata().correlationId(),
                command.metadata().analysisRunId(),
                btmJobId,
                sourceSnapshotId,
                command.attributes()
            ),
            TARGET_POLICY_VERSION,
            new InstrumentationTargetPlanningDomain.InstrumentationTargetPolicy(
                DEFAULT_MAX_TARGETS,
                List.of(InstrumentationTargetPlanningDomain.ProbeKind.METHOD_ENTRY),
                false,
                "source-code"
            ),
            sourceFacts.facts(),
            List.of(sourceFacts.artifact()),
            semanticArtifacts
        );
    }

    private static RepositoryToBtmOrchestrationStatus pendingStatus(
        String correlationId,
        AnalysisRunId analysisRunId,
        AnalysisJobId repositoryJobId,
        SourceSnapshotId sourceSnapshotId,
        Map<String, String> attributes
    ) {
        return status(
            correlationId,
            analysisRunId,
            repositoryJobId,
            sourceSnapshotId,
            AnalysisCompleteness.INCOMPLETE,
            OrchestrationState.WAITING_FOR_REPOSITORY,
            BtmDeliveryReadiness.NOT_READY,
            true,
            PENDING_REPOSITORY_DIAGNOSTICS,
            List.of(),
            attributes
        );
    }

    private static RepositoryToBtmOrchestrationStatus status(
        String correlationId,
        AnalysisRunId analysisRunId,
        AnalysisJobId repositoryJobId,
        SourceSnapshotId sourceSnapshotId,
        AnalysisCompleteness completeness,
        OrchestrationState state,
        BtmDeliveryReadiness readiness,
        boolean joernSkipped,
        List<RepositoryToBtmDiagnostic> diagnostics,
        List<AnalysisArtifactReference> generatedArtifacts,
        Map<String, String> attributes
    ) {
        return new RepositoryToBtmOrchestrationStatus(
            RepositoryToBtmOrchestrationDomain.OperationStatus.accepted(correlationId, diagnostics),
            analysisRunId,
            repositoryJobId,
            sourceSnapshotId,
            completeness,
            state,
            readiness,
            joernSkipped,
            diagnostics,
            generatedArtifacts,
            attributes
        );
    }

    private static RepositoryToBtmOrchestrationStatus withCorrelation(
        RepositoryToBtmOrchestrationStatus existing,
        String correlationId
    ) {
        return status(
            correlationId,
            existing.analysisRunId(),
            existing.repositoryAnalysisJobId(),
            existing.sourceSnapshotId(),
            existing.completeness(),
            existing.state(),
            existing.btmDeliveryReadiness(),
            existing.joernSkipped(),
            existing.diagnostics(),
            existing.acceptedGeneratedArtifacts(),
            existing.attributes()
        );
    }

    private static AnalysisCompleteness completeness(
        List<RepositoryToBtmDiagnostic> diagnostics,
        AnalysisCompleteness generatedCompleteness
    ) {
        return diagnostics.stream().anyMatch(RepositoryToBtmDiagnostic::affectsCompleteness)
            || generatedCompleteness != AnalysisCompleteness.COMPLETE
                ? AnalysisCompleteness.INCOMPLETE
                : AnalysisCompleteness.COMPLETE;
    }

    private static RepositoryToBtmDiagnostic diagnostic(WorkerDiagnostic diagnostic) {
        return new RepositoryToBtmDiagnostic(
            diagnostic.code(),
            diagnostic.message(),
            switch (diagnostic.severity()) {
                case INFO -> DiagnosticSeverity.INFO;
                case WARNING -> DiagnosticSeverity.WARNING;
                case ERROR -> DiagnosticSeverity.ERROR;
            },
            diagnostic.retryable(),
            diagnostic.affectsCompleteness()
        );
    }

    private static RepositoryToBtmDiagnostic diagnostic(SourceFactArtifactReaderPort.SourceFactDiagnostic diagnostic) {
        return new RepositoryToBtmDiagnostic(
            diagnostic.code(),
            diagnostic.message(),
            DiagnosticSeverity.WARNING,
            false,
            diagnostic.affectsCompleteness()
        );
    }

    private static RepositoryToBtmDiagnostic diagnostic(
        InstrumentationTargetPlanningDomain.TargetPlanningDiagnostic diagnostic
    ) {
        return new RepositoryToBtmDiagnostic(
            diagnostic.code(),
            diagnostic.message(),
            switch (diagnostic.severity()) {
                case INFO -> DiagnosticSeverity.INFO;
                case WARNING -> DiagnosticSeverity.WARNING;
                case ERROR -> DiagnosticSeverity.ERROR;
            },
            diagnostic.retryable(),
            diagnostic.affectsCompleteness()
        );
    }

    private static Map<String, String> attributes(
        AnalysisJobId repositoryJobId,
        AnalysisJobId astJobId,
        AnalysisJobId joernJobId,
        AnalysisJobId btmJobId,
        int sourceFactCount,
        int targetCount,
        int generatedArtifactCount
    ) {
        var attributes = new LinkedHashMap<String, String>();
        attributes.put("repositoryAnalysisJobId", repositoryJobId.value());
        attributes.put("javaAstAnalysisJobId", astJobId.value());
        if (joernJobId != null) {
            attributes.put("joernAnalysisJobId", joernJobId.value());
        }
        if (btmJobId != null) {
            attributes.put("btmGenerationJobId", btmJobId.value());
        }
        attributes.put("sourceFactCount", Integer.toString(sourceFactCount));
        attributes.put("targetCount", Integer.toString(targetCount));
        attributes.put("generatedArtifactCount", Integer.toString(generatedArtifactCount));
        return Map.copyOf(attributes);
    }

    private static String operationKey(StartRepositoryToBtmCommand command, String operation) {
        return "repository-to-btm:" + operation + ":" + command.metadata().requestId();
    }

    private static List<AnalysisArtifactReference> concat(
        List<AnalysisArtifactReference> first,
        List<AnalysisArtifactReference> second
    ) {
        var combined = new ArrayList<AnalysisArtifactReference>();
        combined.addAll(first);
        combined.addAll(second);
        return List.copyOf(combined);
    }

    private RepositoryToBtmOrchestrationStatus idempotent(
        String idempotencyKey,
        String fingerprint,
        Supplier<RepositoryToBtmOrchestrationStatus> supplier
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
        var key = idempotencyKey.strip();
        var existing = starts.get(key);
        if (existing != null) {
            if (!existing.fingerprint().equals(fingerprint)) {
                throw new IdempotencyConflictException(key);
            }
            return existing.status();
        }
        var status = supplier.get();
        starts.put(key, new StoredStart(fingerprint, status));
        return status;
    }

    private record StoredStart(String fingerprint, RepositoryToBtmOrchestrationStatus status) {
    }

    private record JoernOutcome(
        AnalysisJobId jobId,
        boolean skipped,
        List<AnalysisArtifactReference> semanticArtifacts,
        List<RepositoryToBtmDiagnostic> diagnostics
    ) {
        private JoernOutcome {
            semanticArtifacts = List.copyOf(Objects.requireNonNullElse(semanticArtifacts, List.of()));
            diagnostics = List.copyOf(Objects.requireNonNullElse(diagnostics, List.of()));
        }
    }
}
