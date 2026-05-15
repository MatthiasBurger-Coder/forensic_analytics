package de.burger.forensics.analytics.logging.spring;

import de.burger.forensics.analytics.logging.ForensicLogLevel;
import de.burger.forensics.analytics.logging.ForensicLoggerFactory;
import de.burger.forensics.analytics.logging.ForensicLoggingMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForensicLoggingAutoConfigurationTest {
    @Test
    void createsFactorySettingsAndPostProcessor() {
        var configuration = new ForensicLoggingAutoConfiguration();
        var environment = new MockEnvironment()
            .withProperty("forensics.analytics.logging.mode", "annotated")
            .withProperty("forensics.analytics.logging.default-level", "warn");

        var factory = configuration.forensicLoggerFactory();
        var settings = configuration.forensicLoggingSettings(environment);
        var processor = ForensicLoggingAutoConfiguration.forensicLoggingBeanPostProcessor(settings, factory);

        assertInstanceOf(ForensicLoggerFactory.class, factory);
        assertTrue(settings.enabled());
        assertTrue(settings.matchesBasePackage(ForensicLoggingAutoConfiguration.class));
        assertTrue(settings.shouldLogMethod(publicMethod(), true));
        assertFalse(settings.shouldLogMethod(publicMethod(), false));
        assertNotNull(processor);
    }

    @Test
    void enabledConditionDefaultsToEnabledAndHonorsExplicitFalse() {
        var condition = new ForensicLoggingEnabledCondition();

        assertTrue(condition.matches(conditionContext(new MockEnvironment()), metadata()));
        assertFalse(condition.matches(
            conditionContext(new MockEnvironment().withProperty("forensics.analytics.logging.enabled", " FALSE ")),
            metadata()
        ));
    }

    private static java.lang.reflect.Method publicMethod() {
        try {
            return Sample.class.getMethod("run");
        } catch (NoSuchMethodException error) {
            throw new AssertionError(error);
        }
    }

    private static ConditionContext conditionContext(Environment environment) {
        return new ConditionContext() {
            @Override
            public BeanDefinitionRegistry getRegistry() {
                return null;
            }

            @Override
            public ConfigurableListableBeanFactory getBeanFactory() {
                return null;
            }

            @Override
            public Environment getEnvironment() {
                return environment;
            }

            @Override
            public ResourceLoader getResourceLoader() {
                return null;
            }

            @Override
            public ClassLoader getClassLoader() {
                return ForensicLoggingAutoConfigurationTest.class.getClassLoader();
            }
        };
    }

    private static AnnotatedTypeMetadata metadata() {
        return AnnotationMetadata.introspect(Sample.class);
    }

    public static final class Sample {
        public void run() {
        }
    }
}
