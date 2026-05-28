package de.burger.forensics.analytics.services.joernanalysis.application.port;

import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.SemanticArtifactBytes;
import de.burger.forensics.analytics.services.joernanalysis.domain.JoernCpgAnalysisDomain.SemanticArtifactBytesRequest;

public interface JoernArtifactReaderPort {
    SemanticArtifactBytes read(SemanticArtifactBytesRequest request);
}
