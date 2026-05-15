plugins {
    java
}

dependencies {
    testImplementation(project(":forensic-analytics-application"))
    testImplementation(project(":forensic-analytics-domain"))
    testImplementation(project(":forensic-analytics-engine"))
    testImplementation(project(":forensic-analytics-cli"))
    testImplementation(project(":forensic-analytics-observability"))
    testImplementation(project(":forensic-analytics-adapter-repository-source"))
    testImplementation(project(":forensic-analytics-adapter-joern-docker"))
    testImplementation(project(":forensic-analytics-ingestion-grpc"))
    testImplementation(project(":forensic-analytics-persistence"))
    testImplementation(project(":forensic-analytics-rest"))
    testImplementation(project(":forensic-analytics-bootstrap"))
    testImplementation(project(":forensic-analytics-boot-app"))
    testImplementation(libs.grpc.inprocess)
}
