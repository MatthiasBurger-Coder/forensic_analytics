package de.burger.forensics.analytics.services.btmgeneration.application.port;

import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.AnalysisArtifactReference;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.ReadableBtmArtifact;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.StoredBtmArtifactManifest;

import java.io.InputStream;

public interface BtmArtifactReaderPort {
    StoredBtmArtifactManifest readManifest(AnalysisArtifactReference manifestReference);

    void verify(AnalysisArtifactReference artifact);

    InputStream open(ReadableBtmArtifact artifact);
}
