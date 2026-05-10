package de.burger.forensics.analytics.engine.port;

import de.burger.forensics.analytics.domain.repository.RepositorySource;
import de.burger.forensics.analytics.domain.source.SourceFact;

import java.util.List;

public interface SourceFactScanner {
    List<SourceFact> scan(RepositorySource source);
}
