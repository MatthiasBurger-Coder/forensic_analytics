package de.burger.forensics.analytics.services.javaastanalysis.application.port;

import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.SourceFactArtifactBytes;
import de.burger.forensics.analytics.services.javaastanalysis.domain.JavaAstAnalysisDomain.SourceFactArtifactBytesRequest;

public interface AstResultArtifactReaderPort {
    SourceFactArtifactBytes read(SourceFactArtifactBytesRequest request);
}
