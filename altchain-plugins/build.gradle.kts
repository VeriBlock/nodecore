// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
import groovy.lang.GroovyObject
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig

plugins {
    java
    kotlin("jvm")
    idea
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
    implementation(project(":altchain-sdk"))

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("io.ktor:ktor-client-gson:$ktorVersion")
    implementation("io.ktor:ktor-client-auth-jvm:$ktorVersion")
    implementation("com.google.guava:guava:26.0-jre")

    // Logging
    implementation("io.github.microutils:kotlin-logging:1.6.26")
    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.kotest:kotest-assertions-core:4.6.3")
    testImplementation("io.mockk:mockk:1.12.0")
}

// Exclude logback from everywhere to avoid the slf4j warning
configurations {
    all {
        exclude("ch.qos.logback")
    }
}

tasks.test {
    testLogging {
        exceptionFormat = FULL
    }
}

setupJar("VeriBlock Altchain Plugins", "veriblock.alt.plugins")
val sourcesJar = setupSourcesJar()

publish(
    artifactName = "altchain-plugins",
    sourcesJar = sourcesJar
)

setupJacoco()
