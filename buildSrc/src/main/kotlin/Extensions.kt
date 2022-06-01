import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.task
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.io.File
import java.util.concurrent.TimeUnit

val Project.sourceSets: SourceSetContainer
    get() = extensions.getByName("sourceSets") as SourceSetContainer

fun Project.setupJar(
    specTitle: String,
    packagePath: String
) {
    tasks.named<Jar>("jar") {
        archiveFileName.set("${project.name}-${prettyVersion()}.jar")
        manifest.attributes.apply {
            set("Name", packagePath.replace('.', '/'))
            set("Specification-Title", specTitle)
            set("Specification-Version", prettyVersion())
            set("Specification-Vendor", "Xenios SEZC")
            set("Implementation-Title", packagePath)
            set("Implementation-Version", prettyVersion())
            set("Implementation-Vendor", "Xenios SEZC")
        }
    }
}

fun Project.setupSourcesJar(): Jar {
    return tasks.create<Jar>("sourcesJar") {
        getArchiveClassifier().set("sources")
        from(sourceSets["main"].allSource)
    }
}

fun Project.publish(artifactName: String, sourcesJar: Jar) {
    extensions.configure<PublishingExtension>("publishing") {
        publications {
            register<MavenPublication>("mavenJava") {
                groupId = "veriblock"
                artifactId = artifactName
                version = prettyVersion()

                from(components["java"])
                artifact(sourcesJar) {
                    classifier = "sources"
                }
            }
        }
    }
}

fun Project.customTests(name: String, testTaskConfig: Test.() -> Unit = {}) {
    // Create source set
    val testSourceSet = sourceSets.create("test-$name") {
        java {
            compileClasspath += sourceSets.getByName("main").output + configurations.named("testRuntimeClasspath").get()
            runtimeClasspath += output + compileClasspath
        }
    }

    // Create new test task
    task<Test>("test${name.split("-").joinToString("") { it.capitalize() }}") {
        description = "Run $name tests"
        group = "verification"
        testClassesDirs = sourceSets["test-$name"].output.classesDirs
        classpath = sourceSets["test-$name"].runtimeClasspath
        outputs.upToDateWhen { false }

        testTaskConfig()
    }

    // Configure IDE
    val idea = extensions.getByName("idea") as? IdeaModel
    idea?.module {
        sourceDirs = sourceDirs - testSourceSet.java.srcDirs
        testSourceDirs = testSourceDirs + testSourceSet.java.srcDirs
        sourceDirs = sourceDirs - testSourceSet.resources.srcDirs
        testSourceDirs = testSourceDirs + testSourceSet.resources.srcDirs
        // Hack for kotlin
        sourceDirs = sourceDirs - file("src/test-$name/kotlin")
        testSourceDirs = testSourceDirs + file("src/test-$name/kotlin")
    }
}

fun Project.setupJacoco() {
    apply(plugin = "jacoco")

    tasks.named<JacocoReport>("jacocoTestReport") {
        additionalSourceDirs.from(files(sourceSets.getByName("main").allJava.srcDirs))
        sourceDirectories.from(files(sourceSets.getByName("main").allSource.srcDirs))
        classDirectories.from(files(sourceSets.getByName("main").output))

        reports {
            xml.isEnabled = true
            html.isEnabled = false
            csv.isEnabled = false
        }
    }
}

fun String.execute(cwd: File): String {
    return try {
        val parts = this.split("\\s".toRegex())
        val proc = ProcessBuilder(*parts.toTypedArray())
            .directory(cwd)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        //println("$ $this")
        proc.waitFor(8, TimeUnit.SECONDS)
        proc.errorStream.use {
            val output = it.bufferedReader().readText()
            if (output.isNotEmpty()) {
                System.err.print(output)
                println("ERRORS:\n$output")
            }
        }
        proc.inputStream.use {
            val output = it.bufferedReader().readText().trim()
            //println(output)
            output
        }
    } catch (ex: java.io.IOException) {
        ex.printStackTrace()
        ex.message ?: "IOException"
    }
}
