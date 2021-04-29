plugins {
    java
    idea
    `java-library`
    `maven-publish`
    id("com.jfrog.artifactory")
}

dependencies {
    api(project(":veriblock-core"))

    api("org.apache.logging.log4j:log4j-api:$log4jVersion")
    api("org.apache.logging.log4j:log4j-core:$log4jVersion")
    api("com.google.code.gson:gson:2.8.2")
    api("org.slf4j:slf4j-api:$slf4jVersion")

    testImplementation("junit:junit:4.12")
}

// Exclude logback from everywhere to avoid the slf4j warning
configurations {
    all {
        exclude("ch.qos.logback")
    }
}

setupJar("VeriBlock NodeCore Universal Client Protocol", "nodecore.api.ucp")
val sourcesJar = setupSourcesJar()

publish(
    artifactName = "nodecore-ucp",
    sourcesJar = sourcesJar
)

setupJacoco()
