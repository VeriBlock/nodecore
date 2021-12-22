// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.adarshr.gradle.testlogger.TestLoggerExtension
import com.adarshr.gradle.testlogger.TestLoggerPlugin
import com.adarshr.gradle.testlogger.theme.ThemeType

plugins {
    java
    kotlin("jvm")
    idea
    id("java-library")
    `java-library`
    `maven-publish`
    id("com.jfrog.artifactory")
    id("com.adarshr.test-logger") version "3.1.0"
}

configurations.all {
    // check for updates every build for changing modules
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
}

val log4jVersion = "2.16.0"

dependencies {
    api(project(":veriblock-core"))
    api(project(":veriblock-extensions"))
    api(project(":nodecore-grpc"))
    api(project(":nodecore-p2p"))

    implementation("io.ktor:ktor-network-jvm:1.6.4")

    // Logging
    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4jVersion")
    implementation("io.github.microutils:kotlin-logging:2.1.16")

    implementation("org.freemarker:freemarker:2.3.31")

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.kotest:kotest-assertions-core-jvm:4.6.3")
    testImplementation("io.mockk:mockk:1.12.0")
}

setupJar("VeriBlock Lite Toolkit", "veriblock.lite")

val sourcesJar = setupSourcesJar()

publish(
    artifactName = "nodecore-spv",
    sourcesJar = sourcesJar
)

setupJacoco()

tasks.test {
    useJUnitPlatform()
    maxParallelForks = 1
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
tasks.withType<JavaCompile> {
    targetCompatibility = "1.8"
    sourceCompatibility = "1.8"
}

tasks.withType<Test> {
    useJUnitPlatform()

    reports {
        html.required.set(true)
        junitXml.required.set(true)
        junitXml.apply {
            isOutputPerTestCase = true // defaults to false
        }
    }
}

plugins.withType<TestLoggerPlugin> {
    configure<TestLoggerExtension> {
        theme = ThemeType.MOCHA
        showExceptions = true
        showStackTraces = true
        showFullStackTraces = false
        showCauses = true
        slowThreshold = 20000
        showSummary = true
        showSimpleNames = false
        showPassed = true
        showSkipped = true
        showFailed = true
        showStandardStreams = true
        showPassedStandardStreams = false
        showSkippedStandardStreams = false
        showFailedStandardStreams = true
        logLevel = LogLevel.LIFECYCLE
    }
}
