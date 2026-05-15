package de.burger.forensics.analytics.loggingfixture;

import de.burger.forensics.analytics.logging.ForensicLoggable;

@ForensicLoggable
public final class ClassAnnotatedService implements SampleService {
    @Override
    public String run(String value) {
        return "class-annotated";
    }
}
