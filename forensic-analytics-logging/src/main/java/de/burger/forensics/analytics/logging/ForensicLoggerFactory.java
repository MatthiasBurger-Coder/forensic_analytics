package de.burger.forensics.analytics.logging;

import java.util.Objects;
import java.util.function.Function;

public final class ForensicLoggerFactory {
    private final Function<Class<?>, System.Logger> systemLoggerProvider;

    public ForensicLoggerFactory() {
        this(source -> System.getLogger(source.getName()));
    }

    public ForensicLoggerFactory(Function<Class<?>, System.Logger> systemLoggerProvider) {
        this.systemLoggerProvider = Objects.requireNonNull(
            systemLoggerProvider,
            "systemLoggerProvider must not be null"
        );
    }

    public ForensicLogger logger(Class<?> source) {
        var verifiedSource = Objects.requireNonNull(source, "source must not be null");
        return new SystemForensicLogger(systemLoggerProvider.apply(verifiedSource));
    }

    static ForensicLoggerFactory system() {
        return new ForensicLoggerFactory();
    }
}
