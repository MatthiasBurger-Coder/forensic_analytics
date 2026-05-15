package de.burger.forensics.analytics.testbed;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "de.burger.forensics.analytics", importOptions = ImportOption.DoNotIncludeTests.class)
class SpringBootArchitectureTest {
    @ArchTest
    static final ArchRule spring_dependencies_stay_inside_boot_app =
        noClasses()
            .that()
            .resideOutsideOfPackage("de.burger.forensics.analytics.boot..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..");
}
