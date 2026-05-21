package de.burger.forensics.analytics.services.analysisstore.application;

import de.burger.forensics.analytics.services.analysisstore.adapter.out.memory.InMemoryAnalysisJobRepository;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactCategory;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJobId;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisRunId;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisWorkerKind;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactByteAccess;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactByteCustody;
import de.burger.forensics.analytics.services.analysisstore.domain.ArtifactReference;
import de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.AcceptedStaticSourceFact;
import de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.InstrumentationTargetPolicy;
import de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.PlanInstrumentationTargetsCommand;
import de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.ProbeKind;
import de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.StaticSourceLocation;
import de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.TargetPlanningMetadata;
import de.burger.forensics.analytics.services.analysisstore.domain.SourceSnapshotId;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstrumentationTargetPlanningApplicationServiceTest {
    private final AnalysisJobApplicationService analysisJobs = new AnalysisJobApplicationService(
        new InMemoryAnalysisJobRepository(),
        Clock.fixed(Instant.parse("2026-05-17T10:15:30Z"), ZoneOffset.UTC)
    );
    private final InstrumentationTargetPlanningApplicationService service = new InstrumentationTargetPlanningApplicationService(analysisJobs);

    @Test
    void plansDeterministicTargetsFromAcceptedStaticFacts() {
        submitPlanningJob(defaultArtifacts());
        var command = command(
            false,
            fact("fact-b", "src/main/java/b/B.java", "b.B", "run", 20, "java-ast/source-facts.json", AnalysisCompleteness.COMPLETE),
            fact("fact-a", "src/main/java/a/A.java", "a.A", "run", 10, "java-ast/source-facts.json", AnalysisCompleteness.COMPLETE)
        );

        var result = service.plan("plan-key", command);
        var sameResult = service.plan("plan-key", commandWithRequestId("retry-request", false,
            fact("fact-b", "src/main/java/b/B.java", "b.B", "run", 20, "java-ast/source-facts.json", AnalysisCompleteness.COMPLETE),
            fact("fact-a", "src/main/java/a/A.java", "a.A", "run", 10, "java-ast/source-facts.json", AnalysisCompleteness.COMPLETE)
        ));

        assertSame(result, sameResult);
        assertEquals(AnalysisCompleteness.COMPLETE, result.completeness());
        assertEquals("analysis-store-service", result.selection().ownerService());
        assertEquals("target-policy-v1", result.selection().policyVersion());
        assertEquals("correlation-1", result.selection().correlationId());
        assertEquals(2, result.selection().targetCount());
        assertEquals(0, result.targets().getFirst().orderIndex());
        assertEquals("fact-a", result.targets().getFirst().sourceFactId());
        assertEquals("java-ast/source-facts.json", result.targets().getFirst().sourceFactArtifactReference());
        assertEquals("", result.targets().getFirst().semanticNodeId());
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    void keepsSemanticMappingGapsIncompleteWithoutInventingSemanticNodeIds() {
        submitPlanningJob(defaultArtifacts());
        var command = command(
            true,
            fact("fact-a", "src/main/java/a/A.java", "a.A", "run", 10, "java-ast/source-facts.json", AnalysisCompleteness.COMPLETE)
        );

        var result = service.plan("plan-semantic-key", command);

        assertEquals(AnalysisCompleteness.INCOMPLETE, result.completeness());
        assertEquals(AnalysisCompleteness.INCOMPLETE, result.targets().getFirst().completeness());
        assertEquals("", result.targets().getFirst().semanticNodeId());
        assertEquals("", result.targets().getFirst().semanticArtifactReference());
        assertEquals(List.of("SEMANTIC_NODE_MAPPING_UNAVAILABLE"), result.diagnostics().stream()
            .map(diagnostic -> diagnostic.code())
            .toList());
        assertTrue(result.diagnostics().getFirst().affectsCompleteness());
    }

    @Test
    void rejectsConflictingIdempotencyAndMissingAcceptedArtifactReferences() {
        submitPlanningJob(defaultArtifacts());
        var command = command(
            false,
            fact("fact-a", "src/main/java/a/A.java", "a.A", "run", 10, "java-ast/source-facts.json", AnalysisCompleteness.COMPLETE)
        );
        var missingArtifact = new PlanInstrumentationTargetsCommand(
            metadata(),
            "target-policy-v1",
            policy(false),
            List.of(fact("fact-a", "src/main/java/a/A.java", "a.A", "run", 10, "java-ast/missing.json", AnalysisCompleteness.COMPLETE)),
            List.of(artifact("java-ast/source-facts.json", AnalysisCompleteness.COMPLETE)),
            List.of(artifact("joern/semantic.json", AnalysisCompleteness.COMPLETE))
        );

        service.plan("plan-key", command);

        assertThrows(IdempotencyConflictException.class, () -> service.plan("plan-key", command(
            false,
            fact("fact-b", "src/main/java/b/B.java", "b.B", "run", 20, "java-ast/source-facts.json", AnalysisCompleteness.COMPLETE)
        )));

        var result = service.plan("missing-artifact-key", missingArtifact);
        assertEquals(AnalysisCompleteness.UNKNOWN, result.completeness());
        assertEquals(0, result.targets().size());
        assertEquals("SOURCE_FACT_ARTIFACT_NOT_ACCEPTED", result.diagnostics().getFirst().code());
    }

    @Test
    void recordsIncompleteInputsAndTargetLimitAsDiagnostics() {
        submitPlanningJob(List.of(artifact("java-ast/source-facts.json", AnalysisCompleteness.INCOMPLETE)));
        var command = new PlanInstrumentationTargetsCommand(
            metadata(),
            "target-policy-v1",
            new InstrumentationTargetPolicy(1, List.of(ProbeKind.METHOD_ENTRY, ProbeKind.THROW), false, "source-code"),
            List.of(
                fact("fact-a", "src/main/java/a/A.java", "a.A", "run", 10, "java-ast/source-facts.json", AnalysisCompleteness.INCOMPLETE),
                fact("fact-b", "src/main/java/b/B.java", "b.B", "run", 20, "java-ast/source-facts.json", AnalysisCompleteness.COMPLETE)
            ),
            List.of(artifact("java-ast/source-facts.json", AnalysisCompleteness.INCOMPLETE)),
            List.of()
        );

        var result = service.plan("limit-key", command);

        assertEquals(1, result.targets().size());
        assertEquals(AnalysisCompleteness.INCOMPLETE, result.completeness());
        assertFalse(result.diagnostics().isEmpty());
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals("SOURCE_FACT_ARTIFACT_INCOMPLETE")));
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals("STATIC_FACT_INCOMPLETE")));
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals("TARGET_LIMIT_EXCEEDED")));
    }

    @Test
    void recordsTargetLimitWhenRemainingFactsAreSkipped() {
        submitPlanningJob(defaultArtifacts());
        var command = new PlanInstrumentationTargetsCommand(
            metadata(),
            "target-policy-v1",
            new InstrumentationTargetPolicy(1, List.of(ProbeKind.METHOD_ENTRY), false, "source-code"),
            List.of(
                fact("fact-a", "src/main/java/a/A.java", "a.A", "run", 10, "java-ast/source-facts.json", AnalysisCompleteness.COMPLETE),
                fact("fact-b", "src/main/java/b/B.java", "b.B", "run", 20, "java-ast/source-facts.json", AnalysisCompleteness.COMPLETE)
            ),
            List.of(artifact("java-ast/source-facts.json", AnalysisCompleteness.COMPLETE)),
            List.of()
        );

        var result = service.plan("limit-after-fact-key", command);

        assertEquals(1, result.targets().size());
        assertEquals(AnalysisCompleteness.INCOMPLETE, result.completeness());
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals("TARGET_LIMIT_EXCEEDED")));
    }

    @Test
    void rejectsUnregisteredRequestArtifactsAsIncompleteInput() {
        submitPlanningJob(defaultArtifacts());
        var command = new PlanInstrumentationTargetsCommand(
            metadata(),
            "target-policy-v1",
            policy(false),
            List.of(fact("fact-a", "src/main/java/a/A.java", "a.A", "run", 10, "java-ast/forged-source-facts.json", AnalysisCompleteness.COMPLETE)),
            List.of(artifact("java-ast/forged-source-facts.json", AnalysisCompleteness.COMPLETE)),
            List.of()
        );

        var result = service.plan("forged-artifact-key", command);

        assertEquals(AnalysisCompleteness.UNKNOWN, result.completeness());
        assertEquals(0, result.targets().size());
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals("SOURCE_FACT_ARTIFACT_NOT_ACCEPTED")));
    }

    @Test
    void validatesStoredJobEnvelopeAndIdempotencyKey() {
        submitPlanningJob(defaultArtifacts());
        var mismatchedRun = new PlanInstrumentationTargetsCommand(
            new TargetPlanningMetadata(
                "request-1",
                "schema-v1",
                "correlation-1",
                new AnalysisRunId("other-run"),
                new AnalysisJobId("job-1"),
                new SourceSnapshotId("snapshot-1"),
                Map.of("tenant", "demo")
            ),
            "target-policy-v1",
            policy(false),
            List.of(fact("fact-a", "src/main/java/a/A.java", "a.A", "run", 10, "java-ast/source-facts.json", AnalysisCompleteness.COMPLETE)),
            List.of(artifact("java-ast/source-facts.json", AnalysisCompleteness.COMPLETE)),
            List.of()
        );
        var mismatchedSnapshot = new PlanInstrumentationTargetsCommand(
            new TargetPlanningMetadata(
                "request-1",
                "schema-v1",
                "correlation-1",
                new AnalysisRunId("run-1"),
                new AnalysisJobId("job-1"),
                new SourceSnapshotId("other-snapshot"),
                Map.of("tenant", "demo")
            ),
            "target-policy-v1",
            policy(false),
            List.of(fact("fact-a", "src/main/java/a/A.java", "a.A", "run", 10, "java-ast/source-facts.json", AnalysisCompleteness.COMPLETE)),
            List.of(artifact("java-ast/source-facts.json", AnalysisCompleteness.COMPLETE)),
            List.of()
        );

        assertThrows(IllegalArgumentException.class, () -> service.plan("mismatched-run-key", mismatchedRun));
        assertThrows(IllegalArgumentException.class, () -> service.plan("mismatched-snapshot-key", mismatchedSnapshot));
        assertThrows(IllegalArgumentException.class, () -> service.plan("", command(
            false,
            fact("fact-a", "src/main/java/a/A.java", "a.A", "run", 10, "java-ast/source-facts.json", AnalysisCompleteness.COMPLETE)
        )));
    }

    @Test
    void recordsEmptyFactsUnsupportedFactsAndIncompleteSemanticArtifacts() {
        submitPlanningJob(List.of(
            artifact("java-ast/source-facts.json", AnalysisCompleteness.COMPLETE),
            artifact("joern/semantic.json", AnalysisCompleteness.INCOMPLETE)
        ));
        var emptyFacts = new PlanInstrumentationTargetsCommand(
            metadata(),
            "target-policy-v1",
            policy(false),
            List.of(),
            List.of(artifact("java-ast/source-facts.json", AnalysisCompleteness.COMPLETE)),
            List.of()
        );
        var unsupportedFact = new PlanInstrumentationTargetsCommand(
            metadata(),
            "target-policy-v1",
            policy(false),
            List.of(unsupportedFact("fact-a", "java-class")),
            List.of(artifact("java-ast/source-facts.json", AnalysisCompleteness.COMPLETE)),
            List.of()
        );
        var incompleteSemantic = new PlanInstrumentationTargetsCommand(
            metadata(),
            "target-policy-v1",
            policy(false),
            List.of(fact("fact-a", "src/main/java/a/A.java", "a.A", "run", 10, "java-ast/source-facts.json", AnalysisCompleteness.COMPLETE)),
            List.of(artifact("java-ast/source-facts.json", AnalysisCompleteness.COMPLETE)),
            List.of(artifact("joern/semantic.json", AnalysisCompleteness.INCOMPLETE))
        );

        var emptyFactsResult = service.plan("empty-facts-key", emptyFacts);
        var unsupportedFactResult = service.plan("unsupported-fact-key", unsupportedFact);
        var incompleteSemanticResult = service.plan("incomplete-semantic-key", incompleteSemantic);

        assertEquals(AnalysisCompleteness.UNKNOWN, emptyFactsResult.completeness());
        assertTrue(emptyFactsResult.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals("NO_ACCEPTED_STATIC_FACTS")));
        assertEquals(AnalysisCompleteness.UNKNOWN, unsupportedFactResult.completeness());
        assertTrue(unsupportedFactResult.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals("UNSUPPORTED_STATIC_FACT_TYPE")));
        assertEquals(AnalysisCompleteness.INCOMPLETE, incompleteSemanticResult.completeness());
        assertTrue(incompleteSemanticResult.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals("SEMANTIC_ARTIFACT_INCOMPLETE")));
    }

    private void submitPlanningJob(List<AnalysisArtifactReference> artifacts) {
        analysisJobs.submit(
            "submit-target-planning-job",
            "correlation-1",
            new AnalysisRunId("run-1"),
            new AnalysisJobId("job-1"),
            "schema-v1",
            AnalysisWorkerKind.BTM_GENERATION,
            new SourceSnapshotId("snapshot-1"),
            artifacts,
            AnalysisCompleteness.COMPLETE,
            Map.of("tenant", "demo")
        );
    }

    private static AcceptedStaticSourceFact unsupportedFact(String factId, String factType) {
        return new AcceptedStaticSourceFact(
            factId,
            factType,
            new StaticSourceLocation("src/main/java/a/A.java", "a.A", "run", 10, 1),
            "a.A#run()",
            "java-ast/source-facts.json",
            AnalysisCompleteness.COMPLETE
        );
    }

    private static List<AnalysisArtifactReference> defaultArtifacts() {
        return List.of(
            artifact("java-ast/source-facts.json", AnalysisCompleteness.COMPLETE),
            artifact("joern/semantic.json", AnalysisCompleteness.COMPLETE)
        );
    }

    private static PlanInstrumentationTargetsCommand command(boolean requireSemanticArtifacts, AcceptedStaticSourceFact... facts) {
        return commandWithRequestId("request-1", requireSemanticArtifacts, facts);
    }

    private static PlanInstrumentationTargetsCommand commandWithRequestId(
        String requestId,
        boolean requireSemanticArtifacts,
        AcceptedStaticSourceFact... facts
    ) {
        return new PlanInstrumentationTargetsCommand(
            metadata(requestId),
            "target-policy-v1",
            policy(requireSemanticArtifacts),
            List.of(facts),
            List.of(artifact("java-ast/source-facts.json", AnalysisCompleteness.COMPLETE)),
            List.of(artifact("joern/semantic.json", AnalysisCompleteness.COMPLETE))
        );
    }

    private static TargetPlanningMetadata metadata() {
        return metadata("request-1");
    }

    private static TargetPlanningMetadata metadata(String requestId) {
        return new TargetPlanningMetadata(
            requestId,
            "schema-v1",
            "correlation-1",
            new AnalysisRunId("run-1"),
            new AnalysisJobId("job-1"),
            new SourceSnapshotId("snapshot-1"),
            Map.of("tenant", "demo")
        );
    }

    private static InstrumentationTargetPolicy policy(boolean requireSemanticArtifacts) {
        return new InstrumentationTargetPolicy(
            10,
            List.of(ProbeKind.METHOD_ENTRY),
            requireSemanticArtifacts,
            "source-code"
        );
    }

    private static AcceptedStaticSourceFact fact(
        String factId,
        String path,
        String className,
        String methodName,
        int lineNumber,
        String artifactReference,
        AnalysisCompleteness completeness
    ) {
        return new AcceptedStaticSourceFact(
            factId,
            "java-method",
            new StaticSourceLocation(path, className, methodName, lineNumber, 1),
            className + "#" + methodName + "()",
            artifactReference,
            completeness
        );
    }

    private static AnalysisArtifactReference artifact(String path, AnalysisCompleteness completeness) {
        return new AnalysisArtifactReference(
            new ArtifactReference(path, "application/json", "a".repeat(64), 42),
            AnalysisArtifactCategory.STATIC,
            "analysis-store-test",
            "schema-v1",
            completeness,
            new ArtifactByteAccess(
                "analysis-store-test",
                "analysis-job.v1.ArtifactBytes",
                "artifacts/" + path,
                ArtifactByteCustody.PRODUCER_RETAINED
            )
        );
    }
}
