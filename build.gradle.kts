// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
import com.adarshr.gradle.testlogger.TestLoggerExtension
import com.adarshr.gradle.testlogger.theme.ThemeType
import org.gradle.api.logging.LogLevel.LIFECYCLE
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// only jdk8,11..14 are supported
val supportedJdks = listOf(
    JavaVersion.VERSION_1_8, // jdk8
    JavaVersion.VERSION_11,  // jdk11
    JavaVersion.VERSION_12,  // jdk12
    JavaVersion.VERSION_13,  // jdk13
    JavaVersion.VERSION_14,  // jdk14
)
if(supportedJdks.indexOf(JavaVersion.current()) == -1){
    throw GradleException("This build must be run with java[8,11,12,13,14], your version: ${JavaVersion.current()}")
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (this.requested.group == "org.apache.logging.log4j") {
            this.useVersion(log4jVersion)
        }
    }
}

buildscript {
    repositories {
        maven { url = uri("https://plugins.gradle.org/m2/") }
        maven { url = uri("https://repo1.maven.org/maven2") }
    }
    dependencies {
        classpath("com.netflix.nebula:gradle-ospackage-plugin:1.12.2")
        classpath("org.ajoberstar:grgit:2.3.0")
        classpath("org.jfrog.buildinfo:build-info-extractor-gradle:4.15.1")
    }
}

plugins {
    kotlin("jvm") version kotlinVersion
    id("nebula.release") version "6.0.1"
    id("com.jfrog.artifactory") version "4.24.23"
    jacoco
//    id("org.ajoberstar.grgit") version "5.0.0"
    id("org.sonarqube") version "2.8"
    id("nebula.ospackage") version "8.3.0"
    id("com.adarshr.test-logger") version "3.1.0"
    id("com.savvasdalkitsis.module-dependency-graph") version "0.10"
}

allprojects {
    repositories {
        mavenLocal()
        maven { url = uri("https://repo1.maven.org/maven2") }
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
        maven("https://jitpack.io")
        mavenCentral()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            kotlinOptions.jvmTarget = "1.8"
            freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
        }
    }

    apply(plugin="maven-publish")
    apply(plugin="com.jfrog.artifactory")

    artifactory {
        setContextUrl(properties["artifactory_url"])
        publish(closureOf<org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig> {
            repository(delegateClosureOf<groovy.lang.GroovyObject> {
                setProperty("repoKey", properties["artifactory_repoKey"] as String)
                setProperty("username", properties["artifactory_user"])
                setProperty("password", properties["artifactory_password"])
                setProperty("maven", true)
            })

            defaults(delegateClosureOf<groovy.lang.GroovyObject> {
                invokeMethod("publications", "mavenJava")
                setProperty("publishArtifacts", true)
            })
        })
    }

    tasks.withType<Test> {
        jvmArgs = listOf("-Xmx3g")
    }
}

subprojects {
    version = version
}

sonarqube {
    properties {
        property("sonar.projectKey", "vbk:nodecore")
        property("sonar.projectName", "NodeCore Application Suite")
        property("sonar.projectVersion", prettyVersion())
        property("sonar.host.url", "https://sonar.veriblock.org")
        property("sonar.java.binaries", "${project.projectDir}/build/classes")
        property("sonar.java.libraries", "${project.projectDir}/build/libs")
        property("sonar.java.test.binaries", "${project.projectDir}/build/test-results/test/binary")
        property("sonar.junit.reportsPaths", "${project.projectDir}/build/test-results/**/*.xml")
        property("sonar.jacoco.reportPaths", "${project.projectDir}/build/jacoco/test.exec")
        property("sonar.exclusions", "**/generated/*.java")
    }
}

jacoco {
    toolVersion = "0.8.8"
}

tasks.wrapper {
    gradleVersion = "7.5"
}

val teamcityPrint by tasks.creating(DefaultTask::class) {
    group = "versioning"
    doFirst {
        // Print version to configure TeamCity
        println("##teamcity[buildNumber \'${prettyVersion()}\']")
    }
}

allprojects {
    tasks.withType<KotlinCompile> {
        dependsOn(teamcityPrint)
    }
}

val incrementVersion by tasks.creating(DefaultTask::class) {
    group = "versioning"
    doFirst { updateVersion { flatten(it) } }
}
val incrementReleaseCandidate by tasks.creating(DefaultTask::class) {
    group = "versioning"
    doFirst { updateVersion { releaseCandidate() } }
}
val pushVersionTag by tasks.creating(DefaultTask::class) {
    dependsOn(teamcityPrint)
    group = "versioning"
    doFirst { pushVersionTag() }
}

allprojects {
    apply {
        plugin("com.adarshr.test-logger")
    }

    configure<TestLoggerExtension> {
        theme = ThemeType.STANDARD
        showCauses = true
        showExceptions = true
        showStackTraces = true
        showFullStackTraces = false
        slowThreshold = 2000
        showSummary = true
        showSimpleNames = false
        showPassed = true
        showSkipped = true
        showFailed = true
        showStandardStreams = false
        showPassedStandardStreams = false
        showSkippedStandardStreams = true
        showFailedStandardStreams = true
        logLevel = LIFECYCLE
    }

    afterEvaluate {
        tasks.withType<Tar>{
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
        tasks.withType<Zip> {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
        tasks.withType<Copy> {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
    }
}
