package de.burger.forensics.analytics.services.gateway.quality;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertFalse;

@AnalyzeClasses(
    packages = "de.burger.forensics.analytics.services.gateway",
    importOptions = ImportOption.DoNotIncludeTests.class
)
class ForensicGatewayServiceArchitectureTest {
    @ArchTest
    static final ArchRule domain_and_application_stay_free_of_framework_and_transport_types =
        noClasses()
            .that()
            .resideInAnyPackage(
                "de.burger.forensics.analytics.services.gateway.domain..",
                "de.burger.forensics.analytics.services.gateway.application.."
            )
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "org.springframework..",
                "com.sun.net.httpserver..",
                "io.grpc..",
                "com.google.protobuf..",
                "de.burger.forensics.analytics.repositoryanalysis.v1.."
            );

    @ArchTest
    static final ArchRule gateway_does_not_depend_on_worker_service_implementations =
        noClasses()
            .that()
            .resideInAPackage("de.burger.forensics.analytics.services.gateway..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "de.burger.forensics.analytics.services.repositoryanalysis..",
                "de.burger.forensics.analytics.services.analysisstore..",
                "de.burger.forensics.analytics.services.btmgeneration..",
                "de.burger.forensics.analytics.services.javaastanalysis..",
                "de.burger.forensics.analytics.services.joerncpganalysis..",
                "de.burger.forensics.analytics.services.ingestion.."
            );

    @ArchTest
    static final ArchRule service_does_not_depend_on_current_monolith_implementation_modules =
        noClasses()
            .that()
            .resideInAPackage("de.burger.forensics.analytics.services.gateway..")
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
            .resideInAPackage("de.burger.forensics.analytics.services.gateway.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "de.burger.forensics.analytics.services.gateway.application..",
                "de.burger.forensics.analytics.services.gateway.adapter..",
                "de.burger.forensics.analytics.services.gateway.bootstrap.."
            );

    @ArchTest
    static final ArchRule application_does_not_depend_on_adapters_or_bootstrap =
        noClasses()
            .that()
            .resideInAPackage("de.burger.forensics.analytics.services.gateway.application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "de.burger.forensics.analytics.services.gateway.adapter..",
                "de.burger.forensics.analytics.services.gateway.bootstrap.."
            );

    @Test
    void gatewayBuildFileDoesNotDeclareProjectDependencies() throws IOException {
        var content = Files.readString(findGatewayBuildFile());

        assertFalse(content.contains("project("));
    }

    private static Path findGatewayBuildFile() {
        var current = Path.of("").toAbsolutePath();
        while (current != null) {
            var candidate = current.resolve("services/forensic-gateway-service/build.gradle.kts");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("services/forensic-gateway-service/build.gradle.kts not found");
    }
}
