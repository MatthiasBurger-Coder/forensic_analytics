package de.burger.forensics.analytics.logging.spring;

import de.burger.forensics.analytics.logging.ForensicLogLevel;
import de.burger.forensics.analytics.logging.ForensicLoggingMode;
import org.springframework.core.env.Environment;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

record ForensicLoggingSettings(
    boolean enabled,
    String basePackage,
    ForensicLoggingMode mode,
    ForensicLogLevel defaultLevel
) {
    private static final Pattern PACKAGE_NAME = Pattern.compile(
        "[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*"
    );
    private static final String DEFAULT_BASE_PACKAGE = "de.burger.forensics.analytics";

    ForensicLoggingSettings {
        if (basePackage == null || !PACKAGE_NAME.matcher(basePackage).matches()) {
            throw new IllegalArgumentException("basePackage must be a Java package name");
        }
        mode = Objects.requireNonNull(mode, "mode must not be null");
        defaultLevel = Objects.requireNonNull(defaultLevel, "defaultLevel must not be null");
    }

    static ForensicLoggingSettings defaults() {
        return new ForensicLoggingSettings(
            true,
            DEFAULT_BASE_PACKAGE,
            ForensicLoggingMode.APPLICATION,
            ForensicLogLevel.INFO
        );
    }

    static ForensicLoggingSettings from(Environment environment) {
        var verifiedEnvironment = Objects.requireNonNull(environment, "environment must not be null");
        return new ForensicLoggingSettings(
            verifiedEnvironment.getProperty("forensics.analytics.logging.enabled", Boolean.class, true),
            verifiedEnvironment.getProperty("forensics.analytics.logging.base-package", DEFAULT_BASE_PACKAGE),
            enumProperty(
                verifiedEnvironment,
                "forensics.analytics.logging.mode",
                ForensicLoggingMode.class,
                ForensicLoggingMode.APPLICATION
            ),
            enumProperty(
                verifiedEnvironment,
                "forensics.analytics.logging.default-level",
                ForensicLogLevel.class,
                ForensicLogLevel.INFO
            )
        );
    }

    boolean matchesBasePackage(Class<?> targetClass) {
        var packageName = Objects.requireNonNull(targetClass, "targetClass must not be null").getPackageName();
        return packageName.equals(basePackage) || packageName.startsWith(basePackage + ".");
    }

    boolean shouldLogMethod(Method method) {
        return shouldLogMethod(method, false);
    }

    boolean shouldLogMethod(Method method, boolean annotated) {
        var verifiedMethod = Objects.requireNonNull(method, "method must not be null");
        if (verifiedMethod.isBridge()
            || verifiedMethod.isSynthetic()
            || verifiedMethod.getDeclaringClass() == Object.class
            || !Modifier.isPublic(verifiedMethod.getModifiers())) {
            return false;
        }
        return mode == ForensicLoggingMode.APPLICATION || annotated;
    }

    private static <T extends Enum<T>> T enumProperty(
        Environment environment,
        String propertyName,
        Class<T> enumType,
        T defaultValue
    ) {
        var rawValue = environment.getProperty(propertyName);
        if (rawValue == null || rawValue.isBlank()) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(enumType, rawValue.strip().replace('-', '_').toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException(
                propertyName + " must be one of " + String.join(", ", enumNames(enumType)),
                error
            );
        }
    }

    private static <T extends Enum<T>> String[] enumNames(Class<T> enumType) {
        var constants = enumType.getEnumConstants();
        var names = new String[constants.length];
        for (int index = 0; index < constants.length; index++) {
            names[index] = constants[index].name();
        }
        return names;
    }
}
