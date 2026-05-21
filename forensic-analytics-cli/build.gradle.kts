plugins {
    application
}

dependencies {
    implementation(project(":forensic-analytics-application"))
    implementation(project(":forensic-analytics-domain"))
    implementation(project(":forensic-analytics-ingestion-request"))
    implementation(project(":forensic-analytics-observability"))
    implementation(project(":forensic-analytics-persistence"))
    implementation(libs.gson)
}

application {
    mainClass.set("de.burger.forensics.analytics.cli.ForensicAnalyticsCli")
}
