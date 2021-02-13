import org.gradle.api.internal.plugins.WindowsStartScriptGenerator

plugins {
    java
    kotlin("jvm")
    idea
    application
}

dependencies {
    implementation(project(":veriblock-core"))

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Logging
    implementation("io.github.microutils:kotlin-logging:1.6.26")
    implementation("org.apache.logging.log4j:log4j-api:2.13.3")
    implementation("org.apache.logging.log4j:log4j-core:2.13.3")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.13.3")

    // Other
    implementation("me.tongfei:progressbar:0.8.1")
    implementation("commons-codec:commons-codec:1.9")
    implementation("commons-cli:commons-cli:1.4")
    implementation("org.fusesource.jansi:jansi:1.18")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    // HTTP Client
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-gson:$ktorVersion")
}

// Exclude logback from everywhere to avoid the slf4j warning
configurations {
    all {
        exclude("ch.qos.logback")
    }
}

application.applicationName = "bootstrap-downloader"
application.mainClassName = "bootstrap.downloader.DownloaderKt"

tasks.startScripts {
    defaultJvmOpts = listOf("-Xms3g", "-Xmx6g")
}

setupJar("Bootstrap Downloader", "bootstrap.downloader")

tasks.startScripts {
    (windowsStartScriptGenerator as WindowsStartScriptGenerator).template =
        resources.text.fromFile("windowsStartScript.txt")
}
