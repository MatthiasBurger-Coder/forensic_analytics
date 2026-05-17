package de.burger.forensics.analytics.services.repositoryanalysis.quality;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
    packages = "de.burger.forensics.analytics.services.repositoryanalysis",
    importOptions = ImportOption.DoNotIncludeTests.class
)
class RepositoryAnalysisServiceArchitectureTest {
    @ArchTest
    static final ArchRule domain_and_application_stay_free_of_framework_and_transport_types =
        noClasses()
            .that()
            .resideInAnyPackage(
                "de.burger.forensics.analytics.services.repositoryanalysis.domain..",
                "de.burger.forensics.analytics.services.repositoryanalysis.application.."
            )
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "org.springframework..",
                "io.grpc..",
                "com.google.protobuf..",
                "de.burger.forensics.analytics.repositoryanalysis.v1.."
            );

    @ArchTest
    static final ArchRule service_does_not_depend_on_current_monolith_implementation_modules =
        noClasses()
            .that()
            .resideInAPackage("de.burger.forensics.analytics.services.repositoryanalysis..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "de.burger.forensics.analytics.adapter..",
                "de.burger.forensics.analytics.application..",
                "de.burger.forensics.analytics.boot..",
                "de.burger.forensics.analytics.bootstrap..",
                "de.burger.forensics.analytics.cli..",
                "de.burger.forensics.analytics.domain..",
                "de.burger.forensics.analytics.engine..",
                "de.burger.forensics.analytics.ingestion..",
                "de.burger.forensics.analytics.logging..",
                "de.burger.forensics.analytics.observability..",
                "de.burger.forensics.analytics.persistence..",
                "de.burger.forensics.analytics.rest..",
                "de.burger.forensics.analytics.testbed.."
            );

    @ArchTest
    static final ArchRule domain_does_not_depend_on_application_adapters_or_bootstrap =
        noClasses()
            .that()
            .resideInAPackage("de.burger.forensics.analytics.services.repositoryanalysis.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "de.burger.forensics.analytics.services.repositoryanalysis.application..",
                "de.burger.forensics.analytics.services.repositoryanalysis.adapter..",
                "de.burger.forensics.analytics.services.repositoryanalysis.bootstrap.."
            );

    @ArchTest
    static final ArchRule application_does_not_depend_on_adapters_or_bootstrap =
        noClasses()
            .that()
            .resideInAPackage("de.burger.forensics.analytics.services.repositoryanalysis.application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "de.burger.forensics.analytics.services.repositoryanalysis.adapter..",
                "de.burger.forensics.analytics.services.repositoryanalysis.bootstrap.."
            );
}
