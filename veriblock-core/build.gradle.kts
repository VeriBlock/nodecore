import groovy.lang.GroovyObject
import org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig

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
    api("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    api("org.jetbrains.kotlin:kotlin-reflect")

    // Json serialization
    api("com.google.code.gson:gson:2.8.5")

    // Configuration
    api("io.github.config4k:config4k:0.4.0")
    api("commons-cli:commons-cli:1.4")

    // Logging
    api("ch.qos.logback:logback-classic:1.2.3")
    api("io.github.microutils:kotlin-logging:1.6.26")

    // Joda Time
    api("joda-time:joda-time:2.9.9")

    // Unit testing
    testImplementation("junit:junit:4.12")
}

setupJar("VeriBlock Core Library", "org.veriblock.core")
val sourcesJar = setupSourcesJar()

artifactory {
    setContextUrl(properties["artifactory_url"])
    publish(closureOf<PublisherConfig> {
        repository(delegateClosureOf<GroovyObject> {
            setProperty("repoKey", properties["artifactory_repoKey"] as String)
            setProperty("username", properties["artifactory_user"])
            setProperty("password", properties["artifactory_password"])
            setProperty("maven", true)
        })

        defaults(delegateClosureOf<GroovyObject> {
            invokeMethod("publications", "mavenJava")
            setProperty("publishArtifacts", true)
        })
    })
}

publish(
    artifactName = "veriblock-core",
    sourcesJar = sourcesJar
)

setupJacoco()
