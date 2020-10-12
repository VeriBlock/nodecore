plugins {
    java
    idea
    `java-library`
    `maven-publish`
    id("com.jfrog.artifactory")
}

dependencies {
    compile(project(":veriblock-core"))

    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("com.google.code.gson:gson:2.8.2")

    testImplementation("junit:junit:4.12")
}

setupJar("VeriBlock NodeCore Universal Client Protocol", "nodecore.api.ucp")
val sourcesJar = setupSourcesJar()

publish(
    artifactName = "nodecore-ucp",
    sourcesJar = sourcesJar
)

setupJacoco()
