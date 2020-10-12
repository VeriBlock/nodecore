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

    compile(project(":veriblock-core"))
    compile(project(":nodecore-grpc"))

    compile("ch.qos.logback:logback-classic:1.2.3")
    compile("org.apache.commons:commons-lang3:3.7")
    compile("com.google.guava:guava:24.1-jre")
    compile("dnsjava:dnsjava:2.1.8")
}


setupJar("VeriBlock NodeCore P2P", "nodecore.p2p")

val sourcesJar = setupSourcesJar()

publish(
    artifactName = "nodecore-p2p",
    sourcesJar = sourcesJar
)

setupJacoco()
