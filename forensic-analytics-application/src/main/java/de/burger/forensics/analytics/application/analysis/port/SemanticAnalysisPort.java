package de.burger.forensics.analytics.application.analysis.port;

import de.burger.forensics.analytics.application.analysis.command.SemanticAnalysisRequest;
import de.burger.forensics.analytics.application.analysis.result.SemanticAnalysisResult;

public interface SemanticAnalysisPort {
    SemanticAnalysisResult analyze(SemanticAnalysisRequest request);
}
