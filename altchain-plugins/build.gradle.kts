// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
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

    implementation("com.github.kittinunf.fuel:fuel:2.2.0")

    // Logging
    implementation("io.github.microutils:kotlin-logging:1.6.26")

    testImplementation("junit:junit:4.12")
    testImplementation("io.kotlintest:kotlintest-assertions:3.4.2")
    testImplementation("io.mockk:mockk:1.9.3")
}

tasks.test {
    testLogging {
        exceptionFormat = FULL
    }
}

setupJar("VeriBlock Altchain Plugins", "veriblock.alt.plugins")
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
    artifactName = "altchain-plugins",
    sourcesJar = sourcesJar
)

setupJacoco()
