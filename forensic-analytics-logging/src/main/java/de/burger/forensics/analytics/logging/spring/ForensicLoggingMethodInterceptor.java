package de.burger.forensics.analytics.logging.spring;

import de.burger.forensics.analytics.logging.ForensicLoggable;
import de.burger.forensics.analytics.logging.ForensicLoggerFactory;
import de.burger.forensics.analytics.observability.CorrelationContext;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

final class ForensicLoggingMethodInterceptor implements MethodInterceptor {
    private final ForensicLoggingSettings settings;
    private final ForensicLoggerFactory loggerFactory;
    private final Class<?> targetClass;

    ForensicLoggingMethodInterceptor(
        ForensicLoggingSettings settings,
        ForensicLoggerFactory loggerFactory,
        Class<?> targetClass
    ) {
        this.settings = Objects.requireNonNull(settings, "settings must not be null");
        this.loggerFactory = Objects.requireNonNull(loggerFactory, "loggerFactory must not be null");
        this.targetClass = Objects.requireNonNull(targetClass, "targetClass must not be null");
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        var invokedMethod = invocation.getMethod();
        var specificMethod = AopUtils.getMostSpecificMethod(invokedMethod, targetClass);
        var annotation = resolveLoggable(invokedMethod, specificMethod);
        if (!settings.shouldLogMethod(specificMethod, annotation.isPresent())) {
            return invocation.proceed();
        }

        var level = annotation.map(ForensicLoggable::value).orElse(settings.defaultLevel());
        var logger = loggerFactory.logger(targetClass);
        var operation = targetClass.getName() + "." + specificMethod.getName();
        var startedAt = System.nanoTime();
        var scope = openCorrelationIfMissing();

        try {
            logger.operationStarted(operation, level);
            var result = invocation.proceed();
            logger.operationSucceeded(operation, level, elapsedMillis(startedAt));
            return result;
        } catch (Throwable error) {
            logger.operationFailed(operation, elapsedMillis(startedAt), error);
            throw error;
        } finally {
            scope.close();
        }
    }

    private Optional<ForensicLoggable> resolveLoggable(Method invokedMethod, Method specificMethod) {
        var methodAnnotation = AnnotationUtils.findAnnotation(specificMethod, ForensicLoggable.class);
        if (methodAnnotation != null) {
            return Optional.of(methodAnnotation);
        }
        methodAnnotation = AnnotationUtils.findAnnotation(invokedMethod, ForensicLoggable.class);
        if (methodAnnotation != null) {
            return Optional.of(methodAnnotation);
        }
        return Optional.ofNullable(AnnotationUtils.findAnnotation(targetClass, ForensicLoggable.class));
    }

    private static long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    private static CorrelationHandle openCorrelationIfMissing() {
        if (CorrelationContext.current().isPresent()) {
            return () -> {
            };
        }
        var scope = CorrelationContext.openGenerated();
        return scope::close;
    }

    private interface CorrelationHandle {
        void close();
    }
}
