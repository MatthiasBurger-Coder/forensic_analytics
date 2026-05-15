package de.burger.forensics.analytics.ingestion.grpc.quality;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "de.burger.forensics.analytics")
class IngestionGrpcArchitectureTest {
    @ArchTest
    static final ArchRule application_and_domain_do_not_depend_on_grpc_or_proto =
        noClasses()
            .that()
            .resideInAnyPackage("..application..", "..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("io.grpc..", "..ingestion.v1..", "..observability..");

    @ArchTest
    static final ArchRule grpc_adapter_does_not_depend_on_persistence =
        noClasses()
            .that()
            .resideInAPackage("..ingestion.grpc..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..persistence..");

    @ArchTest
    static final ArchRule observability_does_not_depend_on_grpc_or_frameworks =
        noClasses()
            .that()
            .resideInAPackage("..observability..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "io.grpc..",
                "org.springframework..",
                "org.aspectj..",
                "org.slf4j..",
                "..persistence..",
                "..ingestion.v1.."
            );

    @SuppressWarnings("unused")
    private final ClassFileImporter importerReference = new ClassFileImporter();
}
