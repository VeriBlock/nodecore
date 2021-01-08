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
    // Kotlin
    api("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    api("org.jetbrains.kotlin:kotlin-reflect")

    // Json serialization
    api("com.google.code.gson:gson:2.8.5")

    // Configuration
    api("io.github.config4k:config4k:0.4.0")
    api("commons-cli:commons-cli:1.4")

    // Crypto
    api("org.bouncycastle:bcprov-jdk15on:1.68")

    // Coroutines
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")

    // Logging
    api("io.github.microutils:kotlin-logging:1.6.26")
    implementation("org.apache.logging.log4j:log4j-api:2.13.3")
    implementation("org.apache.logging.log4j:log4j-core:2.13.3")

    // NTP time utility
    implementation("com.lyft.kronos:kronos-java:0.0.1-alpha10")

    api("io.netty:netty-buffer:4.1.30.Final")
    api("io.vertx:vertx-core:3.6.2")
    api("com.google.guava:guava:20.0")

    // Joda Time
    api("joda-time:joda-time:2.9.9")

    // Unit testing
    testImplementation("junit:junit:4.12")

    // Testing Assertions
    testImplementation("io.kotlintest:kotlintest-assertions:3.4.1")
}

setupJar("VeriBlock Core Library", "org.veriblock.core")
val sourcesJar = setupSourcesJar()

publish(
    artifactName = "veriblock-core",
    sourcesJar = sourcesJar
)

setupJacoco()
