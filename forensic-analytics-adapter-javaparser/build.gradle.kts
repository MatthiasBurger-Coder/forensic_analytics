plugins {
    java
}

dependencies {
    implementation(project(":forensic-analytics-application"))
    implementation(project(":forensic-analytics-domain"))
    implementation(project(":forensic-analytics-observability"))
    implementation(libs.java.parser.core)
}
