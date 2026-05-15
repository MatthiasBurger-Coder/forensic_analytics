plugins {
    `java-library`
    alias(libs.plugins.protobuf)
}

dependencies {
    api(project(":forensic-analytics-application"))

    implementation(project(":forensic-analytics-domain"))
    implementation(project(":forensic-analytics-observability"))
    implementation(libs.grpc.netty.shaded)
    api(libs.grpc.protobuf)
    api(libs.grpc.stub)
    api(libs.protobuf.java)
    compileOnly(libs.javax.annotation.api)

    testImplementation(libs.grpc.inprocess)
}

protobuf {
    protoc {
        artifact = libs.protobuf.protoc.get().toString()
    }
    plugins {
        create("grpc") {
            artifact = libs.protoc.gen.grpc.java.get().toString()
        }
    }
    generateProtoTasks {
        all().configureEach {
            plugins {
                create("grpc")
            }
        }
    }
}
