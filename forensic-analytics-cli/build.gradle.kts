plugins {
    application
}

dependencies {
    implementation(project(":forensic-analytics-application"))
    implementation(project(":forensic-analytics-domain"))
}

application {
    mainClass.set("de.burger.forensics.analytics.cli.ForensicAnalyticsCli")
}
