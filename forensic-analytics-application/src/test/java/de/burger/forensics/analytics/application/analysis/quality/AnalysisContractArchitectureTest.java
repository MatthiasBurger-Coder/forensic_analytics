package de.burger.forensics.analytics.application.analysis.quality;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "de.burger.forensics.analytics.application.analysis", importOptions = ImportOption.DoNotIncludeTests.class)
class AnalysisContractArchitectureTest {
    @ArchTest
    static final ArchRule analysis_contracts_remain_provider_neutral =
        noClasses()
            .that()
            .resideInAPackage("..application.analysis..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "de.burger.forensics.analytics.persistence..",
                "io.grpc..",
                "org.springframework..",
                "org.apache.kafka..",
                "com.rabbitmq..",
                "redis.clients..",
                "org.neo4j.."
            );
}
