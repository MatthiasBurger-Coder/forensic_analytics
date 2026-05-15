plugins {
    java
}

dependencies {
    implementation(project(":forensic-analytics-application"))
    implementation(project(":forensic-analytics-domain"))
    implementation(libs.gson)
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("--add-modules", "jdk.httpserver"))
}

tasks.withType<Test>().configureEach {
    jvmArgs("--add-modules", "jdk.httpserver")
}
