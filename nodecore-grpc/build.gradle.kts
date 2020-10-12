// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.proto
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
    java
    kotlin("jvm")
    idea
    `java-library`
    `maven-publish`
    id("com.google.protobuf")
    id("com.jfrog.artifactory")
}

dependencies {
    compile(project(":veriblock-core"))

    compile("io.grpc:grpc-netty-shaded:1.25.0")
    compile("io.grpc:grpc-protobuf:1.25.0")
    compile("io.grpc:grpc-stub:1.25.0")

    compileOnly("javax.annotation:javax.annotation-api:1.2")
}

protobuf {
    generatedFilesBaseDir = "$projectDir/src/generated"

    protoc {
        artifact = "com.google.protobuf:protoc:3.6.1"
    }

    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.19.0"
        }
    }

    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                id("grpc") {
                    outputSubDir = "java"
                }
            }
        }
    }
}

sourceSets {
    main {
        proto {}
        java {
            srcDir("$projectDir/src/generated/main/java")
        }
    }
}

setupJar("VeriBlock NodeCore gRPC API", "nodecore.api.grpc")

val sourcesJar = setupSourcesJar()

publish(
    artifactName = "nodecore-grpc",
    sourcesJar = sourcesJar
)

tasks.clean.configure {
    delete("src/generated")
}

setupJacoco()
