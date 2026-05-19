package de.burger.forensics.analytics.services.analysisstore.domain;

import de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.AcceptedStaticSourceFact;
import de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.DiagnosticSeverity;
import de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.InstrumentationTarget;
import de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.InstrumentationTargetPolicy;
import de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.InstrumentationTargetSelection;
import de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.PlanInstrumentationTargetsCommand;
import de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.PlanInstrumentationTargetsResult;
import de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.ProbeKind;
import de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.StaticSourceLocation;
import de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.TargetPlanningDiagnostic;
import de.burger.forensics.analytics.services.analysisstore.domain.InstrumentationTargetPlanningDomain.TargetPlanningMetadata;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstrumentationTargetPlanningDomainTest {
    @Test
    void commandSortsFactsAndArtifactsDeterministically() {
        var command = command(
            List.of(
                fact("fact-b", "src/main/java/b/B.java", 20),
                fact("fact-a", "src/main/java/a/A.java", 10)
            ),
            List.of(artifact("java-ast/source-b.json"), artifact("java-ast/source-a.json")),
            List.of(artifact("joern/semantic-b.json"), artifact("joern/semantic-a.json"))
        );

        assertEquals("fact-a", command.staticFacts().getFirst().factId());
        assertEquals("java-ast/source-a.json", command.sourceFactArtifacts().getFirst().path());
        assertEquals("joern/semantic-a.json", command.semanticArtifacts().getFirst().path());
    }

    @Test
    void rejectsUnsafeReferencesUnsupportedProbeKindsAndNonStaticArtifacts() {
        var metadata = metadata();
        var policy = new InstrumentationTargetPolicy(10, List.of(ProbeKind.METHOD_ENTRY), false, "source-code");

        assertThrows(IllegalArgumentException.class, () -> new StaticSourceLocation(
            "file:/tmp/A.java",
            "a.A",
            "run",
            1,
            1
        ));
        assertThrows(IllegalArgumentException.class, () -> new InstrumentationTargetPolicy(
            10,
            List.of(ProbeKind.UNKNOWN),
            false,
            "source-code"
        ));
        assertThrows(IllegalArgumentException.class, () -> new PlanInstrumentationTargetsCommand(
            metadata,
            "target-policy-v1",
            policy,
            List.of(fact("fact-a", "src/main/java/a/A.java", 10)),
            List.of(runtimeArtifact("runtime/trace.json")),
            List.of()
        ));
        assertThrows(IllegalArgumentException.class, () -> new PlanInstrumentationTargetsCommand(
            metadata,
            "target-policy-v1",
            policy,
            List.of(fact("fact-a", "src/main/java/a/A.java", 10)),
            List.of(),
            List.of()
        ));
    }

    @Test
    void stableIdsUseExplicitInputsOnly() {
        var fact = fact("fact-a", "src/main/java/a/A.java", 10);
        var first = InstrumentationTargetPlanningDomain.stableTargetId(
            new SourceSnapshotId("snapshot-1"),
            "target-policy-v1",
            fact,
            ProbeKind.METHOD_ENTRY
        );
        var second = InstrumentationTargetPlanningDomain.stableTargetId(
            new SourceSnapshotId("snapshot-1"),
            "target-policy-v1",
            fact,
            ProbeKind.METHOD_ENTRY
        );

        assertEquals(first, second);
        assertTrue(first.startsWith("instrumentation-target:"));
    }

    @Test
    void validatesTargetPlanningValueObjects() {
        var normalizedPolicy = new InstrumentationTargetPolicy(
            10,
            List.of(ProbeKind.THROW, ProbeKind.METHOD_ENTRY, ProbeKind.THROW),
            false,
            "source-code"
        );
        var selection = new InstrumentationTargetSelection(
            "selection-1",
            "analysis-store-service",
            "target-policy-v1",
            "fingerprint-1",
            AnalysisCompleteness.COMPLETE,
            "source_path_line_signature_fact_probe_ascending",
            "correlation-1",
            1
        );
        var target = target(ProbeKind.METHOD_ENTRY, "semantic/node.json", 0, 10);
        var diagnostic = new TargetPlanningDiagnostic(
            "INFO",
            "complete",
            DiagnosticSeverity.INFO,
            new SourceSnapshotId("snapshot-1"),
            null,
            null,
            false,
            false
        );
        var result = new PlanInstrumentationTargetsResult(
            metadata(),
            AnalysisCompleteness.COMPLETE,
            selection,
            List.of(target),
            List.of(diagnostic)
        );

        assertEquals(List.of(ProbeKind.METHOD_ENTRY, ProbeKind.THROW), normalizedPolicy.probeKinds());
        assertEquals("semantic/node.json", target.semanticArtifactReference());
        assertEquals("", diagnostic.sourceFactId());
        assertEquals("", diagnostic.artifactPath());
        assertEquals(1, result.selection().targetCount());
        assertThrows(IllegalArgumentException.class, () -> new InstrumentationTargetPolicy(0, List.of(ProbeKind.METHOD_ENTRY), false, "source-code"));
        assertThrows(IllegalArgumentException.class, () -> new InstrumentationTargetPolicy(100_001, List.of(ProbeKind.METHOD_ENTRY), false, "source-code"));
        assertThrows(IllegalArgumentException.class, () -> new InstrumentationTargetPolicy(10, List.of(), false, "source-code"));
        assertThrows(IllegalArgumentException.class, () -> new StaticSourceLocation("src/main/java/a/A.java", "a.A", "run", 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new StaticSourceLocation("src/main/java/a/A.java", "a.A", "run", 1, -1));
        assertThrows(IllegalArgumentException.class, () -> new InstrumentationTargetSelection(
            "selection-1",
            "analysis-store-service",
            "target-policy-v1",
            "fingerprint-1",
            AnalysisCompleteness.COMPLETE,
            "source_path_line_signature_fact_probe_ascending",
            "correlation-1",
            -1
        ));
        assertThrows(IllegalArgumentException.class, () -> target(ProbeKind.UNKNOWN, "", 0, 10));
        assertThrows(IllegalArgumentException.class, () -> target(ProbeKind.METHOD_ENTRY, "", -1, 10));
        assertThrows(IllegalArgumentException.class, () -> target(ProbeKind.METHOD_ENTRY, "", 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new TargetPlanningMetadata(
            "request-1",
            "schema-v1",
            "correlation-1",
            new AnalysisRunId("run-1"),
            new AnalysisJobId("job-1"),
            new SourceSnapshotId("snapshot-1"),
            Map.of("token", "redacted")
        ));
        assertThrows(IllegalArgumentException.class, () -> new TargetPlanningMetadata(
            "request-1",
            "schema-v1",
            "correlation-1",
            new AnalysisRunId("run-1"),
            new AnalysisJobId("job-1"),
            new SourceSnapshotId("snapshot-1"),
            Map.of("workspace", "/tmp/repository")
        ));
    }

    static PlanInstrumentationTargetsCommand command(
        List<AcceptedStaticSourceFact> facts,
        List<AnalysisArtifactReference> sourceArtifacts,
        List<AnalysisArtifactReference> semanticArtifacts
    ) {
        return new PlanInstrumentationTargetsCommand(
            metadata(),
            "target-policy-v1",
            new InstrumentationTargetPolicy(10, List.of(ProbeKind.METHOD_ENTRY), false, "source-code"),
            facts,
            sourceArtifacts,
            semanticArtifacts
        );
    }

    static TargetPlanningMetadata metadata() {
        return new TargetPlanningMetadata(
            "request-1",
            "schema-v1",
            "correlation-1",
            new AnalysisRunId("run-1"),
            new AnalysisJobId("job-1"),
            new SourceSnapshotId("snapshot-1"),
            Map.of("tenant", "demo")
        );
    }

    static AcceptedStaticSourceFact fact(String factId, String path, int line) {
        return new AcceptedStaticSourceFact(
            factId,
            "java-method",
            new StaticSourceLocation(path, "a.A", "run", line, 1),
            "a.A#run()",
            "java-ast/source-facts.json",
            AnalysisCompleteness.COMPLETE
        );
    }

    static AnalysisArtifactReference artifact(String path) {
        return new AnalysisArtifactReference(
            new ArtifactReference(path, "application/json", "a".repeat(64), 42),
            AnalysisArtifactCategory.STATIC,
            "analysis-store-test",
            "schema-v1",
            AnalysisCompleteness.COMPLETE,
            new ArtifactByteAccess(
                "analysis-store-test",
                "analysis-job.v1.ArtifactBytes",
                "artifacts/" + path,
                ArtifactByteCustody.PRODUCER_RETAINED
            )
        );
    }

    static AnalysisArtifactReference runtimeArtifact(String path) {
        return new AnalysisArtifactReference(
            new ArtifactReference(path, "application/json", "a".repeat(64), 42),
            AnalysisArtifactCategory.RUNTIME,
            "runtime-test",
            "schema-v1",
            AnalysisCompleteness.COMPLETE,
            new ArtifactByteAccess(
                "runtime-test",
                "analysis-job.v1.ArtifactBytes",
                "artifacts/" + path,
                ArtifactByteCustody.PRODUCER_RETAINED
            )
        );
    }

    private static InstrumentationTarget target(
        ProbeKind probeKind,
        String semanticArtifactReference,
        int orderIndex,
        int lineNumber
    ) {
        return new InstrumentationTarget(
            "target-1",
            "fact-1",
            "semantic-1",
            "src/main/java/a/A.java",
            "a.A",
            "run",
            "a.A#run()",
            lineNumber,
            probeKind,
            "java-ast/source-facts.json",
            semanticArtifactReference,
            orderIndex,
            AnalysisCompleteness.COMPLETE,
            "source-code"
        );
    }
}
