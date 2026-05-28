package de.burger.forensics.analytics.services.analysisorchestrator.quality;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
    packages = "de.burger.forensics.analytics.services.analysisorchestrator",
    importOptions = ImportOption.DoNotIncludeTests.class
)
class AnalysisOrchestratorServiceArchitectureTest {
    @ArchTest
    static final ArchRule domain_and_application_stay_free_of_framework_and_transport_types =
        noClasses()
            .that()
            .resideInAnyPackage(
                "de.burger.forensics.analytics.services.analysisorchestrator.domain..",
                "de.burger.forensics.analytics.services.analysisorchestrator.application.."
            )
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "org.springframework..",
                "io.grpc..",
                "com.google.protobuf..",
                "de.burger.forensics.analytics.analysisjob.v1.."
            );

    @ArchTest
    static final ArchRule service_does_not_depend_on_current_monolith_implementation_modules =
        noClasses()
            .that()
            .resideInAPackage("de.burger.forensics.analytics.services.analysisorchestrator..")
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
            .resideOutsideOfPackage("de.burger.forensics.analytics.services.analysisorchestrator.bootstrap..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..");

    @ArchTest
    static final ArchRule service_does_not_import_other_service_implementation_classes =
        noClasses()
            .that()
            .resideInAPackage("de.burger.forensics.analytics.services.analysisorchestrator..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "de.burger.forensics.analytics.services.javaastanalysis..",
                "de.burger.forensics.analytics.services.javaparseranalysis..",
                "de.burger.forensics.analytics.services.repositoryanalysis..",
                "de.burger.forensics.analytics.services.repositorysource..",
                "de.burger.forensics.analytics.services.btmgeneration..",
                "de.burger.forensics.analytics.services.joerncpganalysis..",
                "de.burger.forensics.analytics.services.joernanalysis..",
                "de.burger.forensics.analytics.services.analysisstore..",
                "de.burger.forensics.analytics.services.ingestion..",
                "de.burger.forensics.analytics.services.gateway.."
            );

    @ArchTest
    static final ArchRule service_does_not_depend_on_worker_execution_libraries =
        noClasses()
            .that()
            .resideInAPackage("de.burger.forensics.analytics.services.analysisorchestrator..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "com.github.javaparser..",
                "io.joern..",
                "overflowdb.."
            );

    @ArchTest
    static final ArchRule service_does_not_spawn_processes =
        noClasses()
            .that()
            .resideInAPackage("de.burger.forensics.analytics.services.analysisorchestrator..")
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName(ProcessBuilder.class.getName());

    @ArchTest
    static final ArchRule domain_does_not_depend_on_application_adapters_or_bootstrap =
        noClasses()
            .that()
            .resideInAPackage("de.burger.forensics.analytics.services.analysisorchestrator.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "de.burger.forensics.analytics.services.analysisorchestrator.application..",
                "de.burger.forensics.analytics.services.analysisorchestrator.adapter..",
                "de.burger.forensics.analytics.services.analysisorchestrator.bootstrap.."
            );

    @ArchTest
    static final ArchRule application_does_not_depend_on_adapters_or_bootstrap =
        noClasses()
            .that()
            .resideInAPackage("de.burger.forensics.analytics.services.analysisorchestrator.application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "de.burger.forensics.analytics.services.analysisorchestrator.adapter..",
                "de.burger.forensics.analytics.services.analysisorchestrator.bootstrap.."
            );
}
