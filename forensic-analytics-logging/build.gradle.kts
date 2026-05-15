plugins {
    `java-library`
}

dependencies {
    implementation(project(":forensic-analytics-observability"))

    implementation(libs.spring.aop)
    implementation(libs.spring.context) {
        exclude(group = "io.micrometer")
    }

    testImplementation(libs.spring.test)
}
