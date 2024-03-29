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
}

dependencies {
    implementation(project(":veriblock-core"))

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

    // Json serialization
    implementation("com.google.code.gson:gson:2.8.2")

    // Logging
    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")

    implementation("com.diogonunes:JCDP:2.0.3.1")

    implementation("org.jline:jline:3.13.1")
    implementation("org.jline:jline-terminal:3.13.1")
    implementation("org.jline:jline-terminal-jansi:3.21.0")

    implementation("com.google.guava:guava:20.0")
    implementation("com.opencsv:opencsv:4.3.2")

    testImplementation("junit:junit:4.12")
    testImplementation("io.kotest:kotest-runner-junit5:4.3.2")
}

// Exclude logback from everywhere to avoid the slf4j warning
configurations {
    all {
        exclude("ch.qos.logback")
    }
}

setupJar("VeriBlock Shell", "org.veriblock.shell")
val sourcesJar = setupSourcesJar()

publish(
    artifactName = "shell",
    sourcesJar = sourcesJar
)

setupJacoco()
