package de.burger.forensics.analytics.services.javaparseranalysis.application.port;

import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.JavaAstDiagnostic;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.JavaSourceFact;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.RequestMetadata;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.ScanSummary;

import java.util.List;

public interface AstResultArtifactWriterPort {
    AnalysisArtifactReference write(
        RequestMetadata metadata,
        List<JavaSourceFact> sourceFacts,
        List<JavaAstDiagnostic> diagnostics,
        ScanSummary summary
    );
}
