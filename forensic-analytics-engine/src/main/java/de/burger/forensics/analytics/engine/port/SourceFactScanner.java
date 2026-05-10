package de.burger.forensics.analytics.engine.port;

import de.burger.forensics.analytics.engine.RepositorySource;
import de.burger.forensics.analytics.engine.SourceFact;

import java.util.List;

public interface SourceFactScanner {
    List<SourceFact> scan(RepositorySource source);
}
