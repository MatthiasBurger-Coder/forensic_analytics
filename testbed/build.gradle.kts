plugins {
    java
}

description = "Non-production integration and system testbed for FA-MSA-001."

dependencies {
    testImplementation(libs.grpc.inprocess)
}
