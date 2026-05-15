package de.burger.forensics.analytics.loggingfixture;

import de.burger.forensics.analytics.logging.ForensicLoggable;

public final class AnnotatedInterfaceBasedService implements SampleService {
    @Override
    @ForensicLoggable
    public String run(String value) {
        return "annotated";
    }
}
