package de.burger.forensics.analytics.services.repositorysource.quality;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
    packages = "de.burger.forensics.analytics.services.repositorysource",
    importOptions = ImportOption.DoNotIncludeTests.class
)
class RepositorySourceServiceArchitectureTest {
    @ArchTest
    static final ArchRule domain_and_application_stay_free_of_framework_and_transport_types =
        noClasses()
            .that()
            .resideInAnyPackage(
                "de.burger.forensics.analytics.services.repositorysource.domain..",
                "de.burger.forensics.analytics.services.repositorysource.application.."
            )
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "org.springframework..",
                "io.grpc..",
                "com.google.protobuf..",
                "java.sql..",
                "javax.sql..",
                "org.h2..",
                "org.postgresql..",
                "liquibase..",
                "de.burger.forensics.analytics.repositoryanalysis.v1..",
                "de.burger.forensics.analytics.javaastanalysis.v1..",
                "de.burger.forensics.analytics.analysisjob.v1.."
            );

    @ArchTest
    static final ArchRule service_does_not_depend_on_current_monolith_implementation_modules =
        noClasses()
            .that()
            .resideInAPackage("de.burger.forensics.analytics.services.repositorysource..")
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
    static final ArchRule spring_dependencies_stay_inside_service_bootstrap =
        noClasses()
            .that()
            .resideOutsideOfPackage("de.burger.forensics.analytics.services.repositorysource.bootstrap..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..");

    @ArchTest
    static final ArchRule postgres_adapter_stays_inside_outbound_adapter_or_bootstrap =
        noClasses()
            .that()
            .resideOutsideOfPackage("de.burger.forensics.analytics.services.repositorysource.adapter.out.postgres..")
            .and()
            .resideOutsideOfPackage("de.burger.forensics.analytics.services.repositorysource.bootstrap..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("de.burger.forensics.analytics.services.repositorysource.adapter.out.postgres..");

    @ArchTest
    static final ArchRule jdbc_dependencies_stay_inside_outbound_adapters_or_bootstrap =
        noClasses()
            .that()
            .resideOutsideOfPackage("de.burger.forensics.analytics.services.repositorysource.adapter.out..")
            .and()
            .resideOutsideOfPackage("de.burger.forensics.analytics.services.repositorysource.bootstrap..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("java.sql..", "javax.sql..");

    @ArchTest
    static final ArchRule postgresql_and_liquibase_dependencies_stay_inside_postgres_adapter_or_bootstrap =
        noClasses()
            .that()
            .resideOutsideOfPackage("de.burger.forensics.analytics.services.repositorysource.adapter.out.postgres..")
            .and()
            .resideOutsideOfPackage("de.burger.forensics.analytics.services.repositorysource.bootstrap..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.postgresql..", "liquibase..");

    @ArchTest
    static final ArchRule domain_does_not_depend_on_application_adapters_or_bootstrap =
        noClasses()
            .that()
            .resideInAPackage("de.burger.forensics.analytics.services.repositorysource.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "de.burger.forensics.analytics.services.repositorysource.application..",
                "de.burger.forensics.analytics.services.repositorysource.adapter..",
                "de.burger.forensics.analytics.services.repositorysource.bootstrap.."
            );

    @ArchTest
    static final ArchRule application_does_not_depend_on_adapters_or_bootstrap =
        noClasses()
            .that()
            .resideInAPackage("de.burger.forensics.analytics.services.repositorysource.application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "de.burger.forensics.analytics.services.repositorysource.adapter..",
                "de.burger.forensics.analytics.services.repositorysource.bootstrap.."
            );
}
