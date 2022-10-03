import groovy.lang.GroovyObject
import org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig

// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

plugins {
    java
    kotlin("jvm")
    idea
    `java-library`
    `maven-publish`
    id("com.jfrog.artifactory")
    kotlin("plugin.serialization") version kotlinVersion
}

configurations.all {
    // check for updates every build for changing modules
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

    implementation(project(":veriblock-core"))
    implementation(project(":veriblock-shell"))
    implementation(project(":nodecore-grpc"))
    implementation(project(":altchain-sdk"))
    runtimeOnly(project(":altchain-plugins"))

    // Dependency Injection
    implementation("org.koin:koin-core:$koinVersion")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")

    // Logging
    implementation("io.github.microutils:kotlin-logging:1.6.26")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")

    // Persistence
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    // Metrics
    implementation("io.micrometer:micrometer-core:1.1.4")
    implementation("io.micrometer:micrometer-registry-prometheus:1.9.4")

    implementation("commons-cli:commons-cli:1.4")
    implementation("com.google.guava:guava:26.0-jre")
    implementation("com.diogonunes:JCDP:2.0.3.1")

    testImplementation("junit:junit:4.12")
    testImplementation("org.apache.commons:commons-lang3:3.8.1")
    testImplementation("io.kotest:kotest-assertions-core:4.3.2")
}

setupJar("PoP Miners Common Library", "org.veriblock.miners.pop")
val sourcesJar = setupSourcesJar()

publish(
    artifactName = "pop-miners-common",
    sourcesJar = sourcesJar
)

setupJacoco()
