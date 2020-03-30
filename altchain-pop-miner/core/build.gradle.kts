// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
import com.google.protobuf.gradle.proto
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

plugins {
    java
    kotlin("jvm")
    idea
    `java-library`
    id("com.google.protobuf")
}

configurations.all {
    // check for updates every build for changing modules
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
}

dependencies {
    implementation(project(":veriblock-core"))
    implementation(project(":nodecore-grpc"))

    implementation(project(":altchain-sdk"))

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")

    // Logging
    implementation("io.github.microutils:kotlin-logging:1.6.26")
    implementation("ch.qos.logback:logback-classic:1.2.3")

    runtime("org.codehaus.groovy:groovy:2.4.12")

    testImplementation("org.apache.commons:commons-lang3:3.8.1")
    testImplementation("junit:junit:4.12")
    testImplementation("io.kotlintest:kotlintest-assertions:3.4.1")
}

protobuf {
    generatedFilesBaseDir = "$projectDir/src/generated"

    protoc {
        artifact = "com.google.protobuf:protoc:3.2.0"
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

tasks.test {
    testLogging {
        exceptionFormat = FULL
    }
}

setupJar("VeriBlock Lite Toolkit", "veriblock.lite")

setupJacoco()
