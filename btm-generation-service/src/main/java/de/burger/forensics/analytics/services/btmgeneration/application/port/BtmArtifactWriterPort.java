package de.burger.forensics.analytics.services.btmgeneration.application.port;

import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.BtmArtifactWriteRequest;
import de.burger.forensics.analytics.services.btmgeneration.domain.BtmGenerationDomain.GeneratedBtmArtifacts;

public interface BtmArtifactWriterPort {
    GeneratedBtmArtifacts write(BtmArtifactWriteRequest request);
}
