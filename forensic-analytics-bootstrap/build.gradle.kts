plugins {
    application
}

dependencies {
    implementation(project(":forensic-analytics-adapter-repository-source"))
    implementation(project(":forensic-analytics-application"))
    implementation(project(":forensic-analytics-ingestion-grpc"))
    implementation(project(":forensic-analytics-persistence"))
    implementation(project(":forensic-analytics-rest"))
    implementation(libs.grpc.netty.shaded)

    testImplementation(project(":forensic-analytics-domain"))
}

application {
    mainClass.set("de.burger.forensics.analytics.bootstrap.ForensicAnalyticsServerApplication")
}
