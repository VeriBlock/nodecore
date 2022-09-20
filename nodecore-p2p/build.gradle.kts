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
    implementation("io.ktor:ktor-network-jvm:$ktorVersion")

    // Logging
    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")

    implementation("org.apache.commons:commons-lang3:3.7")
    implementation("com.google.guava:guava:24.1-jre")
    implementation("dnsjava:dnsjava:3.3.0")

    // Unit tests
    testImplementation("junit:junit:4.13.2")
    // Mocking
    testImplementation("io.mockk:mockk:$mockkVersion")
    // Test assertions
    testImplementation("io.kotest:kotest-assertions-core:5.4.2")
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
