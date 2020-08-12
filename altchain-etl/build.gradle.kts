plugins {
    kotlin("jvm")
    java
    application
    kotlin("plugin.serialization") version kotlinVersion
}

application.mainClassName = "org.veriblock.altchainetl.MainKt"

setupJar("Altchain ETL", "org.veriblock.altchainetl")

dependencies {
    implementation(project(":veriblock-core"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$kotlinxSerializationVersion")

    implementation("org.koin:koin-core:$koinVersion")

    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")
    implementation("io.ktor:ktor-locations:$ktorVersion")
    implementation("io.ktor:ktor-metrics-micrometer:$ktorVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:1.5.3")

    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    implementation("com.zaxxer:HikariCP:3.4.5")
    runtimeOnly("org.postgresql:postgresql:42.2.14")
    runtimeOnly("org.xerial:sqlite-jdbc:3.32.3.2")

    implementation("io.github.microutils:kotlin-logging:1.8.3")
    implementation("ch.qos.logback:logback-classic:1.2.3")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.koin:koin-test:$koinVersion")
    testImplementation("io.mockk:mockk:1.10.0")
}
