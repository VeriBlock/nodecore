import com.netflix.gradle.plugins.deb.Deb
import groovy.util.Eval
import org.gradle.api.internal.plugins.WindowsStartScriptGenerator
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.redline_rpm.header.Flags.EQUAL
import org.redline_rpm.header.Flags.GREATER

plugins {
    java
    idea
    application
    id("nebula.ospackage")
}

dependencies {
    implementation(project(":veriblock-core"))
    implementation(project(":nodecore-ucp"))
    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4jVersion")

    testImplementation("junit:junit:4.12")
}

// Exclude logback from everywhere to avoid the slf4j warning
configurations {
    all {
        exclude("ch.qos.logback")
    }
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
        args = Eval.me(properties["appArgs"] as String) as List<String>
    }
}

setupJar("NodeCore Reference Proof-of-Work Miner", "nodecore.miners.pow")
val sourcesJar = setupSourcesJar()

application.applicationName = "veriblock-pow-miner"
application.mainClassName = "nodecore.miners.pow.VeriBlockPoWMiner"

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
    packageDescription = "NodeCore Reference Proof-of-Work Miner"
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

