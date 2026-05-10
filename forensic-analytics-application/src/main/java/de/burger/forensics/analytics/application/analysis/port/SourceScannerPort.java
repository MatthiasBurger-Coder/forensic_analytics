package de.burger.forensics.analytics.application.analysis.port;

import de.burger.forensics.analytics.domain.repository.RepositorySource;
import de.burger.forensics.analytics.domain.source.SourceFact;

import java.util.List;

public interface SourceScannerPort {
    List<SourceFact> scan(RepositorySource source);
}
