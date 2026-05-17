package de.burger.forensics.analytics.services.javaastanalysis.application.port;

import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.AnalyzeSourceSnapshotCommand;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.JavaAstScanResult;

public interface JavaSourceScannerPort {
    JavaAstScanResult scan(AnalyzeSourceSnapshotCommand command);
}
