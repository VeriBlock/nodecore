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

    // Sockets
    implementation("io.ktor:ktor-network:2.0.3")

    // Logging
    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")

    implementation("org.apache.commons:commons-lang3:3.7")
    implementation("com.google.guava:guava:24.1-jre")
    implementation("dnsjava:dnsjava:3.3.0")

    // Unit tests
    testImplementation("junit:junit:4.12")
    // Mocking
    testImplementation("io.mockk:mockk:1.9.3")
    // Test assertions
    testImplementation("io.kotest:kotest-assertions-core:4.3.2")
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
