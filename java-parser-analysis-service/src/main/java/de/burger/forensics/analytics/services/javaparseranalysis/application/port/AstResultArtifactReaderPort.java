package de.burger.forensics.analytics.services.javaparseranalysis.application.port;

import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.SourceFactArtifactBytes;
import de.burger.forensics.analytics.services.javaparseranalysis.domain.JavaAstAnalysisDomain.SourceFactArtifactBytesRequest;

public interface AstResultArtifactReaderPort {
    SourceFactArtifactBytes read(SourceFactArtifactBytesRequest request);
}
