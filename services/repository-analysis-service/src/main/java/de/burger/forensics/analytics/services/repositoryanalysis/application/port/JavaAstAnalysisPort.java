package de.burger.forensics.analytics.services.repositoryanalysis.application.port;

import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.JavaAstAnalysisHandoffCommand;
import de.burger.forensics.analytics.services.repositoryanalysis.domain.RepositoryAnalysisDomain.JavaAstAnalysisHandoffResult;

public interface JavaAstAnalysisPort {
    JavaAstAnalysisHandoffResult analyze(JavaAstAnalysisHandoffCommand command);
}
