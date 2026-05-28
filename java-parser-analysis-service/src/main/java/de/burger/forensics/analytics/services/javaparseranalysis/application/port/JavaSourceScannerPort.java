package de.burger.forensics.analytics.services.javaparseranalysis.application.port;

import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.AnalyzeSourceSnapshotCommand;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.JavaAstScanResult;

public interface JavaSourceScannerPort {
    JavaAstScanResult scan(AnalyzeSourceSnapshotCommand command);
}
