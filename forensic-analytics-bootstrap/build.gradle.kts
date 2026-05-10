plugins {
    application
}

dependencies {
    implementation(project(":forensic-analytics-application"))
    implementation(project(":forensic-analytics-ingestion-grpc"))
    implementation(project(":forensic-analytics-persistence"))
    implementation(libs.grpc.netty.shaded)
}

application {
    mainClass.set("de.burger.forensics.analytics.bootstrap.ForensicAnalyticsServerApplication")
}
