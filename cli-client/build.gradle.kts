plugins {
    application
}

dependencies {
    implementation(libs.gson)
}

application {
    mainClass.set("de.burger.forensics.analytics.services.cliclient.bootstrap.CliClientApplication")
}
