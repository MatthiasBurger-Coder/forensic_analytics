package de.burger.forensics.analytics.services.joernanalysis.quality;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
    packages = "de.burger.forensics.analytics.services.joernanalysis",
    importOptions = ImportOption.DoNotIncludeTests.class
)
class JoernCpgAnalysisServiceArchitectureTest {
    @ArchTest
    static final ArchRule domain_and_application_stay_free_of_framework_transport_and_joern_types =
        noClasses()
            .that()
            .resideInAnyPackage(
                "de.burger.forensics.analytics.services.joernanalysis.domain..",
                "de.burger.forensics.analytics.services.joernanalysis.application.."
            )
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "org.springframework..",
                "io.grpc..",
                "com.google.protobuf..",
                "io.joern..",
                "de.burger.forensics.analytics.analysisjob.v1..",
                "de.burger.forensics.analytics.joerncpganalysis.v1..",
                "de.burger.forensics.analytics.repositoryanalysis.v1.."
            );

    @ArchTest
    static final ArchRule service_does_not_depend_on_current_monolith_implementation_modules =
        noClasses()
            .that()
            .resideInAPackage("de.burger.forensics.analytics.services.joernanalysis..")
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
            .resideOutsideOfPackage("de.burger.forensics.analytics.services.joernanalysis.bootstrap..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..");

    @ArchTest
    static final ArchRule domain_does_not_depend_on_application_adapters_or_bootstrap =
        noClasses()
            .that()
            .resideInAPackage("de.burger.forensics.analytics.services.joernanalysis.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "de.burger.forensics.analytics.services.joernanalysis.application..",
                "de.burger.forensics.analytics.services.joernanalysis.adapter..",
                "de.burger.forensics.analytics.services.joernanalysis.bootstrap.."
            );

    @ArchTest
    static final ArchRule application_does_not_depend_on_adapters_or_bootstrap =
        noClasses()
            .that()
            .resideInAPackage("de.burger.forensics.analytics.services.joernanalysis.application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "de.burger.forensics.analytics.services.joernanalysis.adapter..",
                "de.burger.forensics.analytics.services.joernanalysis.bootstrap.."
            );

    @ArchTest
    static final ArchRule process_execution_stays_inside_joern_outbound_adapter =
        noClasses()
            .that()
            .resideOutsideOfPackage("de.burger.forensics.analytics.services.joernanalysis.adapter.out.joern..")
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("java.lang.ProcessBuilder");
}
