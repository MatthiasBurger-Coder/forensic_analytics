package de.burger.forensics.analytics.services.btmgeneration.application;

import de.burger.forensics.analytics.services.btmgeneration.application.port.BtmArtifactWriterPort;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisCompleteness;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BtmArtifactWriteRequest;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BtmDiagnostic;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BtmGenerationSummary;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.DiagnosticSeverity;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.GenerateBtmRulesCommand;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.GenerateBtmRulesResult;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.GeneratedRule;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ProbeKind;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ReproducibilityMetadata;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.RuleTarget;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.DETERMINISTIC_SORT;
import static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.GENERATOR_VERSION;
import static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.PRODUCER_SERVICE;
import static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.factsFingerprint;
import static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.generationFingerprint;
import static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.policyFingerprint;
import static de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.stableRuleId;

public final class BtmGenerationApplicationService {
    private final BtmArtifactWriterPort artifactWriter;

    public BtmGenerationApplicationService(BtmArtifactWriterPort artifactWriter) {
        this.artifactWriter = Objects.requireNonNull(artifactWriter, "artifact writer must not be null");
    }

    public GenerateBtmRulesResult generate(GenerateBtmRulesCommand command) {
        var verifiedCommand = Objects.requireNonNull(command, "command must not be null");
        if (verifiedCommand.facts().targets().size() > verifiedCommand.policy().maxTargets()) {
            throw new IllegalArgumentException("target count exceeds generation policy");
        }
        return generateWithTimeout(verifiedCommand);
    }

    private GenerateBtmRulesResult generateWithTimeout(GenerateBtmRulesCommand command) {
        var executor = Executors.newSingleThreadExecutor();
        try {
            var future = executor.submit(() -> generateInternal(command));
            return future.get(command.policy().timeoutSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException error) {
            throw new BtmGenerationTimeoutException("BTM generation timed out after "
                + command.policy().timeoutSeconds() + " seconds.");
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new BtmGenerationTimeoutException("BTM generation was interrupted.");
        } catch (ExecutionException error) {
            if (error.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("BTM generation failed.", error.getCause());
        } finally {
            executor.shutdownNow();
        }
    }

    private GenerateBtmRulesResult generateInternal(GenerateBtmRulesCommand command) {
        var diagnostics = new ArrayList<BtmDiagnostic>();
        if (command.facts().targets().isEmpty()) {
            diagnostics.add(diagnostic(command, "", "INPUT_FACTS_INCOMPLETE", "No instrumentation targets were delivered."));
        }
        if (command.facts().inputCompleteness() != AnalysisCompleteness.COMPLETE) {
            diagnostics.add(diagnostic(command, "", "INPUT_FACTS_INCOMPLETE", "Delivered analysis facts are incomplete."));
        }

        var rules = command.facts().targets().stream()
            .map(target -> ruleOrDiagnostic(command, target, diagnostics))
            .flatMap(List::stream)
            .toList();

        var completeness = diagnostics.stream().anyMatch(BtmDiagnostic::affectsCompleteness)
            ? AnalysisCompleteness.INCOMPLETE
            : AnalysisCompleteness.COMPLETE;
        if (command.policy().failOnIncompleteFacts() && completeness == AnalysisCompleteness.INCOMPLETE) {
            rules = List.of();
        }

        var summary = new BtmGenerationSummary(
            command.facts().targets().size(),
            rules.size(),
            command.facts().targets().size() - rules.size(),
            command.facts().sourceFactArtifacts().size(),
            command.facts().semanticArtifacts().size(),
            PRODUCER_SERVICE,
            GENERATOR_VERSION,
            command.policy().ruleSchemaVersion()
        );
        var reproducibility = new ReproducibilityMetadata(
            factsFingerprint(command.metadata().sourceSnapshotId(), command.facts()),
            policyFingerprint(command.policy()),
            generationFingerprint(rules, diagnostics),
            GENERATOR_VERSION,
            DETERMINISTIC_SORT
        );
        var artifacts = artifactWriter.write(new BtmArtifactWriteRequest(
            command.metadata(),
            command.policy(),
            rules,
            diagnostics,
            summary,
            reproducibility,
            completeness
        ));
        return new GenerateBtmRulesResult(
            command.metadata(),
            completeness,
            artifacts.artifacts(),
            rules,
            summary,
            reproducibility,
            diagnostics
        );
    }

    private static List<GeneratedRule> ruleOrDiagnostic(
        GenerateBtmRulesCommand command,
        RuleTarget target,
        List<BtmDiagnostic> diagnostics
    ) {
        if (target.probeKind() == ProbeKind.UNKNOWN) {
            diagnostics.add(diagnostic(command, target.targetId(), "UNSUPPORTED_TARGET_KIND", "Instrumentation target uses an unsupported probe kind."));
            return List.of();
        }
        if (!target.canGenerateRule()) {
            diagnostics.add(diagnostic(command, target.targetId(), "AMBIGUOUS_TARGET_MAPPING", "Instrumentation target is missing class, method, signature, path or line information."));
            return List.of();
        }
        return List.of(new GeneratedRule(
            stableRuleId(command.metadata().sourceSnapshotId(), target, command.policy().ruleSchemaVersion()),
            target
        ));
    }

    private static BtmDiagnostic diagnostic(
        GenerateBtmRulesCommand command,
        String targetId,
        String code,
        String message
    ) {
        return new BtmDiagnostic(
            code,
            message,
            DiagnosticSeverity.WARNING,
            command.metadata().sourceSnapshotId(),
            targetId,
            "",
            false,
            true
        );
    }
}
