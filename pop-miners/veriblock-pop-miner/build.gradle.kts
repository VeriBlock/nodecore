// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
import com.netflix.gradle.plugins.deb.Deb
import org.gradle.api.internal.plugins.WindowsStartScriptGenerator
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.redline_rpm.header.Flags.EQUAL
import org.redline_rpm.header.Flags.GREATER

plugins {
    java
    kotlin("jvm")
    idea
    application
    kotlin("plugin.serialization") version kotlinVersion
    id("nebula.ospackage")
}

configurations.all {
    // check for updates every build for changing modules
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("org.codehaus.groovy:groovy:2.4.12")

    implementation(project(":nodecore-ucp"))
    implementation(project(":veriblock-shell"))
    implementation(project(":pop-miners:veriblock-pop-miners-common"))

    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("ch.qos.logback.contrib:logback-json-classic:0.1.5")
    implementation("ch.qos.logback.contrib:logback-jackson:0.1.5")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.9.3")
    implementation("org.apache.commons:commons-lang3:3.0")
    implementation("com.google.protobuf:protobuf-gradle-plugin:0.8.6")
    implementation("org.reflections:reflections:0.9.11")
    implementation("com.diogonunes:JCDP:2.0.3.1")
    implementation("org.quartz-scheduler:quartz:2.2.1")
    implementation("org.quartz-scheduler:quartz-jobs:2.2.1")
    implementation("org.bitcoinj:bitcoinj-core:0.15.8")

    // Database
    implementation("org.jetbrains.exposed:exposed-core:0.23.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.23.1")
    implementation("com.zaxxer:HikariCP:3.3.1")
    implementation("org.xerial:sqlite-jdbc:3.23.1")

    // Dependency Injection
    implementation("org.koin:koin-core:$koinVersion")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:$coroutinesVersion")

    // Http API
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-locations:$ktorVersion")
    // Serialization
    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.9.8")
    // Metrics
    implementation("io.ktor:ktor-metrics-micrometer:$ktorVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:1.1.4")
    // Swagger integration, expecting an official integration to be released soon
    implementation("com.github.papsign:Ktor-OpenAPI-Generator:reworked-model-SNAPSHOT")

    // Protobuf Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.20.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:0.20.0")

    // Logging
    implementation("io.github.microutils:kotlin-logging:1.6.26")

    // Test
    testImplementation("junit:junit:4.12")
    // Mocks
    testImplementation("org.mockito:mockito-core:2.+")
    // Better mocks
    testImplementation("io.mockk:mockk:1.9.3")
    // Better assertions
    testImplementation("io.kotlintest:kotlintest-assertions:3.4.2")
}

tasks.test {
    testLogging {
        exceptionFormat = FULL
    }
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
    standardOutput = System.out
    if (project.hasProperty("appArgs")) {
        args = groovy.util.Eval.me(properties["appArgs"] as String) as List<String>
    }
}

setupJar("VeriBlock Proof-of-Proof (PoP) Miner", "org.veriblock.miners.pop")
val sourcesJar = setupSourcesJar()

application.applicationName = "veriblock-pop-miner"
application.mainClassName = "org.veriblock.miners.pop.VeriBlockPoPMiner"

tasks.distZip {
    archiveFileName.set("${application.applicationName}-${prettyVersion()}.zip")
}
tasks.distTar {
    archiveFileName.set("${application.applicationName}-${prettyVersion()}.tar")
}

tasks.startScripts {
    (windowsStartScriptGenerator as WindowsStartScriptGenerator).template = resources.text.fromFile("windowsStartScript.txt")
}

apply(plugin = "nebula.ospackage")

tasks.register<Deb>("createDeb") {
    dependsOn("installDist")
    packageName = application.applicationName
    packageDescription = "VeriBlock Proof-of-Proof (PoP) Miner"
    archStr = "amd64"
    distribution = "bionic"
    url = "https://github.com/VeriBlock/nodecore"
    version = prettyVersion()
    release = "1"

    requires("default-jre", "1.8", GREATER or EQUAL)
        .or("openjdk-8-jre", "8u242-b08", GREATER or EQUAL)

    into("/opt/nodecore/${application.applicationName}")

    from("$buildDir/install/${application.applicationName}/LICENSE") {
        into("/opt/nodecore/${application.applicationName}")
    }
    from("$buildDir/install/${application.applicationName}/lib") {
        into("lib")
    }
    from("$buildDir/install/${application.applicationName}/bin") {
        into("bin")
        exclude("*.bat")
    }
    link("/usr/local/bin/$packageName", "/opt/nodecore/$packageName/bin/$packageName")
}

setupJacoco()

distributions {
    main {
        contents {
            from ("./src/main/resources/application-default.conf") {
                into("bin")
            }
            rename("application-default.conf", "application.conf")
        }
    }
}
