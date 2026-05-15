package de.burger.forensics.analytics.observability;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "de.burger.forensics.analytics.observability", importOptions = ImportOption.DoNotIncludeTests.class)
class ObservabilityArchitectureTest {
    @ArchTest
    static final ArchRule observability_stays_independent_from_adapters_frameworks_and_core =
        noClasses()
            .that()
            .resideInAPackage("de.burger.forensics.analytics.observability..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "de.burger.forensics.analytics.application..",
                "de.burger.forensics.analytics.domain..",
                "de.burger.forensics.analytics.persistence..",
                "de.burger.forensics.analytics.rest..",
                "de.burger.forensics.analytics.ingestion.grpc..",
                "io.grpc..",
                "org.springframework..",
                "org.aspectj..",
                "org.slf4j..",
                "com.sun.net.httpserver..",
                "com.google.gson.."
            );
}
