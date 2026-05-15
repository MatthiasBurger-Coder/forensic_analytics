package de.burger.forensics.analytics.logging.spring;

import de.burger.forensics.analytics.logging.ForensicLoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
public class ForensicLoggingAutoConfiguration {
    @Bean
    public ForensicLoggerFactory forensicLoggerFactory() {
        return new ForensicLoggerFactory();
    }

    @Bean
    ForensicLoggingSettings forensicLoggingSettings(Environment environment) {
        return ForensicLoggingSettings.from(environment);
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @Conditional(ForensicLoggingEnabledCondition.class)
    static ForensicLoggingBeanPostProcessor forensicLoggingBeanPostProcessor(
        ForensicLoggingSettings settings,
        ForensicLoggerFactory loggerFactory
    ) {
        return new ForensicLoggingBeanPostProcessor(settings, loggerFactory);
    }
}
