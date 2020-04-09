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

configurations.all {
    // check for updates every build for changing modules
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

    runtimeOnly("org.codehaus.groovy:groovy:2.4.12")

    api(project(":veriblock-core"))
    api(project(":nodecore-grpc"))
    api(project(":pop-miners:pop-miners-common"))

    api("org.bitcoinj:bitcoinj-core:0.15.8")

    // Dependency Injection
    implementation("org.koin:koin-core:$koinVersion")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")

    // Logging
    implementation("io.github.microutils:kotlin-logging:1.6.26")
    implementation("ch.qos.logback:logback-classic:1.2.3")

    implementation("org.apache.commons:commons-lang3:3.0")
    implementation("com.google.guava:guava:26.0-jre")
    implementation("com.diogonunes:JCDP:2.0.3.1")

    // Scheduling
    implementation("org.quartz-scheduler:quartz:2.2.1")
    implementation("org.quartz-scheduler:quartz-jobs:2.2.1")

    testImplementation("junit:junit:4.12")
    testImplementation("org.apache.commons:commons-lang3:3.8.1")
    testImplementation("io.kotlintest:kotlintest-assertions:3.4.1")
}

setupJar("VeriBlock PoP Miners Common Library", "org.veriblock.miners.pop")
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
    artifactName = "veriblock-pop-miners-common",
    sourcesJar = sourcesJar
)

setupJacoco()
