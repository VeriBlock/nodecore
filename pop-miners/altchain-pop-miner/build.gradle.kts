// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
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
    idea
    application
    kotlin("plugin.serialization") version kotlinVersion
    id("com.github.node-gradle.node") version "3.4.0"
    id("nebula.ospackage")
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

    implementation(project(":veriblock-core"))
    implementation(project(":veriblock-shell"))
    implementation(project(":nodecore-grpc"))
    implementation(project(":altchain-sdk"))
    implementation(project(":nodecore-spv"))
    implementation(project(":pop-miners:pop-miners-common"))
    runtimeOnly(project(":altchain-plugins"))

    // Dependency Injection
    implementation("org.koin:koin-core:$koinVersion")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")

    // HTTP API
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-locations:$ktorVersion")
    implementation("io.ktor:ktor-gson:$ktorVersion")
    // Serialization
    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.9.8")
    // Metrics
    implementation("io.ktor:ktor-metrics-micrometer:$ktorVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:1.10.2")
    // Swagger integration, expecting an official integration to be released soon
    implementation("com.github.papsign:Ktor-OpenAPI-Generator:$ktorOpenApiGeneratorVersion")
    // SPA
    implementation("com.github.lamba92:ktor-spa:$ktorSpaVersion")

    // Logging
    implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4jVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonDatabindVersion")

    // Protobuf Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$kotlinxSerializationVersion")

    implementation("commons-cli:commons-cli:$commonsCliVersion")
    implementation("com.google.code.gson:gson:$gsonVersion")
    implementation("com.google.guava:guava:$guavaVersion")
    implementation("com.diogonunes:JCDP:$JCDPVersion")

    // Database
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    implementation("com.zaxxer:HikariCP:$hikariCPVersion")
    implementation("org.xerial:sqlite-jdbc:$sqliteVersion")

    testImplementation("junit:junit:$junit4Version")
    testImplementation("org.apache.commons:commons-lang3:$commonsLang3Version")
    testImplementation("io.kotest:kotest-assertions-core:$kotestAssertionsCoreVersion")

    // Integration tests
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    testImplementation("io.ktor:ktor-client-cio:$ktorVersion")
    testImplementation("io.ktor:ktor-client-gson:$ktorVersion")
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

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
    standardOutput = System.out

    if (project.hasProperty("appArgs")) {
        args = Eval.me(properties["appArgs"] as String) as List<String>
    }
}

application.applicationName = "altchain-pop-miner"
application.mainClassName = "org.veriblock.miners.pop.AltchainPoPMiner"

setupJar("Altchain Proof-of-Proof (PoP) Miner", "veriblock.miners.pop")

tasks.distZip {
    archiveFileName.set("${application.applicationName}-${prettyVersion()}.zip")
}
tasks.distTar {
    archiveFileName.set("${application.applicationName}-${prettyVersion()}.tar")
}

tasks.startScripts {
    (windowsStartScriptGenerator as WindowsStartScriptGenerator).template =
        resources.text.fromFile("windowsStartScript.txt")
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

setupJar("Altchain PoP Miner", "org.veriblock.miners.pop")
val sourcesJar = setupSourcesJar()

publish(
    artifactName = "altchain-pop-miner",
    sourcesJar = sourcesJar
)

setupJacoco()

val spaClientDir = "$projectDir/src/main/spa-client"

node {
    version.set("15.14.0")
    npmVersion.set("7.7.6")
    download.set(true)
    nodeProjectDir.set(File(spaClientDir))
}

sourceSets {
    main {
        resources {
            srcDirs("$spaClientDir/dist", "$projectDir/src/main/resources")
        }
    }
}

tasks.named("processResources") {
    dependsOn("npm_run_build")
}

// Cache NPM build
tasks.named("npm_run_build") {
    outputs.cacheIf { true }
}

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

customTests("integration")
