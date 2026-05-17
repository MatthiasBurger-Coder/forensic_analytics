package de.burger.forensics.analytics.services.btmgeneration.domain;

import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactCategory;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ArtifactReference;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BtmDiagnostic;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BtmGenerationSummary;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BtmGenerationPolicy;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.DeliveredFacts;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.DiagnosticSeverity;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.GeneratedBtmArtifacts;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ProbeKind;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.RuleTarget;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.SourceSnapshotId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.factsFingerprint;
import static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.policyFingerprint;
import static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.requireRelativePath;
import static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.requireText;
import static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.safeAttributes;
import static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.stableRuleId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BtmGenerationDomainTest {
    @Test
    void derivesStableRuleIdsFromSnapshotTargetProbeAndSchema() {
        var snapshot = new SourceSnapshotId("snapshot-1");
        var target = target("target-1", ProbeKind.METHOD_ENTRY);
        var same = target("target-1", ProbeKind.METHOD_ENTRY);

        assertEquals(stableRuleId(snapshot, target, "btm-rule-v1"), stableRuleId(snapshot, same, "btm-rule-v1"));
        assertNotEquals(stableRuleId(snapshot, target, "btm-rule-v1"), stableRuleId(snapshot, target("target-2", ProbeKind.METHOD_ENTRY), "btm-rule-v1"));
        assertNotEquals(stableRuleId(snapshot, target, "btm-rule-v1"), stableRuleId(snapshot, target("target-1", ProbeKind.METHOD_EXIT), "btm-rule-v1"));
        assertNotEquals(stableRuleId(snapshot, target, "btm-rule-v1"), stableRuleId(snapshot, target, "btm-rule-v2"));
    }

    @Test
    void sortsDeliveredFactsForDeterministicFingerprints() {
        var snapshot = new SourceSnapshotId("snapshot-1");
        var first = target("a-target", ProbeKind.METHOD_ENTRY);
        var second = target("b-target", ProbeKind.METHOD_EXIT);
        var referenceA = artifact("artifacts/a.json");
        var referenceB = artifact("artifacts/b.json");

        var ordered = new DeliveredFacts(List.of(referenceA, referenceB), List.of(referenceB), List.of(first, second), AnalysisCompleteness.COMPLETE);
        var reversed = new DeliveredFacts(List.of(referenceB, referenceA), List.of(referenceB), List.of(second, first), AnalysisCompleteness.COMPLETE);

        assertEquals(List.of("a-target", "b-target"), ordered.targets().stream().map(RuleTarget::targetId).toList());
        assertEquals(factsFingerprint(snapshot, ordered), factsFingerprint(snapshot, reversed));
    }

    @Test
    void rejectsUnsafePublicContractValues() {
        assertThrows(IllegalArgumentException.class, () -> new RuleTarget("target", "fact", "", "../A.java", "a.A", "run", "a.A#run()", 1, ProbeKind.METHOD_ENTRY));
        assertThrows(IllegalArgumentException.class, () -> new ArtifactReference("file:/tmp/rules.btm", "type", "a".repeat(64), 1));
        assertThrows(IllegalArgumentException.class, () -> new ArtifactReference("rules.btm", "type", "not-a-sha", 1));
        assertThrows(IllegalArgumentException.class, () -> safeAttributes(Map.of("accessToken", "secret")));
        assertThrows(IllegalArgumentException.class, () -> safeAttributes(Map.of("path", "file:/private/source")));
        assertThrows(IllegalArgumentException.class, () -> safeAttributes(Map.of("path", "/private/source")));
        assertThrows(IllegalArgumentException.class, () -> safeAttributes(Map.of("path", "C:/repo/source")));
        assertThrows(IllegalArgumentException.class, () -> safeAttributes(Map.of("path", "../workspace")));
        assertThrows(IllegalArgumentException.class, () -> new BtmGenerationPolicy(0, 1, 1, "btm-rule-v1", false));
        assertThrows(IllegalArgumentException.class, () -> new BtmGenerationPolicy(100_001, 1, 1, "btm-rule-v1", false));
        assertThrows(IllegalArgumentException.class, () -> new BtmGenerationPolicy(1, 0, 1, "btm-rule-v1", false));
        assertThrows(IllegalArgumentException.class, () -> new BtmGenerationPolicy(1, 1_073_741_825L, 1, "btm-rule-v1", false));
        assertThrows(IllegalArgumentException.class, () -> new BtmGenerationPolicy(1, 1, 0, "btm-rule-v1", false));
        assertThrows(IllegalArgumentException.class, () -> new BtmGenerationPolicy(1, 1, 86_401, "btm-rule-v1", false));
        assertThrows(IllegalArgumentException.class, () -> requireText(null, "value"));
        assertThrows(IllegalArgumentException.class, () -> requireText(" ", "value"));
        assertEquals("src/main/java/A.java", requireRelativePath("src/main/java/A.java", "path"));
        assertThrows(IllegalArgumentException.class, () -> requireRelativePath("/absolute/A.java", "path"));
        assertThrows(IllegalArgumentException.class, () -> requireRelativePath("C:/repo/A.java", "path"));
        assertThrows(IllegalArgumentException.class, () -> requireRelativePath("src//A.java", "path"));
        assertThrows(IllegalArgumentException.class, () -> new ArtifactReference("https://store/artifact", "type", "a".repeat(64), 1));
        assertThrows(IllegalArgumentException.class, () -> safeAttributes(Map.of("link", "https://example.test")));
        assertThrows(IllegalArgumentException.class, () -> safeAttributes(Map.of("path", "a\\b")));
        assertThrows(IllegalArgumentException.class, () -> diagnostic("/private/source"));
        assertThrows(IllegalArgumentException.class, () -> diagnostic("C:/repo/source"));
        assertThrows(IllegalArgumentException.class, () -> diagnostic("../workspace"));
        assertThrows(IllegalArgumentException.class, () -> diagnostic("https://store/artifact"));
        assertEquals("btm/rules.btm", diagnostic("btm/rules.btm").artifactPath());
    }

    @Test
    void fingerprintsPoliciesByBehaviorRelevantValues() {
        var policy = new BtmGenerationPolicy(10, 1_000, 60, "btm-rule-v1", false);

        assertEquals(policyFingerprint(policy), policyFingerprint(new BtmGenerationPolicy(10, 1_000, 60, "btm-rule-v1", false)));
        assertNotEquals(policyFingerprint(policy), policyFingerprint(new BtmGenerationPolicy(10, 1_000, 60, "btm-rule-v1", true)));
    }

    @Test
    void reportsRuleTargetCompletenessWithoutGuessingMissingMappings() {
        assertEquals("", new RuleTarget("target", "fact", null, "", "", "", "", 0, ProbeKind.UNKNOWN).semanticNodeId());
        assertEquals(false, new RuleTarget("target", "fact", "", "", "a.A", "run", "a.A#run()", 1, ProbeKind.METHOD_ENTRY).canGenerateRule());
        assertEquals(false, new RuleTarget("target", "fact", "", "src/A.java", "", "run", "a.A#run()", 1, ProbeKind.METHOD_ENTRY).canGenerateRule());
        assertEquals(false, new RuleTarget("target", "fact", "", "src/A.java", "a.A", "", "a.A#run()", 1, ProbeKind.METHOD_ENTRY).canGenerateRule());
        assertEquals(false, new RuleTarget("target", "fact", "", "src/A.java", "a.A", "run", "", 1, ProbeKind.METHOD_ENTRY).canGenerateRule());
        assertEquals(false, new RuleTarget("target", "fact", "", "src/A.java", "a.A", "run", "a.A#run()", 0, ProbeKind.METHOD_ENTRY).canGenerateRule());
        assertEquals(false, new RuleTarget("target", "fact", "", "src/A.java", "a.A", "run", "a.A#run()", 1, ProbeKind.UNKNOWN).canGenerateRule());
    }

    @Test
    void rejectsNegativeGeneratedCountsAndArtifactTotals() {
        assertThrows(IllegalArgumentException.class, () -> new BtmGenerationSummary(-1, 0, 0, 0, 0, "producer", "version", "schema"));
        assertThrows(IllegalArgumentException.class, () -> new BtmGenerationSummary(0, -1, 0, 0, 0, "producer", "version", "schema"));
        assertThrows(IllegalArgumentException.class, () -> new BtmGenerationSummary(0, 0, -1, 0, 0, "producer", "version", "schema"));
        assertThrows(IllegalArgumentException.class, () -> new GeneratedBtmArtifacts(List.of(), -1));
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

    private static BtmDiagnostic diagnostic(String artifactPath) {
        return new BtmDiagnostic(
            "INVALID_ARTIFACT_REFERENCE",
            "invalid artifact reference",
            DiagnosticSeverity.WARNING,
            new SourceSnapshotId("snapshot-1"),
            "target-1",
            artifactPath,
            false,
            true
        );
    }
}
