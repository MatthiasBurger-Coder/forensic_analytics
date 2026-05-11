plugins {
    java
}

dependencies {
    testImplementation(project(":forensic-analytics-application"))
    testImplementation(project(":forensic-analytics-domain"))
    testImplementation(project(":forensic-analytics-engine"))
    testImplementation(project(":forensic-analytics-cli"))
    testImplementation(project(":forensic-analytics-adapter-repository-source"))
    testImplementation(project(":forensic-analytics-adapter-joern-docker"))
}
