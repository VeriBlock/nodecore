// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
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
    implementation(project(":veriblock-core"))
    implementation(project(":veriblock-extensions"))
    implementation(project(":nodecore-grpc"))
    implementation(project(":nodecore-p2p"))

    implementation("io.ktor:ktor-network-jvm:$ktorVersion")

    // Logging
    implementation("io.github.microutils:kotlin-logging:1.6.26")
    implementation("org.apache.logging.log4j:log4j-api:2.13.3")
    implementation("org.apache.logging.log4j:log4j-core:2.13.3")

    implementation("org.freemarker:freemarker:2.3.14")

    //veriblock-core and nodecore-grpc plus their dependencies
    implementation(fileTree(mapOf("dir" to "$projectDir/../lib/", "include" to listOf("*.jar"))))

    testImplementation("junit:junit:4.12")
    testImplementation("io.kotest:kotest-assertions-core-jvm:4.3.0")
    // required if you want to use Mockito for unit tests
    testCompile("org.mockito:mockito-core:2.7.22")
    testImplementation("io.mockk:mockk:1.9.3")
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
