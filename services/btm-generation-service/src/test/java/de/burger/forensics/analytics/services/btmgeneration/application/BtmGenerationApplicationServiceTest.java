package de.burger.forensics.analytics.services.btmgeneration.application;

import de.burger.forensics.analytics.services.btmgeneration.adapter.out.filesystem.FileSystemBtmArtifactWriter;
import de.burger.forensics.analytics.services.btmgeneration.application.port.BtmArtifactWriterPort;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactCategory;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisJobId;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisRunId;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ArtifactReference;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BtmArtifactWriteRequest;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BtmGenerationPolicy;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.DeliveredFacts;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.GenerateBtmRulesCommand;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.GeneratedBtmArtifacts;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ProbeKind;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.RequestMetadata;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.RuleTarget;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.SourceSnapshotId;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.PRODUCER_SERVICE;
import static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.sha256;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BtmGenerationApplicationServiceTest {
    @Test
    void generatesDeterministicRulesIndependentOfInputOrder() {
        var writer = new RecordingWriter();
        var service = new BtmGenerationApplicationService(writer);

        var first = service.generate(command(List.of(target("b-target", ProbeKind.METHOD_EXIT), target("a-target", ProbeKind.METHOD_ENTRY))));
        var firstContent = FileSystemBtmArtifactWriter.renderBtm(writer.lastRequest);
        var firstRuleIds = first.generatedRules().stream().map(rule -> rule.ruleId()).toList();

        var second = service.generate(command(List.of(target("a-target", ProbeKind.METHOD_ENTRY), target("b-target", ProbeKind.METHOD_EXIT))));
        var secondContent = FileSystemBtmArtifactWriter.renderBtm(writer.lastRequest);

        assertEquals(firstRuleIds, second.generatedRules().stream().map(rule -> rule.ruleId()).toList());
        assertEquals(firstContent, secondContent);
        assertEquals(AnalysisCompleteness.COMPLETE, first.completeness());
        assertEquals(List.of("a-target", "b-target"), first.generatedRules().stream().map(rule -> rule.target().targetId()).toList());
        assertTrue(firstContent.contains("AT ENTRY\n"));
        assertTrue(firstContent.contains("AT EXIT\n"));
        assertEquals(-1, firstContent.indexOf("\r"));
    }

    @Test
    void marksIncompleteTargetsAndUnknownProbeKindsWithoutFabricatingRules() {
        var writer = new RecordingWriter();
        var service = new BtmGenerationApplicationService(writer);
        var incomplete = new RuleTarget("missing", "fact-missing", "", "", "", "", "", 0, ProbeKind.METHOD_ENTRY);
        var unknown = target("unknown", ProbeKind.UNKNOWN);

        var result = service.generate(command(List.of(incomplete, unknown)));

        assertEquals(AnalysisCompleteness.INCOMPLETE, result.completeness());
        assertEquals(List.of(), result.generatedRules());
        assertEquals(List.of("AMBIGUOUS_TARGET_MAPPING", "UNSUPPORTED_TARGET_KIND"), result.diagnostics().stream().map(diagnostic -> diagnostic.code()).toList());
        assertTrue(FileSystemBtmArtifactWriter.renderBtm(writer.lastRequest).contains("not observed runtime evidence"));
    }

    @Test
    void keepsIncompleteDeliveredFactsExplicitAndCanFailClosed() {
        var open = new BtmGenerationApplicationService(new RecordingWriter()).generate(command(
            List.of(target("target", ProbeKind.THROW)),
            AnalysisCompleteness.INCOMPLETE,
            false
        ));
        var closed = new BtmGenerationApplicationService(new RecordingWriter()).generate(command(
            List.of(target("target", ProbeKind.THROW)),
            AnalysisCompleteness.INCOMPLETE,
            true
        ));

        assertEquals(1, open.generatedRules().size());
        assertEquals(AnalysisCompleteness.INCOMPLETE, open.completeness());
        assertEquals(0, closed.generatedRules().size());
        assertEquals("INPUT_FACTS_INCOMPLETE", closed.diagnostics().getFirst().code());
    }

    @Test
    void rejectsPolicyViolationsAndPropagatesWriterFailures() {
        var service = new BtmGenerationApplicationService(new RecordingWriter());
        assertThrows(IllegalArgumentException.class, () -> service.generate(command(
            List.of(target("one", ProbeKind.METHOD_ENTRY), target("two", ProbeKind.METHOD_ENTRY)),
            AnalysisCompleteness.COMPLETE,
            false,
            new BtmGenerationPolicy(1, 100_000, 60, "btm-rule-v1", false)
        )));

        var writerFailure = new BtmGenerationApplicationService(request -> {
            throw new BtmArtifactException("cannot write");
        });
        assertThrows(BtmArtifactException.class, () -> writerFailure.generate(command(List.of(target("one", ProbeKind.METHOD_ENTRY)))));

        var nonRuntimeFailure = new BtmGenerationApplicationService(request -> {
            throw new AssertionError("boom");
        });
        assertThrows(IllegalStateException.class, () -> nonRuntimeFailure.generate(command(List.of(target("one", ProbeKind.METHOD_ENTRY)))));
    }

    @Test
    void timesOutSlowGenerationWork() {
        var service = new BtmGenerationApplicationService(request -> {
            try {
                Thread.sleep(2_000);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            }
            return new GeneratedBtmArtifacts(List.of(), 0);
        });

        assertThrows(BtmGenerationTimeoutException.class, () -> service.generate(command(
            List.of(target("one", ProbeKind.METHOD_ENTRY)),
            AnalysisCompleteness.COMPLETE,
            false,
            new BtmGenerationPolicy(10, 100_000, 1, "btm-rule-v1", false)
        )));
    }

    @Test
    void ruleIdsChangeWhenBehaviorRelevantInputsChange() {
        var service = new BtmGenerationApplicationService(new RecordingWriter());
        var entry = service.generate(command(List.of(target("target", ProbeKind.METHOD_ENTRY)))).generatedRules().getFirst().ruleId();
        var exit = service.generate(command(List.of(target("target", ProbeKind.METHOD_EXIT)))).generatedRules().getFirst().ruleId();

        assertNotEquals(entry, exit);
    }

    private static GenerateBtmRulesCommand command(List<RuleTarget> targets) {
        return command(targets, AnalysisCompleteness.COMPLETE, false);
    }

    private static GenerateBtmRulesCommand command(
        List<RuleTarget> targets,
        AnalysisCompleteness completeness,
        boolean failOnIncompleteFacts
    ) {
        return command(targets, completeness, failOnIncompleteFacts, new BtmGenerationPolicy(10, 100_000, 60, "btm-rule-v1", failOnIncompleteFacts));
    }

    private static GenerateBtmRulesCommand command(
        List<RuleTarget> targets,
        AnalysisCompleteness completeness,
        boolean failOnIncompleteFacts,
        BtmGenerationPolicy policy
    ) {
        return new GenerateBtmRulesCommand(
            metadata(),
            policy,
            new DeliveredFacts(List.of(artifact("source-facts.json")), List.of(artifact("semantic.json")), targets, completeness)
        );
    }

    private static RequestMetadata metadata() {
        return new RequestMetadata(
            "request-1",
            "idempotency-1",
            "btm-generation-v1",
            "correlation-1",
            new AnalysisRunId("run-1"),
            new AnalysisJobId("job-1"),
            new SourceSnapshotId("snapshot-1"),
            "btm-generation-service-test",
            Map.of("tenant", "demo")
        );
    }

    private static RuleTarget target(String targetId, ProbeKind probeKind) {
        return new RuleTarget(
            targetId,
            "fact-" + targetId,
            "semantic-" + targetId,
            "src/main/java/a/A.java",
            "a.A",
            "run",
            "a.A#run()",
            12,
            probeKind
        );
    }

    private static AnalysisArtifactReference artifact(String path) {
        return new AnalysisArtifactReference(
            new ArtifactReference(path, "application/vnd.forensic-analytics.source-facts.v1+json", "a".repeat(64), 12),
            AnalysisArtifactCategory.STATIC,
            "analysis-store-service",
            "source-facts-v1",
            AnalysisCompleteness.COMPLETE
        );
    }

    private static final class RecordingWriter implements BtmArtifactWriterPort {
        private BtmArtifactWriteRequest lastRequest;

        @Override
        public GeneratedBtmArtifacts write(BtmArtifactWriteRequest request) {
            lastRequest = request;
            var bytes = FileSystemBtmArtifactWriter.renderBtm(request).getBytes(StandardCharsets.UTF_8);
            return new GeneratedBtmArtifacts(List.of(new AnalysisArtifactReference(
                new ArtifactReference("btm/rules.btm", "application/vnd.forensic-analytics.btm-rules.v1+btm", sha256(bytes), bytes.length),
                AnalysisArtifactCategory.GENERATED,
                PRODUCER_SERVICE,
                request.summary().ruleSchemaVersion(),
                request.completeness()
            )), bytes.length);
        }
    }
}
