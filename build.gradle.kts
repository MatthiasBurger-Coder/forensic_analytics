import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

plugins {
    base
    alias(libs.plugins.protobuf) apply false
}

allprojects {
    group = "de.burger.forensics.analytics"
    version = "0.1.0-SNAPSHOT"
}

val coverageExcludes = listOf(
    "de/burger/forensics/analytics/ingestion/v1/**",
)
val junitBom = libs.junit.bom
val archUnitJunit5 = libs.archunit.junit5
val jacocoVersion = libs.versions.jacoco

subprojects {
    apply(plugin = "java")
    apply(plugin = "jacoco")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    dependencies {
        add("testImplementation", platform(junitBom))
        add("testImplementation", "org.junit.jupiter:junit-jupiter")
        add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
        add("testImplementation", archUnitJunit5)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    extensions.configure<JacocoPluginExtension> {
        toolVersion = jacocoVersion.get()
    }

    val sourceSets = extensions.getByType<SourceSetContainer>()
    val mainOutput = sourceSets.named("main").get().output
    val filteredClasses = files(mainOutput.classesDirs.files.map {
        fileTree(it) {
            exclude(coverageExcludes)
        }
    })

    tasks.withType<JacocoReport>().configureEach {
        dependsOn(tasks.named("test"))
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
        classDirectories.setFrom(filteredClasses)
    }

    tasks.withType<JacocoCoverageVerification>().configureEach {
        dependsOn(tasks.named("test"))
        classDirectories.setFrom(filteredClasses)
        violationRules {
            rule {
                limit {
                    minimum = "0.80".toBigDecimal()
                }
            }
        }
    }
}

tasks.register("checkPackageCoverage") {
    group = "verification"
    description = "Verifies per-package line and branch coverage from JaCoCo XML reports."

    dependsOn(subprojects.map { it.tasks.named("jacocoTestReport") })

    doLast {
        val reportFile = layout.buildDirectory.file("reports/coverage/package-coverage.txt").get().asFile
        reportFile.parentFile.mkdirs()

        val rows = mutableListOf<String>()
        val failures = mutableListOf<String>()

        rows += "Package coverage report"
        rows += "Line threshold: 80.00%"
        rows += "Branch threshold: 80.00%"
        rows += "packageName\tlineCoverage\tbranchCoverage\tmissedLines\tmissedBranches\ttotalLines\ttotalBranches"

        subprojects.forEach { subproject ->
            val xml = subproject.layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml").get().asFile
            if (!xml.exists()) {
                return@forEach
            }

            val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            documentBuilder.setEntityResolver { _, _ -> InputSource(StringReader("")) }
            val document = documentBuilder.parse(xml)
            val packages = document.getElementsByTagName("package")

            for (index in 0 until packages.length) {
                val packageNode = packages.item(index)
                val packageName = packageNode.attributes.getNamedItem("name").nodeValue.replace('/', '.')
                val counters = packageNode.childNodes

                var missedLines = 0
                var coveredLines = 0
                var missedBranches = 0
                var coveredBranches = 0

                for (counterIndex in 0 until counters.length) {
                    val counter = counters.item(counterIndex)
                    if (counter.nodeName != "counter") {
                        continue
                    }
                    val type = counter.attributes.getNamedItem("type").nodeValue
                    val missed = counter.attributes.getNamedItem("missed").nodeValue.toInt()
                    val covered = counter.attributes.getNamedItem("covered").nodeValue.toInt()

                    when (type) {
                        "LINE" -> {
                            missedLines = missed
                            coveredLines = covered
                        }
                        "BRANCH" -> {
                            missedBranches = missed
                            coveredBranches = covered
                        }
                    }
                }

                val totalLines = missedLines + coveredLines
                val totalBranches = missedBranches + coveredBranches
                if (totalLines == 0) {
                    continue
                }

                val lineCoverage = coveredLines.toDouble() / totalLines
                val branchCoverage = if (totalBranches == 0) null else coveredBranches.toDouble() / totalBranches
                val lineText = "%.2f%%".format(lineCoverage * 100)
                val branchText = branchCoverage?.let { "%.2f%%".format(it * 100) } ?: "n/a"

                rows += listOf(
                    packageName,
                    lineText,
                    branchText,
                    missedLines,
                    missedBranches,
                    totalLines,
                    totalBranches,
                ).joinToString("\t")

                if (lineCoverage < 0.80) {
                    failures += "$packageName line coverage is $lineText"
                }
                if (branchCoverage != null && branchCoverage < 0.80) {
                    failures += "$packageName branch coverage is $branchText"
                }
            }
        }

        reportFile.writeText(rows.joinToString(System.lineSeparator()) + System.lineSeparator())

        if (failures.isNotEmpty()) {
            throw GradleException(
                "Package coverage verification failed:${System.lineSeparator()}" +
                    failures.joinToString(System.lineSeparator()),
            )
        }
    }
}
