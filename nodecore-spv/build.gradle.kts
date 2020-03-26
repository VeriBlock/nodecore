// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    java
    kotlin("jvm")
    idea
    id("java-library")
    id("com.google.protobuf")
}

configurations.all {
    // check for updates every build for changing modules
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
}

dependencies {
    implementation(project(":veriblock-core"))
    implementation(project(":veriblock-extensions"))
    implementation(project(":nodecore-grpc"))
    implementation(project(":nodecore-p2p"))

    implementation("com.github.veriblock.alt-integration:core:0.0.7")

    runtime("org.codehaus.groovy:groovy:2.4.12")

    compile("ch.qos.logback:logback-classic:1.2.3")
    compile("org.xerial:sqlite-jdbc:3.28.0")
    compile("com.sparkjava:spark-core:2.8.0")

    compile("com.google.protobuf:protobuf-java:3.6.1")
    compile("com.google.protobuf:protobuf-java-util:3.6.1")

    compile("org.freemarker:freemarker:2.3.14")

    //veriblock-core and nodecore-grpc plus their dependencies
    implementation(fileTree(mapOf("dir" to "$projectDir/../lib/", "include" to listOf("*.jar"))))

    testImplementation("junit:junit:4.12")
    // required if you want to use Mockito for unit tests
    testCompile("org.mockito:mockito-core:2.7.22")
}

protobuf {
    generatedFilesBaseDir = "$projectDir/src/generated"

    protoc {
        artifact = "com.google.protobuf:protoc:3.5.1"
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
    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
    }
}

setupJar("VeriBlock Lite Toolkit", "veriblock.lite")

setupJacoco()
