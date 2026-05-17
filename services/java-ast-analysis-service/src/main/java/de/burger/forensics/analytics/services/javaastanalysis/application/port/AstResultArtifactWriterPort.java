package de.burger.forensics.analytics.services.javaastanalysis.application.port;

import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.JavaAstDiagnostic;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.JavaSourceFact;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.RequestMetadata;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.ScanSummary;

import java.util.List;

public interface AstResultArtifactWriterPort {
    AnalysisArtifactReference write(
        RequestMetadata metadata,
        List<JavaSourceFact> sourceFacts,
        List<JavaAstDiagnostic> diagnostics,
        ScanSummary summary
    );
}
