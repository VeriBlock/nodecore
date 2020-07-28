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
    implementation("ch.qos.logback:logback-classic:1.2.3")

    // Other
    implementation("me.tongfei:progressbar:0.8.1")
    implementation("commons-codec:commons-codec:1.9")
    implementation("commons-cli:commons-cli:1.4")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    // HTTP Client
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-gson:$ktorVersion")
}

application.applicationName = "bootstrap-downloader"
application.mainClassName = "bootstrap.downloader.DownloaderKt"

setupJar("Bootstrap Downloader", "bootstrap.downloader")
