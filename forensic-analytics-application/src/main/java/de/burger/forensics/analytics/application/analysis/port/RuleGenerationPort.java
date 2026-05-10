package de.burger.forensics.analytics.application.analysis.port;

import de.burger.forensics.analytics.application.analysis.command.RuleGenerationRequest;
import de.burger.forensics.analytics.application.analysis.result.RuleGenerationResult;

public interface RuleGenerationPort {
    RuleGenerationResult generate(RuleGenerationRequest request);
}
