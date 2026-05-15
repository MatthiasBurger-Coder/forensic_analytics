package de.burger.forensics.analytics.logging;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "de.burger.forensics.analytics.logging", importOptions = ImportOption.DoNotIncludeTests.class)
class LoggingArchitectureTest {
    private static final Set<String> ACCEPTED_SPRING_PACKAGES = Set.of(
        "org.springframework.aop.",
        "org.springframework.beans.",
        "org.springframework.context.",
        "org.springframework.core."
    );

    @ArchTest
    static final ArchRule logging_does_not_depend_on_core_or_adapters =
        noClasses()
            .that()
            .resideInAPackage("de.burger.forensics.analytics.logging..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "de.burger.forensics.analytics.domain..",
                "de.burger.forensics.analytics.application..",
                "de.burger.forensics.analytics.persistence..",
                "de.burger.forensics.analytics.rest..",
                "de.burger.forensics.analytics.ingestion.grpc..",
                "de.burger.forensics.analytics.ingestion.v1..",
                "de.burger.forensics.analytics.adapter..",
                "io.grpc..",
                "com.sun.net.httpserver..",
                "com.google.gson..",
                "org.slf4j..",
                "ch.qos.logback..",
                "org.apache.logging.log4j..",
                "org.springframework.boot..",
                "org.aspectj..",
                "io.micrometer..",
                "io.opentelemetry.."
            );

    @ArchTest
    static final ArchRule root_logging_api_stays_independent_from_spring =
        noClasses()
            .that()
            .resideInAPackage("de.burger.forensics.analytics.logging")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..");

    @ArchTest
    static final ArchRule logging_uses_only_accepted_spring_framework_packages =
        classes()
            .that()
            .resideInAPackage("de.burger.forensics.analytics.logging..")
            .should(dependOnlyOnAcceptedSpringPackages());

    private static ArchCondition<JavaClass> dependOnlyOnAcceptedSpringPackages() {
        return new ArchCondition<>("depend only on accepted Spring Framework packages") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                item.getDirectDependenciesFromSelf().stream()
                    .filter(dependency -> dependency.getTargetClass().getName().startsWith("org.springframework."))
                    .filter(dependency -> isUnexpectedSpringDependency(dependency.getTargetClass()))
                    .forEach(dependency -> events.add(SimpleConditionEvent.violated(item, dependency.getDescription())));
            }
        };
    }

    private static boolean isUnexpectedSpringDependency(JavaClass targetClass) {
        return ACCEPTED_SPRING_PACKAGES.stream()
            .noneMatch(acceptedPackage -> targetClass.getName().startsWith(acceptedPackage));
    }
}
