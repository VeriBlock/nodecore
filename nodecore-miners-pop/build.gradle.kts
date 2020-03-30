// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
import com.google.protobuf.gradle.proto
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import org.gradle.api.internal.plugins.WindowsStartScriptGenerator
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

plugins {
    java
    kotlin("jvm")
    idea
    application
    id("com.google.protobuf")
}

configurations.all {
    // check for updates every build for changing modules
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
}

dependencies {
    // Using compile(in all dependencies because of the jar's classpath reference)

    // Kotlin
    compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compile("org.jetbrains.kotlin:kotlin-reflect")

    compile("org.codehaus.groovy:groovy:2.4.12")

    compile(project(":nodecore-ucp"))
    compile(project(":nodecore-grpc"))
    compile(project(":veriblock-shell"))

    compile("ch.qos.logback:logback-classic:1.2.3")
    compile("commons-cli:commons-cli:1.4")
    compile("org.apache.commons:commons-lang3:3.0")
    compile("com.google.protobuf:protobuf-gradle-plugin:0.8.6")
    compile("com.google.code.gson:gson:2.8.2")
    compile("org.reflections:reflections:0.9.11")
    compile("com.diogonunes:JCDP:2.0.3.1")
    compile("org.quartz-scheduler:quartz:2.2.1")
    compile("org.quartz-scheduler:quartz-jobs:2.2.1")
    compile("com.j256.ormlite:ormlite-core:5.1")
    compile("com.j256.ormlite:ormlite-jdbc:5.1")
    compile("org.xerial:sqlite-jdbc:3.23.1")
    compile("org.bitcoinj:bitcoinj-core:0.15.6")

    // Dependency Injection
    compile("org.koin:koin-core:$koinVersion")

    // Coroutines
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")

    // Http API
    compile("io.ktor:ktor-server-netty:$ktorVersion")
    compile("io.ktor:ktor-locations:$ktorVersion")
    compile("io.ktor:ktor-gson:$ktorVersion")
    // Swagger integration, expecting an official integration to be released soon
    compile("com.github.nielsfalk:ktor-swagger:0.5.0")

    // Logging
    compile("io.github.microutils:kotlin-logging:1.6.25")

    // Test
    testCompile("junit:junit:4.12")
    testCompile("org.mockito:mockito-core:2.+")
    // Better mocks
    testImplementation("io.mockk:mockk:1.9.3")
    // Better testing framework
    testCompile("io.kotlintest:kotlintest-runner-junit5:3.4.2")
    // Better assertions
    testCompile("io.kotlintest:kotlintest-assertions:3.4.2")
}

protobuf {
    generatedFilesBaseDir = "$projectDir/src/generated"

    protoc {
        artifact = "com.google.protobuf:protoc:3.6.1"
    }
}

sourceSets {
    main {
        proto {}
        java {
            srcDir("$projectDir/src/generated/main/java")
        }
    }
}

tasks.test {
    useJUnitPlatform()
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

setupJar("VeriBlock Proof-of-Proof (PoP) Miner", "nodecore.miners.pop")
val sourcesJar = setupSourcesJar()

application.applicationName = "nodecore-pop"
application.mainClassName = "nodecore.miners.pop.ProgramKt"

tasks.distZip {
    archiveFileName.set("${application.applicationName}-${prettyVersion()}.zip")
}
tasks.distTar {
    archiveFileName.set("${application.applicationName}-${prettyVersion()}.tar")
}

tasks.startScripts {
    dependsOn(tasks.jar)
    classpath = files(tasks.jar.get().archiveFile)
    (windowsStartScriptGenerator as WindowsStartScriptGenerator).template = resources.text.fromFile("windowsStartScript.txt")
}

setupJacoco()
