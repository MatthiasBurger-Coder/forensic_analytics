plugins {
    application
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":forensic-analytics-adapter-joern-docker"))
    implementation(project(":forensic-analytics-adapter-repository-source"))
    implementation(project(":forensic-analytics-application"))
    implementation(project(":forensic-analytics-ingestion-grpc"))
    implementation(project(":forensic-analytics-observability"))
    implementation(project(":forensic-analytics-persistence"))
    implementation(project(":forensic-analytics-rest"))
    implementation(libs.grpc.netty.shaded)
    implementation(libs.spring.boot) {
        exclude(group = "io.micrometer")
    }
    implementation(libs.spring.boot.autoconfigure) {
        exclude(group = "io.micrometer")
    }

    testImplementation(libs.spring.test)
}

application {
    mainClass.set("de.burger.forensics.analytics.boot.ForensicAnalyticsApplication")
}
