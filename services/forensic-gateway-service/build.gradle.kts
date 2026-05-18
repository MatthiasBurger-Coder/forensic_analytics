plugins {
    application
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(libs.gson)
    implementation(libs.spring.boot) {
        exclude(group = "io.micrometer")
    }
    implementation(libs.spring.boot.autoconfigure) {
        exclude(group = "io.micrometer")
    }

    testImplementation(libs.spring.test)
}

application {
    mainClass.set("de.burger.forensics.analytics.services.gateway.bootstrap.ForensicGatewayServiceApplication")
}
