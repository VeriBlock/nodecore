plugins {
    java
    kotlin("jvm")
    idea
    `java-library`
    `maven-publish`
    id("com.jfrog.artifactory")
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

    implementation(project(":veriblock-core"))
    implementation(project(":nodecore-grpc"))

    // Logging
    implementation("org.apache.logging.log4j:log4j-api:2.13.3")
    implementation("org.apache.logging.log4j:log4j-core:2.13.3")

    implementation("org.apache.commons:commons-lang3:3.7")
    implementation("com.google.guava:guava:24.1-jre")
    implementation("dnsjava:dnsjava:3.3.0")
}

// Exclude logback from everywhere to avoid the slf4j warning
configurations {
    all {
        exclude("ch.qos.logback")
    }
}

setupJar("VeriBlock NodeCore P2P", "nodecore.p2p")

val sourcesJar = setupSourcesJar()

publish(
    artifactName = "nodecore-p2p",
    sourcesJar = sourcesJar
)

setupJacoco()
