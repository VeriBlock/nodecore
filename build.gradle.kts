// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath("com.netflix.nebula:nebula-release-plugin:6.0.0")
        classpath("com.netflix.nebula:gradle-ospackage-plugin:1.12.2")
        classpath("org.ajoberstar:grgit:1.1.0")
        classpath("org.jfrog.buildinfo:build-info-extractor-gradle:4.15.1")
        classpath("com.google.protobuf:protobuf-gradle-plugin:0.8.8")
    }
}

plugins {
    kotlin("jvm") version kotlinVersion
    id("nebula.release") version "6.0.0"
    id("com.jfrog.artifactory") version "4.15.1"
    jacoco
    id("org.sonarqube") version "2.8"
    id("nebula.ospackage") version "8.3.0"
}

allprojects {
    repositories {
        mavenLocal()
        jcenter()
        maven("https://jitpack.io")
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
}

subprojects {
    version = version
}

nebulaRelease {
    addReleaseBranchPattern("""(release(-|\/))?\d+(.\d+)?(.\d+)?""")
}

tasks.named("releaseCheck").configure {
    doLast {
        // Print version to configure TeamCity
        println("##teamcity[buildNumber \'${prettyVersion()}\']")
    }
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
    toolVersion = "0.8.5"
}

tasks.wrapper {
    gradleVersion = "6.3"
}
