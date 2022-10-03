// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    java
    kotlin("jvm")
    idea
    id("java-library")
    `java-library`
    `maven-publish`
    id("com.jfrog.artifactory")
}

configurations.all {
    // check for updates every build for changing modules
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
}

dependencies {
    api(project(":veriblock-core"))
    api(project(":veriblock-extensions"))
    api(project(":nodecore-grpc"))
    api(project(":nodecore-p2p"))

    implementation("io.ktor:ktor-network-jvm:$ktorVersion")

    // Logging
    implementation("io.github.microutils:kotlin-logging:1.6.26")
    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")

    implementation("org.freemarker:freemarker:2.3.31")

    testImplementation("junit:junit:4.12")
    testImplementation("io.kotest:kotest-assertions-core-jvm:4.3.0")
    testImplementation("io.mockk:mockk:$mockkVersion")
}

tasks.test {
    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
    }
}

setupJar("VeriBlock Lite Toolkit", "veriblock.lite")

val sourcesJar = setupSourcesJar()

publish(
    artifactName = "nodecore-spv",
    sourcesJar = sourcesJar
)

setupJacoco()
