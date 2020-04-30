import com.netflix.gradle.plugins.deb.Deb
import org.gradle.api.internal.plugins.WindowsStartScriptGenerator
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.redline_rpm.header.Flags.EQUAL
import org.redline_rpm.header.Flags.GREATER

plugins {
    java
    kotlin("jvm")
    idea
    application
    id("nebula.ospackage")
}

configurations.all {
    // check for updates every build for changing modules
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
}

dependencies {
    implementation(project(":veriblock-core"))
    implementation(project(":nodecore-grpc"))
    implementation(project(":veriblock-shell"))
    implementation(project(":nodecore-spv"))

    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("commons-cli:commons-cli:1.4")
    implementation("commons-io:commons-io:2.5")

    // Dependency Injection
    implementation("org.koin:koin-core:$koinVersion")

    implementation("joda-time:joda-time:2.9.9")
    implementation("org.reflections:reflections:0.9.11")
    implementation("com.diogonunes:JCDP:2.0.3.1")
    implementation("com.google.code.gson:gson:2.8.2")
    implementation("org.jline:jline:3.9.0")
    implementation("org.jline:jline-terminal:3.9.0")
    implementation("org.jline:jline-terminal-jansi:3.9.0")
    implementation("io.netty:netty-tcnative-boringssl-static:2.0.7.Final")
    implementation("com.opencsv:opencsv:4.3.2")

    // Testing
    testImplementation("junit:junit:4.12")
}

tasks.test {
    testLogging {
        exceptionFormat = FULL
    }
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
    standardOutput = System.out

    if (project.hasProperty("appArgs")) {
        args = groovy.util.Eval.me(properties["appArgs"] as String) as List<String>
    }
}

application.applicationName = "nodecore-cli"
application.mainClassName = "nodecore.cli.NodeCoreCLI"

setupJar("NodeCore Command-Line Interface", "nodecore.cli")

tasks.distZip {
    archiveFileName.set("${application.applicationName}-${prettyVersion()}.zip")
}
tasks.distTar {
    archiveFileName.set("${application.applicationName}-${prettyVersion()}.tar")
}

tasks.startScripts {
    (windowsStartScriptGenerator as WindowsStartScriptGenerator).template =
        resources.text.fromFile("windowsStartScript.txt")
}

apply(plugin = "nebula.ospackage")

tasks.register<Deb>("createDeb") {
    dependsOn("installDist")
    packageName = application.applicationName
    packageDescription = "NodeCore Command-Line Interface"
    archStr = "amd64"
    distribution = "bionic"
    url = "https://github.com/VeriBlock/nodecore"
    version = prettyVersion()
    release = "1"

    requires("default-jre", "1.8", GREATER or EQUAL)
        .or("openjdk-8-jre", "8u242-b08", GREATER or EQUAL)

    into("/opt/nodecore/${application.applicationName}")

    from("$buildDir/install/${application.applicationName}/LICENSE") {
        into("/opt/nodecore/${application.applicationName}")
    }
    from("$buildDir/install/${application.applicationName}/lib") {
        into("lib")
    }
    from("$buildDir/install/${application.applicationName}/bin") {
        into("bin")
        exclude("*.bat")
    }
    link("/usr/local/bin/$packageName", "/opt/nodecore/$packageName/bin/$packageName")
}

setupJacoco()
