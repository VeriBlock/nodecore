// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
import groovy.lang.GroovyObject
import org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig

plugins {
    java
    kotlin("jvm")
    idea
    `java-library`
    `maven-publish`
    id("com.jfrog.artifactory")
}

dependencies {
    implementation(project(":veriblock-core"))

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

    // Configuration
    implementation("io.github.config4k:config4k:0.4.0")

    // Logging
    implementation("io.github.microutils:kotlin-logging:1.6.26")

    // Reflection
    implementation("org.reflections:reflections:0.9.12")

    // Testing
    testImplementation("junit:junit:4.12")
}

setupJar("VeriBlock Altchain SDK", "org.veriblock.alt.sdk")
val sourcesJar = setupSourcesJar()

publish(
    artifactName = "altchain-sdk",
    sourcesJar = sourcesJar
)

setupJacoco()
