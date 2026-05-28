plugins {
    application
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.protobuf)
}

dependencies {
    implementation(libs.grpc.netty.shaded)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.stub)
    implementation(libs.protobuf.java)
    implementation(libs.spring.boot) {
        exclude(group = "io.micrometer")
    }
    implementation(libs.spring.boot.autoconfigure) {
        exclude(group = "io.micrometer")
    }
    compileOnly(libs.javax.annotation.api)

    testImplementation(libs.grpc.inprocess)
    testImplementation(libs.spring.test)
}

sourceSets {
    main {
        proto {
            srcDir(rootProject.file("contracts/grpc"))
            include("**/forensic-ingestion.proto")
            exclude("**/analysis-job.proto")
            exclude("**/btm-generation.proto")
            exclude("**/java-ast-analysis.proto")
            exclude("**/joern-cpg-analysis.proto")
            exclude("**/repository-analysis.proto")
        }
    }
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

application {
    mainClass.set("de.burger.forensics.analytics.services.ingestion.bootstrap.ForensicIngestionServiceApplication")
}
