package de.burger.forensics.analytics.logging.spring;

import de.burger.forensics.analytics.logging.ForensicLogLevel;
import de.burger.forensics.analytics.logging.ForensicLoggingMode;
import org.springframework.core.env.Environment;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.Objects;

record ForensicLoggingSettings(
    boolean enabled,
    String basePackage,
    ForensicLoggingMode mode,
    ForensicLogLevel defaultLevel
) {
    private static final String DEFAULT_BASE_PACKAGE = "de.burger.forensics.analytics";

    ForensicLoggingSettings {
        if (!isPackageName(basePackage)) {
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

    private static boolean isPackageName(String value) {
        if (value == null || value.isBlank() || value.startsWith(".") || value.endsWith(".")) {
            return false;
        }
        for (var segment : value.split("\\.", -1)) {
            if (!isPackageSegment(segment)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPackageSegment(String segment) {
        if (segment.isEmpty() || !isIdentifierStart(segment.charAt(0))) {
            return false;
        }
        for (var index = 1; index < segment.length(); index++) {
            if (!isIdentifierPart(segment.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isIdentifierStart(char value) {
        return value == '_' || value >= 'A' && value <= 'Z' || value >= 'a' && value <= 'z';
    }

    private static boolean isIdentifierPart(char value) {
        return isIdentifierStart(value) || value >= '0' && value <= '9';
    }
}
