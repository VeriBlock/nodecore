// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
import com.netflix.gradle.plugins.deb.Deb
import groovy.util.Eval
import org.gradle.api.internal.plugins.WindowsStartScriptGenerator
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.redline_rpm.header.Flags.EQUAL
import org.redline_rpm.header.Flags.GREATER

plugins {
    java
    kotlin("jvm")
    kotlin("plugin.serialization") version kotlinVersion
    idea
    application
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

    runtimeOnly("org.codehaus.groovy:groovy:2.4.12")

    implementation(project(":veriblock-core"))

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.0")

    // Logging
    implementation("io.github.microutils:kotlin-logging:1.6.26")
    implementation("ch.qos.logback:logback-classic:1.2.3")

    implementation("org.bouncycastle:bcprov-jdk15on:1.60")
}

application.applicationName = "debug"
application.mainClassName = "org.veriblock.core.debug.Debug"

setupJar("Debug Tool", "veriblock.miners.core.debug")

tasks.distZip {
    archiveFileName.set("${application.applicationName}-${prettyVersion()}.zip")
}
tasks.distTar {
    archiveFileName.set("${application.applicationName}-${prettyVersion()}.tar")
}
