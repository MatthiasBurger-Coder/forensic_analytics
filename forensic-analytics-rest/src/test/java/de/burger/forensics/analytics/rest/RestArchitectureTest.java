package de.burger.forensics.analytics.rest;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "de.burger.forensics.analytics", importOptions = ImportOption.DoNotIncludeTests.class)
class RestArchitectureTest {
    @ArchTest
    static final ArchRule application_and_domain_do_not_depend_on_rest_infrastructure =
        noClasses()
            .that()
            .resideInAnyPackage(
                "de.burger.forensics.analytics.application..",
                "de.burger.forensics.analytics.domain.."
            )
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "com.sun.net.httpserver..",
                "com.google.gson..",
                "de.burger.forensics.analytics.observability.."
            );

    @ArchTest
    static final ArchRule rest_does_not_depend_on_grpc_or_persistence =
        noClasses()
            .that()
            .resideInAPackage("de.burger.forensics.analytics.rest..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "de.burger.forensics.analytics.ingestion.grpc..",
                "de.burger.forensics.analytics.persistence.."
            );
}
