package de.burger.forensics.analytics.logging.spring;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

final class ForensicLoggingEnabledCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        var value = context.getEnvironment().getProperty("forensics.analytics.logging.enabled", "true");
        return !"false".equalsIgnoreCase(value.strip());
    }
}
