package de.burger.forensics.analytics.loggingfixture;

import org.springframework.context.SmartLifecycle;

public final class LifecycleFixture implements SmartLifecycle {
    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    public String run() {
        return "lifecycle";
    }
}
