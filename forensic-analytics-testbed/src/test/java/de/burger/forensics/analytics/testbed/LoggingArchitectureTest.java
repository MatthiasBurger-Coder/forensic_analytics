package de.burger.forensics.analytics.testbed;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "de.burger.forensics.analytics", importOptions = ImportOption.DoNotIncludeTests.class)
class LoggingArchitectureTest {
    @ArchTest
    static final ArchRule domain_and_application_do_not_depend_on_logging_infrastructure =
        noClasses()
            .that()
            .resideInAnyPackage(
                "de.burger.forensics.analytics.domain..",
                "de.burger.forensics.analytics.application.."
            )
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "de.burger.forensics.analytics.observability..",
                "org.slf4j..",
                "ch.qos.logback..",
                "org.apache.logging.log4j..",
                "org.springframework..",
                "org.aspectj..",
                "io.micrometer..",
                "io.opentelemetry.."
            );

    @ArchTest
    static final ArchRule observability_does_not_depend_on_core_adapters_or_external_logging =
        noClasses()
            .that()
            .resideInAPackage("de.burger.forensics.analytics.observability..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "de.burger.forensics.analytics.domain..",
                "de.burger.forensics.analytics.application..",
                "de.burger.forensics.analytics.persistence..",
                "de.burger.forensics.analytics.rest..",
                "de.burger.forensics.analytics.ingestion.grpc..",
                "de.burger.forensics.analytics.ingestion.v1..",
                "io.grpc..",
                "com.sun.net.httpserver..",
                "com.google.gson..",
                "org.slf4j..",
                "ch.qos.logback..",
                "org.apache.logging.log4j..",
                "org.springframework..",
                "org.aspectj.."
            );

    @ArchTest
    static final ArchRule inbound_adapters_do_not_import_concrete_logging_providers_or_aop =
        noClasses()
            .that()
            .resideInAnyPackage(
                "de.burger.forensics.analytics.rest..",
                "de.burger.forensics.analytics.ingestion.grpc..",
                "de.burger.forensics.analytics.cli..",
                "de.burger.forensics.analytics.bootstrap.."
            )
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "org.slf4j..",
                "ch.qos.logback..",
                "org.apache.logging.log4j..",
                "org.springframework..",
                "org.aspectj..",
                "io.micrometer..",
                "io.opentelemetry.."
            );
}
