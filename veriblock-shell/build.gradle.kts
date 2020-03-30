import groovy.lang.GroovyObject
import org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig

// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
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

    compile("ch.qos.logback:logback-classic:1.2.3")
    compile("com.diogonunes:JCDP:2.0.3.1")

    compile("org.jline:jline:3.13.1")
    compile("org.jline:jline-terminal:3.13.1")
    compile("org.jline:jline-terminal-jansi:3.13.1")

    compile("com.google.guava:guava:20.0")
    implementation("com.opencsv:opencsv:4.3.2")

    testImplementation("junit:junit:4.12")
    testImplementation("io.kotlintest:kotlintest-runner-junit5:3.3.0")
}

setupJar("VeriBlock Shell", "org.veriblock.shell")
val sourcesJar = setupSourcesJar()

artifactory {
    setContextUrl(properties["artifactory_url"])
    publish(closureOf<PublisherConfig> {
        repository(delegateClosureOf<GroovyObject> {
            setProperty("repoKey", properties["artifactory_repoKey"] as String)
            setProperty("username", properties["artifactory_user"])
            setProperty("password", properties["artifactory_password"])
            setProperty("maven", true)
        })

        defaults(delegateClosureOf<GroovyObject> {
            invokeMethod("publications", "mavenJava")
            setProperty("publishArtifacts", true)
        })
    })
}

publish(
    artifactName = "shell",
    sourcesJar = sourcesJar
)

setupJacoco()
