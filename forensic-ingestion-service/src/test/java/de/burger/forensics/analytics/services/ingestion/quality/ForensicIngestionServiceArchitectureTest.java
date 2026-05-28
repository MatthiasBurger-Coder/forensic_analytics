package de.burger.forensics.analytics.services.ingestion.quality;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
    packages = "de.burger.forensics.analytics.services.ingestion",
    importOptions = ImportOption.DoNotIncludeTests.class
)
class ForensicIngestionServiceArchitectureTest {
    @ArchTest
    static final ArchRule domain_and_application_stay_free_of_framework_and_transport_types =
        noClasses()
            .that()
            .resideInAnyPackage(
                "de.burger.forensics.analytics.services.ingestion.domain..",
                "de.burger.forensics.analytics.services.ingestion.application.."
            )
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "org.springframework..",
                "io.grpc..",
                "com.google.protobuf..",
                "de.burger.forensics.analytics.ingestion.v1.."
            );

    @ArchTest
    static final ArchRule service_does_not_depend_on_current_monolith_implementation_modules =
        noClasses()
            .that()
            .resideInAPackage("de.burger.forensics.analytics.services.ingestion..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "de.burger.forensics.analytics.application..",
                "de.burger.forensics.analytics.domain..",
                "de.burger.forensics.analytics.persistence..",
                "de.burger.forensics.analytics.observability..",
                "de.burger.forensics.analytics.ingestion.grpc.."
            );
}
