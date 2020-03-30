import groovy.util.Eval
import org.gradle.api.internal.plugins.WindowsStartScriptGenerator
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

plugins {
    java
    idea
    application
}

dependencies {
    compile(project(":veriblock-core"))
    compile(project(":nodecore-ucp"))
    compile("ch.qos.logback:logback-classic:1.2.3")

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
        args = Eval.me(properties["appArgs"] as String) as List<String>
    }
}

setupJar("NodeCore Reference Proof-of-Work Miner", "nodecore.miners.pow")
val sourcesJar = setupSourcesJar()

application.applicationName = "nodecore-pow"
application.mainClassName = "nodecore.miners.pow.MainClass"

tasks.distZip {
    archiveFileName.set("${application.applicationName}-${prettyVersion()}.zip")
}
tasks.distTar {
    archiveFileName.set("${application.applicationName}-${prettyVersion()}.tar")
}

tasks.startScripts {
    (windowsStartScriptGenerator as WindowsStartScriptGenerator).template = resources.text.fromFile("windowsStartScript.txt")
}
