plugins {
    java
    idea
    `java-library`
    `maven-publish`
    id("com.jfrog.artifactory")
}

dependencies {

    api(project(":nodecore-ucp"))
    api(project(":nodecore-grpc"))

    api("commons-cli:commons-cli:1.4")
    api("org.apache.commons:commons-lang3:3.0")
    api("com.google.protobuf:protobuf-gradle-plugin:0.8.6")
    api("org.reflections:reflections:0.9.11")
    api("com.diogonunes:JCDP:2.0.3.1")
    api("org.quartz-scheduler:quartz:2.2.1")
    api("org.quartz-scheduler:quartz-jobs:2.2.1")
    api("com.j256.ormlite:ormlite-core:6.1")
    api("com.j256.ormlite:ormlite-jdbc:6.1")
    api("org.xerial:sqlite-jdbc:$sqliteVersion")
    api("org.bitcoinj:bitcoinj-core:0.14.7")
    api("org.apache.logging.log4j:log4j-api:$log4jVersion")
    api("org.apache.logging.log4j:log4j-core:$log4jVersion")
    api("org.slf4j:slf4j-api:$slf4jVersion")
    api("com.google.code.gson:gson:2.8.2")
    api("io.netty:netty-buffer:4.1.30.Final")
    api("io.vertx:vertx-core:3.6.2")

    testImplementation("junit:junit:4.12")
    // Testing Assertions
    testImplementation("io.kotest:kotest-assertions-core:4.3.2")
}

// Exclude logback from everywhere to avoid the slf4j warning
configurations {
    all {
        exclude("ch.qos.logback")
    }
}

setupJar("VeriBlock Extensions", "org.veriblock.extensions")
val sourcesJar = setupSourcesJar()

publish(
    artifactName = "veriblock-extensions",
    sourcesJar = sourcesJar
)

setupJacoco()
