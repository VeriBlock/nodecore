plugins {
    java
    idea
    `java-library`
    `maven-publish`
    id("com.jfrog.artifactory")
}

dependencies {
    compile(project(":veriblock-core"))

    implementation("org.apache.logging.log4j:log4j-api:2.13.3")
    implementation("org.apache.logging.log4j:log4j-core:2.13.3")
    implementation("com.google.code.gson:gson:2.8.2")

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
