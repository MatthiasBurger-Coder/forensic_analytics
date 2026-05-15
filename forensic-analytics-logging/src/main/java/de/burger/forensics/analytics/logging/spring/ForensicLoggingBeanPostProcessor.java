package de.burger.forensics.analytics.logging.spring;

import de.burger.forensics.analytics.logging.ForensicLoggable;
import de.burger.forensics.analytics.logging.ForensicLogger;
import de.burger.forensics.analytics.logging.ForensicLoggerFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

final class ForensicLoggingBeanPostProcessor implements BeanPostProcessor, Ordered {
    private final ForensicLoggingSettings settings;
    private final ForensicLoggerFactory loggerFactory;

    ForensicLoggingBeanPostProcessor(ForensicLoggingSettings settings, ForensicLoggerFactory loggerFactory) {
        this.settings = Objects.requireNonNull(settings, "settings must not be null");
        this.loggerFactory = Objects.requireNonNull(loggerFactory, "loggerFactory must not be null");
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Objects.requireNonNull(bean, "bean must not be null");
        var targetClass = AopUtils.getTargetClass(bean);
        if (!isEligible(bean, targetClass)) {
            return bean;
        }

        var proxyFactory = new ProxyFactory();
        proxyFactory.setTarget(bean);
        proxyFactory.addAdvice(new ForensicLoggingMethodInterceptor(settings, loggerFactory, targetClass));

        if (Modifier.isFinal(targetClass.getModifiers())) {
            var interfaces = proxiedInterfaces(targetClass);
            if (interfaces.length == 0) {
                return bean;
            }
            proxyFactory.setInterfaces(interfaces);
        } else {
            proxyFactory.setProxyTargetClass(true);
        }

        return proxyFactory.getProxy(targetClass.getClassLoader());
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private boolean isEligible(Object bean, Class<?> targetClass) {
        return settings.enabled()
            && targetClass != null
            && !AopUtils.isAopProxy(bean)
            && !isLoggingInfrastructure(targetClass)
            && !isSpringConfiguration(targetClass)
            && !SmartLifecycle.class.isAssignableFrom(targetClass)
            && settings.matchesBasePackage(targetClass)
            && hasLoggableMethod(targetClass);
    }

    private boolean hasLoggableMethod(Class<?> targetClass) {
        return Arrays.stream(targetClass.getMethods())
            .anyMatch(method -> settings.shouldLogMethod(
                AopUtils.getMostSpecificMethod(method, targetClass),
                hasLoggableAnnotation(targetClass, method)
            ));
    }

    private static boolean hasLoggableAnnotation(Class<?> targetClass, java.lang.reflect.Method method) {
        var specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);
        return AnnotationUtils.findAnnotation(specificMethod, ForensicLoggable.class) != null
            || AnnotationUtils.findAnnotation(method, ForensicLoggable.class) != null
            || AnnotationUtils.findAnnotation(targetClass, ForensicLoggable.class) != null;
    }

    private static boolean isLoggingInfrastructure(Class<?> targetClass) {
        return targetClass.getName().startsWith("de.burger.forensics.analytics.logging.")
            || BeanPostProcessor.class.isAssignableFrom(targetClass)
            || ForensicLogger.class.isAssignableFrom(targetClass)
            || ForensicLoggerFactory.class.isAssignableFrom(targetClass);
    }

    private static boolean isSpringConfiguration(Class<?> targetClass) {
        return AnnotationUtils.findAnnotation(targetClass, Configuration.class) != null;
    }

    private static Class<?>[] proxiedInterfaces(Class<?> targetClass) {
        var interfaces = new LinkedHashSet<Class<?>>();
        Arrays.stream(targetClass.getInterfaces()).forEach(interfaceType -> collectInterfaces(interfaceType, interfaces));
        return interfaces.stream()
            .filter(proxiedInterface -> proxiedInterface != SmartLifecycle.class)
            .toArray(Class<?>[]::new);
    }

    private static void collectInterfaces(Class<?> type, Set<Class<?>> interfaces) {
        if (type == null || type == Object.class) {
            return;
        }
        if (type.isInterface()) {
            interfaces.add(type);
        }
        interfaces.addAll(Arrays.asList(type.getInterfaces()));
        Arrays.stream(type.getInterfaces()).forEach(interfaceType -> collectInterfaces(interfaceType, interfaces));
        collectInterfaces(type.getSuperclass(), interfaces);
    }
}
