package de.burger.forensics.analytics.loggingfixture;

public final class InterfaceBasedService implements SampleService {
    @Override
    public String run(String value) {
        return "done";
    }
}
